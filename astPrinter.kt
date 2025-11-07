class AstPrinter {
    fun print(expr: Expr): String = when (expr) {
        is Expr.Literal -> when (val v = expr.value) {
            is Double -> v.toString()
            is String -> if (v.contains(' ')) v else v // keep as-is
            is Boolean -> v.toString()
            null -> "nil"
            else -> v.toString()
        }
        is Expr.Grouping -> "(group ${print(expr.expression)})"
        is Expr.Unary -> "(${expr.operator.lexeme} ${print(expr.right)})"
        is Expr.Binary -> "(${expr.operator.lexeme} ${print(expr.left)} ${print(expr.right)})"
        is Expr.Postfix -> "(${expr.name}${expr.operator.lexeme})"
    }
}
