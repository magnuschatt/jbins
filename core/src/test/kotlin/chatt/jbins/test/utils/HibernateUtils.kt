package chatt.jbins.test.utils

import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.Transaction
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.Configuration
import java.util.*

const val DB_URL = "jdbc:postgresql://docker:5432/jbins"
const val DB_USER = "postgres"
const val DB_PASS = "postgres"
const val POSTGRES_DRIVER_CLASS = "org.postgresql.Driver"
const val POSTGRES_DIALECT = "org.hibernate.dialect.PostgreSQL94Dialect"

val sessionFactory: SessionFactory by lazy { newSessionFactory() }

fun newSessionFactory(): SessionFactory {
    val properties = Properties().apply {
        put("hibernate.hbm2ddl.auto", "update")
        put("hibernate.show_sql", "true")
        put("hibernate.dialect", POSTGRES_DIALECT)
        put("hibernate.connection.driver_class", POSTGRES_DRIVER_CLASS)
        put("hibernate.connection.url", DB_URL)
        put("hibernate.connection.username", DB_USER)
        put("hibernate.connection.password", DB_PASS)
    }

    val serviceRegistry = StandardServiceRegistryBuilder().applySettings(properties).build()
    val config = Configuration().apply { addProperties(properties) }
    return config.buildSessionFactory(serviceRegistry)
}

fun newSession(): Session = sessionFactory.openSession()

fun <R>transaction(block: (Session) -> R): R = newSession().use { session ->
    var tx: Transaction? = null
    try {
        tx = session.beginTransaction()
        val result = block(session)
        tx.commit()
        return result
    } catch (ex: Throwable) {
        tx?.rollback()
        throw ex
    }
}

