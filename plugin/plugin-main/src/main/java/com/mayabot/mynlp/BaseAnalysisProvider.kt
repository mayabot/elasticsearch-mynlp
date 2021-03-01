package com.mayabot.mynlp

import org.elasticsearch.common.settings.Settings
import org.elasticsearch.env.Environment
import org.elasticsearch.index.IndexSettings
import org.elasticsearch.indices.analysis.AnalysisModule.*

class MynlpDmsAnalysisProvider<T>(
    val dmsManager: DmsService,
    val blocker: (DmsService, IndexSettings, Environment, String, Settings) -> T
) : AnalysisProvider<T> {

    override fun get(
        indexSettings: IndexSettings,
        environment: Environment,
        name: String,
        settings: Settings
    ): T {
        return blocker(dmsManager, indexSettings, environment, name, settings)
    }
}