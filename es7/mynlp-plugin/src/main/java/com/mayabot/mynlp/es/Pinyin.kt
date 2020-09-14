package com.mayabot.mynlp.es

import com.mayabot.nlp.pinyin.Pinyins
import com.mayabot.nlp.pinyin.split.PinyinSplits
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

/**
 * 人名、产品名，等短小的名字类型的数据。
 * 需要进行拼音分词。
 *
 * TODO 沪人社规〔2017〕43号 处理标点符号 offset丢失的情况
 */

fun main() {
    val x = Pinyins.convert("沪人社规〔2017〕43号")
        .asList()
    println(x)
}

class PinyinProvider(
    indexSettings: IndexSettings,
    environment: Environment,
    val name: String,
    settings: Settings
) :
    AbstractIndexAnalyzerProvider<Analyzer>(indexSettings, name, settings) {


    override fun get(): Analyzer {
        return PinyinAnalyzer(mode(name))
    }
}

class PinyinTokenizerFactory(
    indexSettings: IndexSettings,
    environment: Environment,
    val name: String,
    settings: Settings
) :
    AbstractTokenizerFactory(indexSettings, settings) {

    override fun create(): Tokenizer {
        return PinyinTokenizer(mode(name))
    }

}

private fun mode(name: String) = when (name) {
    "pinyin" -> 1
    "pinyin-fuzzy" -> 2
    "pinyin-head" -> 3
    "pinyin-stream" -> 4
    "pinyin-keyword" -> 5
    "pinyin-fuzzy-keyword" -> 6
    "pinyin-head-keyword" -> 7
    else -> 1
}


class PinyinAnalyzer(val mode: Int = 1) : Analyzer() {

    override fun createComponents(fieldName: String): TokenStreamComponents {
        return TokenStreamComponents(PinyinTokenizer(mode))
    }

}

class PinyinTokenizer(val mode: Int = 1) : Tokenizer() {

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

    val pinyinService = Pinyins.service()

    val pinyinSplitService = PinyinSplits.service


    /**
     * 返回下一个Token
     *
     * @return 是否有Token
     */
    override fun incrementToken(): Boolean {
        clearAttributes()

        if (buffer == null) {
            return false
        }

        val iter = buffer!!

        var pos = 1
        while (iter.isNotEmpty()) {
            val term = iter.pop()
            if (term?.word == null) {
                pos++
                continue
            }

            positionAttr.positionIncrement = pos
            termAtt.setEmpty().append(term.word)

            val len = if (mode == 4) {
                term.length()
            } else {
                1
            }

            offsetAtt.setOffset(term.offset, term.offset + len)
            return true
        }

        return false
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

        val ll = LinkedList<WordTerm>()

        when (mode) {
            1 -> {
                val re = pinyinService.text2Pinyin(this.input.readText())
                // 拼音
                re.asList().forEachIndexed { index, s ->
                    ll += WordTerm(s, Nature.x, index)
                }
            }
            2 -> {
                //模糊拼音
                val re = pinyinService.text2Pinyin(this.input.readText())
                re.fuzzy(true)
                // 拼音
                re.asList().forEachIndexed { index, s ->
                    ll += WordTerm(s, Nature.x, index)
                }
            }
            3 -> {
                //拼音头字母
                val re = pinyinService.text2Pinyin(this.input.readText())

                re.asHeadList().forEachIndexed { index, s ->
                    ll += WordTerm(if(s==null) null else "$s", Nature.x, index)
                }
            }
            4 -> {
                //拼音流
                val list = pinyinSplitService.split(this.input.readText())

                var offset = 0
                list.forEach { w ->
                    ll += WordTerm(w, Nature.x, offset)
                    offset += w.length
                }
            }
            5 -> {
                // pinyin-keyword
                val re = pinyinService.text2Pinyin(this.input.readText())
                ll += WordTerm(re.asString(""), Nature.x, 0)
            }
            6 -> {
                // pinyin-fuzzy-keyword
                val re = pinyinService.text2Pinyin(this.input.readText())
                re.fuzzy(true)
                ll += WordTerm(re.asString(""), Nature.x, 0)
            }
            7 -> {
                // pinyin-head-keyword
                val re = pinyinService.text2Pinyin(this.input.readText())
                ll += WordTerm(re.asHeadString(""), Nature.x, 0)
            }

        }


        buffer = ll
    }
}