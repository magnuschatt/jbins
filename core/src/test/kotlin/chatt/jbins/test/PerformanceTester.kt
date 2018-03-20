package chatt.jbins.test

import chatt.jbins.JbinDocument.Companion.ID_PATH
import chatt.jbins.JbinFilter
import chatt.jbins.JbinFilter.Comparator.EQ
import chatt.jbins.test.utils.jbinTransaction
import chatt.jbins.utils.toDocument
import java.util.*
import kotlin.system.measureNanoTime

val random = Random(1337)

fun main(args: Array<String>) {

    val ids = (1..10).map { rand() }
    val path = "age"
    val filters = (1..10).map { JbinFilter.Match(path, EQ, ids[random.nextInt(ids.size)]) }

    var nanoTime: Long = 0
    measureNanoTime {
        jbinTransaction { db ->
            db.getTable("users").apply { create() }


            for (k in 1..100) {
                for (i in 1..150) {
                    val table = db.getTable("users").apply { create() }
                    val users = (1..100).map {
                        val id = UUID.randomUUID().toString().replace("-", "")
                        mapOf(ID_PATH to id, "name" to "Magnus", path to ids[random.nextInt(ids.size)]).toDocument()
                    }
                    table.insert(users)
                }
                println("$k/100")
            }

        }

        jbinTransaction { db ->

            db.getTable("users").createIndex(path)

            val filter = JbinFilter.Match(path, EQ, ids[random.nextInt(ids.size)])
            measureNanoTime {
                val selectWhere = db.getTable("users").select(filter)
                println("Result size: " + selectWhere.size)
            }

            filters.forEach {
                db.getTable("users").select(it)
            }
        }

        jbinTransaction { db ->
            nanoTime = measureNanoTime {
                filters.forEachIndexed { i, it ->
                    db.getTable("users").select(it)
                    println("$i/1000")
                }
            }
        }
    }

    println(nanoTime / 1_000_000_000.0 / 1000.0)
    System.exit(0)

}

fun rand() = UUID.randomUUID().toString().replace("-","")