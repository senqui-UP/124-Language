class RuntimeError(val token: Token?, message: String) : RuntimeException(message)
class Break : RuntimeException(null, null, false, false)
class Continue : RuntimeException(null, null, false, false)
