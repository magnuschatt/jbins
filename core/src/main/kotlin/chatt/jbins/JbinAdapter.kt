package chatt.jbins

interface JbinAdapter {
    fun executeUpdate(sql: String, parameters: List<Any> = emptyList()): Int
    fun executeQuery(sql: String, parameters: List<Any> = emptyList()): List<Any?>
}