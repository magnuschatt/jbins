package chatt.jbins

sealed class JbinFilter {

    data class Or(private val children: Collection<JbinFilter>) : JbinFilter(), Collection<JbinFilter> by children {
        constructor(vararg filters: JbinFilter): this(filters.toList())
    }

    data class And(private val children: Collection<JbinFilter>) : JbinFilter(), Collection<JbinFilter> by children {
        constructor(vararg filters: JbinFilter): this(filters.toList())
    }

    data class Match(val path: String,
                     val comparator: Comparator,
                     val value: Any,
                     val matchAll: Boolean = false) : JbinFilter()

    enum class Comparator {
        EQ, NEQ, GT, GTE, LT, LTE
    }

}
