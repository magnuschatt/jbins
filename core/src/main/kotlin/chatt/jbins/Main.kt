package chatt.jbins

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException


fun main(args: Array<String>) {
    println("-------- PostgreSQL " + "JDBC Connection Testing ------------")

    try {

        Class.forName("org.postgresql.Driver")

    } catch (e: ClassNotFoundException) {

        println("Where is your PostgreSQL JDBC Driver? " + "Include in your library path!")
        e.printStackTrace()
        return

    }


    println("PostgreSQL JDBC Driver Registered!")

    val connection: Connection?

    try {

        connection = DriverManager.getConnection(
                "jdbc:postgresql://docker:5432/jbins",
                "postgres",
                "postgres"
        )

    } catch (e: SQLException) {

        println("Connection Failed! Check output console")
        e.printStackTrace()
        return

    }


    if (connection != null) {
        println("You made it, take control your database now!")
    } else {
        println("Failed to make connection!")
    }
}