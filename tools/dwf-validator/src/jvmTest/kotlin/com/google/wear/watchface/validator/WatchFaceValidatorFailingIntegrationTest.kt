package com.google.wear.watchface.validator

import com.google.wear.watchface.validator.error.AttributeValueError
import com.google.wear.watchface.validator.error.ExpressionSyntaxError
import com.google.wear.watchface.validator.error.GLOBAL_ERROR_KEY
import com.google.wear.watchface.validator.error.IllegalAttributeError
import com.google.wear.watchface.validator.error.IllegalTagError
import com.google.wear.watchface.validator.error.RequiredConditionFailedError
import com.google.wear.watchface.validator.error.TagOccurrenceError
import com.google.wear.watchface.validator.error.ValidationError
import com.google.wear.watchface.validator.error.ValidationResult
import com.google.wear.watchface.validator.error.VersionEliminationError
import com.google.wear.watchface.validator.specification.WFF_SPECIFICATION
import kotlin.test.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class WatchFaceValidatorFailingIntegrationTest(
    val expectedError: ErrorOutput,
    val filePath: String,
) {

    companion object {
        const val FAKE_KEY = ""
        const val FAKE_VALUE = ""
        const val FAKE_ERROR_MESSAGE = "Condition check failed."

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: \"{1}\" = \"{0}\"")
        /* Validation Error thrown against failing test case path*/
        fun testCases(): List<Array<Any>> =
            listOf(
                /* WatchFace */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "watchface/watchFaceMissingScene.xml",
                ),
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_ERROR_MESSAGE)),
                    "watchface/watchFaceExtraAttribute.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "watchface/watchFaceNonIntegerHeight.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "watchface/watchFaceMissingAttributes.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "watchface/watchface_invalid_child.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "watchface/watchface_invalid_corner_radius_x_string.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "watchface/watchface_invalid_corner_radius_y_string.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "watchface/watchface_invalid_clipshape.xml",
                ),

                /* Metadata */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "metadata/missingKeyMetadata.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "metadata/missingValueMetadata.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "metadata/invalidClockTypeMetadata.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "metadata/invalidStepGoalValuesMetadata.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "metadata/invalidPreviewTimeValuesMetadata.xml",
                ),

                /* Group */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/group/group_invalid_name.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/group/group_invalid_x.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/group/group_invalid_y.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/group/group_invalid_width.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/group/group_invalid_height.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/group/group_invalid_pivotX.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/group/group_invalid_pivotY.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/group/group_invalid_alpha.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/group/group_invalid_renderMode.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/group/group_invalid_tintColor.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "group/group/group_invalid_child.xml",
                ),

                /* PartAnimatedImage */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animatedImage/invalid_missing_animation_controller.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animatedImage/invalid_missing_animation.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animatedImage/invalid_multiple_animation_types.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animatedImage/invalid_missing_play.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animatedImage/invalid_play_value.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animatedImage/invalid_delayplay_value.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animatedImage/invalid_missing_resource.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animatedImage/invalid_missing_format.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animatedImage/invalid_format_value.xml",
                ),
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animatedImage/invalid_unexpected_attribute.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animatedImage/invalid_unexpected_element.xml",
                ),

                /* AnimationController */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animationController/invalid_missing_play.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animationController/invalid_play_value.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animationController/invalid_delayplay_value.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animationController/invalid_delayrepeat_value.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animationController/invalid_repeat_value.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animationController/invalid_loopcount_value.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animationController/invalid_resumeplayback_value.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animationController/invalid_beforeplaying_value.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animationController/invalid_afterplaying_value.xml",
                ),
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animationController/invalid_unexpected_attribute.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animationController/invalid_unexpected_element.xml",
                ),

                /* PartAnimatedImage */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/partAnimatedImage/invalid_missing_x.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/partAnimatedImage/invalid_missing_y.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/partAnimatedImage/invalid_missing_width.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/partAnimatedImage/invalid_missing_height.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/partAnimatedImage/invalid_x_value.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/partAnimatedImage/invalid_pivotX_value.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/partAnimatedImage/invalid_alpha_value.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/partAnimatedImage/invalid_scaleX_value.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/partAnimatedImage/invalid_renderMode_value.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/partAnimatedImage/invalid_tintColor_value.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/partAnimatedImage/invalid_blendMode_value.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/partAnimatedImage/invalid_missing_animation_controller.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/partAnimatedImage/invalid_multiple_animation_sources.xml",
                ),
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/partAnimatedImage/invalid_unexpected_attribute.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/partAnimatedImage/invalid_unexpected_element.xml",
                ),

                /* AnimatedImages */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animatedImages/invalid_missing_change.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animatedImages/invalid_change_value.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animatedImages/invalid_missing_image_child.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animatedImages/invalid_changedirection_value.xml",
                ),
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animatedImages/invalid_unexpected_attribute.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "group/part/animatedImage/animatedImages/invalid_unexpected_element.xml",
                ),

                /* AnalogClock */
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/analogclock/analog_clock_invalid_pivotx_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/analogclock/analog_clock_invalid_pivotx_too_high.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/analogclock/analog_clock_invalid_pivoty_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/analogclock/analog_clock_invalid_pivoty_too_high.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/analogclock/analog_clock_invalid_alpha_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/analogclock/analog_clock_invalid_alpha_too_high.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/analogclock/analog_clock_invalid_scalex.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/analogclock/analog_clock_invalid_scaley.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/analogclock/analog_clock_invalid_render_mode.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/analogclock/analog_clock_invalid_tint_color.xml",
                ),

                /* DigitalClock */
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/digitalclock/digital_clock_invalid_pivotx_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/digitalclock/digital_clock_invalid_pivotx_too_high.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/digitalclock/digital_clock_invalid_pivoty_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/digitalclock/digital_clock_invalid_pivoty_too_high.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/digitalclock/digital_clock_invalid_alpha_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/digitalclock/digital_clock_invalid_alpha_too_high.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/digitalclock/digital_clock_invalid_scalex.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/digitalclock/digital_clock_invalid_scaley.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/digitalclock/digital_clock_invalid_render_mode.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/digitalclock/digital_clock_invalid_tint_color.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "clock/digitalclock/digital_clock_invalid_missing_timetext.xml",
                ),

                /* HourHand */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "clock/hourhand/hour_hand_invalid_missing_resource.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/hourhand/hour_hand_invalid_pivotx_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/hourhand/hour_hand_invalid_pivotx_too_high.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/hourhand/hour_hand_invalid_pivoty_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/hourhand/hour_hand_invalid_pivoty_too_high.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/hourhand/hour_hand_invalid_alpha_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/hourhand/hour_hand_invalid_alpha_too_high.xml",
                ),

                /* MinuteHand */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "clock/minutehand/minute_hand_invalid_missing_resource.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/minutehand/minute_hand_invalid_pivotx_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/minutehand/minute_hand_invalid_pivotx_too_high.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/minutehand/minute_hand_invalid_pivoty_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/minutehand/minute_hand_invalid_pivoty_too_high.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/minutehand/minute_hand_invalid_alpha_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/minutehand/minute_hand_invalid_alpha_too_high.xml",
                ),

                /* SecondHand */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "clock/secondhand/second_hand_invalid_missing_resource.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/secondhand/second_hand_invalid_pivotx_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/secondhand/second_hand_invalid_pivotx_too_high.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/secondhand/second_hand_invalid_pivoty_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/secondhand/second_hand_invalid_pivoty_too_high.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/secondhand/second_hand_invalid_alpha_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/secondhand/second_hand_invalid_alpha_too_high.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "clock/secondhand/second_hand_invalid_both_sweep_and_tick.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/secondhand/second_hand_invalid_sweep_frequency.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/secondhand/second_hand_invalid_tick_duration_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/secondhand/second_hand_invalid_tick_duration_too_high.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/secondhand/second_hand_invalid_tick_strength_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/secondhand/second_hand_invalid_tick_strength_too_high.xml",
                ),

                /* TimeText */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "clock/timetext/time_text_invalid_missing_format.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/timetext/time_text_invalid_format.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/timetext/time_text_invalid_hour_format.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/timetext/time_text_invalid_align.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/timetext/time_text_invalid_pivotx_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/timetext/time_text_invalid_pivotx_too_high.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/timetext/time_text_invalid_pivoty_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/timetext/time_text_invalid_pivoty_too_high.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/timetext/time_text_invalid_alpha_too_low.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/timetext/time_text_invalid_alpha_too_high.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "clock/timetext/time_text_invalid_tint_color.xml",
                ),

                /* Scene */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "scene/emptyScene.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "scene/invalidBackgroundColorScene.xml",
                ),
                arrayOf(
                    PartialFailure(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            2 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            3 to
                                mutableListOf(VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))),
                        )
                    ),
                    "scene/transformChildScene.xml",
                ),

                /* PartText */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/text/partText/invalid_parttext_multiple_text_elements.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/text/partText/invalid_parttext_no_text_element.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/text/text/invalid_text_align.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/text/text/invalid_text_ellipsis.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/text/text/invalid_text_maxlines.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/text/text/invalid_text_isautosize.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/text/textCircular/invalid_textcircular_missing_attributes.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/text/textCircular/invalid_textcircular_direction.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/text/font/invalid_font_missing_family.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/text/font/invalid_font_missing_size.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/text/font/invalid_font_color.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/text/font/invalid_font_slant.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/text/font/invalid_font_width.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/text/font/invalid_font_weight.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/text/decoration/invalid_shadow_missing_color.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/text/decoration/invalid_outline_missing_color.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/text/decoration/invalid_outglow_missing_color.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/text/formatter/invalid_inlineimage_missing_resource.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/text/formatter/invalid_inlineimage_missing_dimensions.xml",
                ),

                /* PartImage */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/image/partImage/invalid_partimage_multiple_image_sources.xml",
                ),
                arrayOf(
                    GlobalError(TagOccurrenceError("Image", 2, 1..1)),
                    "group/part/image/partImage/invalid_partimage_duplicate_child.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/image/images/invalid_image_no_resource.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/image/images/invalid_images_bad_change.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/image/images/invalid_images_no_image_child.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/image/images/invalid_images_bad_changedirection.xml",
                ),
                arrayOf(
                    AllVersionsFail(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(
                                        FAKE_ERROR_MESSAGE,
                                        (3..MAX_WFF_VERSION).toSet(),
                                    ),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            2 to
                                mutableListOf(
                                    VersionEliminationError(
                                        FAKE_ERROR_MESSAGE,
                                        (3..MAX_WFF_VERSION).toSet(),
                                    ),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            3 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                            4 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                        )
                    ),
                    "group/part/image/photos/invalid_photos_no_source.xml",
                ),
                arrayOf(
                    AllVersionsFail(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(
                                        FAKE_ERROR_MESSAGE,
                                        (3..MAX_WFF_VERSION).toSet(),
                                    ),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            2 to
                                mutableListOf(
                                    VersionEliminationError(
                                        FAKE_ERROR_MESSAGE,
                                        (3..MAX_WFF_VERSION).toSet(),
                                    ),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            3 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                            4 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                        )
                    ),
                    "group/part/image/photos/invalid_photos_no_defaultimageresource.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/image/photos/invalid_photos_bad_change.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/image/photos/invalid_photos_out_of_range_changeafterevery.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/image/photos/invalid_photos_negative_width.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/image/photos/invalid_photos_negative_height.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/image/photos/invalid_photos_bad_changedirection.xml",
                ),

                /* PartDraw */
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/draw/partDraw/invalid_partdraw_pivot.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/draw/partDraw/invalid_partdraw_angle.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/draw/partDraw/invalid_partdraw_alpha.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/draw/partDraw/invalid_partdraw_scale.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/draw/partDraw/invalid_partdraw_rendermode.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "group/part/draw/partDraw/invalid_partdraw_tintcolor.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/draw/shape/invalid_line.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/draw/shape/invalid_arc.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/draw/shape/invalid_ellipse.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/draw/shape/invalid_roundrectangle.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/draw/style/invalid_fill.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/draw/style/invalid_stroke.xml",
                ),
                arrayOf(
                    AllVersionsFail(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(
                                        FAKE_ERROR_MESSAGE,
                                        (2..MAX_WFF_VERSION).toSet(),
                                    ),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            2 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                            3 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                            4 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                        )
                    ),
                    "group/part/draw/style/invalid_weightedstroke.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/draw/gradient/invalid_lineargradient.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/draw/gradient/invalid_radialgradient.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "group/part/draw/gradient/invalid_sweepgradient.xml",
                ),

                /* ComplicationSlot */
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "complication/complicationslot/invalid_complicationslot_slotid_string.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "complication/complicationslot/invalid_complicationslot_slotid_missing.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "complication/complicationslot/invalid_complicationslot_supportedtypes_unknown.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "complication/complicationslot/invalid_complicationslot_supportedtypes_missing.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "complication/complicationslot/invalid_complicationslot_complication_missing.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "complication/complicationslot/invalid_complicationslot_complication_type_mismatch.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "complication/complicationslot/invalid_complicationslot_boundingbox_missing.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "complication/complicationslot/invalid_complicationslot_height_negative.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "complication/complicationslot/invalid_complicationslot_x_float.xml",
                ),

                /* BoundingBox */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "complication/bounding/invalid_boundingbox_missing_x.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "complication/bounding/invalid_boundingbox_missing_y.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "complication/bounding/invalid_boundingbox_missing_width.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "complication/bounding/invalid_boundingbox_missing_height.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "complication/bounding/invalid_boundingbox_invalid_x.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "complication/bounding/invalid_boundingbox_invalid_y.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "complication/bounding/invalid_boundingbox_invalid_width.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "complication/bounding/invalid_boundingbox_invalid_height.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "complication/bounding/invalid_boundingbox_negative_width.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "complication/bounding/invalid_boundingbox_negative_height.xml",
                ),
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_ERROR_MESSAGE)),
                    "complication/bounding/invalid_boundingbox_extra_attribute.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "complication/bounding/invalid_boundingbox_invalid_child.xml",
                ),

                /* DefaultProviderPolicy */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "complication/defaultproviderpolicy/invalid_defaultproviderpolicy_missing_provider.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "complication/defaultproviderpolicy/invalid_defaultproviderpolicy_missing_type.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "complication/defaultproviderpolicy/invalid_defaultproviderpolicy_invalid_provider.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "complication/defaultproviderpolicy/invalid_defaultproviderpolicy_invalid_type.xml",
                ),
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_ERROR_MESSAGE)),
                    "complication/defaultproviderpolicy/invalid_defaultproviderpolicy_extra_attribute.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "complication/defaultproviderpolicy/invalid_defaultproviderpolicy_invalid_child.xml",
                ),

                /* Complication */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "complication/complication/invalid_complication_missing_type.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "complication/complication/invalid_complication_unknown_type.xml",
                ),
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_ERROR_MESSAGE)),
                    "complication/complication/invalid_complication_extra_attribute.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "complication/complication/invalid_complication_invalid_child.xml",
                ),

                /* BitmapFonts */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "bitmapfonts/emptyBitMapFonts.xml",
                ),
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_ERROR_MESSAGE)),
                    "bitmapfonts/extraAttributeCharacter.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "bitmapfonts/illegalFontElement.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "bitmapfonts/noFontElement.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "bitmapfonts/missingAttributeCharacter.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "bitmapfonts/missingBitMapFontsContainer.xml",
                ),

                /* Condition */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/condition/condition_missing_expressions.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "common/condition/condition_unrecognized_child.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/condition/expressions_empty.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "common/condition/expressions_unrecognized_child.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/condition/expression_missing_name.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/condition/expression_empty_name.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/condition/expression_missing_content.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/condition/expression_duplicate_name.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/condition/compare_missing_expression_attribute.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/condition/compare_empty_expression_attribute.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/condition/compare_expression_not_found.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/condition/compare_empty.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/condition/default_empty.xml",
                ),

                /* Localization */
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "common/localization/localization_invalid_calendar.xml",
                ),
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_KEY)),
                    "common/localization/localization_invalid_attribute.xml",
                ),

                /* ScreenReader */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/screenreader/screenreader_invalid_missing_stringid.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "common/screenreader/screenreader_invalid_empty_stringid.xml",
                ),
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_ERROR_MESSAGE)),
                    "common/screenreader/screenreader_invalid_extra_attribute.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "common/screenreader/screenreader_invalid_child.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/screenreader/parameter_invalid_missing_expression.xml",
                ),
                arrayOf(
                    GlobalError(ExpressionSyntaxError(FAKE_ERROR_MESSAGE)),
                    "common/screenreader/parameter_invalid_empty_expression.xml",
                ),
                arrayOf(
                    GlobalError(ExpressionSyntaxError(FAKE_ERROR_MESSAGE)),
                    "common/screenreader/parameter_invalid_expression.xml",
                ),
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_ERROR_MESSAGE)),
                    "common/screenreader/parameter_invalid_extra_attribute.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "common/screenreader/parameter_invalid_child.xml",
                ),

                /* Variant */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/variant/variant_invalid_missing_mode.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "common/variant/variant_invalid_wrong_mode.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/variant/variant_invalid_missing_target.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/variant/variant_invalid_missing_value.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "common/variant/variant_invalid_duration.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "common/variant/variant_invalid_startoffset.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "common/variant/variant_invalid_interpolation.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "common/variant/variant_invalid_controls.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "common/variant/variant_invalid_angledirection.xml",
                ),
                arrayOf(
                    PartialFailure(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            2 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            3 to
                                mutableListOf(VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))),
                        )
                    ),
                    "common/variant/variant_v4_duration.xml",
                ),
                arrayOf(
                    PartialFailure(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            2 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            3 to
                                mutableListOf(VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))),
                        )
                    ),
                    "common/variant/variant_v4_startoffset.xml",
                ),
                arrayOf(
                    PartialFailure(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            2 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            3 to
                                mutableListOf(VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))),
                        )
                    ),
                    "common/variant/variant_v4_interpolation.xml",
                ),
                arrayOf(
                    PartialFailure(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            2 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            3 to
                                mutableListOf(VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))),
                        )
                    ),
                    "common/variant/variant_v4_controls.xml",
                ),
                arrayOf(
                    PartialFailure(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            2 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            3 to
                                mutableListOf(VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))),
                        )
                    ),
                    "common/variant/variant_v4_angledirection.xml",
                ),

                /* Gyro */
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_ERROR_MESSAGE)),
                    "common/transform/gyro/gyro_invalid_extra_attribute.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "common/transform/gyro/gyro_invalid_child.xml",
                ),

                /* Transform */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/transform/transform/invalid_missing_target.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/transform/transform/invalid_missing_value.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "common/transform/transform/invalid_mode.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "common/transform/transform/invalid_child.xml",
                ),

                /* Animation */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "common/transform/animation/animation_invalid_no_duration.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "common/transform/animation/animation_invalid_duration.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "common/transform/animation/animation_invalid_interpolation.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "common/transform/animation/animation_invalid_controls.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "common/transform/animation/animation_invalid_angleDirection.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "common/transform/animation/animation_invalid_repeat.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "common/transform/animation/animation_invalid_fps.xml",
                ),
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_KEY)),
                    "common/transform/animation/animation_invalid_attribute.xml",
                ),

                /* Reference */
                arrayOf(
                    AllVersionsFail(
                        /* Version elimination error should happen once for using a Reference Tag
                        inside a Scene element,
                                    and again for using the Reference (entirely exclusive to v4) */
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4)),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            2 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4)),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            3 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4)),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            4 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                        )
                    ),
                    "common/reference/reference/reference_invalid_missing_name.xml",
                ),
                arrayOf(
                    AllVersionsFail(
                        /* Version elimination error should happen once for using a Reference Tag
                        inside a Scene element,
                                    and again for using the Reference (entirely exclusive to v4) */
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4)),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            2 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4)),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            3 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4)),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            4 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                        )
                    ),
                    "common/reference/reference/reference_invalid_missing_source.xml",
                ),
                arrayOf(
                    AllVersionsFail(
                        /* Version elimination error should happen once for using a Reference Tag
                        inside a Scene element,
                                    and again for using the Reference (entirely exclusive to v4) */
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4)),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            2 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4)),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            3 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4)),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            4 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                        )
                    ),
                    "common/reference/reference/reference_invalid_missing_defaultValue.xml",
                ),
                arrayOf(
                    AllVersionsFail(
                        /* Version elimination error should happen once for using a Reference Tag
                        inside a Scene element,
                                    and again for using the Reference (entirely exclusive to v4) */
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            2 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            3 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            4 to mutableListOf(IllegalAttributeError(FAKE_KEY)),
                        )
                    ),
                    "common/reference/reference/reference_invalid_attribute.xml",
                ),

                /* UserConfiguration */
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/userConfiguration/user_configurations_no_children_invalid.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/booleanConfiguration/invalid_boolean_configuration_child_in_user_configurations.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/booleanConfiguration/invalid_boolean_configuration_missing_children_in_scene.xml",
                ),
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/booleanConfiguration/invalid_boolean_configuration_optional_attribute_in_scene.xml",
                ),
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/booleanConfiguration/invalid_boolean_configuration_required_attribute_in_scene.xml",
                ),
                arrayOf(
                    PartialFailure(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            2 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            3 to
                                mutableListOf(VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))),
                        )
                    ),
                    "userConfiguration/booleanConfiguration/invalid_boolean_configuration_v4_attribute_in_v1.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "userConfiguration/booleanConfiguration/invalid_boolean_option_bad_id.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/booleanConfiguration/invalid_boolean_option_missing_id.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/booleanConfiguration/invalid_missing_defaultValue.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/booleanConfiguration/invalid_missing_displayName.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/booleanConfiguration/invalid_missing_id.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/listConfiguration/invalid_list_configuration_child_in_user_configurations.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/listConfiguration/invalid_list_configuration_missing_children_in_scene.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/listConfiguration/invalid_list_configuration_missing_id.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/listConfiguration/invalid_list_configuration_missing_displayName.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/listConfiguration/invalid_list_configuration_missing_defaultValue.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/listConfiguration/invalid_list_configuration_bad_defaultValue.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/listConfiguration/invalid_list_option_missing_id.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/listConfiguration/invalid_list_option_with_child_in_user_configurations.xml",
                ),
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/listConfiguration/invalid_list_configuration_attribute_in_scene.xml",
                ),
                arrayOf(
                    PartialFailure(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            2 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))
                                ),
                            3 to
                                mutableListOf(VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(4))),
                        )
                    ),
                    "userConfiguration/listConfiguration/invalid_list_configuration_v4_attribute_in_v1.xml",
                ),
                arrayOf(
                    AllVersionsFail(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(2, 3, 4)),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            2 to
                                mutableListOf(
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            3 to
                                mutableListOf(
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            4 to
                                mutableListOf(
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                        )
                    ),
                    "userConfiguration/flavor/invalid_flavors_no_children.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/flavor/invalid_flavors_with_invalid_child.xml",
                ),
                arrayOf(
                    AllVersionsFail(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(2, 3, 4)),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            2 to
                                mutableListOf(
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            3 to
                                mutableListOf(
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            4 to
                                mutableListOf(
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                        )
                    ),
                    "userConfiguration/flavor/invalid_flavors_missing_defaultValue.xml",
                ),
                arrayOf(
                    AllVersionsFail(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(2, 3, 4)),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            2 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                            3 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                            4 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                        )
                    ),
                    "userConfiguration/flavor/invalid_flavor_missing_id.xml",
                ),
                arrayOf(
                    AllVersionsFail(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(2, 3, 4)),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            2 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                            3 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                            4 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                        )
                    ),
                    "userConfiguration/flavor/invalid_flavor_missing_displayName.xml",
                ),
                arrayOf(
                    AllVersionsFail(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(2, 3, 4)),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            2 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                            3 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                            4 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                        )
                    ),
                    "userConfiguration/flavor/invalid_flavor_no_children.xml",
                ),
                arrayOf(
                    AllVersionsFail(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(2, 3, 4)),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            2 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                            3 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                            4 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                        )
                    ),
                    "userConfiguration/flavor/invalid_configuration_missing_id.xml",
                ),
                arrayOf(
                    AllVersionsFail(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(2, 3, 4)),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            2 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                            3 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                            4 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                        )
                    ),
                    "userConfiguration/flavor/invalid_configuration_missing_optionId.xml",
                ),
                arrayOf(
                    AllVersionsFail(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(2, 3, 4))
                                ),
                            0 to mutableListOf(TagOccurrenceError("Flavor", 21, 1..20)),
                        )
                    ),
                    "userConfiguration/flavor/invalid_flavors_21_children.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/flavor/invalid_configuration_with_child.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/colorConfiguration/invalid_color_configuration_no_children.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/colorConfiguration/invalid_color_configuration_missing_id.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/colorConfiguration/invalid_color_configuration_missing_displayName.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/colorConfiguration/invalid_color_configuration_missing_defaultValue.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/colorConfiguration/invalid_color_configuration_bad_defaultValue.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "userConfiguration/colorConfiguration/invalid_color_configuration_six_colors.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/colorConfiguration/invalid_color_option_missing_id.xml",
                ),
                arrayOf(
                    AllVersionsFailWithSameError(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/colorConfiguration/invalid_color_option_missing_color.xml",
                ),
                arrayOf(
                    GlobalError(AttributeValueError(FAKE_KEY, FAKE_VALUE, FAKE_ERROR_MESSAGE)),
                    "userConfiguration/colorConfiguration/invalid_color_option_bad_color.xml",
                ),
                arrayOf(
                    GlobalError(IllegalTagError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/colorConfiguration/invalid_color_option_with_child.xml",
                ),
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/photosConfiguration/photos_configuration_missing_id_invalid.xml",
                ),
                arrayOf(
                    AllVersionsFail(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(3, 4)),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            2 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(3, 4)),
                                    RequiredConditionFailedError(FAKE_ERROR_MESSAGE),
                                ),
                            3 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                            4 to mutableListOf(RequiredConditionFailedError(FAKE_ERROR_MESSAGE)),
                        )
                    ),
                    "userConfiguration/photosConfiguration/photos_configuration_missing_configType_invalid.xml",
                ),
                arrayOf(
                    GlobalError(IllegalAttributeError(FAKE_ERROR_MESSAGE)),
                    "userConfiguration/photosConfiguration/photos_configuration_invalid_configType_invalid.xml",
                ),
                arrayOf(
                    PartialFailure(
                        mapOf(
                            1 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(3, 4))
                                ),
                            2 to
                                mutableListOf(
                                    VersionEliminationError(FAKE_ERROR_MESSAGE, setOf(3, 4))
                                ),
                        )
                    ),
                    "userConfiguration/photosConfiguration/photos_configuration_old_version_invalid.xml",
                ),
            )
    }

    @Test
    fun test() {
        val doc: WatchFaceDocument =
            JvmWatchFaceDocument.of(XmlReader.fromResource("/integration/$filePath"))
        val validator = WatchFaceValidator(WFF_SPECIFICATION)

        val result: ValidationResult = validator.getValidationResult(doc)
        val errorMapString =
            result.errorMap
                .map { (key, value) -> " VersionKey $key: $value" }
                .joinToString(separator = "\n", prefix = "{\n", postfix = "\n}")

        when (expectedError) {
            /* A special 'global error' has been thrown which is not associated with any version */
            is GlobalError -> {
                val error = result.errorMap[GLOBAL_ERROR_KEY]?.firstOrNull()

                assertTrue(
                    error != null,
                    "Expected a global error but was none. ErrorMap: $errorMapString",
                )
                assertTrue(
                    result is ValidationResult.Failure,
                    "Expected overall failure but was $result. ErrorMap: $errorMapString",
                )
                assertEquals(
                    "Expected ${expectedError.error::class}. But was ${error::class}. ErrorMap: $errorMapString",
                    expectedError.error::class,
                    error::class,
                )
            }

            /* All versions have failed due to a common constraint */
            is AllVersionsFailWithSameError -> {
                val errors: List<ValidationError> =
                    ALL_WFF_VERSIONS.mapNotNull { result.errorMap[it]?.firstOrNull() }

                assertTrue(
                    result is ValidationResult.Failure,
                    "Expected overall failure but was ${result::class}. ErrorMap: $errorMapString",
                )
                assertEquals(
                    "Expected all versions to fail. ErrorMap: $errorMapString",
                    ALL_WFF_VERSIONS.size,
                    errors.size,
                )
                errors.forEach {
                    System.err.println("Actual error class: ${it::class}")
                    assertEquals(
                        "Expected each error to be a ${expectedError.error::class}. ErrorMap: $errorMapString",
                        expectedError.error::class,
                        it::class,
                    )
                }
            }

            /* Some versions have failed due to version-specific constraints */
            is PartialFailure -> {
                val expectedErrors = expectedError.errorMap.values.flatten()
                val actualErrors = result.errorMap.values.flatten()

                assertTrue(
                    result is ValidationResult.PartialSuccess,
                    "Expected Partial Success but was $result. ErrorMap: $errorMapString",
                )
                assertTrue(
                    expectedErrors.size == actualErrors.size,
                    "Expected ${expectedErrors.size} errors but found ${actualErrors.size}. Actual error map: $errorMapString",
                )
                assertTrue(
                    (expectedErrors zip actualErrors).all { it.first::class == it.second::class },
                    "Error maps did not match. Actual error map: $errorMapString",
                )
            }

            is AllVersionsFail -> {
                val expectedErrors = expectedError.errorMap.values.flatten()
                val actualErrors = result.errorMap.values.flatten()

                assertTrue(
                    result is ValidationResult.Failure,
                    "Expected Failure but was $result. ErrorMap: $errorMapString",
                )
                assertTrue(
                    expectedErrors.size == actualErrors.size,
                    "Expected ${expectedErrors.size} errors but found ${actualErrors.size}. Actual error map: $errorMapString",
                )
                assertTrue(
                    (expectedErrors zip actualErrors).all { it.first::class == it.second::class },
                    "Error maps did not match. Actual error map: $errorMapString",
                )
            }
        }
    }
}
