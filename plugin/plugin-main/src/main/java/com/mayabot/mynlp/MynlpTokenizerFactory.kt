package com.mayabot.mynlp

import com.mayabot.nlp.module.lucene.MynlpAnalyzer
import com.mayabot.nlp.module.lucene.MynlpTokenizer
import com.mayabot.nlp.segment.WordTermIterableMode
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.Tokenizer
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.env.Environment
import org.elasticsearch.index.IndexSettings
import org.elasticsearch.index.analysis.AbstractIndexAnalyzerProvider
import org.elasticsearch.index.analysis.AbstractTokenizerFactory
import java.security.AccessController
import java.security.PrivilegedAction

/**
 * @author jimichan
 */
class MynlpTokenizerFactory(
    val service: DmsService,
    indexSettings: IndexSettings,
    environment: Environment,
    name: String,
    /**
     * 分析器的配置信息
     */
    settings: Settings
) :
    AbstractTokenizerFactory(indexSettings,"", settings) {

    private val optionsAndMode = parseSetting2Options(name,settings)

    private val options = parseSetting2Options(name,settings)

    override fun create(): Tokenizer {

        return AccessController.doPrivileged(PrivilegedAction<Tokenizer> {
            MynlpTokenizer(service.buildLexer(optionsAndMode.first),optionsAndMode.second)
        })
    }
}

class MynlpAnalyzerProvider(
    val service: DmsService,
    indexSettings: IndexSettings,
    environment: Environment,
    name: String,
    settings: Settings
) :
    AbstractIndexAnalyzerProvider<Analyzer>(indexSettings, name, settings) {

    private val optionsAndMode = parseSetting2Options(name,settings)

    override fun get(): Analyzer {
        return AccessController.doPrivileged(PrivilegedAction<Analyzer> {
            MynlpAnalyzer(
                service.buildLexer(optionsAndMode.first),optionsAndMode.second
            )
        })
    }
}

/**
 * name:
 * mynlp
 * mynlp_index_atom
 * mynlp_index_overlap
 * mynlp_smart_atom
 * mynlp_smart_overlap
 *
 */
fun parseSetting2Options(name:String,settings: Settings): Pair<LexerOptions,WordTermIterableMode> {
    var lexerType: LexerOptions.LexerType = LexerOptions.LexerType.HMM
    var personName: Boolean = false;
    /**
     * none smart index
     */
    var subWord: String = "none";


    val  isCorrection: Boolean = settings.getAsBoolean("correction", true)
    val filterPunctuaction = settings.getAsBoolean("filter-punctuation", true)
    val filterStopword = settings.getAsBoolean("filter-stopword", false)

//    if(settings.hasValue("lexer")){
//        lexerType = when (settings.get("lexer").toLowerCase()) {
//            "core" -> BIGRAM
//            "bigram" -> BIGRAM
//            "perceptron" -> PERCEPTRON
//            "cws" -> PERCEPTRON
//            else -> {
//                BIGRAM
//            }
//        }
//    }

    if(settings.hasValue("sub-word")){
        subWord = settings.get("sub-word", subWord)
    }else{
        if(name.contains("_smart")){
            subWord = "smart"
        }else if(name.contains("_index")){
            subWord = "index"
        }
    }

    var mode: WordTermIterableMode = WordTermIterableMode.TOP;

    if(settings.hasValue("mode")){
        mode = when (settings.get("mode", "top").toLowerCase()) {
            "top" -> WordTermIterableMode.TOP
            "atom" -> WordTermIterableMode.ATOM
            "overlap" -> WordTermIterableMode.Overlap
            else -> WordTermIterableMode.TOP
        }
    }else{
        if(name.contains("_atom")){
            mode = WordTermIterableMode.ATOM
        }else if (name.contains("_overlap")) {
            mode = WordTermIterableMode.Overlap
        }
    }

    personName =
        settings.getAsBoolean("person-name",settings.getAsBoolean("personName", true))

    return LexerOptions(pos=false,correction = isCorrection,
        customWord = true,personName=personName,
        filterPunctuation = filterPunctuaction,
        filterStopWord = filterStopword,
        subWord = LexerOptions.SubWordMode.valueOf(subWord),
        lexerType = lexerType
    ) to mode
}