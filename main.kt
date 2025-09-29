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


enum class TokenType {
    // Single-character tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, STAR, PERCENT,
    AMPERSAND, PIPE, CARET, TILDE,

    // Multi-character operators
    DOLLAR, DOLLAR_DOLLAR, STAR_STAR,
    EQUAL, EQUAL_EQUAL,
    BANG, BANG_EQUAL,
    GREATER, GREATER_EQUAL, RIGHT_SHIFT,
    LESS, LESS_EQUAL, LEFT_SHIFT,
    
    // Compound assignments
    PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, 
    DOLLAR_EQUAL, PERCENT_EQUAL, STAR_STAR_EQUAL,

    // Literals
    IDENTIFIER, STRING, NUMBER,

    // Keywords
    KEYWORD,

    // Comments
    COMMENT,

    EOF
}

data class Token(
    val type: TokenType,
    val lexeme: String,
    val literal: Any?,
    val line: Int
) {
    override fun toString(): String =
        "Token(type=$type, lexeme='$lexeme', literal=$literal, line=$line)"
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

