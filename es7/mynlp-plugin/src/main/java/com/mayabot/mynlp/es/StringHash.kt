package com.mayabot.mynlp.es

import com.google.common.hash.Hashing
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

class StringHashProvider(
    indexSettings: IndexSettings,
    environment: Environment,
    name: String,
    settings: Settings
) :
    AbstractIndexAnalyzerProvider<Analyzer>(indexSettings, name, settings) {

    override fun get(): Analyzer {
        return StringHashAnalyzer()
    }
}

class StringHashTokenizerFactory(
    indexSettings: IndexSettings,
    environment: Environment,
    name: String,
    settings: Settings
) :
    AbstractTokenizerFactory(indexSettings, settings) {

    override fun create(): Tokenizer {
        return StringHashTokenizer()
    }

}


class StringHashAnalyzer : Analyzer() {

    override fun createComponents(fieldName: String): TokenStreamComponents {
        return TokenStreamComponents(StringHashTokenizer())
    }

}

class StringHashTokenizer : Tokenizer() {

    /**
     * 当前词
     */
    private val termAtt = addAttribute(CharTermAttribute::class.java)

    private var computed = false

    /**
     * 返回下一个Token
     *
     * @return 是否有Token
     */
    val hasher = Hashing.murmur3_128()

    override fun incrementToken(): Boolean {
        clearAttributes()

        return if(computed){
            false
        } else {
            val c = hasher.newHasher()
            this.input.forEachLine { line->
                c.putString(line,Charsets.UTF_8)
            }

            termAtt.setEmpty().append(c.hash().toString())
            computed = true
            true
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
    }

}