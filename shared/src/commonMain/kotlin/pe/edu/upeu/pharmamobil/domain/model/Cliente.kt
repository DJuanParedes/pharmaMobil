package pe.edu.upeu.pharmamobil.domain.model

data class Cliente(
    val id: Long,
    val nombre: String,
    val correo: String,
    val telefono: String?
) {
    init {
        require(id > 0) { "El id del cliente debe ser positivo" }
        require(nombre.isNotBlank()) { "El nombre del cliente es obligatorio" }
        require(correo.isNotBlank() && '@' in correo) { "El correo no es válido" }
    }

    fun obtenerTelefono(): String =
        telefono?.takeIf { it.isNotBlank() } ?: "No registrado"
}
