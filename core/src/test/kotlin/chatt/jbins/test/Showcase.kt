package chatt.jbins.test

import chatt.jbins.JbinFilter.*
import chatt.jbins.JbinFilter.Comparator.*
import chatt.jbins.test.utils.jbinTransaction
import chatt.jbins.toDocument
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*


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
    fun `test replace one where`(): Unit = jbinTransaction { db ->
        val table = db.getTable("users").apply { createIfNotExists() }

        val id = "test-user-where"
        val user = mapOf("_id" to id, "name" to "Magnus", "age" to 27).toDocument()
        val updatedUser = mapOf("_id" to id,"name" to "Magnus", "age" to 28).toDocument()

        table.insert(user)
        val foundUser = table.selectOneById(id)
        assertEquals(user, foundUser)

        // The following replace should fail, since Magnus is not 40
        val wasReplaced1 = table.replaceOneWhere(updatedUser, Match("age", EQ, 40))
        assertFalse(wasReplaced1)
        val foundUpdatedUser1 = table.selectOneById(id)
        assertEquals(user, foundUpdatedUser1)

        // The following replace should succeed, since Magnus is 27
        val wasReplaced2 = table.replaceOneWhere(updatedUser, Match("age", EQ, 27))
        assertTrue(wasReplaced2)
        val foundUpdatedUser2 = table.selectOneById(id)
        assertEquals(updatedUser, foundUpdatedUser2)

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
        assertTrue(all.contains(user1))
        assertTrue(all.contains(user2))
        assertEquals(2, all.size)

        table.delete(user1, user2)
    }

    @Test
    fun `test select by equals`(): Unit = jbinTransaction { db ->
        val table = db.getTable("users").apply { createIfNotExists() }

        val user1 = mapOf("_id" to "bob", "name" to "Magnus", "age" to 27).toDocument()
        val user2 = mapOf("_id" to "rof", "name" to "Jens", "age" to 20).toDocument()
        table.insert(user1, user2)

        val returned = table.selectWhere(Match("name", EQ, "Magnus"))
        assertEquals(user1, returned.first())
        assertEquals(1, returned.size)

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
                Match("name", EQ, "Bob"),
                Match("name", EQ, "Jens")
        )

        val returned = table.selectWhere(filter)
        assertTrue(returned.contains(user2))
        assertTrue(returned.contains(user3))
        assertEquals(2, returned.size)

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
                Match("name", EQ, "Kim"),
                Match("gender", EQ, "female")
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
        val returned = table.selectWhere(Match("color[]", EQ, "red"))
        assertEquals(biker2, returned.first())
        assertEquals(1, returned.size)

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
                Match("color.animal.leg", EQ, "large"),
                Match("color.animal.leg", EQ, "big")
        )

        val returned = table.selectWhere(filter)
        assertEquals(anim2, returned.first())
        assertEquals(1, returned.size)

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
                Match("animals[].organs[]", EQ, "feather"),
                Match("animals[].organs[]", EQ, "beak")
        )

        val returned = table.selectWhere(filter)
        assertEquals(anim1, returned.first())
        assertEquals(1, returned.size)

        table.delete(anim1, anim2)
    }

    @Test
    fun `test select by date range`(): Unit = jbinTransaction { db ->
        val table = db.getTable("events").apply { createIfNotExists() }

        val sf = SimpleDateFormat("yyyy-mm-dd'T'HH:mm:ss")
        val date1 = sf.format(GregorianCalendar(1990, 1, 1).time)
        val date2 = sf.format(GregorianCalendar(1990, 1, 2).time)
        val date3 = sf.format(GregorianCalendar(1990, 1, 3).time)
        val date4 = sf.format(GregorianCalendar(1990, 1, 4).time)
        val date5 = sf.format(GregorianCalendar(1990, 1, 5).time)

        val user1 = mapOf("_id" to "1", "name" to "Kim", "date" to date1).toDocument()
        val user2 = mapOf("_id" to "2", "name" to "Bob", "date" to date2).toDocument()
        val user3 = mapOf("_id" to "3", "name" to "Hans", "date" to date3).toDocument()
        val user4 = mapOf("_id" to "4", "name" to "John", "date" to date4).toDocument()
        val user5 = mapOf("_id" to "5", "name" to "Ida", "date" to date5).toDocument()

        table.insert(user1, user2, user3, user4, user5)

        val filter = And(
                Match("date", GT, date1),
                Match("date", LTE, date4)
        )

        val returned = table.selectWhere(filter)
        assertTrue(returned.contains(user2))
        assertTrue(returned.contains(user3))
        assertTrue(returned.contains(user4))
        assertEquals(3, returned.size)

        table.delete(user1, user2, user3, user4, user5)
    }

    @Test
    fun `test select by number range`(): Unit = jbinTransaction { db ->
        val table = db.getTable("numbers").apply { createIfNotExists() }

        val user1 = mapOf("_id" to "1", "name" to "Kim", "number" to 1).toDocument()
        val user2 = mapOf("_id" to "2", "name" to "Bob", "number" to 2.023).toDocument()
        val user3 = mapOf("_id" to "3", "name" to "Hans", "number" to 3).toDocument()
        val user4 = mapOf("_id" to "4", "name" to "John", "number" to 4.7).toDocument()
        val user5 = mapOf("_id" to "5", "name" to "Ida", "number" to 5.7).toDocument()

        table.insert(user1, user2, user3, user4, user5)

        val filter = And(
                Match("number", GTE, 2.0033),
                Match("number", LT, 5.2)
        )

        val returned = table.selectWhere(filter)
        assertTrue(returned.contains(user2))
        assertTrue(returned.contains(user3))
        assertTrue(returned.contains(user4))
        assertEquals(3, returned.size)

        table.delete(user1, user2, user3, user4, user5)
    }

    @Test
    fun `test select by number any in array`(): Unit = jbinTransaction { db ->
        val table = db.getTable("numbers").apply { createIfNotExists() }

        val user1 = mapOf("_id" to "1", "name" to "Kim", "number" to arrayOf(1, 42)).toDocument()
        val user2 = mapOf("_id" to "2", "name" to "Bob", "number" to arrayOf(-1, 42)).toDocument()
        val user3 = mapOf("_id" to "3", "name" to "Hans", "number" to arrayOf(-31, 1)).toDocument()
        val user4 = mapOf("_id" to "4", "name" to "John", "number" to arrayOf(-1, -42)).toDocument()
        val user5 = mapOf("_id" to "5", "name" to "Ida", "number" to arrayOf(1, 42)).toDocument()

        table.insert(user1, user2, user3, user4, user5)

        val filter = Match("number[]", LTE, 0)

        val returned = table.selectWhere(filter)
        assertTrue(returned.contains(user2))
        assertTrue(returned.contains(user3))
        assertTrue(returned.contains(user4))
        assertEquals(3, returned.size)

        table.delete(user1, user2, user3, user4, user5)
    }

    @Test
    fun `test select by number all in array`(): Unit = jbinTransaction { db ->
        val table = db.getTable("numbers").apply { createIfNotExists() }

        val user1 = mapOf("_id" to "1", "name" to "Kim", "number" to arrayOf(1, -42)).toDocument()
        val user2 = mapOf("_id" to "2", "name" to "Bob", "number" to arrayOf(-1, -42)).toDocument()
        val user3 = mapOf("_id" to "3", "name" to "Hans", "number" to arrayOf(-31, -1)).toDocument()
        val user4 = mapOf("_id" to "4", "name" to "John", "number" to arrayOf(-1, -42)).toDocument()
        val user5 = mapOf("_id" to "5", "name" to "Ida", "number" to arrayOf(1, 42)).toDocument()

        table.insert(user1, user2, user3, user4, user5)

        val filter = Match("number[]", LTE, 0, matchAll = true)

        val returned = table.selectWhere(filter)
        assertTrue(returned.contains(user2))
        assertTrue(returned.contains(user3))
        assertTrue(returned.contains(user4))
        assertEquals(3, returned.size)

        table.delete(user1, user2, user3, user4, user5)
    }

}