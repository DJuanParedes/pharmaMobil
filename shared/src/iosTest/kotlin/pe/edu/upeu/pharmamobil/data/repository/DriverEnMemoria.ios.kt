package pe.edu.upeu.pharmamobil.data.repository
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import pe.edu.upeu.pharmamobil.db.PharmaDb
actual fun driverEnMemoria(): SqlDriver = inMemoryDriver(PharmaDb.Schema)
