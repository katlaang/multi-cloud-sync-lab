package com.cyc26.sync.model

data class FlightPayload(
    val flightNumber: String,
    val status: String,
    val gate: String
)
