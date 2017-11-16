package chatt.jbins

data class JbinTable(val name: String, val database: JbinDatabase) {

    private val adapter = database.adapter
    private val idSize = 255

    private val idColumn = "id"
    private val bodyColumn = "body"
    private val idColumnDef = "$idColumn VARCHAR($idSize) PRIMARY KEY"
    private val bodyColumnDef = "$bodyColumn JSONB NOT NULL"

    fun createIfNotExists() {
        val sql = "CREATE TABLE IF NOT EXISTS $name ($idColumnDef, $bodyColumnDef)"
        adapter.executeUpdate(sql)
    }

    fun insertOne(id: String, json: String) {
        val sql = "INSERT INTO $name ($idColumn, $bodyColumn) VALUES (?, CAST(? AS JSONB))"
        val params = listOf(id, json)
        adapter.executeUpdate(sql, params)
    }

    fun findOneById(id: String): String? {
        val sql = "SELECT CAST(body AS TEXT) FROM $name WHERE id = ?"
        val params = listOf(id)
        return adapter.executeQuery(sql, params).firstOrNull() as String?
    }

}