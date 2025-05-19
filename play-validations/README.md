# Memory Footprint Evaluator

A program that evaluates the memory footprint of the assets in a [Watch Face Format][wff]
(WFF) package.

## Purpose

Watch faces running on Wear OS should be memory efficient so as to ensure the
best experience for the user. To this end, the Memory Footprint Evaluator checks
how much memory a given watch face package might use. These checks are also
conducted by Google Play in reviewing submissions to the Play store.

## How it works

The memory footprint of a drawable asset is defined as 4 times the number of
pixels in the image times the number of frames in the asset (for GIF, WEBP). We
typically need 4 bytes to represent a single pixel in memory (one for each of
the RGBA channels).

The memory footprint of a watch face is the sum of the memory footprints of all
images that are active at any point on a watch face. Some images are mutually
exclusive, for example some are rendered only when a specific `UserStyleSetting`
is set or the watch face is in Active or Ambient mode. This is just an estimate,
or an upper limit for the memory footprint because some images are conditionally
rendered based on expressions. For now, we only take into account user settings
and variants.

## Building the tool

```bash
./gradlew :memory-footprint:jar
java -jar ./memory-footprint/build/libs/memory-footprint.jar --help
```

## Example usage

No guarantee is made that the following exactly corresponds to the settings used
in Google Play reviews, but these represent reasonable settings for evaluation:

```shell
java -jar ./memory-footprint.jar --watch-face MyWatchFace.apk \
  --schema-version 4 \
  --ambient-limit-mb 10 \
  --active-limit-mb 100 \
  --apply-v1-offload-limitations \
  --estimate-optimization
```

[wff]:  https://developer.android.com/training/wearables/wff
