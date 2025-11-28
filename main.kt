import java.io.File

fun main(args: Array<String>) {
    val interpreter = Interpreter()

    if (args.isNotEmpty()) {
        // Debug mode: dump tokens
        if (args[0] == "--dump-tokens" && args.size >= 2) {
            val path = args[1]
            val source = File(path).readText()
            val tokens = Scanner(source).scanTokens()
            for (t in tokens) println("${t.line}\t${t.type}\t'${t.lexeme}'")
            return
        }
        // Script mode: read entire file and execute
        else {
            val path = args[0]
            val source = File(path).readText()
            val scanner = Scanner(source)
            val tokens = scanner.scanTokens()
            val parser = Parser(tokens)
            try {
                val program = parser.parseProgram()
                for (stmt in program.statements) interpreter.execute(stmt)
            } catch (e: RuntimeError) {
                val line = e.token?.line ?: 1
                println("[line $line] Runtime error: ${e.message}")
            } catch (e: Exception) {
                println(e.message)
            }
            return
        }
    }

    // REPL mode
    println("Welcome to PyCraft (type '/kill' to exit)")
    while (true) {
        print("> ")
        val input = readln()
        if (input.trim() == "/kill") break

        val scanner = Scanner(input)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens)

        try {
            if (input.trim().startsWith("/")) {
                val program = parser.parseProgram()
                for (stmt in program.statements) interpreter.execute(stmt)
            } else {
                val expr = parser.parseExpression()
                // println("AST: ${AstPrinter().print(expr)}")          // AST Printer for Debugging
                val value = interpreter.evaluate(expr)
                println(interpreter.stringify(value))
            }
        } catch (e: RuntimeError) {
            val line = e.token?.line ?: 1
            println("[line $line] Runtime error: ${e.message}")
        } catch (e: Exception) {
            println(e.message)
        }
    }
}
