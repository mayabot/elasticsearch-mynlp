import org.elasticsearch.common.xcontent.json.JsonXContent

fun main() {
        println(JsonXContent
            .contentBuilder().startObject().field("x","1").endObject()
            .prettyPrint()

            )
}