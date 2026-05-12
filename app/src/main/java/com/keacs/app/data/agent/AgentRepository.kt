package com.keacs.app.data.agent

import com.keacs.app.data.local.PreferencesManager
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.domain.agent.AgentModelServiceMode
import com.keacs.app.domain.agent.AgentSettings
import com.keacs.app.domain.agent.validateForRequest
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AgentRepository(
    private val settingsProvider: suspend () -> AgentSettings,
    private val client: AgentNetworkClient = HttpUrlConnectionAgentClient(),
    private val suggestionProvider: AgentSuggestionProvider = AgentSuggestionProvider(),
) {
    constructor(
        preferencesManager: PreferencesManager,
        client: AgentNetworkClient = HttpUrlConnectionAgentClient(),
    ) : this(
        settingsProvider = { preferencesManager.agentSettings.first() },
        client = client,
    )

    suspend fun sendMessage(
        message: String,
        localContext: AgentLocalContext = AgentLocalContext(),
        conversationHistory: List<AgentConversationTurn> = emptyList(),
    ): AgentCallResult {
        val settings = settingsProvider()
        val validation = settings.validateForRequest()
        if (!validation.canRequest) {
            return AgentCallResult.ConfigurationRequired(validation.message.orEmpty())
        }
        if (message.isBlank()) {
            return AgentCallResult.ConfigurationRequired("请输入要发送的内容。")
        }
        if (message.isHighRiskAdvice()) {
            return AgentCallResult.Success(boundaryResponse())
        }

        val request = AgentChatRequest(
            clientRequestId = java.util.UUID.randomUUID().toString(),
            deviceIdHash = settings.deviceId,
            message = message.trim(),
            localContext = localContext,
            conversationHistory = conversationHistory.trimForRequest(),
            appVersion = com.keacs.app.BuildConfig.VERSION_NAME,
        )
        return when (val result = client.chat(settings, request)) {
            is AgentCallResult.Success -> AgentCallResult.Success(
                response = result.response.toUserFacingResponse().copy(clientRequestId = request.clientRequestId),
            )
            is AgentCallResult.NetworkFailure -> localFallback(message, localContext, null)
                ?.let { AgentCallResult.Success(it.copy(clientRequestId = request.clientRequestId)) }
                ?: AgentCallResult.NetworkFailure("服务连接失败。已保留输入内容，可以稍后重试或检查配置。")
            is AgentCallResult.InvalidResponse -> localFallback(message, localContext, null)
                ?.let { AgentCallResult.Success(it.copy(clientRequestId = request.clientRequestId)) }
                ?: AgentCallResult.InvalidResponse("没有返回清晰内容。已保留输入内容，可以换个说法再试。")
            else -> result
        }
    }

    suspend fun loadSuggestions(
        today: String,
        recentHistory: List<AgentConversationTurn>,
        localSummary: Map<String, Any?>,
        limit: Int = 4,
    ): List<AgentSuggestion> {
        val local = suggestionProvider.buildLocalSuggestions(today, recentHistory.map { it.content }, localSummary, limit)
        val settings = settingsProvider()
        if (!settings.validateForRequest().canRequest || settings.serviceMode != AgentModelServiceMode.OFFICIAL) {
            return local
        }
        val remote = runCatching {
            client.suggestions(
                settings = settings,
                request = AgentSuggestionRequest(
                    deviceIdHash = settings.deviceId,
                    today = today,
                    recentConversation = recentHistory.trimForRequest(),
                    localSummary = localSummary,
                    limit = limit,
                ),
            )
        }.getOrDefault(emptyList())
            .filter { it.text.isNotBlank() }
            .distinctBy { it.text }
            .take(limit.coerceIn(2, 4))
        return remote.ifEmpty { local }
    }

    suspend fun sendFeedback(
        clientRequestId: String,
        result: String,
        actionTypes: List<String>,
        errorType: String = "",
    ): Boolean {
        if (clientRequestId.isBlank()) return false
        val settings = settingsProvider()
        if (settings.serviceMode != AgentModelServiceMode.OFFICIAL) return true
        if (settings.deviceId.isBlank()) return false
        return client.feedback(
            settings = settings,
            request = AgentFeedbackRequest(
                clientRequestId = clientRequestId,
                deviceIdHash = settings.deviceId,
                result = result,
                actionTypes = actionTypes,
                errorType = errorType,
            ),
        )
    }
}

interface AgentNetworkClient {
    suspend fun chat(settings: AgentSettings, request: AgentChatRequest): AgentCallResult
    suspend fun feedback(settings: AgentSettings, request: AgentFeedbackRequest): Boolean = true
    suspend fun suggestions(settings: AgentSettings, request: AgentSuggestionRequest): List<AgentSuggestion> = emptyList()
}

data class AgentChatRequest(
    val clientRequestId: String,
    val deviceIdHash: String,
    val message: String,
    val localContext: AgentLocalContext,
    val conversationHistory: List<AgentConversationTurn> = emptyList(),
    val timezone: String = "Asia/Shanghai",
    val appVersion: String,
)

data class AgentConversationTurn(
    val role: String,
    val content: String,
)

