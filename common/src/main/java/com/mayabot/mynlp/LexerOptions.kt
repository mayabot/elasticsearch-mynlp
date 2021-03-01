package com.mayabot.mynlp

data class LexerOptions(

    val pos: Boolean = false,
    val correction: Boolean = false,
    val customWord: Boolean = true,
    val personName: Boolean = false,
    val filterPunctuation: Boolean = true,
    val filterStopWord: Boolean = false,
    /**
     * none smart index
     */
    val subWord: SubWordMode = SubWordMode.none,
    val lexerType: LexerType = LexerType.HMM
){
    companion object{
        val DEFAULT = LexerOptions()
    }

    enum class LexerType {
        HMM, PERCEPTRON
    }

    enum class SubWordMode{
        /**
         * 不切子词
         */
        none,

        /**
         * 选取最合理的一条切分路径
         */
        smart,

        /**
         * 索引分词的切分方式，解出所有的可能性
         */
        index
    }
}