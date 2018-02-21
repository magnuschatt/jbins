package chatt.jbins.test

import chatt.jbins.JbinFilter.*
import chatt.jbins.JbinFilter.Comparator.*
import chatt.jbins.test.utils.jbinTransaction
import chatt.jbins.test.utils.newTestTable
import chatt.jbins.utils.document
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*


class Showcase {

    @Test
    fun `test insert, select, replace, and delete`(): Unit = jbinTransaction { db ->
        val table = db.newTestTable()

        val id = "test-user"
        val doc1 = document("_id" to id, "name" to "Magnus", "age" to 27)
        val doc2 = document("_id" to id, "name" to "Magnus", "age" to 28)

        table.insert(doc1)
        assertEquals(doc1, table.selectOneById(id))

        table.replaceOne(doc2)
        assertEquals(doc2, table.selectOneById(id))

        table.deleteById(id)
        assertNull(table.selectOneById(id))
    }

    @Test
    fun `test replace one where`(): Unit = jbinTransaction { db ->
        val table = db.newTestTable()

        val id = "test-user-where"
        val doc1 = document("_id" to id, "name" to "Magnus", "age" to 27)
        val doc2 = document("_id" to id, "name" to "Magnus", "age" to 28)

        table.insert(doc1)
        assertEquals(doc1, table.selectOneById(id))

        assertFalse(table.replaceOneWhere(doc2, Match("age", EQ, 40))) // should fail
        assertEquals(doc1, table.selectOneById(id))

        assertTrue(table.replaceOneWhere(doc2, Match("age", EQ, 27))) // should succeed
        assertEquals(doc2, table.selectOneById(id))

        table.deleteById(id)
        assertNull(table.selectOneById(id))
    }

    @Test
    fun `test select all`(): Unit = jbinTransaction { db ->
        val table = db.newTestTable()

        val user1 = document("name" to "Magnus", "age" to 27)
        val user2 = document("name" to "Jens", "age" to 20)
        table.insert(user1, user2)

        val all = table.selectAll()
        assertTrue(all.contains(user1))
        assertTrue(all.contains(user2))
        assertEquals(2, all.size)

        table.delete(user1, user2)
    }

    @Test
    fun `test select by equals`(): Unit = jbinTransaction { db ->
        val table = db.newTestTable()

        val user1 = document("name" to "Magnus", "age" to 27)
        val user2 = document("name" to "Jens", "age" to 20)
        table.insert(user1, user2)

        val returned = table.selectWhere(Match("name", EQ, "Magnus"))
        assertEquals(user1, returned.first())
        assertEquals(1, returned.size)

        table.delete(user1, user2)
    }

    @Test
    fun `test select by or-filter`(): Unit = jbinTransaction { db ->
        val table = db.newTestTable()

        val user1 = document("name" to "Magnus", "age" to 27)
        val user2 = document("name" to "Bob", "age" to 20)
        val user3 = document("name" to "Jens", "age" to 19)
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
        val table = db.newTestTable()

        val user1 = document("name" to "Kim", "gender" to "female")
        val user2 = document("name" to "Kim", "gender" to "male")
        val user3 = document("name" to "Jens", "gender" to "female")
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
        val table = db.newTestTable()

        val biker1 = document(
                "name" to "Harvey",
                "color" to arrayOf("black", "blue")
        )
        val biker2 = document(
                "name" to "Davidson",
                "color" to arrayOf("red", "blue")
        )

        table.insert(biker1, biker2)
        val returned = table.selectWhere(Match("color[]", EQ, "red"))
        assertEquals(biker2, returned.first())
        assertEquals(1, returned.size)

        table.delete(biker1, biker2)
    }

    @Test
    fun `test select by equals in nested object`(): Unit = jbinTransaction { db ->
        val table = db.newTestTable()

        val anim1 = document(
                "name" to "Boson",
                "color" to mapOf(
                        "animal" to mapOf(
                                "leg" to "short"
                        )
                )
        )
        val anim2 = document(
                "name" to "Boson",
                "color" to mapOf(
                        "animal" to mapOf(
                                "leg" to "big"
                        )
                )
        )

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
        val table = db.newTestTable()

        val anim1 = document(
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
        )
        val anim2 = document(
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
        )

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
        val table = db.newTestTable()

        val sf = SimpleDateFormat("yyyy-mm-dd'T'HH:mm:ss")
        val date1 = sf.format(GregorianCalendar(1990, 1, 1).time)
        val date2 = sf.format(GregorianCalendar(1990, 1, 2).time)
        val date3 = sf.format(GregorianCalendar(1990, 1, 3).time)
        val date4 = sf.format(GregorianCalendar(1990, 1, 4).time)
        val date5 = sf.format(GregorianCalendar(1990, 1, 5).time)

        val user1 = document("name" to "Kim", "date" to date1)
        val user2 = document("name" to "Bob", "date" to date2)
        val user3 = document("name" to "Hans", "date" to date3)
        val user4 = document("name" to "John", "date" to date4)
        val user5 = document("name" to "Ida", "date" to date5)

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
        val table = db.newTestTable()

        val user1 = document("name" to "Kim", "number" to 1)
        val user2 = document("name" to "Bob", "number" to 2.023)
        val user3 = document("name" to "Hans", "number" to 3)
        val user4 = document("name" to "John", "number" to 4.7)
        val user5 = document("name" to "Ida", "number" to 5.7)

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
    fun `test patch where`(): Unit = jbinTransaction { db ->

        // arrange
        val table = db.newTestTable()
        val people = listOf("Kim", "Bob", "Bob", "John", "Ida")
                .map { document("name" to it) }

        table.insert(people)

        // act
        val numUpdated = table.patchWhere(
                filter = Match("name", EQ, "Bob"),
                path = "name",
                newValue = "Hans")

        // assert
        assertEquals(2, numUpdated)

        // clean
        table.delete(people)
    }

    @Test
    fun `test select by number any in array`(): Unit = jbinTransaction { db ->
        val table = db.newTestTable()

        val user1 = document("name" to "Kim", "number" to arrayOf(1, 42))
        val user2 = document("name" to "Bob", "number" to arrayOf(-1, 42))
        val user3 = document("name" to "Hans", "number" to arrayOf(-31, 1))
        val user4 = document("name" to "John", "number" to arrayOf(-1, -42))
        val user5 = document("name" to "Ida", "number" to arrayOf(1, 42))

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
        val table = db.newTestTable()

        val user1 = document("name" to "Kim", "number" to arrayOf(1, -42))
        val user2 = document("name" to "Bob", "number" to arrayOf(-1, -42))
        val user3 = document("name" to "Hans", "number" to arrayOf(-31, -1))
        val user4 = document("name" to "John", "number" to arrayOf(-1, -42))
        val user5 = document("name" to "Ida", "number" to arrayOf(1, 42))

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