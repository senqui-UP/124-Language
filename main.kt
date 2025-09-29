fun main() {
    println("Loading...")
    println("Welcome to PyCraft (type 'quit' to exit)")

    while (true) {
        print("> ")
        val input = readln()

        if (input.trim() == "quit") break

        val scanner = Scanner(input)
        val tokens = scanner.scanTokens()

        for (token in tokens) {
            println(token)
        }
    }
}


enum class TokenType {
    // Arithmetic
    PLUS, MINUS, STAR, DIV, FLOOR_DIV, MOD, POWER,

    // Literals
    IDENTIFIER, STRING, NUMBER,

    // Keywords
    FUNCTION, KILL, SAY, INPUT, SUMMON, EXPR,
    EXECUTE_IF, EXECUTE_FOR, EXECUTE_WHILE,
    RUN, GAMERULE, EFFECT, TEAM,
    AND, AS, BREAK, PASS,

    EOF
}

data class Token(
    val type: TokenType,
    val lexeme: String,
    val literal: Any?,
    val line: Int
)
