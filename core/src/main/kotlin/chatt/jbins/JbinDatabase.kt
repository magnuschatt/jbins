package chatt.jbins

import chatt.jbins.utils.ILIKE_OPERATOR
import chatt.jbins.utils.LIKE_OPERATOR
import chatt.jbins.utils.PostgresFunction
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

data class JbinDatabase(private val adapter: JbinAdapter): JbinAdapter {

    private val logger = KotlinLogging.logger {}

    private val tempTableName = "jbins_func_temp_table"
    private val createTempTable = "CREATE TEMPORARY TABLE IF NOT EXISTS $tempTableName(name VARCHAR(255))"
    private val insertTempTable = "INSERT INTO $tempTableName (name) VALUES (?)"

    companion object {
        val createdFunctionsCache: MutableSet<String> = ConcurrentHashMap.newKeySet()
    }

    init {
        initLikeOperator("flipped_like", LIKE_OPERATOR, "LIKE")
        initLikeOperator("flipped_ilike", ILIKE_OPERATOR, "ILIKE")
    }

    private fun initLikeOperator(funcName: String, operator: String, like: String) {
        if (functionExists(funcName)) return

        val funcParams = "(text, text)"
        val dropOperatorSql = "DROP OPERATOR IF EXISTS $operator $funcParams;"
        val createFuncSql =
                "CREATE OR REPLACE FUNCTION $funcName$funcParams RETURNS bool AS " +
                "'SELECT $2 $like $1;' LANGUAGE SQL IMMUTABLE;"
        val createOperatorSql =
                "CREATE OPERATOR $operator " +
                "(PROCEDURE = $funcName$funcParams, LEFTARG = text, RIGHTARG = text);"
        executeUpdate(dropOperatorSql, emptyList())
        executeUpdate(createFuncSql, emptyList())
        executeUpdate(createOperatorSql, emptyList())
    }

    fun getTable(name: String): JbinTable {
        return JbinTable(name, this)
    }

    fun createFunctionsIfNotExists(functions: Iterable<PostgresFunction>) {
        functions.forEach { func ->
            if (!JbinDatabase.createdFunctionsCache.contains(func.name)) {
                executeUpdate(createTempTable)

                if (!functionExists(func.name)) {
                    executeUpdate(insertTempTable, listOf(func.name))
                    executeUpdate(func.sql)
                } else if (!functionCreatedRecently(func.name)) {
                    JbinDatabase.createdFunctionsCache.add(func.name)
                }
            }
        }
    }

    private fun functionCreatedRecently(funcName: String): Boolean {
        val select = executeQuery("SELECT COUNT(*) FROM $tempTableName WHERE name = ?", listOf(funcName))
        return select.first() != 0.toBigInteger()
    }

    private fun functionExists(funcName: String): Boolean {
        val count = executeQuery("SELECT COUNT(*) FROM pg_proc WHERE proname = ?", listOf(funcName))
        return count.first() != 0.toBigInteger()
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