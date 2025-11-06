fun main() {
    println("Loading...")
    println("Welcome to PyCraft (type '/kill' to exit)")

    val printer = AstPrinter()

    while (true) {
        print("> ")
        val input = readln()
        if (input.trim() == "/kill") break

        val scanner = Scanner(input)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens)

        try {
            if (input.trim().startsWith("/")) {
                val statements = parser.parseStatements()
                for (stmt in statements) println(stmt)
            } else {
                val expr = parser.parseExpression()
                println(printer.print(expr))
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }
}
