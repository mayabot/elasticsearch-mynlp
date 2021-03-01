package com.mayabot.mynlp

import com.alibaba.fastjson.JSON
import com.mayabot.mynlp.MynlpResourceService.*
import com.mayabot.nlp.common.logging.InternalLoggerFactory
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.net.SocketTimeoutException

/**
 * 基于Restfull接口的MynlpDynamicResourceClient实现
 *
 * Restfull接口规范xxxx，是由maya nlp资源管理服务平台提供
 *
 * 该对象有缓存
 */
class HttpMynlpResourceService(
    mynlpServer:String
) : MynlpResourceService, Runnable {

    companion object {
        val logger = InternalLoggerFactory.getInstance(HttpMynlpResourceService::class.java)
    }

    private var thread: Thread? = null

    private var threadRunFlag = true

    private val listenPointMap = ConcurrentHashMap<ListenPointKey,ListenPoint>()

    private val baseServerUrl = mynlpServer.removeSuffix("/")

    init {
        startListenThread()
    }

    /**
     * /api/v1/nlpdata/core-dict
     *
     * response：
     * {
     *  data: [{word,operate}]
     * }
     */
    override fun loadCoreDict(): List<CoreDictVO> {
        val json = request("/api/v1/core_dict")
        return JSON.parseObject(json,CoreDictDataVO::class.java)?.data?: listOf()
    }


    override fun loadCoreDictBiGram(): List<CoreDictBiGramVO> {
        val json = request("/api/v1/core_dict_bigram")
        return JSON.parseObject(json,CoreDictBiGramDataVO::class.java)?.data?: listOf()
    }

    override fun loadWord(): List<BizWordVO> {
        val json = request("/api/v1/nlpdata/biz-dict")
        val data =  JSON.parseObject(json, BizWordVOData::class.java)?.data ?: listOf()
        return data
    }

    override fun loadCorrection(): List<CorrectionVO> {
        val json = request("/api/v1/nlpdata/correction")
        val data =  JSON.parseObject(json, CorrectionVOData::class.java)?.data ?: listOf()
        return data
    }

    override fun loadPinyinDict(): List<PinyinVO> {
        val json = request("/api/v1/nlpdata/pinyin")
        return JSON.parseObject(json, PinyinVOData::class.java)?.data ?: listOf()
    }

    /**
     * 后台线程触发监听
     */
    override fun run() {
        // 注意如果一个模块没有加载过数据，那么永远不会触发监听器
        while (threadRunFlag) {
            try {

                val query = listenPointMap.map { (key,value)->
                    //module:version
                    "${key.module}:${value.version}"
                }.joinToString(separator = ",")

                // /version/watch?query=
                try {
                    val res = httpGet("/api/v1/version/watch?query=${query}",0)
                    val watchResult = JSON.parseObject(res,WatchApiResult::class.java)

                    if (watchResult.success && watchResult.data.isNotEmpty()) {
                        for (vo in watchResult.data) {
                            val xx = listenPointMap[ListenPointKey(vo.module)]
                            if(xx!=null){
                                xx.version = vo.version
                                for (listener in xx.listeners()) {
                                    try {
                                        listener()
                                    } catch (e: Exception) {
                                        logger.error("callback for $vo",e)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: SocketTimeoutException){
                    // 每隔N秒后会服务器端口会超时
                }


                // sleep one minute
                Thread.sleep(60 * 1000)


            } catch (e: Exception) {
                logger.error("",e)
            }
        }
    }

    private data class CoreDictDataVO(
        val data:List<CoreDictVO>,
        val success:Boolean
    )

    private data class CoreDictBiGramDataVO(
        val data:List<CoreDictBiGramVO>,
        val success:Boolean
    )

    private data class BizWordVOData(
        val data:List<BizWordVO>,
        val success:Boolean
    )

    private data class CorrectionVOData(
        val data:List<CorrectionVO>,
        val success:Boolean
    )

    data class PinyinVOData(
        val data:List<PinyinVO>,
        val success:Boolean
    )

    override fun listenCoreDict(callback: () -> Unit){
        this.listen("core_dict",callback)
    }

    override fun listenCoreDictGiGram(callback: () -> Unit){
        this.listen("core_dict_bigram",callback)
    }

    override fun listenCorePinyin(callback: () -> Unit){
        this.listen("pinyin",callback)
    }

    override fun listenWord(callback: () -> Unit){
        this.listen("word",callback)
    }

    override fun listenCorrection(callback: () -> Unit){
        this.listen("correction",callback)
    }

    override fun postEvent(node: String, event: String) {
        //request("/api/v1/event_log", listOf("node" to node,"event" to event))
    }

    private fun listen(module: String, callback: () -> Unit) {
        listenPointMap.getOrPut(ListenPointKey(module)){
            val version = dataVersion(module)
            ListenPoint(version)
        }.add(callback)
    }

    private data class ListenPointKey(
        val module: String
    )

    private class ListenPoint(
        var version:String
    ){
        private val listener = ArrayList<()->Unit>()
        fun add(callback: () -> Unit){
            listener += callback
        }
        fun listeners() = listener.toList()
    }



    private data class WatchApiResult(
        val data:List<VersionVO>,
        val success:Boolean = true
    )

    private data class VersionVO(
        val module: String,
        /**
         * 如果没有scope，如拼音，那么scope为空字符串
         */
        val scope: String,
        val version: String
    )

    private fun dataVersion(module: String,scope:String = ""): String {
        return request("/api/v1/version", listOf("module" to module,"scope" to scope))
    }

    /**
     * [endpoint] start with /
     */
    private fun request(endpoint: String,params:List<Pair<String,String>> = listOf()):String {
        var url = baseServerUrl
        if(!endpoint.startsWith("/")){
            url += "/"
        }
        url += endpoint

        if (params.isNotEmpty()) {
            url += params.joinToString(separator = "&") { "${it.first}=${it.second}" }
        }

        return httpGet(url)
    }

    private fun httpGet(url: String,timeout:Int = 5000): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = timeout
        conn.connect()

        return try {
            val resCode = conn.responseCode

            if (resCode != 200) {
                throw IOException("io exception for url $url")
            }

            conn.inputStream.bufferedReader(Charsets.UTF_8)
                .readText()
        } finally {
            conn.disconnect()
        }
    }



    private fun startListenThread() {
        if (this.thread != null) {
            return
        }
        this.thread = Thread(this).apply {
            isDaemon = true
            this.priority = Thread.MIN_PRIORITY + 2
        }
    }

}