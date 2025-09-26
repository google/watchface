package com.google.wear.watchface.validator

import com.google.wear.watchface.validator.error.ValidationResult
import com.google.wear.watchface.validator.specification.WFF_SPECIFICATION
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class WatchFaceValidatorIntegrationTest(
    val expectedValidVersions: Set<Version>,
    val filePath: String,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: \"{1}\" = \"{0}\"")
        fun testCases(): List<Array<Any>> =
            listOf(
                /* WatchFace */
                arrayOf(ALL_WFF_VERSIONS, "watchface/watchFace.xml"),
                arrayOf(ALL_WFF_VERSIONS, "watchface/watchFaceOptionalAttributes.xml"),
                arrayOf(emptySet<Version>(), "watchface/watchFaceMissingScene.xml"),
                arrayOf(emptySet<Version>(), "watchface/watchFaceExtraAttribute.xml"),
                arrayOf(emptySet<Version>(), "watchface/watchFaceNonIntegerHeight.xml"),
                arrayOf(emptySet<Version>(), "watchface/watchFaceMissingAttributes.xml"),
                arrayOf(ALL_WFF_VERSIONS, "watchface/watchface_valid_corner_radius.xml"),
                arrayOf(ALL_WFF_VERSIONS, "watchface/watchface_valid_clipshape_circle.xml"),
                arrayOf(ALL_WFF_VERSIONS, "watchface/watchface_valid_clipshape_rectangle.xml"),
                arrayOf(ALL_WFF_VERSIONS, "watchface/watchface_valid_clipshape_none.xml"),
                arrayOf(ALL_WFF_VERSIONS, "watchface/watchface_valid_clipshape_default.xml"),
                arrayOf(emptySet<Version>(), "watchface/watchface_invalid_child.xml"),
                arrayOf(
                    emptySet<Version>(),
                    "watchface/watchface_invalid_corner_radius_x_string.xml",
                ),
                arrayOf(
                    emptySet<Version>(),
                    "watchface/watchface_invalid_corner_radius_y_string.xml",
                ),
                arrayOf(emptySet<Version>(), "watchface/watchface_invalid_clipshape.xml"),

                /* Metadata */
                arrayOf(ALL_WFF_VERSIONS, "metadata/singleMetadata.xml"),
                arrayOf(ALL_WFF_VERSIONS, "metadata/multipleMetadata.xml"),
                arrayOf(emptySet<Version>(), "metadata/missingKeyMetadata.xml"),
                arrayOf(emptySet<Version>(), "metadata/missingValueMetadata.xml"),
                arrayOf(ALL_WFF_VERSIONS, "metadata/predefinedValuesMetadata.xml"),
                arrayOf(emptySet<Version>(), "metadata/invalidClockTypeMetadata.xml"),
                arrayOf(emptySet<Version>(), "metadata/invalidClockTypeMetadata.xml"),
                arrayOf(emptySet<Version>(), "metadata/invalidClockTypeMetadata.xml"),

                /* Group */
                arrayOf(ALL_WFF_VERSIONS, "group/group/group_valid_optional_attributes.xml"),
                arrayOf(emptySet<Version>(), "group/group/group_invalid_name.xml"),
                arrayOf(emptySet<Version>(), "group/group/group_invalid_x.xml"),
                arrayOf(emptySet<Version>(), "group/group/group_invalid_y.xml"),
                arrayOf(emptySet<Version>(), "group/group/group_invalid_width.xml"),
                arrayOf(emptySet<Version>(), "group/group/group_invalid_height.xml"),
                arrayOf(emptySet<Version>(), "group/group/group_invalid_pivotX.xml"),
                arrayOf(emptySet<Version>(), "group/group/group_invalid_pivotY.xml"),
                arrayOf(emptySet<Version>(), "group/group/group_invalid_alpha.xml"),
                arrayOf(emptySet<Version>(), "group/group/group_invalid_renderMode.xml"),
                arrayOf(emptySet<Version>(), "group/group/group_invalid_tintColor.xml"),
                arrayOf(emptySet<Version>(), "group/group/group_invalid_child.xml"),

                /* PartAnimatedImage */
                arrayOf(ALL_WFF_VERSIONS, "group/part/animatedImage/animatedImage/valid_basic.xml"),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "group/part/animatedImage/animatedImage/valid_all_attributes.xml",
                ),

                /* AnimationController */
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "group/part/animatedImage/animationController/valid_basic.xml",
                ),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "group/part/animatedImage/animationController/valid_all_attributes.xml",
                ),

                /* PartAnimatedImage */
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "group/part/animatedImage/partAnimatedImage/valid_basic.xml",
                ),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "group/part/animatedImage/partAnimatedImage/valid_all_attributes.xml",
                ),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "group/part/animatedImage/partAnimatedImage/valid_with_thumbnail.xml",
                ),

                /* AnimatedImages */
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "group/part/animatedImage/animatedImages/valid_basic.xml",
                ),
                arrayOf(
                    (4..MAX_WFF_VERSION).toSet(),
                    "group/part/animatedImage/animatedImages/valid_all_attributes.xml",
                ),

                /* ImageFilter */
                arrayOf(ALL_WFF_VERSIONS, "group/part/image/imageFilter/valid_basic.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/image/imageFilter/valid_all_attributes.xml"),

                /* AnalogClock */
                arrayOf(ALL_WFF_VERSIONS, "clock/analogclock/analog_clock_valid_basic.xml"),
                arrayOf(ALL_WFF_VERSIONS, "clock/analogclock/analog_clock_valid_attributes.xml"),

                /* DigitalClock */
                arrayOf(ALL_WFF_VERSIONS, "clock/digitalclock/digital_clock_valid_basic.xml"),
                arrayOf(ALL_WFF_VERSIONS, "clock/digitalclock/digital_clock_valid_attributes.xml"),

                /* HourHand */
                arrayOf(ALL_WFF_VERSIONS, "clock/hourhand/hour_hand_valid_basic.xml"),
                arrayOf(ALL_WFF_VERSIONS, "clock/hourhand/hour_hand_valid_attributes.xml"),

                /* MinuteHand */
                arrayOf(ALL_WFF_VERSIONS, "clock/minutehand/minute_hand_valid_basic.xml"),
                arrayOf(ALL_WFF_VERSIONS, "clock/minutehand/minute_hand_valid_attributes.xml"),

                /* SecondHand */
                arrayOf(ALL_WFF_VERSIONS, "clock/secondhand/second_hand_valid_basic_sweep.xml"),
                arrayOf(ALL_WFF_VERSIONS, "clock/secondhand/second_hand_valid_basic_tick.xml"),
                arrayOf(ALL_WFF_VERSIONS, "clock/secondhand/second_hand_valid_no_child.xml"),
                arrayOf(ALL_WFF_VERSIONS, "clock/secondhand/second_hand_valid_attributes.xml"),

                /* TimeText */
                arrayOf(ALL_WFF_VERSIONS, "clock/timetext/time_text_valid_basic.xml"),
                arrayOf(ALL_WFF_VERSIONS, "clock/timetext/time_text_valid_attributes.xml"),

                /* Scene */
                arrayOf(ALL_WFF_VERSIONS, "scene/basicScene.xml"),
                arrayOf(ALL_WFF_VERSIONS, "scene/backgroundColorScene.xml"),
                arrayOf(emptySet<Version>(), "scene/invalidBackgroundColorScene.xml"),
                arrayOf(emptySet<Version>(), "scene/emptyScene.xml"),
                arrayOf(ALL_WFF_VERSIONS, "scene/manyChildScene.xml"),
                arrayOf((4..MAX_WFF_VERSION).toSet(), "scene/transformChildScene.xml"),

                /* PartText */
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/partText/valid_parttext_name.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/partText/valid_parttext_pivot.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/partText/valid_parttext_angle.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/partText/valid_parttext_alpha.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/partText/valid_parttext_scale.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/partText/valid_parttext_renderMode.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/partText/valid_parttext_tintColor.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/text/valid_text_align.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/text/valid_text_ellipsis.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/text/valid_text_maxlines.xml"),
                arrayOf(setOf(3, 4), "group/part/text/text/valid_text_isautosize.xml"),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "group/part/text/textCircular/valid_textcircular_all_attributes.xml",
                ),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/font/valid_font_all_attributes.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/font/valid_font_shadow.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/font/valid_font_outline.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/font/valid_font_outglow.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/font/valid_font_underline.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/font/valid_font_strikethrough.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/font/valid_font_inlineimage.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/font/valid_font_template.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/font/valid_font_upper.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/text/font/valid_font_lower.xml"),

                /* PartImage */
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "group/part/image/partImage/valid_partimage_with_image.xml",
                ),
                arrayOf(ALL_WFF_VERSIONS, "group/part/image/images/valid_images_with_change.xml"),
                arrayOf(
                    (4..MAX_WFF_VERSION).toSet(),
                    "group/part/image/images/valid_images_with_changedirection.xml",
                ),
                arrayOf((3..MAX_WFF_VERSION).toSet(), "group/part/image/photos/valid_photos.xml"),
                arrayOf(
                    (4..MAX_WFF_VERSION).toSet(),
                    "group/part/image/photos/valid_photos_with_all_attributes.xml",
                ),

                /* PartDraw */
                arrayOf(ALL_WFF_VERSIONS, "group/part/draw/partDraw/valid_partdraw_name.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/draw/partDraw/valid_partdraw_pivot.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/draw/partDraw/valid_partdraw_angle.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/draw/partDraw/valid_partdraw_alpha.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/draw/partDraw/valid_partdraw_scale.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/draw/partDraw/valid_partdraw_rendermode.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/draw/partDraw/valid_partdraw_tintcolor.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/draw/shape/valid_line.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/draw/shape/valid_arc.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/draw/shape/valid_ellipse.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/draw/shape/valid_roundrectangle.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/draw/style/valid_fill.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/draw/style/valid_stroke.xml"),
                arrayOf(
                    (2..MAX_WFF_VERSION).toSet(),
                    "group/part/draw/style/valid_weightedstroke.xml",
                ),
                arrayOf(ALL_WFF_VERSIONS, "group/part/draw/gradient/valid_lineargradient.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/draw/gradient/valid_radialgradient.xml"),
                arrayOf(ALL_WFF_VERSIONS, "group/part/draw/gradient/valid_sweepgradient.xml"),

                /* ComplicationSlot */
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "complication/complicationslot/valid_complicationslot.xml",
                ),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "complication/complicationslot/valid_complicationslot_multiple_supported_types.xml",
                ),

                /* BoundingBox */
                arrayOf(ALL_WFF_VERSIONS, "complication/bounding/valid_boundingbox.xml"),

                /* DefaultProviderPolicy */
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "complication/defaultproviderpolicy/valid_defaultproviderpolicy.xml",
                ),

                /* Complication */
                arrayOf(ALL_WFF_VERSIONS, "complication/complication/valid_complication.xml"),

                /* BitmapFonts */
                arrayOf(emptySet<Version>(), "bitmapfonts/emptyBitMapFonts.xml"),
                arrayOf(emptySet<Version>(), "bitmapfonts/extraAttributeCharacter.xml"),
                arrayOf(emptySet<Version>(), "bitmapfonts/illegalFontElement.xml"),
                arrayOf(emptySet<Version>(), "bitmapfonts/missingAttributeCharacter.xml"),
                arrayOf(emptySet<Version>(), "bitmapfonts/missingBitMapFontsContainer.xml"),
                arrayOf(emptySet<Version>(), "bitmapfonts/duplicateFontName.xml"),
                arrayOf(emptySet<Version>(), "bitmapfonts/duplicateArtifactName.xml"),
                arrayOf(ALL_WFF_VERSIONS, "bitmapfonts/manyValidFonts.xml"),
                arrayOf(ALL_WFF_VERSIONS, "bitmapfonts/manyValidArtifacts.xml"),
                arrayOf(ALL_WFF_VERSIONS, "bitmapfonts/singleCharacterBitmapFont.xml"),
                arrayOf(ALL_WFF_VERSIONS, "bitmapfonts/singleWordBitmapFont.xml"),
                arrayOf((2..MAX_WFF_VERSION).toSet(), "bitmapfonts/singleWordWithMargin.xml"),

                /* Condition */
                arrayOf(ALL_WFF_VERSIONS, "common/condition/condition_valid.xml"),

                /* Localization */
                arrayOf(ALL_WFF_VERSIONS, "common/localization/localization_valid_basic.xml"),
                arrayOf(ALL_WFF_VERSIONS, "common/localization/localization_valid_locale.xml"),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "common/localization/localization_valid_all_attributes.xml",
                ),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "common/localization/localization_valid_no_attributes.xml",
                ),

                /* ScreenReader */
                arrayOf(ALL_WFF_VERSIONS, "common/screenreader/screenreader_valid_basic.xml"),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "common/screenreader/screenreader_valid_multiple_parameters.xml",
                ),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "common/screenreader/screenreader_valid_no_parameters.xml",
                ),

                /* Variant */
                arrayOf(ALL_WFF_VERSIONS, "common/variant/variant_valid_basic.xml"),
                arrayOf(
                    (4..MAX_WFF_VERSION).toSet(),
                    "common/variant/variant_valid_v4_attributes.xml",
                ),

                /* Gyro */
                arrayOf(ALL_WFF_VERSIONS, "common/transform/gyro/gyro_valid_basic.xml"),
                arrayOf(ALL_WFF_VERSIONS, "common/transform/gyro/gyro_valid_all_attributes.xml"),

                /* Transform */
                arrayOf(ALL_WFF_VERSIONS, "common/transform/transform/valid_transform.xml"),
                arrayOf(ALL_WFF_VERSIONS, "common/transform/transform/valid_mode_by.xml"),
                arrayOf(ALL_WFF_VERSIONS, "common/transform/transform/valid_mode_to.xml"),

                /* Animation */
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "common/transform/animation/animation_valid_duration.xml",
                ),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "common/transform/animation/animation_valid_interpolation.xml",
                ),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "common/transform/animation/animation_valid_controls.xml",
                ),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "common/transform/animation/animation_valid_angleDirection.xml",
                ),
                arrayOf(ALL_WFF_VERSIONS, "common/transform/animation/animation_valid_repeat.xml"),
                arrayOf(ALL_WFF_VERSIONS, "common/transform/animation/animation_valid_fps.xml"),

                /* Reference */
                arrayOf(
                    (4..MAX_WFF_VERSION).toSet(),
                    "common/reference/reference/reference_valid_basic.xml",
                ),

                /* UserConfiguration */
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "userConfiguration/userConfiguration/user_configurations_valid.xml",
                ),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "userConfiguration/booleanConfiguration/valid_boolean_configuration.xml",
                ),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "userConfiguration/booleanConfiguration/valid_boolean_configuration_with_optional_attributes.xml",
                ),
                arrayOf(
                    (4..MAX_WFF_VERSION).toSet(),
                    "userConfiguration/booleanConfiguration/valid_boolean_configuration_v4.xml",
                ),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "userConfiguration/booleanConfiguration/valid_boolean_option_with_children.xml",
                ),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "userConfiguration/listConfiguration/valid_list_configuration.xml",
                ),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "userConfiguration/listConfiguration/valid_list_configuration_with_optional_attributes.xml",
                ),
                arrayOf(
                    (4..MAX_WFF_VERSION).toSet(),
                    "userConfiguration/listConfiguration/valid_list_configuration_v4.xml",
                ),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "userConfiguration/listConfiguration/valid_list_option_with_children.xml",
                ),
                arrayOf((2..MAX_WFF_VERSION).toSet(), "userConfiguration/flavor/valid_flavor.xml"),
                arrayOf(
                    (2..MAX_WFF_VERSION).toSet(),
                    "userConfiguration/flavor/valid_flavor_with_optional_attributes.xml",
                ),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "userConfiguration/colorConfiguration/valid_color_configuration.xml",
                ),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "userConfiguration/colorConfiguration/valid_color_configuration_with_many_colors.xml",
                ),
                arrayOf(
                    ALL_WFF_VERSIONS,
                    "userConfiguration/colorConfiguration/valid_color_configuration_with_optional_attributes.xml",
                ),
                arrayOf(
                    (3..MAX_WFF_VERSION).toSet(),
                    "userConfiguration/photosConfiguration/photos_configuration_valid.xml",
                ),
            )
    }

    @Test
    fun test() {
        val doc: WatchFaceDocument =
            JvmWatchFaceDocument.of(XmlReader.fromResource("/integration/$filePath"))
        val validator = WatchFaceValidator(WFF_SPECIFICATION)

        val result: ValidationResult = validator.getValidationResult(doc)
        val validVersions = result.validVersions
        val errorMapString =
            result.errorMap
                .map { (key, value) -> " VersionKey $key: $value" }
                .joinToString(separator = "\n", prefix = "{\n", postfix = "\n}")

        assertEquals(
            "Validation failed with errors: $errorMapString ",
            expectedValidVersions,
            validVersions,
        )
    }
}
