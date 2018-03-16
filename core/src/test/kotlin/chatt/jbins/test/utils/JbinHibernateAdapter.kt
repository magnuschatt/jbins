package chatt.jbins.test.utils

import chatt.jbins.JbinAdapter
import org.hibernate.Session

class JbinHibernateAdapter(private val session: Session) : JbinAdapter {

    private fun prepareQuery(sql: String, parameters: List<Any>) = session.createNativeQuery(sql).apply {
        parameters.forEachIndexed { i, param -> setParameter(i+1, param) }
    }

    override fun executeUpdate(sql: String, parameters: List<Any>): Int {
        return prepareQuery(sql, parameters).executeUpdate()
    }

    override fun executeQuery(sql: String, parameters: List<Any>): List<Any> {
        return prepareQuery(sql, parameters).resultList
    }

}
