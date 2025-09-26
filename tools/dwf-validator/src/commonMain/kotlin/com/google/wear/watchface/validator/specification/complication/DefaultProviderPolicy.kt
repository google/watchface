package com.google.wear.watchface.validator.specification.complication

import com.google.wear.watchface.validator.constraint.constraint

fun defaultProviderPolicy() =
    constraint("DefaultProviderPolicy") {
        allVersions()
            .require(
                /* Attributes */
                attribute("defaultSystemProvider", enum(DEFAULT_PROVIDERS)),
                attribute("defaultSystemProviderType", enum(COMPLICATION_TYPES)),
            )
            .allow(
                /* Attributes */
                attribute("primaryProvider"),
                attribute("secondaryProvider"),

                /* Child Elements */
                attribute("primaryProviderType", enum(COMPLICATION_TYPES)),
                attribute("secondaryProviderType", enum(COMPLICATION_TYPES)),
            )
    }
