package chatt.jbins

sealed class JbinFilter {

    data class Or(val children: Collection<JbinFilter>) : JbinFilter(), Collection<JbinFilter> by children {
        constructor(vararg filters: JbinFilter): this(filters.toList())
    }

    data class And(val children: Collection<JbinFilter>) : JbinFilter(), Collection<JbinFilter> by children {
        constructor(vararg filters: JbinFilter): this(filters.toList())
    }

    data class Equals(val path: String, val value: String) : JbinFilter()

}
