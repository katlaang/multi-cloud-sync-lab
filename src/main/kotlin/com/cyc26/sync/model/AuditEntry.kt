package com.cyc26.sync.model

import java.time.Instant

data class AuditEntry(
    val timestamp: Instant = Instant.now(),
    val type: String,
    val provider: CloudProvider? = null,
    val message: String
)
