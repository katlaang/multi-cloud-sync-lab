package com.cyc26.sync.model

import java.time.Instant

data class FlightRecord(
    val entityId: String,
    val flightNumber: String,
    val status: String,
    val gate: String,
    val version: Long,
    val lastUpdated: Instant,
    val source: CloudProvider
)
