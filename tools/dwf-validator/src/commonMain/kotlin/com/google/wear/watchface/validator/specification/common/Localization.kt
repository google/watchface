package com.google.wear.watchface.validator.specification.common

import com.google.wear.watchface.validator.constraint.constraint

internal val CALENDAR_OPTIONS =
    setOf(
        "BUDDHIST",
        "CHINESE",
        "COPTIC",
        "DANGI",
        "ETHIOPIC",
        "ETHIOPIC_AMETE_ALEM",
        "GREGORIAN",
        "HEBREW",
        "INDIAN",
        "ISLAMIC",
        "ISLAMIC_CIVIL",
        "ISLAMIC_UMALQURA",
        "JAPANESE",
        "PERSIAN",
        "ROC",
    )

fun localization() =
    constraint("Localization") {
        allVersions()
            .allow(
                /* Attributes */
                attribute("locales"),
                attribute("timeZone"),
                attribute("calendar", enum(CALENDAR_OPTIONS)),
            )
    }
