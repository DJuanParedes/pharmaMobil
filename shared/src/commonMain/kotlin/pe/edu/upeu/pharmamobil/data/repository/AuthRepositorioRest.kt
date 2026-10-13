package pe.edu.upeu.pharmamobil.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.*
import io.ktor.client.statement.request
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobil.data.local.*
import pe.edu.upeu.pharmamobil.data.remote.dto.*
import pe.edu.upeu.pharmamobil.domain.model.*
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository

class AuthRepositorioRest(
    private val cliente: HttpClient,
    private val almacen: AlmacenSeguro,
    private val estado: EstadoSesion,
) : AuthRepository {
    private val mutex = Mutex()
    override suspend fun iniciarSesion(credenciales: Credenciales): Sesion = mutex.withLock {
        if (credenciales.usuario.isBlank() || credenciales.clave.isBlank())
            throw SesionException(ErrorSesion.CredencialesInvalidas)
        val dto = traducir {
            cliente.post("auth/login") {
                setBody(LoginRequestDto(credenciales.usuario.trim(), credenciales.clave))
            }.body<LoginResponseDto>()
        }
        check(dto.token.isNotBlank() && dto.tipo.equals("Bearer", true))
        try { almacen.guardarToken(dto.token) }
        catch (e: Exception) { throw SesionException(ErrorSesion.Almacenamiento) }
        cliente.authProvider<BearerAuthProvider>()?.clearToken()
        Sesion(dto.token, dto.username, dto.nombreCompleto, dto.permisos.toSet()).also {
            estado.limpiarAviso(); estado.actualizar(it)
        }
    }
    override suspend fun sesionGuardada(): Sesion? = mutex.withLock {
        val token = try { almacen.leerToken() }
        catch (e: Exception) { throw SesionException(ErrorSesion.Almacenamiento) }
        if (token.isNullOrBlank()) return@withLock null
        try {
            val dto = traducir { cliente.get("auth/me").body<UsuarioActualDto>() }
            Sesion(token, dto.username, dto.empleadoNombre, dto.permisos.toSet()).also(estado::actualizar)
        } catch (e: SesionException) {
            if (e.error == ErrorSesion.SesionExpirada || e.error == ErrorSesion.CredencialesInvalidas) {
                almacen.borrarToken(); cliente.authProvider<BearerAuthProvider>()?.clearToken()
                estado.expirar(); null
            } else throw e
        }
    }
    override suspend fun cerrarSesion() = mutex.withLock {
        almacen.borrarToken()
        cliente.authProvider<BearerAuthProvider>()?.clearToken()
        estado.actualizar(null); estado.limpiarAviso()
    }
    private suspend fun <T> traducir(block: suspend () -> T): T = try { block() }
    catch (e: CancellationException) { throw e }
    catch (e: ResponseException) {
        throw SesionException(when (e.response.status.value) {
            401 -> if (e.response.request.url.encodedPath.endsWith("/auth/login"))
                ErrorSesion.CredencialesInvalidas else ErrorSesion.SesionExpirada
            403 -> ErrorSesion.SinPermiso
            else -> ErrorSesion.Servidor
        })
    } catch (e: SesionException) { throw e }
    catch (e: Exception) { throw SesionException(ErrorSesion.SinConexion) }
}
