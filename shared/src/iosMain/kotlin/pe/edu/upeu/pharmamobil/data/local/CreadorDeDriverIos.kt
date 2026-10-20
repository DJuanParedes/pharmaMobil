package pe.edu.upeu.pharmamobil.data.local
import app.cash.sqldelight.db.SqlDriver
import pe.edu.upeu.pharmamobil.db.PharmaDb
import app.cash.sqldelight.driver.native.NativeSqliteDriver
class CreadorDeDriverIos : CreadorDeDriver {
    override fun crear(): SqlDriver = NativeSqliteDriver(PharmaDb.Schema, "pharma.db")
}
