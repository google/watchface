# Watch Face Format specification

## Overview

The watch face format specifcation allows you to validate your watch faces and
operation of watch face generating tools. The specification is  
[provided as XSD files][xsd-files].

There are different versions of the specification, which have different
capabilities and compatibilities. Please see [this guide][wff-versions] for more
details.

## Format validator

In addition to the XSD files, a validator is provided that can be used to check
specific watch face XML files.

### Building the validator

```shell
cd third_party/wff
./gradlew :specification:validator:build
```

The resulting JAR file can then be found at: `specification/validator/build/libs/dwf-format-2-validator-1.0.jar`

### Usage

To check whether a watch face is valid, invoke the validator as follows:

```shell
java -jar dwf-format-2-validator-1.0.jar <format-version> <any options> <your-watchface.xml> <more-watchface.xml>
```

For example:

```shell
java -jar dwf-format-2-validator-1.0.jar 2 ~/MyWatchface/res/raw/watchface.xml
```

[xsd-files]: specification/documents/1
[wff-versions]: https://developer.android.com/training/wearables/wff/versions