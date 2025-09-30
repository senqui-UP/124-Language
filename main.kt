
enum class TokenType {
    // Single-character tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, STAR, PERCENT,
    AMPERSAND, PIPE, CARET, TILDE,

    // Multi-character operators
    DOLLAR, DOLLAR_DOLLAR, STAR_STAR,
    EQUAL, EQUAL_EQUAL,
    EXCL, EXCL_EQUAL,
    GREATER, GREATER_EQUAL, RIGHT_SHIFT,
    LESS, LESS_EQUAL, LEFT_SHIFT,

    //logical operators
    IN,IS,AND,OR,NOT,NOT_IN,IS_NOT,


    // Compound assignments
    PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, 
    DOLLAR_EQUAL, PERCENT_EQUAL, STAR_STAR_EQUAL,

    // Literals
    IDENTIFIER, STRING, NUMBER,

    // Keywords
    KEYWORD,

    // No COMMENT TokenType since we ignore it

    EOF
}

// ------------------------------
// Token Class
// ------------------------------
data class Token(
    val type: TokenType,
    val lexeme: String,
    val literal: Any? = null,
    val line: Int
) {
    override fun toString(): String =
        "Token(type=$type, lexeme='$lexeme', literal=$literal, line=$line)"
}

// ------------------------------
// Scanner Class
// ------------------------------
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
        "as", "break", "pass"
    )

    // Multiword Keywords List
    private val multiWordKeywords = listOf(
        "/execute if",
        "/execute for", 
        "/execute while",

    )

    // ------------------------------
    // Main Scanning
    // ------------------------------
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
            '(' -> numberLiteral()
            ')' -> addToken(TokenType.RIGHT_PAREN)
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            '~' -> addToken(TokenType.TILDE)
            '^' -> addToken(TokenType.CARET)
            "in"-> addToken(TokenType.IN)
            "is"-> addToken(TokenType.IS)  
            "and"-> addToken(TokenType.AND)
            "or"-> addToken(TokenType.OR)
            "not"-> addToken(TokenType.NOT)
            "not in"-> addToken(TokenType.NOT_IN)
            "is not"-> addToken(TokenType.IS_NOT)
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

            '!' -> addToken(if (match('=')) TokenType.EXCL_EQUAL else TokenType.EXCL)
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

            ' ', '\r', '\t' -> {} // ignore whitespace
            '\n' -> line++

            '"' -> quotedString() // optional: keep supporting quoted string

            '/' -> {
                if (peekAhead("whisper")) {
                    comment()
                } else {
                    stringOrKeyword()
                }
            }

            '@' -> identifier()

            else -> {
                when {
                    c.isLetter() -> stringOrKeyword()
                    else -> error("Unexpected character '$c'")
                }
            }
        }
    }

    // ------------------------------
    // Token Helpers
    // ------------------------------
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
    private fun peekNext(): Char = if (current + 1 >= source.length) '\u0000' else source[current + 1]
    private fun peekAhead(word: String): Boolean {
        if (current + word.length > source.length) return false
        return source.substring(current, current + word.length) == word
    }

    private fun isAtEnd(): Boolean = current >= source.length
    private fun advance(): Char = source[current++]
    private fun error(message: String) = println("Error at line $line: $message")

    // ------------------------------
    // Extra Handlers
    // ------------------------------
    private fun comment() {
        repeat("whisper".length) { advance() }
        while (peek() == ' ' || peek() == '\t') advance()
        if (peek() == '"') {
            advance()
            while (!isAtEnd() && peek() != '"') {
                if (peek() == '\n') line++
                advance()
            }
            if (!isAtEnd()) advance()
        } else {
            while (!isAtEnd() && peek() != '\n') advance()
        }
    }

    private fun stringOrKeyword() {
        // Check multi-word keywords first
        for (kw in multiWordKeywords) {
            if (source.substring(current - 1).startsWith(kw)) {
                repeat(kw.length - 1) { advance() }
                addToken(TokenType.KEYWORD)
                return
            }
        }

        // Check single-word keyword
        while (!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_')) advance()
        val text = source.substring(start, current)

        if (keywords.contains(text)) {
            addToken(TokenType.KEYWORD)
        } else {
            stringLiteral()
        }
    }

    private fun stringLiteral() {
        while (!isAtEnd() && peek() != '\n' &&
               peek() != ',' && peek() != ')' &&
               peek() != '}' && !peek().isWhitespace() &&
               !keywords.any { source.startsWith(it, current) }) {
            advance()
        }
        val value = source.substring(start, current)
        addToken(TokenType.STRING, value)
    }

    private fun numberLiteral() {
        if (source[start] != '(') return

        val numberStart = current
        while (!isAtEnd() && (peek().isDigit() || peek() == '.')) advance()

        if (peek() != ')') {
            error("Number must end with ')'")
            return
        }

        val numberText = source.substring(numberStart, current)
        val numberValue = parseNumber(numberText)
        advance() // consume ')'
        addToken(TokenType.NUMBER, numberValue)
    }

    private fun identifier() {
        if (!peek().isLetter()) {
            error("Identifier must start with @ followed by a letter")
            return
        }
        advance() // first letter
        var length = 2
        while ((peek().isLetterOrDigit() || peek() == '_') && length < 51) {
            advance()
            length++
        }
        if (length > 50) error("Identifier exceeds 50 characters")
        val text = source.substring(start, current)
        addToken(TokenType.IDENTIFIER, text)
    }

    private fun quotedString() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++
            advance()
        }
        if (isAtEnd()) {
            error("Unterminated string")
            return
        }
        advance() // closing quote
        val value = source.substring(start + 1, current - 1)
        addToken(TokenType.STRING, value)
    }

    fun parseNumber(text: String): Double? {
        return try { text.toDouble() } catch (e: NumberFormatException) { null }
    }
}

// ------------------------------
// Main REPL
// ------------------------------
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
