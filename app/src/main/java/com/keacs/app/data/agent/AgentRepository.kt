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

        val request = AgentChatRequest(
            clientRequestId = java.util.UUID.randomUUID().toString(),
            deviceIdHash = settings.deviceId,
            message = message.trim(),
            localContext = localContext,
            conversationHistory = conversationHistory.trimForRequest(),
            appVersion = com.keacs.app.BuildConfig.VERSION_NAME,
        )
        if (message.isHighRiskAdvice()) {
            return AgentCallResult.Success(boundaryResponse().copy(clientRequestId = request.clientRequestId))
        }
        return when (val result = client.chat(settings, request)) {
            is AgentCallResult.Success -> AgentCallResult.Success(
                response = result.response.toUserFacingResponse().copy(
                    clientRequestId = request.clientRequestId,
                    replySource = AgentReplySource.MODEL,
                ),
            )
            is AgentCallResult.Timeout -> AgentCallResult.Success(
                response = timeoutFallback(message, localContext)
                    .copy(clientRequestId = request.clientRequestId),
            )
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
        reason: String = "",
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
                reason = reason,
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
    val replySource: AgentReplySource = AgentReplySource.MODEL,
    val needsMoreContext: Boolean = false,
    val contextRequests: List<AgentContextRequest> = emptyList(),
    val actions: List<AgentActionPreview> = emptyList(),
    val warnings: List<String> = emptyList(),
)

enum class AgentReplySource {
    MODEL,
    AUTO,
}

