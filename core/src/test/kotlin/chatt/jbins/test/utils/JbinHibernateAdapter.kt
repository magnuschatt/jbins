package chatt.jbins.test.utils

import chatt.jbins.JbinAdapter
import org.hibernate.Session
import org.hibernate.query.NativeQuery

class JbinHibernateAdapter(private val session: Session) : JbinAdapter {

    private fun prepareQuery(sql: String, parameters: List<Any>): NativeQuery<*> {

        val split = sql.split("?")
        val finalSql = StringBuilder()

        split.dropLast(1).forEachIndexed { i, sqlPart -> finalSql.append("$sqlPart:p$i") }
        finalSql.append(split.last())

        return session.createNativeQuery(finalSql.toString()).apply {
            parameters.forEachIndexed { i, param ->
                setParameter("p$i", param)
            }
        }
    }

    override fun executeUpdate(sql: String, parameters: List<Any>): Int {
        return prepareQuery(sql, parameters).executeUpdate()
    }

    override fun executeQuery(sql: String, parameters: List<Any>): List<Any> {
        return prepareQuery(sql, parameters).resultList
    }

}

