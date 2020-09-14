package com.mayabot.mynlp.es

import com.mayabot.nlp.segment.Nature
import com.mayabot.nlp.segment.WordTerm
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.Tokenizer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.env.Environment
import org.elasticsearch.index.IndexSettings
import org.elasticsearch.index.analysis.AbstractIndexAnalyzerProvider
import org.elasticsearch.index.analysis.AbstractTokenizerFactory
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

/**
 * 中文发文字号
 * 连续的中文或者数字
 */

val IssueNumPattern = Pattern.compile("([\\u4E00-\\u9FA5\\uFE30-\\uFFA0)]+|\\d+)")!!

private fun parseIssueNum(text: String): LinkedList<WordTerm> {
    val mat = IssueNumPattern.matcher(text)
    val re = LinkedList<WordTerm>()
    while (mat.find()) {
        val name = mat.group()
        val from = mat.start()
        re += WordTerm(name, Nature.x, from)
    }
    return re
}

class ChineseIssueNumProvider(
    indexSettings: IndexSettings,
    environment: Environment,
    name: String,
    settings: Settings
) :
    AbstractIndexAnalyzerProvider<Analyzer>(indexSettings, name, settings) {

    override fun get(): Analyzer {
        return ChineseIssueNumAnalyzer()

    }
}

class ChineseIssueTokenizerFactory(
    indexSettings: IndexSettings,
    environment: Environment,
    name: String,
    settings: Settings
) :
    AbstractTokenizerFactory(indexSettings, settings) {

    override fun create(): Tokenizer {
        return ChineseIssueNumTokenizer()
    }

}


class ChineseIssueNumAnalyzer : Analyzer() {

    override fun createComponents(fieldName: String): TokenStreamComponents {
        return TokenStreamComponents(ChineseIssueNumTokenizer())
    }

}

class ChineseIssueNumTokenizer : Tokenizer() {

    /**
     * 当前词
     */
    private val termAtt = addAttribute(CharTermAttribute::class.java)

    /**
     * 偏移量
     */
    private val offsetAtt = addAttribute(OffsetAttribute::class.java)

    /**
     * Position Increment
     */
    private val positionAttr = addAttribute(PositionIncrementAttribute::class.java)


    //    private final PositionLengthAttribute positionLenAttr = addAttribute(PositionLengthAttribute.class);

    private var buffer: LinkedList<WordTerm>? = null


    /**
     * 返回下一个Token
     *
     * @return 是否有Token
     */
    override fun incrementToken(): Boolean {
        clearAttributes()

        val iter = buffer

        return if (iter == null || iter.isEmpty()) {
            false
        } else {
            val term = iter.pop()
            if (term == null) {
                false
            } else {
                positionAttr.positionIncrement = 1
                termAtt.setEmpty().append(term.word)
                offsetAtt.setOffset(term.offset, term.offset + term.length())
                true
            }
        }
    }


    /**
     * This method is called by a consumer before it begins consumption using
     * [.incrementToken].
     *
     *
     * Resets this stream to a clean state. Stateful implementations must implement
     * this method so that they can be reused, just as if they had been created fresh.
     *
     *
     * If you override this method, always call `super.reset()`, otherwise
     * some internal state will not be correctly reset (e.g., [Tokenizer] will
     * throw [IllegalStateException] on further usage).
     */
    @Throws(IOException::class)
    override fun reset() {
        super.reset()
        buffer = parseIssueNum(this.input.readText())
    }

}