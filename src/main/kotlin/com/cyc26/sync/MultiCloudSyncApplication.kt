package com.cyc26.sync

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MultiCloudSyncApplication

fun main(args: Array<String>) {
    runApplication<MultiCloudSyncApplication>(*args)
}
