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
    fun `test select by equals`(): Unit = jbinTransaction { db ->
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
    fun `test select by or-filter`(): Unit = jbinTransaction { db ->
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
    fun `test select by and-filter`(): Unit = jbinTransaction { db ->
        val table = db.getTable("users").apply { createIfNotExists() }

        val user1 = mapOf("_id" to "id1x", "name" to "Kim", "gender" to "female").toDocument()
        val user2 = mapOf("_id" to "id2x", "name" to "Kim", "gender" to "male").toDocument()
        val user3 = mapOf("_id" to "id3x", "name" to "Jens", "gender" to "female").toDocument()
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

    @Test
    fun `test select by equals any in array`(): Unit = jbinTransaction { db ->
        val table = db.getTable("bikers").apply { createIfNotExists() }

        val biker1 = mapOf(
                "_id" to "id1yy",
                "name" to "Harvey",
                "color" to arrayOf("black", "blue")
        ).toDocument()
        val biker2 = mapOf(
                "_id" to "id2yy",
                "name" to "Davidson",
                "color" to arrayOf("red", "blue")
        ).toDocument()

        table.insert(biker1, biker2)
        val returned = table.selectWhere(Equals("color[]", "red"))
        assertEquals(1, returned.size)
        assertEquals(biker2, returned.first())

        table.delete(biker1, biker2)
    }

    @Test
    fun `test select by equals in nested object`(): Unit = jbinTransaction { db ->
        val table = db.getTable("animals").apply { createIfNotExists() }

        val anim1 = mapOf(
                "_id" to "id7",
                "name" to "Boson",
                "color" to mapOf(
                        "animal" to mapOf(
                                "leg" to "short"
                        )
                )
        ).toDocument()
        val anim2 = mapOf(
                "_id" to "id8",
                "name" to "Boson",
                "color" to mapOf(
                        "animal" to mapOf(
                                "leg" to "big"
                        )
                )
        ).toDocument()

        table.insert(anim1, anim2)

        val filter = Or(
                Equals("color.animal.leg", "large"),
                Equals("color.animal.leg", "big")
        )

        val returned = table.selectWhere(filter)
        assertEquals(1, returned.size)
        assertEquals(anim2, returned.first())

        table.delete(anim1, anim2)
    }

    @Test
    fun `test select by equals in nested object and array`(): Unit = jbinTransaction { db ->
        val table = db.getTable("animals").apply { createIfNotExists() }

        val anim1 = mapOf(
                "_id" to "ida7",
                "name" to "Boson",
                "animals" to arrayOf(
                        mapOf(
                                "name" to "duck",
                                "organs" to arrayOf("feather", "liver")
                        ),
                        mapOf(
                                "name" to "parrot",
                                "organs" to arrayOf("colors", "eye")
                        )
                )
        ).toDocument()
        val anim2 = mapOf(
                "_id" to "ida8",
                "name" to "Boson",
                "animals" to arrayOf(
                        mapOf(
                                "name" to "dog",
                                "organs" to arrayOf("mouth", "liver")
                        ),
                        mapOf(
                                "name" to "cat",
                                "organs" to arrayOf("mouth", "liver")
                        )
                )
        ).toDocument()

        table.insert(anim1, anim2)

        val filter = Or(
                Equals("animals[].organs[]", "feather"),
                Equals("animals[].organs[]", "beak")
        )

        val returned = table.selectWhere(filter)
        assertEquals(1, returned.size)
        assertEquals(anim1, returned.first())

        table.delete(anim1, anim2)
    }

}