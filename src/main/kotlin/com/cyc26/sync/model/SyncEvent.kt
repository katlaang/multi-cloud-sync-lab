package com.cyc26.sync.model

import java.time.Instant
import java.util.UUID

data class SyncEvent(
    val eventId: UUID,
    val entityId: String,
    val version: Long,
    val source: CloudProvider,
    val timestamp: Instant,
    val operation: Operation,
    val payload: FlightPayload
)
