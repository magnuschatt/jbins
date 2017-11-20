package chatt.jbins.test

import chatt.jbins.JbinFilter.*
import chatt.jbins.test.utils.jbinTransaction
import chatt.jbins.toDocument
import org.junit.Assert.*
import org.junit.Test

class Showcase {

    @Test
    fun `test insert, select, replace, and delete`(): Unit = jbinTransaction { db ->
        val table = db.getTable("users").apply { createIfNotExists() }

        val id = "test-user"
        val user = mapOf("_id" to id, "name" to "Magnus", "age" to 27).toDocument()
        val updatedUser = mapOf("_id" to id,"name" to "Magnus", "age" to 28).toDocument()

        table.insert(user)
        val foundUser = table.selectOneById(id)
        assertEquals(user, foundUser)

        table.replaceOne(updatedUser)
        val foundUpdatedUser = table.selectOneById(id)
        assertEquals(updatedUser, foundUpdatedUser)

        table.deleteById(id)
        assertNull(table.selectOneById(id))
    }

    @Test
    fun `test select all`(): Unit = jbinTransaction { db ->
        val table = db.getTable("users").apply { createIfNotExists() }

        val user1 = mapOf("_id" to "sam", "name" to "Magnus", "age" to 27).toDocument()
        val user2 = mapOf("_id" to "lol", "name" to "Jens", "age" to 20).toDocument()
        table.insert(user1, user2)

        val all = table.selectAll()
        assertEquals(2, all.size)
        assertTrue(all.contains(user1))
        assertTrue(all.contains(user2))
        table.delete(user1, user2)
    }

    @Test
    fun `test select by value`(): Unit = jbinTransaction { db ->
        val table = db.getTable("users").apply { createIfNotExists() }

        val user1 = mapOf("_id" to "bob", "name" to "Magnus", "age" to 27).toDocument()
        val user2 = mapOf("_id" to "rof", "name" to "Jens", "age" to 20).toDocument()
        table.insert(user1, user2)

        val returned = table.selectWhere(Equals("name", "Magnus"))
        assertEquals(1, returned.size)
        assertEquals(user1, returned.first())

        table.delete(user1, user2)
    }

    @Test
    fun `test select by value or value`(): Unit = jbinTransaction { db ->
        val table = db.getTable("users").apply { createIfNotExists() }

        val user1 = mapOf("_id" to "bob", "name" to "Magnus", "age" to 27).toDocument()
        val user2 = mapOf("_id" to "rof", "name" to "Bob", "age" to 20).toDocument()
        val user3 = mapOf("_id" to "sof", "name" to "Jens", "age" to 19).toDocument()
        table.insert(user1, user2, user3)

        val filter = Or(
                Equals("name", "Bob"),
                Equals("name", "Jens")
        )

        val returned = table.selectWhere(filter)
        assertEquals(2, returned.size)
        assertTrue(returned.contains(user2))
        assertTrue(returned.contains(user3))

        table.delete(user1, user2, user3)
    }

    @Test
    fun `test select by value and value`(): Unit = jbinTransaction { db ->
        val table = db.getTable("users").apply { createIfNotExists() }

        val user1 = mapOf("_id" to "id1", "name" to "Kim", "gender" to "female").toDocument()
        val user2 = mapOf("_id" to "id2", "name" to "Kim", "gender" to "male").toDocument()
        val user3 = mapOf("_id" to "id3", "name" to "Jens", "gender" to "male").toDocument()
        table.insert(user1, user2, user3)

        val filter = And(
                Equals("name", "Kim"),
                Equals("gender", "female")
        )

        val returned = table.selectWhere(filter)
        assertEquals(1, returned.size)
        assertEquals(user1, returned.first())

        table.delete(user1, user2, user3)
    }

}