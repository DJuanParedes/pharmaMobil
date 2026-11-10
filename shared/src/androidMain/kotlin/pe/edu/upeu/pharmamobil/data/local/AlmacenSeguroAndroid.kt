package pe.edu.upeu.pharmamobil.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// Se conserva la API solicitada por la guía; está deprecada desde security-crypto 1.1.
@Suppress("DEPRECATION")
class AlmacenSeguroAndroid(contexto: Context) : AlmacenSeguro {
    private val llave = MasterKey.Builder(contexto)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val prefs = EncryptedSharedPreferences.create(
        contexto, "sesion_segura", llave,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    override fun guardarPerfil(perfil: String) { check(prefs.edit().putString("perfil", perfil).commit()) }
    override fun leerPerfil(): String? = prefs.getString("perfil", null)
    override fun borrarPerfil() { check(prefs.edit().remove("perfil").commit()) }
    override fun guardarToken(token: String) {
        check(prefs.edit().putString("jwt", token).commit()) { "No se pudo guardar la sesión" }
    }
    override fun leerToken(): String? = prefs.getString("jwt", null)
    override fun borrarToken() {
        check(prefs.edit().remove("jwt").remove("perfil").commit()) { "No se pudo borrar la sesión" }
    }
}
