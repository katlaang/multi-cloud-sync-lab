package com.cyc26.sync.model

data class DemoState(
    val clouds: List<CloudState>,
    val audit: List<AuditEntry>
)
