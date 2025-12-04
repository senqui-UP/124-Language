import kotlin.math.floor
import kotlin.math.pow

class Interpreter(
    initialEnv: Environment = Environment()
) {
    private var env: Environment = initialEnv

    init {
        defineNativeFunctions()
    }

    fun evaluate(expr: ParseNode.ExprNode): Any? = when (expr) {
        is ParseNode.ExprNode.LiteralNode -> resolveLiteral(expr.token)
        is ParseNode.ExprNode.GroupingNode -> evaluate(expr.expr)
        is ParseNode.ExprNode.UnaryNode -> evalUnary(expr)
        is ParseNode.ExprNode.BinaryNode -> evalBinary(expr)
        is ParseNode.ExprNode.PostfixNode -> evalPostfix(expr)
        is ParseNode.ExprNode.CallNode -> evalCall(expr)
    }

    fun execute(stmt: ParseNode.StmtNode) {
        when (stmt) {
            is ParseNode.StmtNode.SayNode -> println(interpolate(stmt.message.literal as? String ?: stmt.message.lexeme))
            is ParseNode.StmtNode.SummonNode -> env.define(stmt.name.lexeme, stmt.value?.literal ?: defaultValueForType(stmt.type.lexeme))
            is ParseNode.StmtNode.ExprAssignNode -> env.assign(stmt.name.lexeme, evaluate(stmt.expr))
            is ParseNode.StmtNode.SetNode -> executeAssign(stmt)
            is ParseNode.StmtNode.FunctionDeclNode -> declareFunction(stmt)
            is ParseNode.StmtNode.ReturnNode -> throw Return(stmt.value?.let { evaluate(it) })
            is ParseNode.StmtNode.ExecuteIfNode -> executeIf(stmt)
            is ParseNode.StmtNode.ExecuteWhileNode -> executeWhile(stmt)
            is ParseNode.StmtNode.ExecuteForNode -> executeFor(stmt)
            is ParseNode.StmtNode.KillNode -> {} // handled by main loop
        }
    }

    fun executeBlock(stmts: List<ParseNode.StmtNode>, scope: Environment) {
        val previous = env
        try {
            env = scope
            stmts.forEach { execute(it) }
        } finally {
            env = previous
        }
    }

    // ==== Statements =======================================================
    private fun executeAssign(stmt: ParseNode.StmtNode.SetNode) {
        val right = evaluate(stmt.expr)
        val current = env.get(stmt.name.lexeme)
        val result = when (stmt.op.type) {
            TokenType.EQUAL -> right
            TokenType.PLUS_EQUAL -> plus(current, right, stmt.op)
            TokenType.MINUS_EQUAL -> numeric(current, right, stmt.op) { a, b -> a - b }
            TokenType.STAR_EQUAL -> numeric(current, right, stmt.op) { a, b -> a * b }
            TokenType.DOLLAR_EQUAL -> divide(current, right, stmt.op, floorDiv = false)
            TokenType.DOLLAR_DOLLAR_EQUAL -> divide(current, right, stmt.op, floorDiv = true)
            TokenType.PERCENT_EQUAL -> numeric(current, right, stmt.op) { a, b -> a % b }
            TokenType.STAR_STAR_EQUAL -> numeric(current, right, stmt.op) { a, b -> a.pow(b) }
            else -> throw RuntimeError(stmt.op, "Unsupported assignment operator.")
        }
        env.assign(stmt.name.lexeme, result)
    }

    private fun executeIf(stmt: ParseNode.StmtNode.ExecuteIfNode) {
        val firstCond = evalConditionTokens(stmt.condition)
        if (isTruthy(firstCond)) {
            executeBlock(stmt.body, Environment(enclosing = env))
            return
        }

        for (elif in stmt.elifBranches) {
            val condVal = evalConditionTokens(elif.condition)
            if (isTruthy(condVal)) {
                executeBlock(elif.body, Environment(enclosing = env))
                return
            }
        }

        stmt.elseBranch?.let { executeBlock(it, Environment(enclosing = env)) }
    }

    private fun executeWhile(stmt: ParseNode.StmtNode.ExecuteWhileNode) {
        while (isTruthy(evalConditionTokens(stmt.condition))) {
            executeBlock(stmt.body, Environment(enclosing = env))
        }
    }

    private fun executeFor(stmt: ParseNode.StmtNode.ExecuteForNode) {
        val target = evaluate(stmt.rangeExpr)
        val iterable = iterableForLoop(target, stmt.rangeKeyword, stmt.inKeyword)
        for (item in iterable) {
            val loopEnv = Environment(enclosing = env)
            loopEnv.define(stmt.selector.lexeme, item)
            executeBlock(stmt.body, loopEnv)
        }
    }

    private fun evalCall(expr: ParseNode.ExprNode.CallNode): Any? {
        val callee = evaluate(expr.callee)
        val arguments = expr.arguments.map { evaluate(it) }
        if (callee !is Callable) {
            throw RuntimeError(expr.paren, "Can only call functions.")
        }
        if (arguments.size != callee.arity()) {
            throw RuntimeError(expr.paren, "Expected ${callee.arity()} arguments but got ${arguments.size}.")
        }
        return callee.call(this, arguments)
    }

    private fun evalUnary(e: ParseNode.ExprNode.UnaryNode): Any? {
        val r = evaluate(e.operand)
        return when (e.operator.type) {
            TokenType.MINUS -> -asNumber(e.operator, r)
            TokenType.PLUS  -> +asNumber(e.operator, r)
            TokenType.TILDE -> asInt(e.operator, r).inv()
            TokenType.EXCL, TokenType.NOT -> !isTruthy(r)
            else -> throw RuntimeError(e.operator, "Unknown unary operator '${e.operator.lexeme}'")
        }
    }

    private fun evalBinary(b: ParseNode.ExprNode.BinaryNode): Any? {
        // short-circuit logical operators
        if (b.operator.type == TokenType.AND) {
            val left = evaluate(b.left)
            return if (isTruthy(left)) evaluate(b.right) else left
        }
        if (b.operator.type == TokenType.OR) {
            val left = evaluate(b.left)
            return if (isTruthy(left)) left else evaluate(b.right)
        }

        val l = evaluate(b.left)
        val r = evaluate(b.right)
        return when (b.operator.type) {
            // arithmetic
            TokenType.PLUS  -> plus(l, r, b.operator)
            TokenType.MINUS -> numeric(l, r, b.operator) { a, c -> a - c }
            TokenType.STAR  -> numeric(l, r, b.operator) { a, c -> a * c }
            TokenType.DOLLAR -> divide(l, r, b.operator, floorDiv = false)
            TokenType.DOLLAR_DOLLAR -> divide(l, r, b.operator, floorDiv = true)
            TokenType.PERCENT -> numeric(l, r, b.operator) { a, c -> a % c }
            TokenType.STAR_STAR -> numeric(l, r, b.operator) { a, c -> a.pow(c) }

            // shifts
            TokenType.LEFT_SHIFT -> (asInt(b.operator, l) shl asInt(b.operator, r))
            TokenType.RIGHT_SHIFT -> (asInt(b.operator, l) shr asInt(b.operator, r))

            // bitwise
            TokenType.AMPERSAND -> (asInt(b.operator, l) and asInt(b.operator, r))
            TokenType.PIPE      -> (asInt(b.operator, l) or  asInt(b.operator, r))
            TokenType.CARET     -> (asInt(b.operator, l) xor asInt(b.operator, r))

            // equality
            TokenType.EQUAL_EQUAL -> looseEq(l, r)
            TokenType.EXCL_EQUAL  -> !truthyBool(looseEq(l, r))
            TokenType.STRICT_EQUAL -> strictEq(l, r)

            // comparison + membership + identity
            TokenType.LESS         -> compare(b.operator, l, r) { a, c -> a <  c }
            TokenType.LESS_EQUAL   -> compare(b.operator, l, r) { a, c -> a <= c }
            TokenType.GREATER      -> compare(b.operator, l, r) { a, c -> a >  c }
            TokenType.GREATER_EQUAL-> compare(b.operator, l, r) { a, c -> a >= c }
            TokenType.IN     -> membership(l, r, b.operator, not = false)
            TokenType.NOT_IN -> membership(l, r, b.operator, not = true)
            TokenType.IS     -> identity(l, r, b.operator, not = false)
            TokenType.IS_NOT -> identity(l, r, b.operator, not = true)

            else -> throw RuntimeError(b.operator, "Unsupported operator '${b.operator.lexeme}'.")
        }
    }

    private fun evalPostfix(p: ParseNode.ExprNode.PostfixNode): Any? {
        val name = when (val operand = p.operand) {
            is ParseNode.ExprNode.LiteralNode -> operand.token.lexeme
            else -> throw RuntimeError(p.operator, "Postfix operand must be an identifier.")
        }
        val original = try { env.get(name) } catch (e: RuntimeError) {
            throw RuntimeError(p.operator, "Undefined variable $name.")
        }
        val n = toNumOrNull(original)
            ?: throw RuntimeError(p.operator, "Postfix operator requires numeric value.")
        val updated = when (p.operator.type) {
            TokenType.PLUS_PLUS -> n + 1
            TokenType.MINUS_MINUS -> n - 1
            else -> throw RuntimeError(p.operator, "Unsupported postfix operator '${p.operator.lexeme}'.")
        }
        val toStore: Any = when (original) {
            is Int -> updated.toInt()
            else -> updated
        }
        env.assign(name, toStore)
        return original
    }

    private fun declareFunction(stmt: ParseNode.StmtNode.FunctionDeclNode) {
        val fn = Function(
            name = stmt.name.lexeme,
            params = stmt.params,
            body = stmt.body,
            closure = env
        )
        env.define(stmt.name.lexeme, fn)
    }

    private fun resolveLiteral(token: Token): Any? = when (token.type) {
        TokenType.NUMBER -> token.literal
        TokenType.TRUE -> true
        TokenType.FALSE -> false
        TokenType.NIL -> null
        TokenType.IDENTIFIER -> env.get(token.lexeme)
        TokenType.STRING -> {
            val lex = token.literal ?: token.lexeme
            if (lex is String && (lex.startsWith("@") || lex.startsWith("#"))) {
                env.get(lex)
            } else if (lex is String) {
                try { env.get(lex) } catch (e: RuntimeError) { lex }
            } else lex
        }
        else -> token.literal ?: token.lexeme
    }

    fun stringify(value: Any?): String = when (value) {
        null -> "null"
        is Boolean -> value.toString()
        is Double -> {
            val i = value.toInt()
            if (i.toDouble() == value) i.toString() else value.toString()
        }
        is Int -> value.toString()
        is Callable -> value.toString()
        else -> value.toString()
    }

    private fun isTruthy(x: Any?): Boolean = when (x) {
        null -> false
        is Boolean -> x
        is Double -> x != 0.0
        is Int -> x != 0
        is String -> x.isNotEmpty()
        else -> true
    }
    private fun truthyBool(x: Any?) = (x as? Boolean) ?: isTruthy(x)

    private fun asNumber(opTok: Token, v: Any?): Double =
        when (v) {
            is Double -> v
            is Int -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: throw RuntimeError(opTok, "Operand must be a number.")
            is Boolean -> if (v) 1.0 else 0.0
            null -> throw RuntimeError(opTok, "Operand must be a number.")
            else -> throw RuntimeError(opTok, "Operand must be a number.")
        }

    private fun asInt(opTok: Token, v: Any?): Int = asNumber(opTok, v).toInt()

    private fun numeric(l: Any?, r: Any?, op: Token, f: (Double, Double) -> Double): Double {
        val a = asNumber(op, l)
        val b = asNumber(op, r)
        return f(a, b)
    }

    private fun plus(l: Any?, r: Any?, op: Token): Any? {
        val ln = (l as? String)?.toDoubleOrNull() ?: (l as? Double)
        val rn = (r as? String)?.toDoubleOrNull() ?: (r as? Double)
        return if (ln != null && rn != null) (ln + rn) else "${stringify(l)}${stringify(r)}"
    }

    private fun divide(l: Any?, r: Any?, op: Token, floorDiv: Boolean): Any {
        val a = asNumber(op, l)
        val b = asNumber(op, r)
        if (b == 0.0) throw RuntimeError(op, "Division by zero.")
        return if (floorDiv) floor(a / b).toInt() else a / b
    }

    private fun compare(tok: Token, l: Any?, r: Any?, cmp: (Double, Double) -> Boolean): Boolean {
        val a = asNumber(tok, l)
        val b = asNumber(tok, r)
        return cmp(a, b)
    }

    private fun looseEq(l: Any?, r: Any?): Boolean {
        if ((l is Boolean || l is String) && (r is Boolean || r is String)) {
            val lb = toBoolOrNull(l); val rb = toBoolOrNull(r)
            if (lb != null && rb != null) return lb == rb
        }
        val ln = toNumOrNull(l); val rn = toNumOrNull(r)
        if (ln != null && rn != null) return ln == rn
        return stringify(l) == stringify(r)
    }

    private fun strictEq(l: Any?, r: Any?): Boolean = when {
        l == null && r == null -> true
        l is Boolean && r is Boolean -> l == r
        (l is Double || l is Int) && (r is Double || r is Int) ->
            asNumber(Token(TokenType.NUMBER, "", null, 1), l) ==
            asNumber(Token(TokenType.NUMBER, "", null, 1), r)
        l is String && r is String -> l == r
        else -> false
    }

    private fun toBoolOrNull(x: Any?): Boolean? = when (x) {
        is Boolean -> x
        is String -> when (x) {
            "1","true","True","TRUE" -> true
            "0","false","False","FALSE" -> false
            else -> null
        }
        is Double -> if (x == 0.0 || x == 1.0) x == 1.0 else null
        is Int -> if (x == 0 || x == 1) x == 1 else null
        else -> null
    }

    private fun toNumOrNull(x: Any?): Double? = when (x) {
        is Double -> x
        is Int -> x.toDouble()
        is String -> x.toDoubleOrNull()
        is Boolean -> if (x) 1.0 else 0.0
        else -> null
    }

    private fun membership(l: Any?, r: Any?, tok: Token, not: Boolean): Boolean {
        val ok = (l is String && r is String && r.contains(l))
        return if (not) !ok else ok
    }

    private fun iterableForLoop(value: Any?, rangeKeyword: Token?, fallbackTok: Token): Iterable<Any?> {
        if (rangeKeyword != null) {
            val limit = try { asInt(rangeKeyword, value) } catch (e: RuntimeError) {
                throw RuntimeError(rangeKeyword, "Range limit must be numeric.")
            }
            val capped = if (limit < 0) 0 else limit
            return 0 until capped
        }

        return when (value) {
            null -> emptyList()
            is Iterable<*> -> value
            is Array<*> -> value.asList()
            is Map<*, *> -> value.entries
            is String -> value.toList()
            is Int -> 0 until (if (value < 0) 0 else value)
            is Double -> {
                val n = asInt(fallbackTok, value)
                0 until (if (n < 0) 0 else n)
            }
            else -> throw RuntimeError(fallbackTok, "For-loop target must be a number, string, map, array, or iterable.")
        }
    }

    private fun identity(l: Any?, r: Any?, tok: Token, not: Boolean): Boolean {
        val type = (r as? String)?.lowercase()
        val isType = when (type) {
            "int" -> l is Int || (l is Double && l % 1.0 == 0.0)
            "float", "double" -> l is Double
            "bool" -> l is Boolean
            "char" -> l is String && l.length == 1
            "string" -> l is String
            else -> false
        }
        return if (not) !isType else isType
    }

    private fun defaultValueForType(typeLexeme: String): Any? = when (typeLexeme.lowercase()) {
        "int" -> 0
        "float","double" -> 0.0
        "bool" -> false
        "char" -> ""
        "string" -> ""
        else -> null
    }

    private fun defineNativeFunctions() {
        env.define("clock", object : Callable {
            override fun arity() = 0
            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? =
                System.currentTimeMillis() / 1000.0
            override fun toString(): String = "<native fn clock>"
        })
        env.define("print", object : Callable {
            override fun arity() = 1
            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
                println(interpreter.stringify(arguments[0]))
                return null
            }
            override fun toString(): String = "<native fn print>"
        })
        env.define("toString", object : Callable {
            override fun arity() = 1
            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? =
                interpreter.stringify(arguments[0])
            override fun toString(): String = "<native fn toString>"
        })
    }

    private fun interpolate(raw: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < raw.length) {
            if (raw[i] == '{') {
                val end = raw.indexOf('}', i + 1)
                if (end > i) {
                    val inside = raw.substring(i + 1, end).trim()
                    val value = runCatching { evalInlineExpression(inside) }.getOrElse {
                        runCatching { env.get(inside) }.getOrNull()
                    }
                    sb.append(stringify(value ?: inside))
                    i = end + 1
                    continue
                }
            }
            sb.append(raw[i])
            i++
        }
        return sb.toString()
    }

    private fun evalInlineExpression(src: String): Any? {
        val tokens = Scanner(src).scanTokens()
        val parser = Parser(tokens)
        val expr = parser.parseExpression()
        return evaluate(expr)
    }

    // ==== Re-parse /execute condition ======================================
    private fun evalConditionTokens(tokens: List<Token>): Any? {
        val eofLine = tokens.lastOrNull()?.line ?: 1
        val tokensWithEof = tokens + Token(TokenType.EOF, "", null, eofLine)
        val parser = Parser(tokensWithEof)
        val expr = parser.parseExpression()
        return evaluate(expr)
    }
}
