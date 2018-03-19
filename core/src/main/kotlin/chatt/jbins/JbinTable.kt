package chatt.jbins

import chatt.jbins.JbinDocument.Companion.ID_PATH
import chatt.jbins.JbinFilter.*
import chatt.jbins.JbinFilter.Comparator.EQ
import chatt.jbins.utils.PostgresFunction
import chatt.jbins.utils.getPostgresFunction
import chatt.jbins.utils.splitToElements

data class JbinTable(private val name: String,
                     private val database: JbinDatabase) {

    private val idSize = 255
    private val idColumnDef = "id VARCHAR($idSize) PRIMARY KEY"
    private val bodyColumnDef = "body JSONB NOT NULL"

    fun create(ifNotExists: Boolean = true) {
        val ifNotExistsPart = if (ifNotExists) "IF NOT EXISTS" else ""
        val sql = "CREATE TABLE $ifNotExistsPart \"$name\" ($idColumnDef, $bodyColumnDef)"
        database.executeUpdate(sql)
    }

    fun drop(ifExists: Boolean = true) {
        val ifNotExistsPart = if (ifExists) "IF EXISTS" else ""
        val sql = "DROP TABLE $ifNotExistsPart \"$name\""
        database.executeUpdate(sql)
    }

    fun insert(vararg documents: JbinDocument) = insert(documents.toList())
    fun insert(documents: Collection<JbinDocument>) {
        if (documents.isEmpty()) return
        val valuesSql = documents.joinToString(separator = ", ", transform = { "(?, CAST(? AS JSONB))" })
        val sql = "INSERT INTO \"$name\" (id, body) VALUES $valuesSql"
        val params = documents.flatMap { listOf(it.id, it.body) }
        database.executeUpdate(sql, params)
    }

    fun replaceOne(document: JbinDocument) = replaceOneWhere(document, null)
    fun replaceOneWhere(document: JbinDocument, filter: JbinFilter?): Boolean {
        val idMatch = Match(ID_PATH, EQ, document.id)
        val andFilter = if (filter == null) idMatch else And(idMatch, filter)
        val translation = JbinFilterTranslator.translate(andFilter)
        createFunctionsIfNotExists(translation.functions)

        val sql = "UPDATE \"$name\" SET body = CAST(? AS JSONB) WHERE ${translation.sql}"
        val params = listOf(document.body) + translation.params
        return database.executeUpdate(sql, params) == 1
    }

    fun patchWhere(filter: JbinFilter?, path: String, newValue: String): Int {
        val translation = JbinFilterTranslator.translate(filter)
        createFunctionsIfNotExists(translation.functions)
        val elements = splitToElements(path)

        if (elements.any { it.isArray }) {
            throw IllegalArgumentException("Arrays not allowed in updateWhere path: $path")
        }

        val pathPart = elements.joinToString(separator = ",", prefix = "{", postfix = "}") { it.name }
        val params = listOf(pathPart, newValue) + translation.params
        val sql = "UPDATE \"$name\" SET body = jsonb_set(body, CAST(? AS TEXT[]), to_jsonb(?)) WHERE ${translation.sql}"
        return database.executeUpdate(sql, params)
    }

    fun delete(vararg documents: JbinDocument): Int = delete(documents.toList())
    fun delete(documents: Collection<JbinDocument>): Int = deleteById(documents.map { it.id })
    fun deleteById(vararg ids: String): Int = deleteById(ids.toList())
    fun deleteById(ids: Collection<String>): Int {
        if (ids.isEmpty()) return 0
        return deleteWhere(Or(ids.map { Match(ID_PATH, EQ, it) }))
    }

    fun deleteAll() = deleteWhere(null)
    fun deleteWhere(filter: JbinFilter?): Int {
        val (filterSql, params, functions) = JbinFilterTranslator.translate(filter)
        createFunctionsIfNotExists(functions)
        val sql = "DELETE FROM \"$name\" WHERE $filterSql"
        return database.executeUpdate(sql, params)
    }

    fun selectOneById(id: String): JbinDocument? = selectById(id).firstOrNull()
    fun selectById(vararg ids: String): List<JbinDocument> = selectById(ids.toList())
    fun selectById(ids: Collection<String>): List<JbinDocument> = selectWhere(Or(ids.map { Match(ID_PATH, EQ, it) }))
    fun selectAll(): List<JbinDocument> = selectWhere(null)
    fun selectWhere(filter: JbinFilter?): List<JbinDocument> {
        val (filterSql, params, functions) = JbinFilterTranslator.translate(filter)
        createFunctionsIfNotExists(functions)
        val sql = "SELECT id, CAST(body AS TEXT) FROM \"$name\" WHERE $filterSql"
        return database.executeQuery(sql, params).toDocuments()
    }

    fun createIndex(path: String) {
        val elements = splitToElements(path)
        val func = getPostgresFunction(path)

        createFunctionsIfNotExists(listOf(func))

        val arrayIndex = elements.any { it.isArray }
        val ginPart = if (arrayIndex) "USING GIN " else ""
        val sql = "CREATE INDEX IF NOT EXISTS ${func.name}_index ON \"$name\" $ginPart(${func.name}(body))"
        database.executeUpdate(sql)
    }

    private fun createFunctionsIfNotExists(functions: Iterable<PostgresFunction>) {
        functions.forEach { func ->
            if (!JbinDatabase.createdFunctionsCache.contains(func.name)) {
                if (!database.functionExists(func.name)) database.executeUpdate(func.sql)
                JbinDatabase.createdFunctionsCache.add(func.name)
            }
        }
    }

    private fun List<Any>.toDocuments(): List<JbinDocument> {
        return this.map { it as Array<*> }.map { JbinDocument(it[0].toString(), it[1].toString()) }
    }

}