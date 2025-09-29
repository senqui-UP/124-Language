fun main() {
    println("Loading...")
    println("Welcome to PyCraft (type 'quit' to exit)")

    while (true) {
        print("> ")
        val input = readln()

        if (input.trim() == "quit") break

        val scanner = Scanner(input)
        val tokens = scanner.scanTokens()

        for (token in tokens) {
            println(token)
        }
    }
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
