import com.mayabot.nlp.segment.Lexers

fun main() {
    val lexer = Lexers.coreBuilder()
            .withPersonName()
            .build()

    println(lexer.scan("俞正声主持召开全国政协第五十三次主席会议"))
}