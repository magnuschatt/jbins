package chatt.jbins

import java.util.concurrent.ConcurrentHashMap

data class JbinDatabase(private val adapter: JbinAdapter): JbinAdapter by adapter {

    companion object {
        val createdFunctionsCache: MutableSet<String> = ConcurrentHashMap.newKeySet()
    }

    fun getTable(name: String): JbinTable {
        return JbinTable(name, this)
    }

}