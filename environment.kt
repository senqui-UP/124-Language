class Environment(val enclosing: Environment? = null) {
    private val values = mutableMapOf<String, Any?>()

    fun define(name: String, value: Any?) { 
        values[name] = value 
    }

    fun get(name: String): Any? =
        if (name in values) values[name]
        else enclosing?.get(name)
            ?: throw RuntimeError(null, "Undefined variable $name.")

    fun assign(name: String, value: Any?) {
        if (name in values) {
            values[name] = value
        } else if (enclosing != null) {
            enclosing.assign(name, value)
        } else {
            throw RuntimeError(null, "Undefined variable $name.")
        }
    }
}