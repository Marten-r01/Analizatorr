package analizator

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {
    fun connect(
        url: String,
        driver: String,
        user: String,
        password: String
    ) {
        Database.connect(
            url = url,
            driver = driver,
            user = user,
            password = password
        )
    }

    fun createSchema() {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                ExperimentsTable,
                SequencesTable,
                OrfsTable,
                ProteinsTable
            )
        }
    }

    fun resetSchema() {
        transaction {
            SchemaUtils.drop(
                ProteinsTable,
                OrfsTable,
                SequencesTable,
                ExperimentsTable
            )
            SchemaUtils.create(
                ExperimentsTable,
                SequencesTable,
                OrfsTable,
                ProteinsTable
            )
        }
    }

    fun clearAll() {
        transaction {
            ProteinsTable.deleteAll()
            OrfsTable.deleteAll()
            SequencesTable.deleteAll()
            ExperimentsTable.deleteAll()
        }
    }
}
