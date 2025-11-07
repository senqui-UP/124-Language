class Parser(private val tokens: List<Token>) {
    private var current = 0

    // ---------- Public entry points ----------
    fun parseStatements(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd()) {
            statements.add(statement())
        }
        return statements
    }

    fun parseExpression(): Expr {
        val expr = expression()
        expectEOF("Unexpected tokens after expression.")
        return expr
    }

    // ---------- Statements (unchanged shape; still minimal) ----------
private fun statement(): Stmt {
    if (match(TokenType.KEYWORD)) {
        val kw = previous().lexeme
        return when {
            kw.startsWith("/say")     -> sayStatement()
            kw.startsWith("/summon")  -> summonStatement()
            kw.startsWith("/expr")    -> exprStatement()
            kw.startsWith("/set")     -> setStatement()   // <— NEW
            kw.startsWith("/execute") -> executeStatement()
            kw.startsWith("/kill")    -> Stmt.Kill
            else -> throw error(previous(), "Unknown keyword: $kw")
        }
    }
    throw error(peek(), "Unexpected token in statement.")
}

    private fun sayStatement(): Stmt {
        val sayLine = previous().line
        val message = if (check(TokenType.STRING)) {
            advance().literal as String
        } else {
            val sb = StringBuilder()
            while (!isAtEnd() && peek().line == sayLine) {
                sb.append(advance().lexeme)
                if (!isAtEnd() && peek().line == sayLine) sb.append(' ')
            }
            sb.toString()
        }
        // Defensive: consume any leftover tokens on the same line to avoid stray words becoming next statements
        while (!isAtEnd() && peek().line == sayLine) advance()
        return Stmt.Say(message.trimEnd())
    }

    private fun summonStatement(): Stmt {
        // /summon <type> @var [(number)]
        val typeTok = consumeOneOf("Expected type after /summon", TokenType.STRING, TokenType.IDENTIFIER)
        val name = consume(TokenType.IDENTIFIER, "Expected variable name").lexeme

        var value: Double? = null
        if (match(TokenType.LEFT_PAREN)) {
            val numTok = consume(TokenType.NUMBER, "Expected numeric literal")
            consume(TokenType.RIGHT_PAREN, "Expected ')' after number")
            value = (numTok.literal as? Double)
        } else if (match(TokenType.NUMBER)) {
            // allow bare numeric initializer too
            value = (previous().literal as? Double)
        }

        return Stmt.Summon(typeTok.lexeme, name, value)
    }

    private fun exprStatement(): Stmt {
        // /expr @var { <expression> }
        val name = consume(TokenType.IDENTIFIER, "Expected variable name").lexeme
        consume(TokenType.LEFT_BRACE, "Expected '{' before expression")
        val expr = expression()
        consume(TokenType.RIGHT_BRACE, "Expected '}' after expression")
        return Stmt.ExprAssign(name, expr) // was: AstPrinter().print(expr)
    }


    private fun executeStatement(): Stmt {
        // Minimal stub that respects "/execute ... run { ... } [else { ... }]"
        val cond = expressionUntilKeyword("run")
        val thenBranch = mutableListOf<Stmt>()
        consume(TokenType.KEYWORD, "Expected 'run' keyword")
        consume(TokenType.LEFT_BRACE, "Expected '{' to start run block")
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) thenBranch.add(statement())
        consume(TokenType.RIGHT_BRACE, "Expected '}' after run block")

        var elseBranch: List<Stmt>? = null
        // Only consume an else keyword if it actually is 'else' (or '/execute else').
        if (check(TokenType.KEYWORD) && (peek().lexeme == "else" || peek().lexeme == "/execute else")) {
            advance()
            consume(TokenType.LEFT_BRACE, "Expected '{' to start else block")
            val list = mutableListOf<Stmt>()
            while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) list.add(statement())
            consume(TokenType.RIGHT_BRACE, "Expected '}' after else block")
            elseBranch = list
        }

        return Stmt.Execute(cond, thenBranch, elseBranch)
    }
