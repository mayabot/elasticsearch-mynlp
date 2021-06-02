package com.mayabot.mynlp

import com.mayabot.mynlp.HttpMynlpResourceService.CorrectionVO
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


class DmsService(
    private val client: HttpMynlpResourceService,
     val mynlp: Mynlp
) {

    private val customDictionary = MemCustomDictionary()

    private val correctionDictionary = MemCorrectionDictionary()

    private val stopDict = DefaultStopWordDict(true)

    /**
     * 基于词典的分词器。对外提供编程方式的分词服务访问
     */
    val innerLexer: Lexer = buildInnerPos(false)

    val innerLexerPos: Lexer = buildInnerPos(true)

    private fun buildInnerPos(doPos: Boolean): Lexer {
        val builder = mynlp.lexerBuilder()
            .hmm()
            .withCorrection(correctionDictionary)
            .withCustomDictionary(customDictionary)
            .withPersonName()
        if (doPos){
            builder.withPos()
        }

        val collector = builder.collector()

        collector.fillSubwordCustomDict(customDictionary)

        collector.smartPickup{ spup->
            spup.setBlackListCallback {word->
                word[0] == '副' && word[word.length - 1] == '长'
            }
        }.done()


        return builder.build()
    }

    internal fun init() {
        // 加载自定义词典，监听如果变化，那么自动重载
        loadCustomDictionary(client.loadWord())
        loadCorrectionDictionary(client.loadCorrection())

        Thread(Runnable {

            try {
                Thread.sleep(60*1000*5)
                // 最简单的实现
                loadCustomDictionary(client.loadWord())
                loadCorrectionDictionary(client.loadCorrection())

            } catch (e: Exception) {

            }
        }).apply {
            isDaemon = true
            this.priority = Thread.MIN_PRIORITY + 2
        }.start()

//        client.listenWord(scope) {
//            logger.info("Notify: Word change scope:$scope")
//            bizWordLibService.rebuild(client.loadWord(scope))
//            loadCustomDictionary()
//            loadStopword()
//            logger.info("业务词库加载完成 $scope")
//            client.postEvent(node,MynlpConfigModule.word,"业务词库加载完成",scope)
//        }
//
//        client.listenCorrection(scope) {
//            logger.info("Notify: Correction change scope:$scope")
//            loadCorrectionDictionary()
//            logger.info("分词纠错加载完成 $scope")
//            client.postEvent(node,MynlpConfigModule.correction,"分词纠错加载完成",scope)
//        }
    }

    /**
     * 创建MynlpTokenizer
     */
    fun createTokenizer(options: LexerOptions, iterMode: WordTermIterableMode): MynlpTokenizer {
        return MynlpTokenizer(buildLexer(options),iterMode)
    }

    /**
     * 创建个性化的HMM分词器
     */
    fun buildLexer(
        options: LexerOptions = LexerOptions.DEFAULT
    ): LexerReader {
        val builder = mynlp.lexerBuilder()

        when(options.lexerType){
            LexerOptions.LexerType.HMM ->{
                builder.hmm()
            }
            LexerOptions.LexerType.PERCEPTRON ->{
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
            if (lexerType == LexerOptions.LexerType.HMM && personName) {
                builder.withPersonName()
            }


            when (subWord) {
                LexerOptions.SubWordMode.none ->{

                }
                LexerOptions.SubWordMode.smart ->{
                    val collector = builder.collector()
                    if (lexerType == LexerOptions.LexerType.PERCEPTRON) {
                        collector.fillSubwordDict()
                    }
                    if (customWord) {
                        collector.fillSubwordCustomDict(customDictionary)
                    }
                    collector.smartPickup{ spup->
                        spup.setBlackListCallback {word->
                            word[0] == '副' && word[word.length - 1] == '长'
                        }
                    }.done()
                }
                LexerOptions.SubWordMode.index ->{
                    //TODO 最小切分长度还可以再设置
                    val collector = builder.collector()
                    if (lexerType == LexerOptions.LexerType.PERCEPTRON) {
                        collector.fillSubwordDict()
                    }
                    if (customWord) {
                        collector.fillSubwordCustomDict(customDictionary)
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

    private fun loadCustomDictionary(words:List<String>) {
        val coreDict = mynlp.getInstance<CoreDictionary>()
        customDictionary.clear()
        words.forEach {
            if (!coreDict.contains(it)) {
                customDictionary.addWord(it)
            }
        }
        customDictionary.rebuild()
    }


    private fun loadCorrectionDictionary(data:List<CorrectionVO> ) {
        correctionDictionary.clear()
        data.forEach {
            correctionDictionary.addRule(it.rule)
        }
        correctionDictionary.rebuild()
    }

}