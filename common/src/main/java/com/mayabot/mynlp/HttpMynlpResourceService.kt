package com.mayabot.mynlp

import com.mayabot.nlp.common.logging.InternalLoggerFactory
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


/**
 * 基于Restfull接口的MynlpDynamicResourceClient实现
 *
 * Restfull接口规范xxxx，是由maya nlp资源管理服务平台提供
 *
 * 该对象有缓存
 */
class HttpMynlpResourceService(
    mynlpServer: String
) {

    companion object {
        val logger = InternalLoggerFactory.getInstance(HttpMynlpResourceService::class.java)
    }

    private val baseServerUrl = mynlpServer.removeSuffix("/")

    fun loadWord(): List<String> {
        if (baseServerUrl.isBlank()) {
            return emptyList()
        }
        val text = request("/words")
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        return lines
    }

    //商品/和/服务
    fun loadCorrection(): List<CorrectionVO> {
        if (baseServerUrl.isBlank()) {
            return emptyList()
        }

        val text = request("/correction")
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        return lines.map { CorrectionVO(it) }
    }


    /**
     * /api/v1/nlpdata/core-dict
     *
     * response：
     * {
     *  data: [{word,operate}]
     * }
     */
    fun loadCoreDict(): List<CoreDictVO> {
//        val json = request("/api/v1/core_dict")
//        return JSON.parseObject(json,CoreDictDataVO::class.java)?.data?: listOf()
        return emptyList()
    }


    fun loadCoreDictBiGram(): List<CoreDictBiGramVO> {
//        val json = request("/api/v1/core_dict_bigram")
//        return JSON.parseObject(json,CoreDictBiGramDataVO::class.java)?.data?: listOf()
        return emptyList()
    }


    fun loadPinyinDict(): List<PinyinVO> {
//        val json = request("/api/v1/nlpdata/pinyin")
//        return JSON.parseObject(json, PinyinVOData::class.java)?.data ?: listOf()
        return emptyList()
    }


    /**
     * [endpoint] start with /
     */
    private fun request(endpoint: String, params: List<Pair<String, String>> = listOf()): String {
        var url = baseServerUrl + "/" + endpoint.removePrefix("/")

        if (params.isNotEmpty()) {
            url += params.joinToString(separator = "&") { "${it.first}=${it.second}" }
        }

        return httpGet(url)
    }

    private fun httpGet(url: String, timeout: Int = 5000): String {
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

    /**
     * text = 朝朝盈
     * pinyin = zhao,zhao,yin
     */
    data class PinyinVO(
        val text: String,
        val pinyin: String
    )

    /**
     * text = 商品/和/服务
     */
    data class CorrectionVO(
        val rule: String
    )

    data class CoreDictBiGramVO(
        val wordA: String,
        val wordB: String,
        val count: Int = 100
    )

    data class CoreDictVO(

        val word: String,
        /**
         * ADD | DELETE
         */
        val operate: String,

        val count: Int
    )


}