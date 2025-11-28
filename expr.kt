//allows polymorphism by uniting all subclasses into one class
sealed class Expr {
    //compacting literal types into one node extends expr
    data class Literal(val value: Any?) : Expr()
    data class Grouping(val expression: Expr) : Expr()
    //
    data class Unary(val operator: Token, val right: Expr) : Expr()
    data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr()
    data class Postfix(val name: String, val operator: Token) : Expr()
}
