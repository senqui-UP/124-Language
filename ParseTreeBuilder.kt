object ParseTreeBuilder {
    fun fromProgram(program: Program): ParseTreeNode =
        ParseTreeNode(
            label = "Program",
            children = program.statements.map { stmtNode(it) }
        )

    private fun stmtNode(stmt: Stmt): ParseTreeNode = when (stmt) {
        is Stmt.Say -> ParseTreeNode(
            label = "Say",
            children = listOf(valueNode("message", stmt.message))
        )
        is Stmt.Summon -> ParseTreeNode(
            label = "Summon",
            children = listOf(
                valueNode("type", stmt.type),
                valueNode("name", stmt.name),
                stmt.value?.let { valueNode("value", it.toString()) } ?: ParseTreeNode("value", "nil")
            )
        )
        is Stmt.ExprAssign -> ParseTreeNode(
            label = "ExprAssign",
            children = listOf(
                valueNode("name", stmt.name),
                exprNode(stmt.expr)
            )
        )
        is Stmt.Assign -> ParseTreeNode(
            label = "Assign ${stmt.op.lexeme}",
            children = listOf(
                valueNode("name", stmt.name),
                exprNode(stmt.expr)
            )
        )
        is Stmt.Execute -> ParseTreeNode(
            label = "Execute",
            children = listOfNotNull(
                ParseTreeNode("condition", stmt.condition),
                ParseTreeNode(
                    label = "then",
                    children = stmt.thenBranch.map { stmtNode(it) }
                ),
                stmt.elseBranch?.let { elseBranch ->
                    ParseTreeNode("else", children = elseBranch.map { stmtNode(it) })
                }
            )
        )
        is Stmt.Kill -> ParseTreeNode("Kill")
    }

    private fun exprNode(expr: Expr): ParseTreeNode = when (expr) {
        is Expr.Literal -> ParseTreeNode("Literal", expr.value?.toString() ?: "nil")
        is Expr.Grouping -> ParseTreeNode(
            label = "Grouping",
            children = listOf(exprNode(expr.expression))
        )
        is Expr.Unary -> ParseTreeNode(
            label = "Unary ${expr.operator.lexeme}",
            children = listOf(exprNode(expr.right))
        )
        is Expr.Binary -> ParseTreeNode(
            label = "Binary ${expr.operator.lexeme}",
            children = listOf(
                exprNode(expr.left),
                exprNode(expr.right)
            )
        )
        is Expr.Postfix -> ParseTreeNode(
            label = "Postfix ${expr.operator.lexeme}",
            children = listOf(valueNode("name", expr.name))
        )
    }

    private fun valueNode(label: String, raw: String): ParseTreeNode =
        ParseTreeNode(label, raw)
}
