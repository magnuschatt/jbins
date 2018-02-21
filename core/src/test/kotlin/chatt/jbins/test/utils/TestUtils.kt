package chatt.jbins.test.utils

import chatt.jbins.JbinDatabase
import chatt.jbins.JbinTable

private var tableCounter = 0

fun JbinDatabase.newTestTable(): JbinTable {
    return getTable("test_table${tableCounter++}").apply { createIfNotExists() }
}