data class AgentFeedbackRequest(
    val clientRequestId: String,
    val deviceIdHash: String,
    val result: String,
    val actionTypes: List<String>,
    val errorType: String = "",
    val reason: String = "",
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
    data class Timeout(val message: String) : AgentCallResult
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
        replySource = AgentReplySource.AUTO,
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
    buildLocalOperationPreview(message, localContext, warning)
        ?: buildLocalScheduledPreview(message, localContext, warning)
        ?: buildLocalRecordPreview(message, localContext, warning)
        ?: buildLocalStatsReply(message, localContext, warning)

private fun timeoutFallback(
    message: String,
    localContext: AgentLocalContext,
): AgentChatResponse =
    localFallback(message, localContext, null)
        ?.copy(replySource = AgentReplySource.AUTO)
        ?: AgentChatResponse(
            reply = "**自动回复**\n\n大模型 60 秒内没有返回回复，你可以稍后再试，或换个说法继续。",
            replySource = AgentReplySource.AUTO,
        )

private fun buildLocalOperationPreview(
    message: String,
    localContext: AgentLocalContext,
    warning: String?,
): AgentChatResponse? {
    if (message.containsAny("删", "删除", "去掉")) {
        val records = localContext.records
        if (records.isEmpty()) {
            return askUserResponse("没有找到可删除的账目", "请补充日期、金额、分类或账户后再试。", warning)
        }
        if (records.size > 1) {
            return askUserResponse("匹配到多笔账目", "请先说明要删除哪一笔。", warning)
        }
        return AgentChatResponse(
            reply = "**请确认删除**\n\n删除前请核对明细。",
            actions = listOf(
                AgentActionPreview(
                    type = "delete_record",
                    title = "删除账目",
                    description = "删除 1 笔账目。",
                    impactCount = 1,
                    records = records,
                    riskNotice = "删除后无法在助手里直接恢复。",
                ),
            ),
            warnings = listOfNotNull(warning),
        )
    }
    if (message.containsAny("改", "修改", "调成", "改成")) {
        val records = localContext.records
        if (records.isEmpty()) {
            return askUserResponse("没有找到可修改的账目", "请补充日期、金额、分类或账户后再试。", warning)
        }
        if (records.size > 1) {
            return askUserResponse("匹配到多笔账目", "请先说明要修改哪一笔。", warning)
        }
        val updated = localContext.updatedRecord(records.single(), message)
            ?: return askUserResponse("还缺少修改内容", "请说明要改金额、分类、账户、日期还是备注。", warning)
        return AgentChatResponse(
            reply = "**请确认修改**\n\n确认后才会更新本机账本。",
            actions = listOf(
                AgentActionPreview(
                    type = "update_record",
                    title = "修改账目",
                    description = "修改 1 笔账目。",
                    impactCount = 1,
                    records = listOf(updated),
                    riskNotice = "请核对修改后的金额、日期、分类和账户。",
                ),
            ),
            warnings = listOfNotNull(warning),
        )
    }
    return null
}

private fun buildLocalScheduledPreview(
    message: String,
    localContext: AgentLocalContext,
    warning: String?,
): AgentChatResponse? {
    if (!message.containsAny("定时", "每周", "每月", "每年", "周期", "房租")) return null
    if (message.containsAny("停用", "关闭", "取消")) {
        val schedules = localContext.scheduledRecords
        if (schedules.isEmpty()) {
            return askUserResponse("没有找到定时记账", "请补充定时记账名称、金额或周期后再试。", warning)
        }
        if (schedules.size > 1) {
            return askUserResponse("匹配到多条定时记账", "请说明要停用哪一条。", warning)
        }
        return AgentChatResponse(
            reply = "**请确认停用**\n\n确认后这条定时记账不再自动生成。",
            actions = listOf(
                AgentActionPreview(
                    type = "disable_scheduled_record",
                    title = "停用定时记账",
                    description = "停用 1 条定时记账。",
                    impactCount = 1,
                    scheduledRecords = schedules,
                    riskNotice = "停用后可在定时记账页重新开启。",
                ),
            ),
            warnings = listOfNotNull(warning),
        )
    }
    val amountCent = message.extractAmountCent()
        ?: return askUserResponse("还缺少金额", "请补充定时记账的金额。", warning)
    val frequency = message.resolveFrequency()
        ?: return askUserResponse("还缺少定时规则", "请补充周期和生成日期，例如“每月 1 号房租 2500”。", warning)
    val type = if (message.looksLikeIncome()) RecordTypeIncome else RecordTypeExpense
    val categoryName = localContext.resolveCategoryName(type, message)
    val accountName = localContext.resolveAccountName(message)
    val schedule = mutableMapOf<String, Any?>(
        "type" to type,
        "amountCent" to amountCent,
        "frequency" to frequency,
        "nextRunAt" to message.resolveNextRunAt(frequency),
        "categoryName" to categoryName,
        "note" to message.take(40),
        "isEnabled" to true,
    )
    if (type == RecordTypeIncome) {
        schedule["toAccountName"] = accountName
    } else {
        schedule["fromAccountName"] = accountName
    }
    return AgentChatResponse(
        reply = "**请确认这条定时记账**\n\n确认后会按周期自动生成账目。",
        actions = listOf(
            AgentActionPreview(
                type = "create_scheduled_record",
                title = "新增定时记账",
                description = "新增 1 条定时记账。",
                impactCount = 1,
                scheduledRecords = listOf(schedule),
                riskNotice = "请核对金额、周期、分类、账户和下次生成时间。",
            ),
        ),
        warnings = listOfNotNull(warning),
    )
}

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
    val accountName = localContext.resolveAccountName(message)
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
    Regex("""(\d+(?:\.\d{1,2})?)\s*(元|块)?""").findAll(this).forEach { match ->
        if (getOrNull(match.range.last + 1) == '号') {
            return@forEach
        }
        val amountCent = runCatching {
            BigDecimal(match.groupValues[1])
                .multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
                .takeIf { it > 0 }
        }.getOrNull()
        if (amountCent != null) {
            return amountCent
        }
    }
    return null
}

private fun String.extractUpdateAmountCent(): Long? {
    val match = Regex("""(?:改成|调成|改为|变成|改到)\s*(\d+(?:\.\d{1,2})?)\s*(元|块)?""").find(this)
    if (match != null) {
        return runCatching {
            BigDecimal(match.groupValues[1])
                .multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
                .takeIf { it > 0 }
        }.getOrNull()
    }
    return extractAmountCent()
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

private fun String.resolveFrequency(): String? =
    when {
        containsAny("每周", "周一", "周二", "周三", "周四", "周五", "周六", "周日", "星期") -> "WEEKLY"
        containsAny("每年", "每一年") -> "YEARLY"
        containsAny("每月", "每个月", "月初", "月末", "房租") -> "MONTHLY"
        else -> null
    }

private fun String.resolveNextRunAt(frequency: String): Long {
    val calendar = Calendar.getInstance(Locale.getDefault())
    when (frequency) {
        "WEEKLY" -> {
            val target = resolveWeekday()
            val current = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
            var delta = (target - current + 7) % 7
            if (delta == 0) delta = 7
            calendar.add(Calendar.DAY_OF_YEAR, delta)
        }
        "YEARLY" -> calendar.add(Calendar.YEAR, 1)
        else -> {
            val day = resolveMonthDay()
            if (calendar.get(Calendar.DAY_OF_MONTH) >= day) {
                calendar.add(Calendar.MONTH, 1)
            }
            calendar.set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 28))
        }
    }
    calendar.set(Calendar.HOUR_OF_DAY, 9)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun String.resolveMonthDay(): Int =
    Regex("""(\d{1,2})\s*号""").find(this)?.groupValues?.getOrNull(1)?.toIntOrNull()
        ?: if (contains("月末")) 28 else 1

