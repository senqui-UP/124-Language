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
