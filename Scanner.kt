    class Scanner(private val source: String) {
        private val tokens = mutableListOf<Token>()
        private var start = 0
        private var current = 0
        private var line = 1

        // Keywords List
    private val keywords = setOf(
        ".mcmeta",
        "/function", "/kill", "/say", "/source", "/summon", "/set", "/expr",
        "/execute", "/return", "/stop", "/skip", "/whisper",
        "run", "else", "elif", "as", "from", "in", "range", "const"
    )
        // Logical operator keywords
        private val logicalOperators = mapOf(
            "in" to TokenType.IN,
            "is" to TokenType.IS,
            "and" to TokenType.AND,
            "or" to TokenType.OR,
            "not" to TokenType.NOT
        )

        // Multi-word logical operators (must be checked first)
        private val multiWordLogicalOps = mapOf(
            "not in" to TokenType.NOT_IN,
            "is not" to TokenType.IS_NOT
        )

        // Multiword Keywords List
        private val multiWordKeywords = listOf(
            "/execute if",
            "/execute elif",
            "/execute else",
            "/execute for",
            "/execute while"
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
                // FIX: add LEFT_PAREN handling via smart lookahead (see case below)

                ')' -> addToken(TokenType.RIGHT_PAREN)
                '{' -> addToken(TokenType.LEFT_BRACE)
                '}' -> addToken(TokenType.RIGHT_BRACE)
                '[' -> addToken(TokenType.LEFT_BRACKET)
                ']' -> addToken(TokenType.RIGHT_BRACKET)
                ',' -> addToken(TokenType.COMMA)
                ':' -> addToken(TokenType.STRING, ":")
                '.' -> {
                    if (peekAhead("mcmeta")) {
                        repeat("mcmeta".length) { advance() }
                        addToken(TokenType.KEYWORD) // represents ".mcmeta"
                    } else addToken(TokenType.DOT)
                }
                '~' -> addToken(TokenType.TILDE)
                '^' -> addToken(TokenType.CARET)
                '&' -> addToken(TokenType.AMPERSAND)
                '|' -> addToken(TokenType.PIPE)

                '-' -> {
                    when {
                        match('-') -> addToken(TokenType.MINUS_MINUS)
                        match('=') -> addToken(TokenType.MINUS_EQUAL)
                        else -> addToken(TokenType.MINUS)
                    }
                }
                '+' -> {
                    when {
                        match('+') -> addToken(TokenType.PLUS_PLUS)
                        match('=') -> addToken(TokenType.PLUS_EQUAL)
                        else -> addToken(TokenType.PLUS)
                    }
                }
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
                        match('$') -> {
                            if (match('=')) addToken(TokenType.DOLLAR_DOLLAR_EQUAL) // $$=
                            else addToken(TokenType.DOLLAR_DOLLAR)                  // $$
                        }
                        match('=') -> addToken(TokenType.DOLLAR_EQUAL)              // $=
                        else -> addToken(TokenType.DOLLAR)                          // $
                    }
                }

                    '!' -> addToken(if (match('=')) TokenType.EXCL_EQUAL else TokenType.EXCL)
                '=' -> {
                    if (match('=')) {
                        if (match('=')) addToken(TokenType.STRICT_EQUAL)   // ===
                        else addToken(TokenType.EQUAL_EQUAL)               // ==
                    } else addToken(TokenType.EQUAL)                       // =
                }
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

                '"' -> quotedString()

                '/' -> {
                    // Special-case commands that should consume the rest of the line as a message
                    if (peekAhead("whisper")) {
                        comment()
                    } else if (peekAhead("say")) {
                        // produce KEYWORD for "/say"
                        repeat("say".length) { advance() }
                        addToken(TokenType.KEYWORD)
                        // consume spaces
                        while (peek() == ' ' || peek() == '\t') advance()
                        // capture rest of line as a single STRING token (raw)
                        val msgStart = current
                        while (!isAtEnd() && peek() != '\n') advance()
                        val msg = source.substring(msgStart, current)
                        tokens.add(Token(TokenType.STRING, msg, msg, line))
                    } else {
                        stringOrKeyword()
                    }
                }

                '@' -> identifier()

                // FIX: bare numbers like 42, 3.14
                in '0'..'9' -> {
                    number() // <— new helper below (non-destructive to your (num) style)
                }

                // FIX: smart '(' handling — either a plain LEFT_PAREN or a (number) literal
                '(' -> {
                    if (looksLikeParenNumber()) {
                        numberLiteralFromParens() // consumes the digits and the ')', emits NUMBER
                    } else {
                        addToken(TokenType.LEFT_PAREN) // regular grouping paren
                    }
                }

                else -> {
                    when {
                        c.isLetter() -> wordOrOperator()
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

        private fun wordOrOperator() {
            // First, check for multi-word logical operators
            for ((op, type) in multiWordLogicalOps) {
                if (source.substring(current - 1).startsWith(op)) {
                    val nextCharIdx = current - 1 + op.length
                    if (nextCharIdx >= source.length ||
                        !source[nextCharIdx].isLetterOrDigit()
                    ) {
                        repeat(op.length - 1) { advance() }
                        addToken(type)
                        return
                    }
                }
            }

            while (!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_')) advance()
            val text = source.substring(start, current)

            if (text == "true") {
                addToken(TokenType.TRUE)
            } else if (text == "false") {
                addToken(TokenType.FALSE)
            } else if (text == "null" || text == "nil") {
                addToken(TokenType.NIL)
            }
            else if (logicalOperators.contains(text)) {
                addToken(logicalOperators[text]!!)
            } else {
                if (keywords.contains(text)) {
                    addToken(TokenType.KEYWORD)
                } else {
                    addToken(TokenType.STRING, text)
                }
            }
        }

        private fun stringOrKeyword() {
            for (kw in multiWordKeywords) {
                if (source.substring(current - 1).startsWith(kw)) {
                    repeat(kw.length - 1) { advance() }
                    addToken(TokenType.KEYWORD)
                    return
                }
            }

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

        private fun number() {
            while (!isAtEnd() && peek().isDigit()) advance()
            if (!isAtEnd() && peek() == '.' && peekNext().isDigit()) {
                advance() // consume '.'
                while (!isAtEnd() && peek().isDigit()) advance()
            }
            val text = source.substring(start, current)
            val value = parseNumber(text)
            addToken(TokenType.NUMBER, value)
        }

        private fun looksLikeParenNumber(): Boolean {
            var i = current // position right after '('
            if (i >= source.length || !source[i].isDigit()) return false

            // integer part
            while (i < source.length && source[i].isDigit()) i++

            if (i < source.length && source[i] == '.') {
                val afterDot = i + 1
                if (afterDot >= source.length || !source[afterDot].isDigit()) return false
                i = afterDot
                while (i < source.length && source[i].isDigit()) i++
            }

            if (i < source.length && source[i] == ')') return true
            return false
        }

        // consumes number and emits NUMBER
        private fun numberLiteralFromParens() {
            val numberStart = current
            while (!isAtEnd() && (peek().isDigit())) advance()
            if (!isAtEnd() && peek() == '.') {
                advance()
                while (!isAtEnd() && peek().isDigit()) advance()
            }
            val numberText = source.substring(numberStart, current)
            val numberValue = parseNumber(numberText)
            advance()
            addToken(TokenType.NUMBER, numberValue)
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
            advance() 
            addToken(TokenType.NUMBER, numberValue)
        }

        private fun identifier() {
            if (!peek().isLetter()) {
                error("Identifier must start with @ followed by a letter")
                return
            }
            advance() 
            var length = 2
            while ((peek().isLetterOrDigit() || peek() == '_') && length < 51) {
                advance()
                length++
            }
            if (length > 50) error("Identifier exceeds 50 characters")
            val text = source.substring(start, current)
            addToken(TokenType.IDENTIFIER, null)
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
            advance() 
            val value = source.substring(start + 1, current - 1)
            addToken(TokenType.STRING, value)
        }

        fun parseNumber(text: String): Double? {
            return try { text.toDouble() } catch (e: NumberFormatException) { null }
        }
    }
