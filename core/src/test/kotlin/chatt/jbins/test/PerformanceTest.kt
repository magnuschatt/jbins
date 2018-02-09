package chatt.jbins.test

import chatt.jbins.test.utils.jbinTransaction
import chatt.jbins.toDocument
import java.util.*
import kotlin.system.measureNanoTime

fun main(args: Array<String>) {

    val nanoTime = measureNanoTime {
        jbinTransaction { db ->
            db.getTable("users").apply { createIfNotExists() }

            for (k in 1..100) {
                for (i in 1..150) {
                    val table = db.getTable("users").apply { createIfNotExists() }
                    val users = (1..100).map {
                        val id = UUID.randomUUID().toString().replace("-", "")
                        mapOf("_id" to id, "name" to "Magnus", "age" to 27).toDocument()
                    }
                    table.insert(users)
                }
                println("$k/100")
            }
        }
    }

    println(nanoTime / 1_000_000_000.0)

}