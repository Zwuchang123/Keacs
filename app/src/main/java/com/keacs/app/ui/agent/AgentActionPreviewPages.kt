package com.keacs.app.ui.agent

import com.keacs.app.data.agent.AgentActionPreview

internal enum class AgentPreviewItemType {
    RECORD,
    SCHEDULE,
}

internal data class AgentPreviewItem(
    val type: AgentPreviewItemType,
    val index: Int,
    val pageLabel: String,
    val data: Map<String, Any?>,
)

internal fun AgentActionPreview.previewItems(): List<AgentPreviewItem> {
    val recordItems = records.mapIndexed { index, item ->
        AgentPreviewItem(
            type = AgentPreviewItemType.RECORD,
            index = index,
            pageLabel = "账目 ${index + 1}/${records.size}",
            data = item,
        )
    }
    val scheduleItems = scheduledRecords.mapIndexed { index, item ->
        AgentPreviewItem(
            type = AgentPreviewItemType.SCHEDULE,
            index = index,
            pageLabel = "定时 ${index + 1}/${scheduledRecords.size}",
            data = item,
        )
    }
    return recordItems + scheduleItems
}
