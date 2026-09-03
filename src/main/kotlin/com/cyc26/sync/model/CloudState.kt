package com.cyc26.sync.model

data class CloudState(
    val provider: CloudProvider,
    val online: Boolean,
    val current: FlightRecord?,
    val pendingEvents: Int
)
