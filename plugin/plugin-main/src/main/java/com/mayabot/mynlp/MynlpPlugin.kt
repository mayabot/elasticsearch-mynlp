package com.mayabot.mynlp

import com.mayabot.nlp.Mynlp
import org.apache.lucene.analysis.Analyzer
import org.elasticsearch.SpecialPermission
import org.elasticsearch.common.logging.Loggers
import org.elasticsearch.common.settings.Setting
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.index.analysis.AnalyzerProvider
import org.elasticsearch.index.analysis.TokenizerFactory
import org.elasticsearch.indices.analysis.AnalysisModule
import org.elasticsearch.plugins.AnalysisPlugin
import org.elasticsearch.plugins.Plugin
import org.elasticsearch.plugins.SearchPlugin
import java.nio.file.Path
import java.nio.file.Paths
import java.security.AccessController
import java.security.PrivilegedAction

class MynlpPlugin(
    val settings: Settings, val configPath: Path
) : Plugin(), SearchPlugin, AnalysisPlugin{

    private val mynlpServer:String

    private val mynlp: Mynlp

    private val dmsService:DmsService

    init {
        logger.info("start load MynlpManaPlugin ...")
        System.getSecurityManager()?.let {
            AccessController.checkPermission(SpecialPermission())
        }

        Mynlp.configer().setDataDir(
            Paths.get(configPath.toString(),"mynlp.data").toString()
        )

        Mynlp.configer().setCacheDir(
            Paths.get(System.getProperty("java.io.tmpdir"),"mynlp.temp").toString()
        )

        mynlpServer = ExtSettings.server.get(settings)

        this.dmsService = AccessController
            .doPrivileged(
                PrivilegedAction<DmsService>{
                    val mynlp = Mynlp.instance()
                    DmsService(HttpMynlpResourceService(mynlpServer),mynlp)
                })

        this.mynlp = dmsService.mynlp

        logger.info("Mynlp Plugin init ok")
    }

    override fun getTokenizers(): MutableMap<String, AnalysisModule.AnalysisProvider<TokenizerFactory>> {
        val extra =  HashMap<String, AnalysisModule.AnalysisProvider<TokenizerFactory>>()

        extra["mynlp"] = MynlpDmsAnalysisProvider(dmsService,::MynlpTokenizerFactory)
        extra["mynlp_index_atom"] = MynlpDmsAnalysisProvider(dmsService,::MynlpTokenizerFactory)
        extra["mynlp_index_overlap"] = MynlpDmsAnalysisProvider(dmsService,::MynlpTokenizerFactory)
        extra["mynlp_smart_atom"] = MynlpDmsAnalysisProvider(dmsService,::MynlpTokenizerFactory)
        extra["mynlp_smart_overlap"] = MynlpDmsAnalysisProvider(dmsService,::MynlpTokenizerFactory)

        return extra
    }

    override fun getAnalyzers(): MutableMap<String, AnalysisModule.AnalysisProvider<AnalyzerProvider<out Analyzer>>> {
        val extra =  HashMap<String, AnalysisModule.AnalysisProvider<AnalyzerProvider<out Analyzer>>>()

        extra["mynlp"] = MynlpDmsAnalysisProvider(dmsService,::MynlpAnalyzerProvider)
        extra["mynlp_index_atom"] = MynlpDmsAnalysisProvider(dmsService,::MynlpAnalyzerProvider)
        extra["mynlp_index_overlap"] = MynlpDmsAnalysisProvider(dmsService,::MynlpAnalyzerProvider)
        extra["mynlp_smart_atom"] = MynlpDmsAnalysisProvider(dmsService,::MynlpAnalyzerProvider)
        extra["mynlp_smart_overlap"] = MynlpDmsAnalysisProvider(dmsService,::MynlpAnalyzerProvider)

        return extra
    }

    /**
     * 返回插件自定义的配置项
     * 如果不明确指明settings，那么es启动时会校验报错
     */
    override fun getSettings(): MutableList<Setting<*>> {
        return ArrayList<Setting<*>>().apply {
            ExtSettings.server
        }
    }

    companion object{
        val logger = Loggers.getLogger(MynlpPlugin::class.java,"mynlp")
    }

    object ExtSettings {
        val server: Setting<String> = Setting.simpleString("mynlp.server", "", Setting.Property.NodeScope)
    }
}