data class AgentLocalContext(
    val timeContext: Map<String, String> = emptyMap(),
    val categories: List<Map<String, Any?>> = emptyList(),
    val accounts: List<Map<String, Any?>> = emptyList(),
    val records: List<Map<String, Any?>> = emptyList(),
    val stats: Map<String, Any?> = emptyMap(),
    val scheduledRecords: List<Map<String, Any?>> = emptyList(),
)

data class AgentChatResponse(
    val clientRequestId: String = "",
    val reply: String,
    val needsMoreContext: Boolean = false,
    val contextRequests: List<AgentContextRequest> = emptyList(),
    val actions: List<AgentActionPreview> = emptyList(),
    val warnings: List<String> = emptyList(),
)

data class AgentFeedbackRequest(
    val clientRequestId: String,
    val deviceIdHash: String,
    val result: String,
    val actionTypes: List<String>,
    val errorType: String = "",
)

data class AgentSuggestionRequest(
    val deviceIdHash: String,
    val today: String,
    val recentConversation: List<AgentConversationTurn> = emptyList(),
    val localSummary: Map<String, Any?> = emptyMap(),
    val limit: Int = 4,
)

data class AgentContextRequest(
    val type: String,
    val reason: String = "",
)

data class AgentActionPreview(
    val actionId: String = "",
    val type: String,
    val title: String,
    val description: String = "",
    val impactCount: Int = 0,
    val records: List<Map<String, Any?>> = emptyList(),
    val scheduledRecords: List<Map<String, Any?>> = emptyList(),
    val riskNotice: String = "",
)

sealed interface AgentCallResult {
    data class Success(val response: AgentChatResponse) : AgentCallResult
    data class ConfigurationRequired(val message: String) : AgentCallResult
    data class NetworkFailure(val message: String) : AgentCallResult
    data class InvalidResponse(val message: String) : AgentCallResult
}

fun AgentActionPreview.requiresConfirmation(): Boolean =
    type in setOf(
        "create_record",
        "update_record",
        "delete_record",
        "batch_update_records",
        "create_scheduled_record",
        "update_scheduled_record",
        "disable_scheduled_record",
    )

internal fun AgentSettings.chatUrl(): String {
    val baseUrl = endpointBaseUrl
    return when (serviceMode) {
        AgentModelServiceMode.OFFICIAL -> "$baseUrl/api/agent/runs/stream"
        AgentModelServiceMode.CUSTOM -> "$baseUrl/chat/completions"
    }
}

internal fun AgentSettings.feedbackUrl(): String =
    "${endpointBaseUrl}/api/agent/feedback"

internal fun AgentSettings.suggestionsUrl(): String =
    "${endpointBaseUrl}/api/agent/suggestions"

private fun String.isHighRiskAdvice(): Boolean {
    val highRiskTerms = listOf("股票", "基金", "理财", "投资", "贷款", "借钱", "借贷", "保险", "收益", "税", "法律", "医疗")
    val decisionTerms = listOf("建议", "推荐", "该不该", "能不能买", "买什么", "投什么", "收益率", "预测", "划算吗")
    return highRiskTerms.any { contains(it) } && decisionTerms.any { contains(it) }
}

private fun boundaryResponse(): AgentChatResponse =
    AgentChatResponse(
        reply = "**不能处理这类建议**\n\n我可以帮你看账本记录、支出结构和消费习惯。",
        actions = listOf(
            AgentActionPreview(
                type = "answer_only",
                title = "边界提示",
                description = "仅说明助手能力边界，不执行任何写入。",
            ),
        ),
    )

private fun AgentChatResponse.toUserFacingResponse(): AgentChatResponse =
    copy(
        reply = reply.toUserFacingText(),
        warnings = warnings.map { it.toUserFacingText() }.filter { it.isNotBlank() },
    )

private fun String.toUserFacingText(): String {
    val blockedTerms = listOf("JSON", "json", "提示词", "系统提示", "工具调用", "后端", "接口", "开发", "代码", "schema")
    val cleanedLines = lineSequence()
        .map { it.trimEnd() }
        .filterNot { line -> blockedTerms.any { term -> line.contains(term) } }
        .filterNot { it.startsWith("```") }
        .toList()
    val paragraphs = cleanedLines.joinToString("\n")
        .split(Regex("""\n{3,}"""))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(MAX_REPLY_PARAGRAPHS)
    return paragraphs.joinToString("\n\n").ifBlank { "我没整理出清晰结果，请换个说法再试。" }
}

private fun List<AgentConversationTurn>.trimForRequest(): List<AgentConversationTurn> {
    val recent = takeLast(MAX_HISTORY_TURNS)
    var total = 0
    val result = ArrayDeque<AgentConversationTurn>()
    for (turn in recent.asReversed()) {
        val text = turn.content.take(MAX_TURN_LENGTH)
        if (text.isBlank()) continue
        if (total + text.length > MAX_HISTORY_CHARS) break
        total += text.length
        result.addFirst(AgentConversationTurn(role = turn.role, content = text))
    }
    return result.toList()
}

