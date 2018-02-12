package chatt.jbins.test

import chatt.jbins.JbinFilter
import chatt.jbins.JbinFilter.Comparator.EQ
import chatt.jbins.test.utils.jbinTransaction
import chatt.jbins.toDocument
import java.util.*
import kotlin.system.measureNanoTime

val random = Random(1337)

fun main(args: Array<String>) {


    var nanoTime: Long = 0
    measureNanoTime {
        jbinTransaction { db ->
            db.getTable("users").apply { createIfNotExists() }

            for (k in 1..100) {
                for (i in 1..150) {
                    val table = db.getTable("users").apply { createIfNotExists() }
                    val users = (1..100).map {
                        val id = UUID.randomUUID().toString().replace("-", "")
                        mapOf("_id" to id, "name" to "Magnus", "age" to arrayOf(rand(), rand())).toDocument()
                    }
                    table.insert(users)
                }
                println("$k/100")
            }

            val path = "age[]"
            db.getTable("users").createIndex(path)

            val filter = JbinFilter.Match(path, EQ, 99)
             measureNanoTime {
                val selectWhere = db.getTable("users").selectWhere(filter)
                println("Result size: " + selectWhere.size)
            }

            nanoTime = measureNanoTime {
                for (i in 1..10) {
                    db.getTable("users").selectWhere(JbinFilter.Match(path, EQ, random.nextInt(100) + 1))
                    println("$i/10")
                }
            }
        }
    }

    println(nanoTime / 1_000_000_000.0 / 10.0)
    System.exit(0)

}

fun rand() = random.nextInt(100) + 1