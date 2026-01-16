package com.google.wear.watchface.validator.specification.group.part.text.formatter

import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.common.parameter

/**
 * Specification constraint for a 'Template' element. These elements can contain 'c-like' string
 * formatters in their content.
 */
fun template() =
    constraint("Template") {
        allVersions()
            .allow(
                /* Child Elements */
                childElement("Parameter", ::parameter)
                // TODO(b/443729856): validate content as a string template
            )
    }
