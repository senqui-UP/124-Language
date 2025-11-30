class Function(
    private val name: String,
    private val params: List<Token>,
    private val body: List<ParseNode.StmtNode>,
    private val closure: Environment
) : Callable {
    override fun arity(): Int = params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val localEnv = Environment(enclosing = closure)
        params.forEachIndexed { idx, param ->
            localEnv.define(param.lexeme, arguments[idx])
        }
        return try {
            interpreter.executeBlock(body, localEnv)
            null
        } catch (r: Return) {
            r.value
        }
    }

    override fun toString(): String = "<fn $name>"
}

