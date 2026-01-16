package com.google.wear.watchface.validator

import com.google.wear.watchface.validator.error.ErrorMap
import com.google.wear.watchface.validator.error.ValidationError

sealed interface ErrorOutput

data class GlobalError(val error: ValidationError) : ErrorOutput

data class AllVersionsFailWithSameError(val error: ValidationError) : ErrorOutput

data class PartialFailure(val errorMap: ErrorMap) : ErrorOutput

data class AllVersionsFail(val errorMap: ErrorMap) : ErrorOutput