private fun setStatement(): Stmt {
    // /set @var <assign_op> <expression>
    val name = consume(TokenType.IDENTIFIER, "Expected variable name").lexeme

    // Recognize one of: "=", "+=", "-=", "*=", $=, $$=, %=, **=
    val op = when {
        match(TokenType.EQUAL)           -> previous()
        match(TokenType.PLUS_EQUAL)      -> previous()
        match(TokenType.MINUS_EQUAL)     -> previous()
        match(TokenType.STAR_EQUAL)      -> previous()
        match(TokenType.DOLLAR_EQUAL)    -> previous()
        match(TokenType.PERCENT_EQUAL)   -> previous()
        match(TokenType.STAR_STAR_EQUAL) -> previous()
        else -> throw error(peek(), "Expected an assignment operator after variable.")
    }

    val expr = if (match(TokenType.LEFT_BRACE)) {
        val e = expression()
        consume(TokenType.RIGHT_BRACE, "Expected '}' after expression")
        e
    } else {
        expression()
    }
    return Stmt.Assign(name, op, expr)
}

    private fun expressionUntilKeyword(stop: String): String {
        val sb = StringBuilder()
        while (!check(TokenType.KEYWORD) && !isAtEnd()) sb.append(advance().lexeme).append(' ')
        return sb.toString().trim()
    }

    // =====================================================================
    //                           EXPRESSIONS
    // Matches your grammar layers using only your existing TokenType set.
    // =====================================================================

    // expression -> logical_or
    private fun expression(): Expr = logicalOr()

    // logical_or -> logical_and { "or" logical_and }
    private fun logicalOr(): Expr {
        var expr = logicalAnd()
        while (match(TokenType.OR)) {
            val op = previous()
            val right = logicalAnd()
            expr = Expr.Binary(expr, op, right)
        }
        return expr
    }

    // logical_and -> logical_not { "and" logical_not }
    private fun logicalAnd(): Expr {
        var expr = logicalNot()
        while (match(TokenType.AND)) {
            val op = previous()
            val right = logicalNot()
            expr = Expr.Binary(expr, op, right)
        }
        return expr
    }

    // logical_not -> "!" logical_not | "not" logical_not | bitwise_or
    private fun logicalNot(): Expr {
        if (match(TokenType.EXCL, TokenType.NOT)) {
            val op = previous()
            val right = logicalNot()
            return Expr.Unary(op, right)
        }
        return bitwiseOr()
    }

    // bitwise_or -> bitwise_xor { "|" bitwise_xor }
    private fun bitwiseOr(): Expr {
        var expr = bitwiseXor()
        while (match(TokenType.PIPE)) {
            val op = previous()
            val right = bitwiseXor()
            expr = Expr.Binary(expr, op, right)
        }
        return expr
    }

    // bitwise_xor -> bitwise_and { "^" bitwise_and }
    private fun bitwiseXor(): Expr {
        var expr = bitwiseAnd()
        while (match(TokenType.CARET)) {
            val op = previous()
            val right = bitwiseAnd()
            expr = Expr.Binary(expr, op, right)
        }
        return expr
    }

    // bitwise_and -> equality { "&" equality }
    private fun bitwiseAnd(): Expr {
        var expr = equality()
        while (match(TokenType.AMPERSAND)) {
            val op = previous()
            val right = equality()
            expr = Expr.Binary(expr, op, right)
        }
        return expr
    }

    // equality -> comparison { ( "==" | "!=" ) comparison }
    // (Use 'is'/'is not' down in comparison per your grammar.)
    private fun equality(): Expr {
        var expr = comparison()
        while (match(TokenType.EXCL_EQUAL, TokenType.EQUAL_EQUAL)) {
            val op = previous()
            val right = comparison()
            expr = Expr.Binary(expr, op, right)
        }
        return expr
    }

    // comparison -> shift { comparison_op shift }
    // comparison_op -> "<" | "<=" | ">" | ">=" | "is" | "is not" | "in" | "not in"
    private fun comparison(): Expr {
        var expr = shift()
        while (true) {
            val matched = match(
                TokenType.LESS, TokenType.LESS_EQUAL,
                TokenType.GREATER, TokenType.GREATER_EQUAL,
                TokenType.IS, TokenType.IS_NOT,
                TokenType.IN, TokenType.NOT_IN
            )
            if (!matched) break
            val op = previous()
            val right = shift()
            expr = Expr.Binary(expr, op, right)
        }
        return expr
    }

    // shift -> add_sub { ( "<<" | ">>" ) add_sub }
    private fun shift(): Expr {
        var expr = addSub()
        while (match(TokenType.LEFT_SHIFT, TokenType.RIGHT_SHIFT)) {
            val op = previous()
            val right = addSub()
            expr = Expr.Binary(expr, op, right)
        }
        return expr
    }

    // add_sub -> multi_div { ( "+" | "-" ) multi_div }
    private fun addSub(): Expr {
        var expr = multiDiv()
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            val op = previous()
            val right = multiDiv()
            expr = Expr.Binary(expr, op, right)
        }
        return expr
    }

    // multi_div -> exponent { ( "*" | "$" | "$$" | "%" ) exponent }
    private fun multiDiv(): Expr {
        var expr = exponent()
        while (match(TokenType.STAR, TokenType.DOLLAR, TokenType.DOLLAR_DOLLAR, TokenType.PERCENT)) {
            val op = previous()
            val right = exponent()
            expr = Expr.Binary(expr, op, right)
        }
        return expr
    }

    // exponent -> unary [ "**" unary ]   (right-associative)
    private fun exponent(): Expr {
        var expr = unary()
        if (match(TokenType.STAR_STAR)) {
            val op = previous()
            val right = unary() // right-assoc: recurse on the right chain by calling exponent() here if you prefer deeper **
            expr = Expr.Binary(expr, op, right)
        }
        return expr
    }

    // unary -> ( "+" | "-" | "~" | "!" | "not" ) unary | primary
    private fun unary(): Expr {
        if (match(TokenType.PLUS, TokenType.MINUS, TokenType.TILDE, TokenType.EXCL, TokenType.NOT)) {
            val op = previous()
            val right = unary()
            return Expr.Unary(op, right)
        }
        return primary()
    }

    // term -> var_id | "(" expression ")" | NUMBER | TRUE | FALSE | NIL | STRING
    // (Postfix inc/dec & func_call omitted for now; see notes.)
    private fun primary(): Expr {
        when {
            match(TokenType.NUMBER) -> return Expr.Literal(previous().literal as Double?)

            match(TokenType.TRUE)  -> return Expr.Literal(true)
            match(TokenType.FALSE) -> return Expr.Literal(false)
            match(TokenType.NIL)   -> return Expr.Literal(null)

            match(TokenType.STRING) -> return Expr.Literal(previous().literal as String)

            match(TokenType.LEFT_PAREN) -> {
                val e = expression()
                consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
                return Expr.Grouping(e)
            }

            // var_id -> "@" IDENTIFIER (your scanner emits IDENTIFIER whose lexeme includes '@name')
            match(TokenType.IDENTIFIER) -> {
                // Treat identifiers as string-likes for now (your runtime typing rules will coerce later).
                // If you want variables to resolve later, keep as literal name for now.
                return Expr.Literal(previous().lexeme)
            }
        }
        throw error(peek(), "Expect expression.")
    }

    // ---------- helpers ----------
    private fun match(vararg types: TokenType): Boolean {
        for (type in types) if (check(type)) { advance(); return true }
        return false
    }

    private fun check(type: TokenType): Boolean = !isAtEnd() && peek().type == type
    private fun advance(): Token { if (!isAtEnd()) current++; return previous() }
    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }
    private fun consumeOneOf(message: String, vararg types: TokenType): Token {
        for (t in types) if (check(t)) return advance()
        throw error(peek(), message)
    }
    private fun expectEOF(message: String) { if (!isAtEnd()) throw error(peek(), message) }

    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF
    private fun peek(): Token = tokens[current]
    private fun previous(): Token = tokens[current - 1]

    private fun error(token: Token, message: String): ParseError =
        ParseError("[line ${token.line}] Error at '${token.lexeme}': $message")

    class ParseError(override val message: String): RuntimeException(message)
}
