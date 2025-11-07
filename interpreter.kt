import kotlin.math.floor
import kotlin.math.pow

class RuntimeError(val token: Token?, message: String) : RuntimeException(message)

class Environment(val enclosing: Environment? = null) {
    private val values = mutableMapOf<String, Any?>()

    fun define(name: String, value: Any?) { values[name] = value }

    fun get(name: String): Any? =
        if (name in values) values[name]
        else enclosing?.get(name)
            ?: throw RuntimeError(null, "Undefined variable $name.")

    fun assign(name: String, value: Any?) {
        if (name in values) {
            values[name] = value
        } else if (enclosing != null) {
            enclosing.assign(name, value)
        } else {
            throw RuntimeError(null, "Undefined variable $name.")
        }
    }
}

class Interpreter(
    initialEnv: Environment = Environment()
) {
    private var env: Environment = initialEnv
    // ===== Public API =====
    fun evaluate(expr: Expr): Any? = when (expr) {
        is Expr.Literal  -> resolveLiteral(expr.value)
        is Expr.Grouping -> evaluate(expr.expression)
        is Expr.Unary    -> evalUnary(expr)
        is Expr.Binary   -> evalBinary(expr)
        is Expr.Postfix  -> evalPostfix(expr)
    }

    fun execute(stmt: Stmt) {
        when (stmt) {
            is Stmt.Say -> {
                println(interpolate(stmt.message))
            }
            is Stmt.Summon -> {
                // /summon <type> @name [(number)] | bare number
                env.define(stmt.name, stmt.value ?: defaultValueForType(stmt.type))
            }
            is Stmt.ExprAssign -> {
                val v = evaluate(stmt.expr)
                env.assign(stmt.name, v)
            }
            is Stmt.Assign -> {
                val right = evaluate(stmt.expr)
                val current = env.get(stmt.name)
                val result = when (stmt.op.type) {
                    TokenType.EQUAL -> right
                    TokenType.PLUS_EQUAL -> plus(current, right, stmt.op)
                    TokenType.MINUS_EQUAL -> numeric(current, right, stmt.op) { a, b -> a - b }
                    TokenType.STAR_EQUAL -> numeric(current, right, stmt.op) { a, b -> a * b }
                    TokenType.DOLLAR_EQUAL -> divide(current, right, stmt.op, floorDiv=false)
                    TokenType.PERCENT_EQUAL -> numeric(current, right, stmt.op) { a, b -> a % b }
                    TokenType.STAR_STAR_EQUAL -> numeric(current, right, stmt.op) { a, b -> a.pow(b) }
                    else -> throw RuntimeError(stmt.op, "Unsupported assignment operator.")
                }
                env.assign(stmt.name, result)
            }
            is Stmt.Execute -> {
                // Your parser currently stores the condition as a *raw string*.
                // We evaluate truthiness by rescanning & parsing that expression now.
                val condVal = evalConditionString(stmt.condition)
                val branch = if (isTruthy(condVal)) stmt.thenBranch else stmt.elseBranch
                if (branch != null) executeBlock(branch, Environment(enclosing = env))
            }
            is Stmt.Kill -> { /* handled by main loop */ }
        }
    }

    private fun executeBlock(statements: List<Stmt>, newEnv: Environment) {
        val previous = env
        try {
            env = newEnv
            for (s in statements) execute(s)
        } finally {
            env = previous
        }
    }

    // ===== Expression evaluation =====

    private fun evalUnary(e: Expr.Unary): Any? {
        val r = evaluate(e.right)
        return when (e.operator.type) {
            TokenType.MINUS -> -asNumber(e.operator, r)
            TokenType.PLUS  -> +asNumber(e.operator, r)
            TokenType.TILDE -> asInt(e.operator, r).inv()
            TokenType.EXCL, TokenType.NOT -> !isTruthy(r)
            else -> throw RuntimeError(e.operator, "Unknown unary operator '${e.operator.lexeme}'")
        }
    }

    private fun evalBinary(b: Expr.Binary): Any? {
        val l = evaluate(b.left)
        val r = evaluate(b.right)
        return when (b.operator.type) {
            // arithmetic
            TokenType.PLUS  -> plus(l, r, b.operator)
            TokenType.MINUS -> numeric(l, r, b.operator) { a, c -> a - c }
            TokenType.STAR  -> numeric(l, r, b.operator) { a, c -> a * c }
            TokenType.DOLLAR -> divide(l, r, b.operator, floorDiv=false)
            TokenType.DOLLAR_DOLLAR -> divide(l, r, b.operator, floorDiv=true)
            TokenType.PERCENT -> numeric(l, r, b.operator) { a, c -> a % c }
            TokenType.STAR_STAR -> numeric(l, r, b.operator) { a, c -> a.pow(c) }

            // shifts
            TokenType.LEFT_SHIFT -> (asInt(b.operator, l) shl asInt(b.operator, r))
            TokenType.RIGHT_SHIFT -> (asInt(b.operator, l) shr asInt(b.operator, r))

            // bitwise
            TokenType.AMPERSAND -> (asInt(b.operator, l) and asInt(b.operator, r))
            TokenType.PIPE      -> (asInt(b.operator, l) or  asInt(b.operator, r))
            TokenType.CARET     -> (asInt(b.operator, l) xor asInt(b.operator, r))

            // logical already parsed as binary "and/or"
            TokenType.AND -> if (isTruthy(l)) r else l
            TokenType.OR  -> if (isTruthy(l)) l else r

            // equality
            TokenType.EQUAL_EQUAL -> looseEq(l, r)
            TokenType.EXCL_EQUAL  -> !truthyBool(looseEq(l, r))
            TokenType.STRICT_EQUAL -> strictEq(l, r)

            // comparison + membership + identity
            TokenType.LESS         -> compare(b.operator, l, r) { a, c -> a <  c }
            TokenType.LESS_EQUAL   -> compare(b.operator, l, r) { a, c -> a <= c }
            TokenType.GREATER      -> compare(b.operator, l, r) { a, c -> a >  c }
            TokenType.GREATER_EQUAL-> compare(b.operator, l, r) { a, c -> a >= c }
            TokenType.IN     -> membership(l, r, b.operator, not=false)
            TokenType.NOT_IN -> membership(l, r, b.operator, not=true)
            TokenType.IS     -> identity(l, r, b.operator, not=false)
            TokenType.IS_NOT -> identity(l, r, b.operator, not=true)

            else -> throw RuntimeError(b.operator, "Unsupported operator '${b.operator.lexeme}'.")
        }
    }

    private fun evalPostfix(p: Expr.Postfix): Any? {
        val original = try { env.get(p.name) } catch (e: RuntimeError) {
            throw RuntimeError(p.operator, "Undefined variable ${p.name}.")
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
        env.assign(p.name, toStore)
        return original
    }

    // ===== Conversions & helpers =====

    // Variables are scanned as STRINGs like "@x" (per current primary rule); resolve them here.
    private fun resolveLiteral(v: Any?): Any? {
        if (v is String && v.startsWith("@") && v.length > 1) {
            return env.get(v) // @var
        }
        return v
    }

    fun stringify(value: Any?): String = when (value) {
        null -> "nil"
        is Boolean -> value.toString()
        is Double -> {
            val i = value.toInt()
            if (i.toDouble() == value) i.toString() else value.toString()
        }
        else -> value.toString()
    }

    private fun isTruthy(x: Any?): Boolean {
        return when (x) {
            null -> false
            is Boolean -> x
            is Double -> x != 0.0
            is Int -> x != 0
            is String -> x.isNotEmpty() // simple rule: strings are truthy iff non-empty
            else -> true
        }
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
        // Implicit conversion precedence: Boolean → Numeric → String
        // If both numeric-parsable -> numeric add; else -> concatenate as strings
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
        // Try boolean → numeric → string
        if ((l is Boolean || l is String) && (r is Boolean || r is String)) {
            val lb = toBoolOrNull(l); val rb = toBoolOrNull(r)
            if (lb != null && rb != null) return lb == rb
        }
        val ln = toNumOrNull(l); val rn = toNumOrNull(r)
        if (ln != null && rn != null) return ln == rn
        return stringify(l) == stringify(r)
    }

    private fun strictEq(l: Any?, r: Any?): Boolean {
        // Same “type class” and same value
        return when {
            l == null && r == null -> true
            l is Boolean && r is Boolean -> l == r
            (l is Double || l is Int) && (r is Double || r is Int) -> asNumber(Token(TokenType.NUMBER,"",null,1), l) ==
                                                                     asNumber(Token(TokenType.NUMBER,"",null,1), r)
            l is String && r is String -> l == r
            else -> false
        }
    }

    private fun toBoolOrNull(x: Any?): Boolean? = when (x) {
        is Boolean -> x
        is String -> when (x) {
            "1","true","True","TRUE" -> true
            "0","false","False","FALSE" -> false
            else -> null
        }
        is Double -> if (x == 0.0 || x == 1.0) x == 1.0 else null
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
        // Strings only for now (e.g., "ell" in "hello")
        val ok = (l is String && r is String && r.contains(l))
        return if (not) !ok else ok
    }

    private fun identity(l: Any?, r: Any?, tok: Token, not: Boolean): Boolean {
        // “is” against type names (right operand a String type label): int|float|double|bool|char|String
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

    // No longer used; keep as helper if needed
    private fun safeGet(name: String, stmt: Stmt): Any? =
        try { env.get(name) } catch (_: RuntimeError) { null }

    // ===== String interpolation for /say =====
    // supports: literal text + {@var} or {#...} (we only resolve @var here)
    private fun interpolate(raw: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < raw.length) {
            if (raw[i] == '{') {
                val end = raw.indexOf('}', i+1)
                if (end > i) {
                    val inside = raw.substring(i+1, end).trim()
                    val value = if (inside.startsWith("@")) {
                        stringify(runCatching { env.get(inside) }.getOrNull())
                    } else {
                        // future: function call evaluation {#func(...)}
                        inside
                    }
                    sb.append(value)
                    i = end + 1
                    continue
                }
            }
            sb.append(raw[i])
            i++
        }
        return sb.toString()
    }

    // ===== Re-parse /execute condition (current parser stores raw string) =====
    private fun evalConditionString(cond: String): Any? {
        val scanner = Scanner(cond)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens)
        val expr = parser.parseExpression()
        return evaluate(expr)
    }
}
