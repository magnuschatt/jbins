package chatt.jbins

import chatt.jbins.JbinDocument.Companion.ID_PATH
import chatt.jbins.JbinFilter.*
import chatt.jbins.JbinFilter.Comparator.EQ
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

    fun replaceOne(document: JbinDocument, where: JbinFilter = True()): Boolean {
        val andFilter = And(Match(ID_PATH, EQ, document.id), where)
        val translation = JbinFilterTranslator.translate(andFilter)
        database.createFunctionsIfNotExists(translation.functions)

        val sql = "UPDATE \"$name\" SET body = CAST(? AS JSONB) WHERE ${translation.sql}"
        val params = listOf(document.body) + translation.params
        return database.executeUpdate(sql, params) == 1
    }

    fun patch(where: JbinFilter = True(), path: String, newJson: String, createMissing: Boolean = true): Int {
        val translation = JbinFilterTranslator.translate(where)
        database.createFunctionsIfNotExists(translation.functions)
        val elements = splitToElements(path)

        if (elements.any { it.isArray }) {
            throw IllegalArgumentException("Arrays not allowed in updateWhere path: $path")
        }

        val pathPart = elements.joinToString(separator = ",", prefix = "{", postfix = "}") { it.name }
        val params = listOf(pathPart, newJson, createMissing) + translation.params
        val sql = "UPDATE \"$name\" SET body = jsonb_set(body, CAST(? AS TEXT[]), CAST(? AS JSONB), ?) WHERE ${translation.sql}"
        return database.executeUpdate(sql, params)
    }

    fun delete(vararg documents: JbinDocument): Int = delete(documents.toList())
    fun delete(documents: Collection<JbinDocument>): Int = deleteById(documents.map { it.id })
    fun deleteById(vararg ids: String): Int = deleteById(ids.toList())
    fun deleteById(ids: Collection<String>): Int {
        if (ids.isEmpty()) return 0
        return delete(Or(ids.map { Match(ID_PATH, EQ, it) }))
    }

    fun delete(where: JbinFilter = True()): Int {
        val (filterSql, params, functions) = JbinFilterTranslator.translate(where)
        database.createFunctionsIfNotExists(functions)
        val sql = "DELETE FROM \"$name\" WHERE $filterSql"
        return database.executeUpdate(sql, params)
    }

    fun selectOneById(id: String): JbinDocument? = selectById(id).firstOrNull()
    fun selectById(vararg ids: String): List<JbinDocument> = selectById(ids.toList())
    fun selectById(ids: Collection<String>): List<JbinDocument> = select(Or(ids.map { Match(ID_PATH, EQ, it) }))

    fun select(where: JbinFilter = True(),
               limit: Int = 0,
               orderBy: List<Pair<String, SortDirection>> = emptyList()): List<JbinDocument> {

        val translation = JbinFilterTranslator.translate(where)
        val functions = translation.functions.toMutableSet()
        var sql = "SELECT id, CAST(body AS TEXT) FROM \"$name\" WHERE ${translation.sql}"

        if (orderBy.isNotEmpty()) {
            sql += " ORDER BY " + orderBy.joinToString { (path, sortDirection) ->
                val function = getPostgresFunction(path).also { functions.add(it) }
                return@joinToString "${function.name}(body) ${sortDirection.name}"
            }
        }

        if (limit > 0) {
            sql += " LIMIT $limit"
        }

        database.createFunctionsIfNotExists(functions)

        return database.executeQuery(sql, translation.params).toDocuments()
    }

    fun createIndex(vararg paths: String) = createIndex(paths.toList())
    fun createIndex(paths: List<String>) {

        val functions = paths.map { getPostgresFunction(it) }
        database.createFunctionsIfNotExists(functions)

        val indexName = functions
                .joinToString(separator = "_\$\$_", transform = { it.name })
                .replace("jbins_func_", "") + "_jbins_index"

        val indexExpression = functions
                .joinToString(separator = ", ", transform = { "${it.name}(body)" })

        val arrayIndex = paths.flatMap { splitToElements(it) }.any { it.isArray }
        val ginPart = if (arrayIndex) "USING GIN " else ""
        val sql = "CREATE INDEX IF NOT EXISTS $indexName ON \"$name\" $ginPart($indexExpression)"
        database.executeUpdate(sql)
    }

    private fun List<Any>.toDocuments(): List<JbinDocument> {
        return this.map { it as Array<*> }.map { JbinDocument(it[0].toString(), it[1].toString()) }
    }

}