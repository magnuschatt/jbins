package chatt.jbins

data class JbinDatabase(private val adapter: JbinAdapter): JbinAdapter by adapter {

    fun getTable(name: String): JbinTable {
        return JbinTable(name, this)
    }

}