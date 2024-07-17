Watch Face Format Optimizer

This tool applies the following optimizations, designed to reduce the runtime
memory footprint of an Android Watch Face Format watch face:

1. Crops and resizes BitmapFonts, adding margins to the Character
   tag to ensure alignment.

2. Crops and resizes PartImage nodes, adjusting the pivot if
   needed.

3. Attempts to quantize images to RGB565 where there will be no
   noticeable loss of fidelity.

4. De-duplicates image resources after cropping, etc...

Usage: wff_optimizer.jar --source PATH/TO/UNZIPPED/APK

That will run the optimizations in place. You can add --verbose to observe what
the tool is doing.