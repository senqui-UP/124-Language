sealed class ParseNode {
    // Statement nodes
    sealed class StmtNode : ParseNode() {
        data class SayNode(val keyword: Token, val message: Token) : StmtNode()
        data class SummonNode(val keyword: Token, val type: Token, val name: Token, val value: Token?) : StmtNode()
        data class SetNode(val keyword: Token, val name: Token, val op: Token, val expr: ExprNode) : StmtNode()
        data class ExecuteNode(
            val keyword: Token,
            val condition: List<Token>,
            val runKeyword: Token,
            val body: List<StmtNode>,
            val elseKeyword: Token?,
            val elseBranch: List<StmtNode>?
        ) : StmtNode()
        data class ExecuteForNode(
            val keyword: Token,
            val forKeyword: Token,
            val selector: Token,
            val runKeyword: Token,
            val body: List<StmtNode>
        ) : StmtNode()
        data class KillNode(val keyword: Token) : StmtNode()
    }

    // Expression nodes
    sealed class ExprNode : ParseNode() {
        data class BinaryNode(val left: ExprNode, val operator: Token, val right: ExprNode) : ExprNode()
        data class UnaryNode(val operator: Token, val operand: ExprNode) : ExprNode()
        data class LiteralNode(val token: Token) : ExprNode()
        data class GroupingNode(val leftParen: Token, val expr: ExprNode, val rightParen: Token) : ExprNode()
        data class PostfixNode(val operand: ExprNode, val operator: Token) : ExprNode()
    }

    // Root
    data class ProgramNode(val statements: List<StmtNode>) : ParseNode()
}