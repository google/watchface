package com.google.wear.watchface.dfx.memory;

import static com.google.wear.watchface.dfx.memory.DrawableResourceDetails.findInMap;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.findSceneNode;

import org.w3c.dom.Document;

import java.util.Map;

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

    long computeAmbientMemoryFootprint() {
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
                        (res) -> findInMap(resourceMemoryMap, res).getTotalFootprintBytes())
                .calculateMaxFootprintBytes();
    }
}
