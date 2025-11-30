sealed class Stmt {
    data class Say(val message: String) : Stmt()
    ///for says
    data class Summon(val type: String, val name: String, val value: Double?) : Stmt()
    // /expr @var { <expression> }
    data class ExprAssign(val name: String, val expr: ParseNode.ExprNode) : Stmt()
    // /set @var <assign_op> <expression>
    data class Assign(val name: String, val op: Token, val expr: ParseNode.ExprNode) : Stmt()

    data class Execute(
        val condition: String,
        val thenBranch: List<Stmt>,
        val elseBranch: List<Stmt>?
    ) : Stmt()

    object Kill : Stmt()
}
