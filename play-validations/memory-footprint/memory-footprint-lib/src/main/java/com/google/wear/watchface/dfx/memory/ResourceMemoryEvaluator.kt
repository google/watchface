package com.google.wear.watchface.dfx.memory


object ResourceMemoryEvaluator {
    fun evaluateMemoryFootprint(watchFacePackage: InputPackage, evaluationSettings: EvaluationSettings): List<MemoryFootprint> {
        val watchFaceData =
            WatchFaceData.fromResourcesStream(
                watchFacePackage.getWatchFaceFiles(), evaluationSettings
            )
        return watchFaceData.watchFaceDocuments.map {
            WatchFaceLayoutEvaluator.evaluate(it, watchFaceData.resourceDetailsMap, evaluationSettings)
        }
    }
}