package com.originflow.system.ui.workspace

data class WorkspaceItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sourcePackage: String?,
    val originalText: String,
    val processedResult: String,
    val actionType: String,
    val timestamp: Long = System.currentTimeMillis()
)
