package chatt.jbins.test

import chatt.jbins.test.utils.withJbinDatabase
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class Showcase {

    private val mapper = ObjectMapper()

    @Test
    fun `showcase insert and find`() = withJbinDatabase { db ->
        val table = db.getTable("users").apply { createIfNotExists() }

        val id = "user1"
        val user = """{ "id": "$id", "name": "Magnus", "age": 27 }"""
        table.insertOne(id, user)

        assertEquals(mapper.readTree(user), mapper.readTree(table.findOneById(id)))
    }

}