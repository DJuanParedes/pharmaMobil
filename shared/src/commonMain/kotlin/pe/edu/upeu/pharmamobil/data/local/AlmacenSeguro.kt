package pe.edu.upeu.pharmamobil.data.local

interface AlmacenSeguro {
    fun guardarPerfil(perfil: String) {}
    fun leerPerfil(): String? = null
    fun borrarPerfil() {}
    fun guardarToken(token: String)
    fun leerToken(): String?
    fun borrarToken()
}
