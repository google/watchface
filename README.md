# Watch Face Format

[Watch Face Format][wff] (WFF) is an XML-based format for defining the appearance and
behavior of watch faces for Wear OS.

This repository contains materials for helping developers work with the Watch
Face Format to build both watch faces and watch face design tools.

## XSD specification

The [XSD specification][xsd-specs] provides you with the specification needed in
order to build validation into your watch face creation tools and processes.

## XSD Validator

The [XSD validator][xsd-validator] is a tool that allows you to check whether
specific watch face XML file represent valid WFF or not, including providing
error information to assist in debugging the watch face.

## Memory footprint

When used on Wear OS devices, watch faces built with WFF must pass a memory-use
validation test to ensure that they run efficiently. For more guidance on this
subject, see the [guidelines on developer.android.com][wff-optimize].

The [Memory footprint evaluator][memory-footprint] allows you to check watch
faces ahead of submission to Play or to incorporate memory usage checking into
your tools and processes.

## Samples

For WFF samples, please see the [Wear OS Samples repository][samples] on GitHub.

## License

Watch Face Format is distributed under the Apache 2.0 license, see the
[LICENSE][license] file.

[license]: LICENSE.txt
[memory-footprint]: play-validations
[wff-optimize]: https://developer.android.com/training/wearables/wff/memory-usage
[wff]: https://developer.android.com/training/wearables/wff/
[samples]: https://github.com/android/wear-os-samples/tree/main/WatchFaceFormat
[xsd-specs]: third_party/wff/specification/documents/1/
[xsd-validator]: third_party/wff/README.md