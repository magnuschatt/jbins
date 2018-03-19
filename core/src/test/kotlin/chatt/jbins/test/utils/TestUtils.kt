package chatt.jbins.test.utils

import chatt.jbins.JbinDatabase
import chatt.jbins.JbinTable

private var tableCounter = 0

fun <R> jbinTransaction(block: (JbinDatabase) -> R): R = transaction { session ->
    return@transaction block(JbinDatabase(JbinHibernateAdapter(session)))
}

fun JbinDatabase.newTestTable(): JbinTable {
    return getTable("test_table${tableCounter++}").apply { create() }
}

fun withTempTable(block: (JbinTable) -> Unit) = jbinTransaction { db ->
    val table = db.newTestTable()
    block(table)
    table.drop()
}