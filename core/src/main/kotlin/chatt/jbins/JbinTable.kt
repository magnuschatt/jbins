package chatt.jbins

data class JbinTable(private val name: String,
                     private val database: JbinDatabase) {

    private val idSize = 255
    private val idColumnDef = "id VARCHAR($idSize) PRIMARY KEY"
    private val bodyColumnDef = "body JSONB NOT NULL"

    fun createIfNotExists() {
        val sql = "CREATE TABLE IF NOT EXISTS \"$name\" ($idColumnDef, $bodyColumnDef)"
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

    fun replaceOne(document: JbinDocument): Boolean {
        val sql = "UPDATE \"$name\" SET body = CAST(? AS JSONB) WHERE id = ?"
        val params = listOf(document.body, document.id)
        return database.executeUpdate(sql, params) == 1
    }

    fun replaceOneWhere(document: JbinDocument, filter: JbinFilter): Boolean {
        val translation = JbinFilterTranslator.translate(filter)
        createFunctionsIfNotExists(translation.functions)
        val sql = "UPDATE \"$name\" SET body = CAST(? AS JSONB) WHERE id = ? AND ${translation.sql}"
        val params = listOf(document.body, document.id) + translation.params
        return database.executeUpdate(sql, params) == 1
    }

    fun patchWhere(filter: JbinFilter, path: String, newValue: String): Int {
        val translation = JbinFilterTranslator.translate(filter)
        createFunctionsIfNotExists(translation.functions)
        val elements = splitToElements(path)

        if (elements.any { it.isArray }) {
            throw IllegalArgumentException("Arrays not allowed in updateWhere path: " + path)
        }

        val pathPart = elements.joinToString(separator = ",", prefix = "{", postfix = "}") { it.name }
        val params = listOf(pathPart, newValue) + translation.params
        val sql = "UPDATE \"$name\" SET body = jsonb_set(body, CAST(? AS TEXT[]), to_jsonb(?)) WHERE ${translation.sql}"
        return database.executeUpdate(sql, params)
    }

    fun patchById(vararg ids: String, path: String, newValue: String): Int = patchById(ids.toList(), path, newValue)
    fun patchById(ids: Collection<String>, path: String, newValue: String): Int {
        return 0
    }

    fun delete(vararg documents: JbinDocument): Int = delete(documents.toList())
    fun delete(documents: Collection<JbinDocument>): Int = deleteById(documents.map { it.id })
    fun deleteById(vararg ids: String): Int = deleteById(ids.toList())
    private fun deleteById(ids: Collection<String>): Int {
        if (ids.isEmpty()) return 0
        val whereSql = ids.joinToString(separator = " OR ", transform = { "id = ?" })
        val sql = "DELETE FROM \"$name\" WHERE $whereSql"
        val params = ids.toList()
        return database.executeUpdate(sql, params)
    }

    fun selectOneById(id: String): JbinDocument? {
        val sql = "SELECT CAST(body AS TEXT) FROM \"$name\" WHERE id = ?"
        val params = listOf(id)
        val body = database.executeQuery(sql, params).firstOrNull() as String?
        return if (body == null) null else JbinDocument(id, body)
    }

    fun selectWhere(filter: JbinFilter): List<JbinDocument> {
        val (filterSql, params, functions) = JbinFilterTranslator.translate(filter)
        createFunctionsIfNotExists(functions)
        val sql = "SELECT id, CAST(body AS TEXT) FROM \"$name\" WHERE $filterSql"
        return database.executeQuery(sql, params).toDocuments()
    }

    fun selectAll(): List<JbinDocument> {
        val sql = "SELECT id, CAST(body AS TEXT) FROM \"$name\""
        return database.executeQuery(sql, emptyList()).toDocuments()
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
                val count = database.executeQuery("SELECT COUNT(*) FROM pg_proc WHERE proname = ?", listOf(func.name))
                if (count.first() == 0.toBigInteger()) {
                    database.executeUpdate(func.sql)
                }
                JbinDatabase.createdFunctionsCache.add(func.name)
            }
        }
    }

    private fun List<Any>.toDocuments(): List<JbinDocument> {
        return this.map { it as Array<*> }.map { JbinDocument(it[0].toString(), it[1].toString()) }
    }

}