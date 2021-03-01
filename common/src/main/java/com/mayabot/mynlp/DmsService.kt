package com.mayabot.mynlp

import com.mayabot.nlp.Mynlp
import com.mayabot.nlp.module.lucene.MynlpTokenizer
import com.mayabot.nlp.segment.Lexer
import com.mayabot.nlp.segment.LexerReader
import com.mayabot.nlp.segment.Sentence
import com.mayabot.nlp.segment.WordTermIterableMode
import com.mayabot.nlp.segment.lexer.bigram.CoreDictionary
import com.mayabot.nlp.segment.plugins.correction.MemCorrectionDictionary
import com.mayabot.nlp.segment.plugins.customwords.MemCustomDictionary
import com.mayabot.nlp.segment.reader.DefaultStopWordDict

/**
 * 每个命名空间都会对应一个实例
 */
class DmsService(
    private val client: MynlpResourceService,
    private val mynlp: Mynlp
) {

    private val customDictionary = MemCustomDictionary()

    private val correctionDictionary = MemCorrectionDictionary()

    private val stopDict = DefaultStopWordDict(true)

    /**
     * 基于词典的分词器。对外提供编程方式的分词服务访问
     */
    val innerLexer: Lexer = mynlp.lexerBuilder()
        .hmm()
        .withCorrection(correctionDictionary)
        .withCustomDictionary(customDictionary)
        .withPersonName().build()

    val innerLexerPos: Lexer = mynlp.lexerBuilder()
        .hmm()
        .withPos()
        .withCorrection(correctionDictionary)
        .withCustomDictionary(customDictionary)
        .withPersonName().build()

    fun init() {
        // 加载自定义词典，监听如果变化，那么自动重载
        // bizWordLibService.rebuild(client.loadWord())
        client.loadWord().map { it.word }

        loadCustomDictionary()

        loadCorrectionDictionary()

//        loadStopword()

        client.listenWord() {
            loadCustomDictionary()
//            loadStopword()
        }

        client.listenCorrection() {
            loadCorrectionDictionary()
        }
    }

    /**
     * 创建个性化的HMM分词器
     */
    fun buildLexer(
        options:LexerOptions = LexerOptions.DEFAULT
    ): LexerReader {
        val builder = mynlp.lexerBuilder()

        when(options.lexerType){
            LexerOptions.LexerType.HMM ->{
                builder.hmm()
            }
            LexerOptions.LexerType.PERCEPTRON->{
                builder.perceptron()
            }
        }

        with(options) {
            if(pos){
                builder.withPos()
            }
            if (correction) {
                builder.withCorrection(correctionDictionary)
            }
            if (customWord) {
                builder.withCustomDictionary(customDictionary)
            }
            if (lexerType ==LexerOptions.LexerType.HMM && personName) {
                builder.withPersonName()
            }

            when (subWord) {
                LexerOptions.SubWordMode.none ->{

                }
                LexerOptions.SubWordMode.smart->{
                    val collector = builder.collector()
                    if (lexerType == LexerOptions.LexerType.PERCEPTRON) {
                        collector.fillSubwordDict()
                    }
                    collector.smartPickup{ spup->
                        spup.setBlackListCallback {word->
                            word[0] == '副' && word[word.length - 1] == '长'
                        }
                    }.done()
                }
                LexerOptions.SubWordMode.index->{
                    //TODO 最小切分长度还可以再设置
                    val collector = builder.collector()
                    if (lexerType == LexerOptions.LexerType.PERCEPTRON) {
                        collector.fillSubwordDict()
                    }
                    collector.indexPickup().done()
                }
            }

            val lexer = builder.build()

            return if(filterStopWord){
                LexerReader.filter(lexer,filterPunctuation,stopDict)
            }else{
                LexerReader.filter(lexer,filterPunctuation,false)
            }
        }
    }

    fun segment(text: String): Sentence {
        return innerLexer.scan(text)
    }

    fun isStopWord(word: String): Boolean {
        return stopDict.contains(word)
    }

    private fun loadCustomDictionary() {
        val coreDict = mynlp.getInstance<CoreDictionary>()
        customDictionary.clear()
        client.loadWord().forEach {
            if (!coreDict.contains(it.word)) {
                customDictionary.addWord(it.word)
            }
        }
        customDictionary.rebuild()
    }

//    private fun loadStopword() {
//        val now = stopDict.reset().toList()
//        // 如果业务词库中规定了词是非停用词，那么需要执行remove动作
//        for (s in now) {
//            bizWordLibService.row(s)?.let {
//                if (it.weight > 0) {
//                    stopDict.remove(s)
//                }
//            }
//        }
//        stopDict.add(bizWordLibService.stopwords())
//        stopDict.rebuild()
//    }

    private fun loadCorrectionDictionary() {
        val data = client.loadCorrection()
        correctionDictionary.clear()
        data.forEach {
            correctionDictionary.addRule(it.rule)
        }
        correctionDictionary.rebuild()
    }


}