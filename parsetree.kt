sealed class ParseNode {
    sealed class StmtNode : ParseNode() {
        data class SayNode(
            val keyword: Token, 
            val message: Token
        ) : StmtNode()
        data class SourceNode(
            val keyword: Token,
            val type: Token?,
            val name: Token,
            val promptTokens: List<Token>
        ) : StmtNode()
        data class SummonNode(
            val keyword: Token, 
            val type: Token, 
            val name: Token, 
            val value: Token?
        ) : StmtNode()
        // data class ExprAssignNode(val keyword: Token, val name: Token, val expr: ExprNode) : StmtNode()
        data class SetNode(
            val keyword: Token, 
            val name: Token, 
            val op: Token, 
            val expr: ExprNode
        ) : StmtNode()
        data class FunctionDeclNode(
            val keyword: Token,
            val name: Token,
            val params: List<Token>,
            val body: List<StmtNode>
        ) : StmtNode()
        data class ReturnNode(
            val keyword: Token, 
            val value: ExprNode?
        ) : StmtNode()
        data class ExecuteIfNode(
            val keyword: Token,
            val condition: List<Token>,
            val runKeyword: Token,
            val body: List<StmtNode>,
            val elifBranches: List<ElifBranch>,
            val elseKeyword: Token?,
            val elseBranch: List<StmtNode>?
        ) : StmtNode()
        data class ElifBranch(
            val keyword: Token,
            val condition: List<Token>,
            val runKeyword: Token,
            val body: List<StmtNode>
        )
        data class ExecuteWhileNode(
            val keyword: Token,
            val condition: List<Token>,
            val runKeyword: Token,
            val body: List<StmtNode>
        ) : StmtNode()
        data class ExecuteForNode(
            val keyword: Token,
            val selector: Token,
            val inKeyword: Token,
            val rangeKeyword: Token?, // null when iterating directly over a collection
            val rangeExpr: ExprNode,
            val runKeyword: Token,
            val body: List<StmtNode>
        ) : StmtNode()
        data class BreakNode(
            val keyword: Token
        ) : StmtNode()
        data class ContinueNode(
            val keyword: Token
        ) : StmtNode()
        data class KillNode(
            val keyword: Token
        ) : StmtNode()
    }

    sealed class ExprNode : ParseNode() {
        data class BinaryNode(
            val left: ExprNode, 
            val operator: Token, 
            val right: ExprNode
        ) : ExprNode()
        data class UnaryNode(
            val operator: Token, 
            val operand: ExprNode
        ) : ExprNode()
        data class LiteralNode(
            val token: Token
        ) : ExprNode()
        data class GroupingNode(
            val leftParen: Token, 
            val expr: ExprNode, 
            val rightParen: Token
        ) : ExprNode()
        data class PostfixNode(
            val operand: ExprNode, 
            val operator: Token
        ) : ExprNode()
        data class CallNode(
            val callee: ExprNode, 
            val paren: Token, 
            val arguments: List<ExprNode>
        ) : ExprNode()
    }

    data class ProgramNode(
        val statements: List<StmtNode>
    ) : ParseNode()
}