private fun localFallback(
    message: String,
    localContext: AgentLocalContext,
    warning: String?,
): AgentChatResponse? =
    buildLocalRecordPreview(message, localContext, warning)
        ?: buildLocalStatsReply(message, localContext, warning)

private fun buildLocalRecordPreview(
    message: String,
    localContext: AgentLocalContext,
    warning: String?,
): AgentChatResponse? {
    if (!message.looksLikeRecordCreation()) return null
    if (message.contains("转")) return null
    val amountCent = message.extractAmountCent() ?: return null
    val type = if (message.looksLikeIncome()) RecordTypeIncome else RecordTypeExpense
    val categoryName = localContext.resolveCategoryName(type, message)
    val accountName = localContext.accounts
        .firstOrNull { account -> account["name"]?.toString()?.takeIf { message.contains(it) } != null }
        ?.get("name")
        ?.toString()
    val occurredAt = message.resolveOccurredAt()
    val record = mutableMapOf<String, Any?>(
        "type" to type,
        "amountCent" to amountCent,
        "occurredAt" to occurredAt,
        "date" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(occurredAt),
        "categoryName" to categoryName,
        "note" to message.take(40),
    )
    if (type == RecordTypeIncome) {
        record["toAccountName"] = accountName
    } else {
        record["fromAccountName"] = accountName
    }
    return AgentChatResponse(
        reply = "**请确认这笔账目**\n\n确认后才会保存到本机账本。",
        actions = listOf(
            AgentActionPreview(
                type = "create_record",
                title = "新增账目",
                description = "确认后新增 1 笔${if (type == RecordTypeIncome) "收入" else "支出"}账目。",
                impactCount = 1,
                records = listOf(record),
                riskNotice = "请核对金额、日期、分类和账户。",
            ),
        ),
        warnings = listOfNotNull(warning),
    )
}

private fun buildLocalStatsReply(
    message: String,
    localContext: AgentLocalContext,
    warning: String?,
): AgentChatResponse? {
    if (!message.looksLikeStatsQuestion()) return null
    val stats = localContext.stats
    val rangeLabel = stats["rangeLabel"]?.toString()?.ifBlank { null } ?: "本次范围"
    val income = stats.longValue("incomeCent") ?: 0L
    val expense = stats.longValue("expenseCent") ?: 0L
    val balance = stats.longValue("balanceCent") ?: (income - expense)
    return AgentChatResponse(
        reply = "**$rangeLabel 账本摘要**\n\n收入：${income.formatMoney()}\n支出：${expense.formatMoney()}\n结余：${balance.formatMoney()}",
        warnings = listOfNotNull(warning),
    )
}

private fun String.looksLikeRecordCreation(): Boolean =
    extractAmountCent() != null &&
        listOf("记", "花", "买", "饭", "工资", "收入", "支出", "转入", "报销").any { contains(it) }

private fun String.looksLikeIncome(): Boolean =
    listOf("收入", "工资", "奖金", "报销", "兼职", "到账").any { contains(it) }

private fun String.looksLikeStatsQuestion(): Boolean =
    listOf("多少", "花了", "支出", "收入", "结余", "总结", "复盘", "本月", "这个月", "最近").any { contains(it) }

private fun String.extractAmountCent(): Long? {
    val match = Regex("""(\d+(?:\.\d{1,2})?)\s*(元|块)?""").find(this) ?: return null
    return runCatching {
        BigDecimal(match.groupValues[1])
            .multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
            .takeIf { it > 0 }
    }.getOrNull()
}

private fun String.resolveOccurredAt(): Long {
    val calendar = Calendar.getInstance(Locale.getDefault())
    if (contains("昨天") || contains("昨日")) {
        calendar.add(Calendar.DAY_OF_YEAR, -1)
    }
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun AgentLocalContext.resolveCategoryName(type: String, message: String): String {
    val direction = if (type == RecordTypeIncome) PresetSeedData.CATEGORY_INCOME else PresetSeedData.CATEGORY_EXPENSE
    val names = categories
        .filter { it["direction"] == direction }
        .mapNotNull { it["name"]?.toString() }
    names.firstOrNull { message.contains(it) }?.let { return it }
    if (type == RecordTypeExpense && listOf("饭", "餐", "咖啡", "早餐", "午饭", "晚饭").any { message.contains(it) }) {
        names.firstOrNull { it == "餐饮" }?.let { return it }
    }
    if (type == RecordTypeIncome && message.contains("工资")) {
        names.firstOrNull { it == "工资" }?.let { return it }
    }
    return names.firstOrNull { it == "其他" } ?: names.firstOrNull().orEmpty()
}

private fun Map<String, Any?>.longValue(key: String): Long? =
    when (val value = this[key]) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }

private fun Long.formatMoney(): String =
    "¥" + moneyFormat.format(this / 100.0)

private val moneyFormat = DecimalFormat("#,##0.##")

private const val RecordTypeExpense = "EXPENSE"
private const val RecordTypeIncome = "INCOME"
private const val MAX_HISTORY_TURNS = 24
private const val MAX_HISTORY_CHARS = 8_000
private const val MAX_TURN_LENGTH = 600
private const val MAX_REPLY_PARAGRAPHS = 4
