package chatt.jbins

import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

data class JbinDatabase(private val adapter: JbinAdapter): JbinAdapter {

    private val logger = KotlinLogging.logger {}

    companion object {
        val createdFunctionsCache: MutableSet<String> = ConcurrentHashMap.newKeySet()
    }

    fun getTable(name: String): JbinTable {
        return JbinTable(name, this)
    }

    override fun executeUpdate(sql: String, parameters: List<Any>): Int {
        logger.debug { "Exec SQL: '$sql' with parameters: $parameters" }
        return adapter.executeUpdate(sql, parameters)
    }

    override fun executeQuery(sql: String, parameters: List<Any>): List<Any> {
        logger.debug { "Exec SQL: '$sql' with parameters: $parameters" }
        return adapter.executeQuery(sql, parameters)
    }

}