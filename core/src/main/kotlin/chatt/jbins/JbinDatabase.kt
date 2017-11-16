package chatt.jbins

data class JbinDatabase(val adapter: JbinAdapter) {

    fun getTable(name: String): JbinTable {
        return JbinTable(name, this)
    }

}