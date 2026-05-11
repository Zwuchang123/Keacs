package com.keacs.app.domain.agent

enum class AgentModelServiceMode(val storageValue: String) {
    OFFICIAL("official"),
    CUSTOM("custom");

    companion object {
        fun fromStorageValue(value: String?): AgentModelServiceMode =
            entries.firstOrNull { it.storageValue == value } ?: OFFICIAL
    }
}

data class AgentSettings(
    val enabled: Boolean = false,
    val serviceMode: AgentModelServiceMode = AgentModelServiceMode.OFFICIAL,
    val officialServiceUrl: String = "",
    val customBaseUrl: String = "",
    val customApiKey: String = "",
    val customModelName: String = "",
    val deviceId: String = "",
    val dataScope: String = "minimal",
) {
    val endpointBaseUrl: String
        get() = when (serviceMode) {
            AgentModelServiceMode.OFFICIAL -> officialServiceUrl
            AgentModelServiceMode.CUSTOM -> customBaseUrl
        }.trimEnd('/')
}

data class AgentSettingsValidation(
    val canRequest: Boolean,
    val message: String?,
)

fun AgentSettings.validateForRequest(): AgentSettingsValidation {
    if (!enabled) {
        return AgentSettingsValidation(
            canRequest = false,
            message = "在线助手未启用，请先到设置页开启。",
        )
    }
    return when (serviceMode) {
        AgentModelServiceMode.OFFICIAL -> validateOfficial()
        AgentModelServiceMode.CUSTOM -> validateCustom()
    }
}

private fun AgentSettings.validateOfficial(): AgentSettingsValidation {
    if (officialServiceUrl.isBlank()) {
        return AgentSettingsValidation(
            canRequest = false,
            message = "官方服务地址未配置，暂时无法使用在线助手。",
        )
    }
    if (!officialServiceUrl.isHttpsUrl()) {
        return AgentSettingsValidation(
            canRequest = false,
            message = "官方服务需要使用 HTTPS 地址。",
        )
    }
    return AgentSettingsValidation(canRequest = true, message = null)
}

private fun AgentSettings.validateCustom(): AgentSettingsValidation {
    if (customBaseUrl.isBlank()) {
        return AgentSettingsValidation(
            canRequest = false,
            message = "请先填写自定义模型访问地址。",
        )
    }
    if (!customBaseUrl.isHttpsUrl() && !customBaseUrl.isLocalHttpUrl()) {
        return AgentSettingsValidation(
            canRequest = false,
            message = "自定义模型访问地址需要使用 HTTPS，本机调试地址除外。",
        )
    }
    if (customApiKey.isBlank()) {
        return AgentSettingsValidation(
            canRequest = false,
            message = "请先填写自定义模型 API Key。",
        )
    }
    return AgentSettingsValidation(canRequest = true, message = null)
}

private fun String.isHttpsUrl(): Boolean =
    trim().startsWith("https://", ignoreCase = true)

private fun String.isLocalHttpUrl(): Boolean {
    val value = trim().lowercase()
    return value.startsWith("http://10.0.2.2") ||
        value.startsWith("http://localhost") ||
        value.startsWith("http://127.0.0.1")
}
