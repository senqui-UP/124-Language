sealed class Stmt {
    data class Say(val message: String) : Stmt()
    data class Summon(val type: String, val name: String, val value: Double?) : Stmt()
    data class ExprAssign(val name: String, val expr: String) : Stmt()
    data class Execute(val condition: String, val thenBranch: List<Stmt>, val elseBranch: List<Stmt>?) : Stmt()
        object Kill : Stmt()

}
