package com.mayabot.mynlp.es

import com.mayabot.nlp.lucene.MynlpTokenizer
import com.mayabot.nlp.summary.SentenceSummary
import org.apache.lucene.analysis.Tokenizer
import org.elasticsearch.common.document.DocumentField
import org.elasticsearch.common.io.stream.StreamOutput
import org.elasticsearch.common.xcontent.ToXContent
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.json.JsonXContent
import org.elasticsearch.plugins.SearchPlugin
import org.elasticsearch.search.SearchExtBuilder
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.fetch.FetchSubPhase
import org.elasticsearch.search.internal.SearchContext
import java.awt.SystemColor.text
import java.security.AccessController
import java.security.PrivilegedAction


fun regSummarySearchExtBuilder() = SearchPlugin.SearchExtSpec<SummarySearchExtBuilder>(
    "summary", { input ->
        SummarySearchExtBuilder(
            input.readString(),
            input.readInt()
        )
    }, { parser ->
        val m = parser.map()
        SummarySearchExtBuilder(
            m.getOrDefault("field","content") as String,
            m.getOrDefault("size",100) as Int
            )
    }
)



class SummaryFetchSubPhase : FetchSubPhase {


    private val sentenceSummary by lazy {
         AccessController.doPrivileged(PrivilegedAction<SentenceSummary> {
             SentenceSummary()
        })

    }

    override fun hitExecute(context: SearchContext, hitContext: FetchSubPhase.HitContext) {
        val ext = context.getSearchExt("summary")  ?: return

        if(ext is SummarySearchExtBuilder){
            val source = context.lookup().source()


            val text = source.extractValue(ext.field)
            if (text is String && text.isNotEmpty()) {
                val out = sentenceSummary.summary(text,ext.size)

                var map = hitContext.hit().fieldsOrNull()
                if (map == null) {
                    map = hashMapOf()
                    hitContext.hit().fields(map)
                }
                map["summary-${ext.field}"] = DocumentField("summary-${ext.field}", listOf(out))

            }
        }
    }

}