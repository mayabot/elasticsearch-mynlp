package com.mayabot.mynlp.es

import org.elasticsearch.common.io.stream.StreamOutput
import org.elasticsearch.common.xcontent.ToXContent
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.search.SearchExtBuilder

const val SummaryExtName = "summary"

class SummarySearchExtBuilder(
    val field: String,
    val size: Int = 100
) : SearchExtBuilder() {

    override fun getWriteableName() = SummaryExtName

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params?): XContentBuilder {
        return builder.field("summary")
            .startObject()
            .field("field", field)
            .field("size", size)
            .endObject()
    }

    override fun writeTo(out: StreamOutput?) {
        out?.writeString(field)
        out?.writeInt(size)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SummarySearchExtBuilder

        if (field != other.field) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = field.hashCode()
        result = 31 * result + size
        return result
    }

}