package com.mayabot.mynlp

/**
 * 对接标准服务
 */
interface MynlpResourceService {

    fun loadPinyinDict(): List<PinyinVO>

    fun loadCoreDict(): List<CoreDictVO>

    fun loadCoreDictBiGram(): List<CoreDictBiGramVO>

    fun loadWord(): List<BizWordVO>

    fun loadCorrection(): List<CorrectionVO>

    fun listenCoreDict(callback: () -> Unit)
    fun listenCoreDictGiGram(callback: () -> Unit)
    fun listenCorePinyin(callback: () -> Unit)
    fun listenWord(callback: () -> Unit)
    fun listenCorrection(callback: () -> Unit)

    fun postEvent(node:String,event:String)

    /**
     * 业务词库
    */
    data class BizWordVO(
        val word:String,
//        val synonyms:List<String>,
//        val weight:Int,
//        val set:Boolean = true
    )

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

