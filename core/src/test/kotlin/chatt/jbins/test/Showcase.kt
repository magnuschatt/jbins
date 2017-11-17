package chatt.jbins.test

import chatt.jbins.test.utils.toJson
import chatt.jbins.test.utils.toMap
import chatt.jbins.test.utils.withJbinDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Showcase {

    @Test
    fun `test insert, find, replace, and delete`() = withJbinDatabase { db ->
        val table = db.getTable("users").apply { createIfNotExists() }

        val id = "test-user"
        val user = mapOf("name" to "Magnus", "age" to 27)
        val updatedUser = mapOf("name" to "Magnus", "age" to 28)

        table.insertOne(id, user.toJson())
        val foundUser = table.findOneById(id)?.toMap()
        assertEquals(user, foundUser)

        table.replaceOneById(id, updatedUser.toJson())
        val foundUpdatedUser = table.findOneById(id)?.toMap()
        assertEquals(updatedUser, foundUpdatedUser)

        table.deleteOneById(id)
        assertNull(table.findOneById(id))
    }

}