package chatt.jbins.test

import chatt.jbins.JbinDocument.Companion.ID_PATH
import chatt.jbins.JbinFilter.*
import chatt.jbins.JbinFilter.Comparator.*
import chatt.jbins.SortDirection.ASC
import chatt.jbins.SortDirection.DESC
import chatt.jbins.test.utils.jbinTransaction
import chatt.jbins.test.utils.newTestTable
import chatt.jbins.test.utils.withTempTable
import chatt.jbins.utils.document
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class Showcase {

    @Test
    fun `test insert, select, replace, and delete`() = withTempTable { table ->
        val id = "test-user"
        val doc1 = document(ID_PATH to id, "name" to "Magnus", "age" to 27)
        val doc2 = document(ID_PATH to id, "name" to "Magnus", "age" to 28)

        table.insert(doc1)
        assertEquals(doc1, table.selectOneById(id))

        table.replaceOne(doc2)
        assertEquals(doc2, table.selectOneById(id))

        table.deleteById(id)
        assertNull(table.selectOneById(id))
    }

    @Test
    fun `test replace one where`() = withTempTable { table ->
        val id = "test-user-where"
        val doc1 = document(ID_PATH to id, "name" to "Magnus", "age" to 27)
        val doc2 = document(ID_PATH to id, "name" to "Magnus", "age" to 28)

        table.insert(doc1)
        assertEquals(doc1, table.selectOneById(id))

        assertFalse(table.replaceOne(doc2, Match("age", EQ, 40))) // should fail
        assertEquals(doc1, table.selectOneById(id))

        assertTrue(table.replaceOne(doc2, Match("age", EQ, 27))) // should succeed
        assertEquals(doc2, table.selectOneById(id))

        table.deleteById(id)
        assertNull(table.selectOneById(id))
    }

    @Test
    fun `test select all`() = withTempTable { table ->
        val user1 = document("name" to "Magnus", "age" to 27)
        val user2 = document("name" to "Jens", "age" to 20)
        table.insert(user1, user2)

        val all = table.select()
        assertTrue(all.contains(user1))
        assertTrue(all.contains(user2))
        assertEquals(2, all.size)
    }

    @Test
    fun `test delete`() = withTempTable { table ->
        val user1 = document("name" to "Magnus", "age" to 27)
        val user2 = document("name" to "Jens", "age" to 20)
        table.insert(user1, user2)

        assertEquals(2, table.select().size)
        table.delete(user1, user2)
    }

    @Test
    fun `test select limited`() = withTempTable { table ->
        val user1 = document("name" to "Magnus", "age" to 27)
        val user2 = document("name" to "Jens", "age" to 20)
        val user3 = document("name" to "Bob", "age" to 20)
        val user4 = document("name" to "Tim", "age" to 20)
        val user5 = document("name" to "Ed", "age" to 20)
        val user6 = document("name" to "Ned", "age" to 20)
        val user7 = document("name" to "Jack", "age" to 20)
        table.insert(user1, user2, user3, user4, user5, user6, user7)

        assertEquals(1, table.select(limit = 1).size)
        assertEquals(2, table.select(limit = 2).size)
        assertEquals(3, table.select(limit = 3).size)
        assertEquals(4, table.select(limit = 4).size)
        assertEquals(5, table.select(limit = 5).size)
    }

    @Test
    fun `test select and order by`() = withTempTable { table ->

        val sortedUsers = listOf("Aaa", "aaa", "aa", "Aa", "a", "A", "AA", "abc", "ABC", "John", "Paul", "xyz")
                .sorted()
                .map { document("name" to it, "age" to 27) }
        table.insert(sortedUsers.shuffled())

        assertEquals(sortedUsers, table.select(orderBy = listOf("name" to ASC)))
        assertEquals(sortedUsers.asReversed(), table.select(orderBy = listOf("name" to DESC)))
    }

    @Test
    fun `test select by equals`() = withTempTable { table ->
        val user1 = document("name" to "Magnus", "age" to 27)
        val user2 = document("name" to "Jens", "age" to 20)
        table.insert(user1, user2)

        val returned = table.select(where = Match("name", EQ, "Magnus"))
        assertEquals(user1, returned.first())
        assertEquals(1, returned.size)
    }

    @Test
    fun `test select by like`() = withTempTable { table ->
        val user1 = document("name" to "Mag", "age" to 27)
        val user2 = document("name" to "Magnesium", "age" to 27)
        val user3 = document("name" to "Nag", "age" to 20)
        table.insert(user1, user2, user3)

        assertEquals(0, table.select(where = Match("name", LIKE, "mag%")).size)
        val returned = table.select(where = Match("name", LIKE, "Mag%"))
        assertTrue(returned.contains(user1))
        assertTrue(returned.contains(user2))
        assertEquals(2, returned.size)
        assertEquals(3, table.select(where = Match("name", LIKE, "%ag%")).size)
    }

    @Test
    fun `test select by case insensitive like`() = withTempTable { table ->
        val user1 = document("name" to "Mag", "age" to 27)
        val user2 = document("name" to "Magnesium", "age" to 27)
        val user3 = document("name" to "Nag", "age" to 20)
        table.insert(user1, user2, user3)

        val returned = table.select(where = Match("name", ILIKE, "mag%"))
        assertTrue(returned.contains(user1))
        assertTrue(returned.contains(user2))
        assertEquals(2, returned.size)
        assertEquals(3, table.select(where = Match("name", ILIKE, "%Ag%")).size)
    }

    @Test
    fun `test select by or-filter`() = withTempTable { table ->
        val user1 = document("name" to "Magnus", "age" to 27)
        val user2 = document("name" to "Bob", "age" to 20)
        val user3 = document("name" to "Jens", "age" to 19)
        table.insert(user1, user2, user3)

        val filter = Or(
                Match("name", EQ, "Bob"),
                Match("name", EQ, "Jens")
        )

        val returned = table.select(where = filter)
        assertTrue(returned.contains(user2))
        assertTrue(returned.contains(user3))
        assertEquals(2, returned.size)
    }

    @Test
    fun `test select by and-filter`() = withTempTable { table ->
        val user1 = document("name" to "Kim", "gender" to "female")
        val user2 = document("name" to "Kim", "gender" to "male")
        val user3 = document("name" to "Jens", "gender" to "female")
        table.insert(user1, user2, user3)

        val filter = And(
                Match("name", EQ, "Kim"),
                Match("gender", EQ, "female")
        )

        val returned = table.select(where = filter)
        assertEquals(1, returned.size)
        assertEquals(user1, returned.first())
    }

    @Test
    fun `test select by equals any in array`() = withTempTable { table ->
        
        val biker1 = document(
                "name" to "Harvey",
                "color" to arrayOf("black", "blue")
        )
        val biker2 = document(
                "name" to "Davidson",
                "color" to arrayOf("red", "blue")
        )

        table.insert(biker1, biker2)
        val returned = table.select(where = Match("color[]", EQ, "red"))
        assertEquals(biker2, returned.first())
        assertEquals(1, returned.size)
    }

    @Test
    fun `test select by equals in nested object`() = withTempTable { table ->

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

        val returned = table.select(where = filter)
        assertEquals(anim2, returned.first())
        assertEquals(1, returned.size)
    }

    @Test
    fun `test select by equals in nested object and array`() = withTempTable { table ->

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

        val returned = table.select(where = filter)
        assertEquals(anim1, returned.first())
        assertEquals(1, returned.size)
    }

    @Test
    fun `test select by date range`() = withTempTable { table ->

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

        val returned = table.select(where = filter)
        assertTrue(returned.contains(user2))
        assertTrue(returned.contains(user3))
        assertTrue(returned.contains(user4))
        assertEquals(3, returned.size)
    }

    @Test
    fun `test select by number range`() = withTempTable { table ->

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

        val returned = table.select(where = filter)
        assertTrue(returned.contains(user2))
        assertTrue(returned.contains(user3))
        assertTrue(returned.contains(user4))
        assertEquals(3, returned.size)
    }

    @Test
    fun `test patch where`() = withTempTable { table ->
        // arrange
        val people = listOf("Kim", "Bob", "Bob", "John", "Ida").map { document("name" to it) }
        table.insert(people)

        // act
        val numUpdated = table.patch(
                where = Match("name", EQ, "Bob"),
                path = "name",
                newJson = "\"Hans\"")

        // assert
        assertEquals(2, numUpdated)
        assertEquals(0, table.select(where = Match("name", EQ, "Bob")).size)
        assertEquals(2, table.select(where = Match("name", EQ, "Hans")).size)
    }

    @Test
    fun `test patch where complex`() = withTempTable { table ->
        // arrange
        val doc1 = document("name" to "m1", "core" to mapOf("color" to "blue"))
        val doc2 = document("name" to "m2")
        table.insert(doc1, doc2)
        assertEquals(1, table.select(IsEmpty("core", false)).size)

        // act
        val numUpdated = table.patch(path = "core", newJson = """{ "color": "red" }""", createMissing = true)

        // assert
        assertEquals(2, numUpdated)
        assertEquals(2, table.select(IsEmpty("core", false)).size)
        assertEquals(2, table.select(Match("core.color", EQ, "red")).size)
    }

    @Test
    fun `test patch null`() = withTempTable { table ->
        // arrange
        val people = listOf("Kim", "Bob", "Bob", "John", "Ida").map { document("name" to it) }
        table.insert(people)

        // act
        val numUpdated = table.patch(
                where = Match("name", EQ, "Bob"),
                path = "name",
                newJson = "null")

        // assert
        assertEquals(2, numUpdated)
        assertEquals(3, table.select(where = IsEmpty("name", false)).size)
        assertEquals(2, table.select(where = IsEmpty("name", true)).size)
    }

    @Test
    fun `test select by number any in array`() = withTempTable { table ->
        val user1 = document("name" to "Kim", "number" to arrayOf(1, 42))
        val user2 = document("name" to "Bob", "number" to arrayOf(-1, 42))
        val user3 = document("name" to "Hans", "number" to arrayOf(-31, 1))
        val user4 = document("name" to "John", "number" to arrayOf(-1, -42))
        val user5 = document("name" to "Ida", "number" to arrayOf(1, 42))

        table.insert(user1, user2, user3, user4, user5)

        val filter = Match("number[]", LTE, 0)

        val returned = table.select(where = filter)
        assertTrue(returned.contains(user2))
        assertTrue(returned.contains(user3))
        assertTrue(returned.contains(user4))
        assertEquals(3, returned.size)
    }

    @Test
    fun `test select by number all in array`() = withTempTable { table ->
        val user1 = document("name" to "Kim", "number" to arrayOf(1, -42))
        val user2 = document("name" to "Bob", "number" to arrayOf(-1, -42))
        val user3 = document("name" to "Hans", "number" to arrayOf(-31, -1))
        val user4 = document("name" to "John", "number" to arrayOf(-1, -42))
        val user5 = document("name" to "Ida", "number" to arrayOf(1, 42))

        table.insert(user1, user2, user3, user4, user5)

        val filter = Match("number[]", LTE, 0, matchAll = true)

        val returned = table.select(where = filter)
        assertTrue(returned.contains(user2))
        assertTrue(returned.contains(user3))
        assertTrue(returned.contains(user4))
        assertEquals(3, returned.size)
    }

    @Test
    fun `test select by whether or not field exists`() = withTempTable { table ->
        val user1 = document("name" to "Kim", "attr" to "bob")
        val user2 = document("name" to "Bob")
        val user3 = document("name" to "Hans")
        val user4 = document("name" to "John", "attr" to "lol")
        val user5 = document("name" to "Ida", "attr" to "rofl")

        table.insert(user1, user2, user3, user4, user5)

        val returned1 = table.select(IsEmpty("attr", true))
        assertTrue(returned1.contains(user2))
        assertTrue(returned1.contains(user3))
        assertEquals(2, returned1.size)

        val returned2 = table.select(IsEmpty("attr", false))
        assertTrue(returned2.contains(user1))
        assertTrue(returned2.contains(user4))
        assertTrue(returned2.contains(user5))
        assertEquals(3, returned2.size)
    }

    @Test
    fun `test select by whether or not field in array exists`() = withTempTable { table ->
        val user1 = document("name" to "Kim", "attr" to listOf("bob"))
        val user2 = document("name" to "Bob")
        val user3 = document("name" to "Hans")
        val user4 = document("name" to "John", "attr" to listOf("lol"))
        val user5 = document("name" to "Ida", "attr" to listOf("rofl"))

        table.insert(user1, user2, user3, user4, user5)

        val returned1 = table.select(IsEmpty("attr[]", true))
        assertTrue(returned1.contains(user2))
        assertTrue(returned1.contains(user3))
        assertEquals(2, returned1.size)

        val returned2 = table.select(IsEmpty("attr[]", false))
        assertTrue(returned2.contains(user1))
        assertTrue(returned2.contains(user4))
        assertTrue(returned2.contains(user5))
        assertEquals(3, returned2.size)
    }

    @Test
    @Ignore("not done")
    fun `test select by joined attribute`() = jbinTransaction { db ->

        val buildings = db.newTestTable()
        val people = db.newTestTable()

        val building1 = document("color" to "reg")
        val building2 = document("color" to "green")
        val building3 = document("color" to "blue")

        val person1 = document("name" to "John", "building" to building1.id)
        val person2 = document("name" to "Paul", "building" to building1.id)
        val person3 = document("name" to "Homer", "building" to building2.id)
        val person4 = document("name" to "Louis", "building" to building2.id)
        val person5 = document("name" to "Bart", "building" to building3.id)

        buildings.insert(building1, building2, building3)
        people.insert(person1, person2, person3, person4, person5)

        val filter = Match("number[]", LTE, 0, matchAll = true)

        val returned = people.select(where = filter)
        assertTrue(returned.contains(person3))
        assertTrue(returned.contains(person4))
        assertEquals(2, returned.size)

        buildings.drop()
        people.drop()
    }

}