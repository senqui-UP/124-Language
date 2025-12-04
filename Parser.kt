class Parser(private val tokens: List<Token>) {
    private var current = 0

    // Public entrypoints -----------------------------------------------------
    fun parseProgram(): ParseNode.ProgramNode = ParseNode.ProgramNode(parseStatements())

    fun parseExpression(): ParseNode.ExprNode {
        val expr = expression()
        expectEOF("Unexpected tokens after expression.")
        return expr
    }

    // Statements -------------------------------------------------------------
    private fun parseStatements(): List<ParseNode.StmtNode> {
        val statements = mutableListOf<ParseNode.StmtNode>()
        while (!isAtEnd()) statements.add(statement())
        return statements
    }

    private fun statement(): ParseNode.StmtNode {
        if (match(TokenType.KEYWORD)) {
            val kw = previous()
            val text = kw.lexeme
            return when {
                text.startsWith("/function") -> functionDeclaration(kw)
                text.startsWith("/say") -> sayStatement(kw)
                text.startsWith("/summon") -> summonStatement(kw)
                text.startsWith("/expr") -> exprAssignStatement(kw)
                text.startsWith("/set") -> setStatement(kw)
                text.startsWith("/execute") -> executeStatement(kw)
                text.startsWith("/return") -> returnStatement(kw)
                text.startsWith("/kill") -> ParseNode.StmtNode.KillNode(kw)
                else -> throw error(kw, "Unknown keyword: ${kw.lexeme}")
            }
        }
        throw error(peek(), "Unexpected token in statement.")
    }

    private fun sayStatement(keyword: Token): ParseNode.StmtNode {
        val sayLine = keyword.line
        val messageToken = if (check(TokenType.STRING)) {
            advance()
        } else {
            val sb = StringBuilder()
            while (!isAtEnd() && peek().line == sayLine) {
                sb.append(advance().lexeme)
                if (!isAtEnd() && peek().line == sayLine) sb.append(' ')
            }
            val msg = sb.toString()
            Token(TokenType.STRING, msg, msg, sayLine)
        }
        while (!isAtEnd() && peek().line == sayLine) advance()
        return ParseNode.StmtNode.SayNode(keyword, messageToken)
    }

    private fun summonStatement(keyword: Token): ParseNode.StmtNode {
        val typeTok = consumeOneOf("Expected type after /summon", TokenType.STRING, TokenType.IDENTIFIER)
        val name = consume(TokenType.IDENTIFIER, "Expected variable name")

        var valueTok: Token? = null
        if (match(TokenType.LEFT_PAREN)) {
            val numTok = consume(TokenType.NUMBER, "Expected numeric literal")
            consume(TokenType.RIGHT_PAREN, "Expected ')' after number")
            valueTok = numTok
        } else if (match(TokenType.NUMBER) || match(TokenType.STRING)) {
            valueTok = previous()
        }

        return ParseNode.StmtNode.SummonNode(keyword, typeTok, name, valueTok)
    }

    private fun functionDeclaration(keyword: Token): ParseNode.StmtNode {
        val name = consumeOneOf("Expected function name", TokenType.STRING, TokenType.IDENTIFIER)
        consume(TokenType.LEFT_PAREN, "Expected '(' after function name")
        val params = mutableListOf<Token>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                params.add(consume(TokenType.IDENTIFIER, "Expected parameter name"))
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters")
        val body = parseBlock()
        return ParseNode.StmtNode.FunctionDeclNode(keyword, name, params, body)
    }

    private fun returnStatement(keyword: Token): ParseNode.StmtNode {
        val value = if (isAtEnd() || check(TokenType.RIGHT_BRACE)) null else expression()
        return ParseNode.StmtNode.ReturnNode(keyword, value)
    }

    private fun exprAssignStatement(keyword: Token): ParseNode.StmtNode {
        val name = consume(TokenType.IDENTIFIER, "Expected variable name")
        consume(TokenType.LEFT_BRACE, "Expected '{' before expression")
        val expr = expression()
        consume(TokenType.RIGHT_BRACE, "Expected '}' after expression")
        return ParseNode.StmtNode.ExprAssignNode(keyword, name, expr)
    }

    private fun setStatement(keyword: Token): ParseNode.StmtNode {
        val name = consume(TokenType.IDENTIFIER, "Expected variable name")
        val op = when {
            match(TokenType.EQUAL) -> previous()
            match(TokenType.PLUS_EQUAL) -> previous()
            match(TokenType.MINUS_EQUAL) -> previous()
            match(TokenType.STAR_EQUAL) -> previous()
            match(TokenType.DOLLAR_EQUAL) -> previous()
            match(TokenType.PERCENT_EQUAL) -> previous()
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
        return ParseNode.StmtNode.SetNode(keyword, name, op, expr)
    }

    private fun executeStatement(keyword: Token): ParseNode.StmtNode {
        val text = keyword.lexeme
        return when {
            text.contains("while") -> executeWhile(keyword)
            text.contains("for") -> executeFor(keyword)
            text.contains("elif") || text.contains("else") ->
                throw error(keyword, "Unexpected '${keyword.lexeme}' without a matching if.")
            else -> executeIf(keyword)
        }
    }

    private fun executeIf(keyword: Token): ParseNode.StmtNode {
        val (conditionTokens, runKeyword) = collectConditionTokens()
        val body = parseBlock()

        val elifBranches = mutableListOf<ParseNode.StmtNode.ElifBranch>()
        while (check(TokenType.KEYWORD) && peek().lexeme.contains("elif")) {
            val elifKeyword = advance()
            val (elifCondition, elifRun) = collectConditionTokens()
            val elifBody = parseBlock()
            elifBranches.add(ParseNode.StmtNode.ElifBranch(elifKeyword, elifCondition, elifRun, elifBody))
        }

        var elseKeyword: Token? = null
        var elseBranch: List<ParseNode.StmtNode>? = null
        if (check(TokenType.KEYWORD) && peek().lexeme.contains("else")) {
            elseKeyword = advance()
            val runKw = consume(TokenType.KEYWORD, "Expected 'run' keyword")
            if (!runKw.lexeme.contains("run")) throw error(runKw, "Expected 'run' keyword")
            elseBranch = parseBlock()
        }

        return ParseNode.StmtNode.ExecuteIfNode(keyword, conditionTokens, runKeyword, body, elifBranches, elseKeyword, elseBranch)
    }

    private fun executeWhile(keyword: Token): ParseNode.StmtNode {
        val (conditionTokens, runKeyword) = collectConditionTokens()
        val body = parseBlock()
        return ParseNode.StmtNode.ExecuteWhileNode(keyword, conditionTokens, runKeyword, body)
    }

    private fun executeFor(keyword: Token): ParseNode.StmtNode {
        val selector = consume(TokenType.IDENTIFIER, "Expected loop variable after /execute for")
        val inKeyword = consume(TokenType.IN, "Expected 'in' after loop variable")
        val rangeKeyword = if (check(TokenType.KEYWORD) && peek().lexeme.contains("range")) advance() else null
        val rangeExpr =
            if (rangeKeyword != null && match(TokenType.LEFT_PAREN)) {
                val expr = expression()
                consume(TokenType.RIGHT_PAREN, "Expected ')' after range expression")
                expr
            } else {
                expression()
            }
        val runKeyword = consume(TokenType.KEYWORD, "Expected 'run' keyword")
        if (!runKeyword.lexeme.contains("run")) throw error(runKeyword, "Expected 'run' keyword")
        val body = parseBlock()
        return ParseNode.StmtNode.ExecuteForNode(keyword, selector, inKeyword, rangeKeyword, rangeExpr, runKeyword, body)
    }

    private fun collectConditionTokens(): Pair<List<Token>, Token> {
        val conditionTokens = mutableListOf<Token>()
        while (!isAtEnd() && !(check(TokenType.KEYWORD) && peek().lexeme.contains("run"))) {
            conditionTokens.add(advance())
        }
        val runKeyword = consume(TokenType.KEYWORD, "Expected 'run' keyword")
        if (!runKeyword.lexeme.contains("run")) throw error(runKeyword, "Expected 'run' keyword")
        return conditionTokens to runKeyword
    }

    private fun parseBlock(): List<ParseNode.StmtNode> {
        consume(TokenType.LEFT_BRACE, "Expected '{' to start block")
        val body = mutableListOf<ParseNode.StmtNode>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) body.add(statement())
        consume(TokenType.RIGHT_BRACE, "Expected '}' after block")
        return body
    }

    // Expressions ------------------------------------------------------------
    private fun expression(): ParseNode.ExprNode = logicalOr()

    private fun logicalOr(): ParseNode.ExprNode {
        var expr = logicalAnd()
        while (match(TokenType.OR)) {
            val op = previous()
            val right = logicalAnd()
            expr = ParseNode.ExprNode.BinaryNode(expr, op, right)
        }
        return expr
    }

    private fun logicalAnd(): ParseNode.ExprNode {
        var expr = logicalNot()
        while (match(TokenType.AND)) {
            val op = previous()
            val right = logicalNot()
            expr = ParseNode.ExprNode.BinaryNode(expr, op, right)
        }
        return expr
    }

    private fun logicalNot(): ParseNode.ExprNode {
        if (match(TokenType.EXCL, TokenType.NOT)) {
            val op = previous()
            val right = logicalNot()
            return ParseNode.ExprNode.UnaryNode(op, right)
        }
        return bitwiseOr()
    }

    private fun bitwiseOr(): ParseNode.ExprNode {
        var expr = bitwiseXor()
        while (match(TokenType.PIPE)) {
            val op = previous()
            val right = bitwiseXor()
            expr = ParseNode.ExprNode.BinaryNode(expr, op, right)
        }
        return expr
    }

    private fun bitwiseXor(): ParseNode.ExprNode {
        var expr = bitwiseAnd()
        while (match(TokenType.CARET)) {
            val op = previous()
            val right = bitwiseAnd()
            expr = ParseNode.ExprNode.BinaryNode(expr, op, right)
        }
        return expr
    }

    private fun bitwiseAnd(): ParseNode.ExprNode {
        var expr = equality()
        while (match(TokenType.AMPERSAND)) {
            val op = previous()
            val right = equality()
            expr = ParseNode.ExprNode.BinaryNode(expr, op, right)
        }
        return expr
    }

    private fun equality(): ParseNode.ExprNode {
        var expr = comparison()
        while (match(TokenType.EXCL_EQUAL, TokenType.EQUAL_EQUAL)) {
            val op = previous()
            val right = comparison()
            expr = ParseNode.ExprNode.BinaryNode(expr, op, right)
        }
        return expr
    }

    private fun comparison(): ParseNode.ExprNode {
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
            expr = ParseNode.ExprNode.BinaryNode(expr, op, right)
        }
        return expr
    }

    private fun shift(): ParseNode.ExprNode {
        var expr = addSub()
        while (match(TokenType.LEFT_SHIFT, TokenType.RIGHT_SHIFT)) {
            val op = previous()
            val right = addSub()
            expr = ParseNode.ExprNode.BinaryNode(expr, op, right)
        }
        return expr
    }

    private fun addSub(): ParseNode.ExprNode {
        var expr = multiDiv()
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            val op = previous()
            val right = multiDiv()
            expr = ParseNode.ExprNode.BinaryNode(expr, op, right)
        }
        return expr
    }

    private fun multiDiv(): ParseNode.ExprNode {
        var expr = exponent()
        while (match(TokenType.STAR, TokenType.DOLLAR, TokenType.DOLLAR_DOLLAR, TokenType.PERCENT)) {
            val op = previous()
            val right = exponent()
            expr = ParseNode.ExprNode.BinaryNode(expr, op, right)
        }
        return expr
    }

    private fun exponent(): ParseNode.ExprNode {
        var expr = unary()
        if (match(TokenType.STAR_STAR)) {
            val op = previous()
            val right = unary()
            expr = ParseNode.ExprNode.BinaryNode(expr, op, right)
        }
        return expr
    }

    private fun unary(): ParseNode.ExprNode {
        if (match(TokenType.PLUS, TokenType.MINUS, TokenType.TILDE, TokenType.EXCL, TokenType.NOT)) {
            val op = previous()
            val right = unary()
            return ParseNode.ExprNode.UnaryNode(op, right)
        }
        return postfix()
    }

    private fun postfix(): ParseNode.ExprNode {
        var expr = call()
        if (match(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS)) {
            val op = previous()
            expr = ParseNode.ExprNode.PostfixNode(expr, op)
        }
        return expr
    }

    private fun call(): ParseNode.ExprNode {
        var expr = term()
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                val paren = previous()
                val args = mutableListOf<ParseNode.ExprNode>()
                if (!check(TokenType.RIGHT_PAREN)) {
                    do {
                        args.add(expression())
                    } while (match(TokenType.COMMA))
                }
                consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.")
                expr = ParseNode.ExprNode.CallNode(expr, paren, args)
            } else break
        }
        return expr
    }

    private fun term(): ParseNode.ExprNode {
        when {
            match(TokenType.NUMBER) -> return ParseNode.ExprNode.LiteralNode(previous())
            match(TokenType.TRUE) -> return ParseNode.ExprNode.LiteralNode(previous())
            match(TokenType.FALSE) -> return ParseNode.ExprNode.LiteralNode(previous())
            match(TokenType.NIL) -> return ParseNode.ExprNode.LiteralNode(previous())
            match(TokenType.STRING) -> return ParseNode.ExprNode.LiteralNode(previous())
            match(TokenType.LEFT_PAREN) -> {
                val leftParen = previous()
                val e = expression()
                val rightParen = consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
                return ParseNode.ExprNode.GroupingNode(leftParen, e, rightParen)
            }
            match(TokenType.IDENTIFIER) -> return ParseNode.ExprNode.LiteralNode(previous())
        }
        throw error(peek(), "Expect expression.")
    }

    // Helpers ----------------------------------------------------------------
    private fun match(vararg types: TokenType): Boolean {
        for (type in types) if (check(type)) {
            advance()
            return true
        }
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

    class ParseError(override val message: String) : RuntimeException(message)
}
