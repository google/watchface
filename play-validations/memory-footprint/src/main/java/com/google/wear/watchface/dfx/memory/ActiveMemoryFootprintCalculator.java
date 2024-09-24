package com.google.wear.watchface.dfx.memory;

import static com.google.wear.watchface.dfx.memory.DrawableResourceDetails.findInMap;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.findSceneNode;

import java.util.Map;
import org.w3c.dom.Document;

/** Computes the memory footprint of a watch face in active. */
public class ActiveMemoryFootprintCalculator {
    private final VariantConfigValue activeConfigValue;
    private final Document document;
    private final Map<String, DrawableResourceDetails> resourceMemoryMap;
    private final EvaluationSettings evaluationSettings;

    ActiveMemoryFootprintCalculator(
            Document document,
            Map<String, DrawableResourceDetails> resourceMemoryMap,
            EvaluationSettings evaluationSettings) {
        this.activeConfigValue = VariantConfigValue.active(evaluationSettings);
        this.document = document;
        this.resourceMemoryMap = resourceMemoryMap;
        this.evaluationSettings = evaluationSettings;
    }

    long computeActiveMemoryFootprint() {
        if (evaluationSettings.isVerbose()) {
            System.out.println(">> Starting active evaluation");
        }
        DrawableNodeConfigTable drawableNodeConfigTable =
                DrawableNodeConfigTable.create(findSceneNode(document), activeConfigValue);
        WatchFaceResourceCollector resourceCollector =
                new WatchFaceResourceCollector(document, resourceMemoryMap, evaluationSettings);
        return new DynamicNodePerConfigurationFootprintCalculator(
                        document,
                        evaluationSettings,
                        activeConfigValue,
                        resourceCollector,
                        drawableNodeConfigTable,
                        this::evaluateResource)
                .calculateMaxFootprintBytes();
    }

    private long evaluateResource(String resource) {
        DrawableResourceDetails details = findInMap(resourceMemoryMap, resource);
        long imageBytes = details.getTotalFootprintBytes();
        if (evaluationSettings.isVerbose()) {
            System.out.printf(
                    "Counting resource %s; %s bytes, %s mb, %s x %s %s%n",
                    resource,
                    details.getBiggestFrameFootprintBytes(),
                    ((double) imageBytes) / 1024 / 1024,
                    details.getWidth(),
                    details.getHeight(),
                    details.canUseRGB565() ? "RGB565" : "ARGB8888");
        }
        return imageBytes;
    }
}
