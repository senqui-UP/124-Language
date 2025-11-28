//allows polymorphism by uniting all subclasses into one class
sealed class Expr {
    data class Literal(val value: Any?)                                     : Expr()
    data class Grouping(val expression: Expr)                               : Expr()
    data class Unary(val operator: Token, val right: Expr)                  : Expr()
    data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr()
    // Postfix ++/-- on a variable (name like "@x")
    data class Postfix(val name: String, val operator: Token)               : Expr()
}
