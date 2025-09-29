// Class for the Scanner
class Scanner(private val source: String) {
    private val tokens = mutableListOf<Token>()
    private var start = 0
    private var current = 0
    private var line = 1

    // Keywords List
    private val keywords = setOf(
        "/function", "/kill", "/say", "/input", "/summon", "/expr",
        "/execute", "/gamerule", "/effect", "/team",
        "run", "else",
        "and", "or", "not", 
        "in", "is",
        "as", "break", "pass"
    )

    // Multiword Keywords List
    private val multiWordKeywords = listOf(
        "/execute if",
        "/execute for", 
        "/execute while",
        "not in",
        "is not"
    )

    // Scan Token function
    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }
        tokens.add(Token(TokenType.EOF, "", null, line))
        return tokens
    }

    private fun scanToken() {
        val c = advance()
        // placeholder: real scanning to come
        println("Scanned char: $c")
    }

    private fun isAtEnd(): Boolean = current >= source.length
    private fun advance(): Char = source[current++]


}


// Main function
fun main() {
    println("Loading...")
    println("Welcome to PyCraft (type '/kill' to exit)")

    while (true) {
        print("> ")
        val input = readln()

        if (input.trim() == "/kill") break

        val scanner = Scanner(input)
        val tokens = scanner.scanTokens()

        for (token in tokens) {
            println(token)
        }
    }
}
