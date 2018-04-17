package chatt.jbins.test

import chatt.jbins.JbinFilter.And
import chatt.jbins.JbinFilter.Comparator.EQ
import chatt.jbins.JbinFilter.Match
import chatt.jbins.test.utils.jbinTransaction
import chatt.jbins.test.utils.newTestTable
import chatt.jbins.utils.document
import org.junit.Test

class RollbackTest {

    /*
    Caused by: org.postgresql.util.PSQLException: ERROR: function jbins_func_rollback_test1(jsonb) does not exist
     */
    @Test
    fun testRollbackScenario() {

        val key1 = "rollback_test1"
        val key2 = "rollback_test2"
        val filter = And(Match(key1, EQ, "Magnus"), Match(key2, EQ, "Magnus"))
        val doc1 = document(key1 to "Magnus", key2 to "Bob")
        val doc2 = document(key1 to "John", key2 to "Rofl")

        try {
            jbinTransaction { db ->
                val table = db.newTestTable()
                table.insert(doc1, doc2)

                table.select(filter)
                table.select(filter)
                throw IllegalStateException("forced rollback")
            }
        } catch (ignored: IllegalStateException) {}

        jbinTransaction { db ->
            val table = db.newTestTable()
            table.insert(doc1, doc2)

            table.select(filter)
            table.select(filter)
        }

    }

}