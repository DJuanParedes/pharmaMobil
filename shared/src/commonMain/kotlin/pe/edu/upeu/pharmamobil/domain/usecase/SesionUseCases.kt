package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository

class IniciarSesionUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(c: Credenciales) = repository.iniciarSesion(c)
}
class CerrarSesionUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.cerrarSesion()
}
