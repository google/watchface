package com.google.wear.watchface.validator.specification.complication

internal val COMPLICATION_TYPES =
    setOf(
        "SHORT_TEXT",
        "LONG_TEXT",
        "MONOCHROMATIC_IMAGE",
        "SMALL_IMAGE",
        "PHOTO_IMAGE",
        "RANGED_VALUE",
        "GOAL_PROGRESS",
        "WEIGHTED_ELEMENTS",
        "EMPTY",
    )

internal val DEFAULT_PROVIDERS =
    setOf(
        "APP_SHORTCUT",
        "DATE",
        "DAY_OF_WEEK",
        "FAVORITE_CONTACT",
        "NEXT_EVENT",
        "STEP_COUNT",
        "SUNRISE_SUNSET",
        "TIME_AND_DATE",
        "UNREAD_NOTIFICATION_COUNT",
        "WATCH_BATTERY",
        "WORLD_CLOCK",
        "DAY_AND_DATE",
        "EMPTY",
    )