private fun String.resolveWeekday(): Int {
    val mapping = listOf("一" to 0, "二" to 1, "三" to 2, "四" to 3, "五" to 4, "六" to 5, "日" to 6, "天" to 6)
    return mapping.firstOrNull { (label, _) -> contains("周$label") || contains("星期$label") }?.second ?: 0
}

private fun AgentLocalContext.updatedRecord(record: Map<String, Any?>, message: String): Map<String, Any?>? {
    val updates = mutableMapOf<String, Any?>()
    message.extractUpdateAmountCent()?.let { updates["amountCent"] = it }
    val type = record["type"]?.toString().orEmpty().ifBlank { RecordTypeExpense }
    resolveTargetCategoryName(type, message)?.let { updates["categoryName"] = it }
    resolveTargetAccountName(message)?.let { accountName ->
        if (type == RecordTypeIncome) {
            updates["toAccountName"] = accountName
        } else {
            updates["fromAccountName"] = accountName
        }
    }
    if (message.containsAny("今天", "昨天", "昨日")) {
        val occurredAt = message.resolveOccurredAt()
        updates["occurredAt"] = occurredAt
        updates["date"] = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(occurredAt)
    }
    return if (updates.isEmpty()) null else record + updates
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
    if (type == RecordTypeExpense && listOf("打车", "地铁", "公交", "车费").any { message.contains(it) }) {
        names.firstOrNull { it == "交通" }?.let { return it }
    }
    if (type == RecordTypeExpense && listOf("房租", "租房", "物业").any { message.contains(it) }) {
        names.firstOrNull { it == "住房" }?.let { return it }
    }
    if (type == RecordTypeIncome && message.contains("工资")) {
        names.firstOrNull { it == "工资" }?.let { return it }
    }
    return names.firstOrNull { it == "其他" } ?: names.firstOrNull().orEmpty()
}

private fun AgentLocalContext.resolveAccountName(message: String): String? {
    accounts.firstOrNull { account ->
        account["name"]?.toString()?.takeIf { message.contains(it) } != null
    }?.get("name")?.toString()?.let { return it }
    accounts.firstOrNull { it["isDefaultRecordAccount"] == true }
        ?.get("name")
        ?.toString()
        ?.let { return it }
    return stats["defaultRecordAccountName"]?.toString()?.takeIf { it.isNotBlank() }
}

private fun AgentLocalContext.resolveTargetCategoryName(type: String, message: String): String? {
    val direction = if (type == RecordTypeIncome) PresetSeedData.CATEGORY_INCOME else PresetSeedData.CATEGORY_EXPENSE
    val tail = message.textAfterUpdateKeyword()
    return categories
        .filter { it["direction"] == direction }
        .mapNotNull { it["name"]?.toString() }
        .firstOrNull { tail.contains(it) }
}

private fun AgentLocalContext.resolveTargetAccountName(message: String): String? {
    val tail = message.textAfterUpdateKeyword()
    return accounts
        .mapNotNull { it["name"]?.toString() }
        .firstOrNull { tail.contains(it) }
}

private fun String.textAfterUpdateKeyword(): String {
    listOf("改成", "调成", "改为", "变成", "改到").forEach { keyword ->
        if (contains(keyword)) return substringAfter(keyword)
    }
    return this
}

private fun String.containsAny(vararg terms: String): Boolean =
    terms.any { contains(it) }

private fun askUserResponse(title: String, description: String, warning: String?): AgentChatResponse =
    AgentChatResponse(
        reply = "**$title**\n\n$description",
        warnings = listOfNotNull(warning),
    )

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
