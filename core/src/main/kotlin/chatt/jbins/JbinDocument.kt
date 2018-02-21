package chatt.jbins

import chatt.jbins.utils.toMap

data class JbinDocument(val id: String, val body: String) {

    private val asMap: Map<String, Any> by lazy {
        body.toMap().toMutableMap().also { it["_id"] = id }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as JbinDocument
        if (asMap != other.asMap) return false
        return true
    }

    override fun hashCode(): Int {
        return asMap.hashCode()
    }


}