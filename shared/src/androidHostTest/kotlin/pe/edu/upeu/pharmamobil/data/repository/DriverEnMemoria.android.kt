package pe.edu.upeu.pharmamobil.data.repository
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import pe.edu.upeu.pharmamobil.db.PharmaDb
actual fun driverEnMemoria(): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
    PharmaDb.Schema.create(it)
}
