package com.google.wear.watchface.validator.specification.common

import com.google.wear.watchface.validator.constraint.constraint

internal val PRE_DEFINED_TARGETS =
    setOf(
        "ALARM",
        "BATTERY_STATUS",
        "CALENDAR",
        "HEALTH_HEART_RATE",
        "MESSAGE",
        "MUSIC_PLAYER",
        "PHONE",
        "SETTINGS",
    )

/**
 * Specification constraint for the `Launch` element. Attribute 'target' can be set to a developer
 * customized name and hence, no validation is appropriate
 */
fun launch() =
    constraint("Launch") {
        allVersions()
            .require(
                /* Attributes */
                attribute("target", enum(PRE_DEFINED_TARGETS) or deepLink())
            )
    }
