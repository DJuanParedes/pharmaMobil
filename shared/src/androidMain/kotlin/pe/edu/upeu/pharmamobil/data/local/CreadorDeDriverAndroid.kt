package pe.edu.upeu.pharmamobil.data.local
import app.cash.sqldelight.db.SqlDriver
import pe.edu.upeu.pharmamobil.db.PharmaDb
import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
class CreadorDeDriverAndroid(private val contexto: Context) : CreadorDeDriver {
    override fun crear(): SqlDriver = AndroidSqliteDriver(PharmaDb.Schema, contexto, "pharma.db")
}
