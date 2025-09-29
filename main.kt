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
    when (c) {
        '(' -> addToken(TokenType.LEFT_PAREN)
        ')' -> addToken(TokenType.RIGHT_PAREN)
        '{' -> addToken(TokenType.LEFT_BRACE)
        '}' -> addToken(TokenType.RIGHT_BRACE)
        ',' -> addToken(TokenType.COMMA)
        '.' -> addToken(TokenType.DOT)
        '~' -> addToken(TokenType.TILDE)
        '^' -> addToken(TokenType.CARET)

        '&' -> addToken(TokenType.AMPERSAND)
        '|' -> addToken(TokenType.PIPE)

        '-' -> addToken(if (match('=')) TokenType.MINUS_EQUAL else TokenType.MINUS)
        '+' -> addToken(if (match('=')) TokenType.PLUS_EQUAL else TokenType.PLUS)
        '%' -> addToken(if (match('=')) TokenType.PERCENT_EQUAL else TokenType.PERCENT)

        '*' -> {
            when {
                match('*') -> addToken(if (match('=')) TokenType.STAR_STAR_EQUAL else TokenType.STAR_STAR)
                match('=') -> addToken(TokenType.STAR_EQUAL)
                else -> addToken(TokenType.STAR)
            }
        }

        '$' -> {
            when {
                match('$') -> addToken(TokenType.DOLLAR_DOLLAR)
                match('=') -> addToken(TokenType.DOLLAR_EQUAL)
                else -> addToken(TokenType.DOLLAR)
            }
        }

        '!' -> addToken(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
        '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
        
        '<' -> {
            when {
                match('<') -> addToken(TokenType.LEFT_SHIFT)
                match('=') -> addToken(TokenType.LESS_EQUAL)
                else -> addToken(TokenType.LESS)
            }
        }
        
        '>' -> {
            when {
                match('>') -> addToken(TokenType.RIGHT_SHIFT)
                match('=') -> addToken(TokenType.GREATER_EQUAL)
                else -> addToken(TokenType.GREATER)
            }
        }

        ' ', '\r', '\t' -> {} 
        '\n' -> line++

        '"' -> quotedString() 

        '/' -> {
            if (peekAhead("whisper")) {
                comment()
            } else {
                keyword()
            }
        }

        '@' -> identifier()

        else -> {
            when {
                c.isDigit() -> error("Numbers must be wrapped in parentheses: ($c...)")
                c.isLetter() -> keywordOrStringLiteral() // commit 8
                else -> error("Unexpected character '$c'")
            }
        }
    }
}

private fun addToken(type: TokenType, literal: Any? = null) {
    val text = source.substring(start, current)
    tokens.add(Token(type, text, literal, line))
}

private fun match(expected: Char): Boolean {
    if (isAtEnd()) return false
    if (source[current] != expected) return false
    current++
    return true
}

private fun peek(): Char = if (isAtEnd()) '\u0000' else source[current]

private fun peekAhead(word: String): Boolean {
    if (current + word.length > source.length) return false
    return source.substring(current, current + word.length) == word
}

private fun error(message: String) {
    println("Error at line $line: $message")
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

