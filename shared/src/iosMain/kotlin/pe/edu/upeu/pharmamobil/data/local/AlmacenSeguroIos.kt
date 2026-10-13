@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package pe.edu.upeu.pharmamobil.data.local

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*

class AlmacenSeguroIos : AlmacenSeguro {
    private fun clave(valor: CFStringRef?): NSCopyingProtocol = CFBridgingRelease(valor) as NSCopyingProtocol
    private fun objeto(valor: CFTypeRef?): Any = CFBridgingRelease(valor)!!
    private inline fun <T> conConsulta(diccionario: NSMutableDictionary, bloque: (CFDictionaryRef) -> T): T {
        val referencia = CFBridgingRetain(diccionario)!!
        return try { bloque(referencia as CFDictionaryRef) } finally { CFRelease(referencia) }
    }
    private fun consulta() = NSMutableDictionary().apply {
        setObject(objeto(kSecClassGenericPassword), clave(kSecClass))
        setObject("pe.edu.upeu.pharmamobil.sesion", clave(kSecAttrService))
        setObject("jwt", clave(kSecAttrAccount))
    }
    override fun guardarToken(token: String) {
        val datos = (token as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!
        val atributos = NSMutableDictionary().apply {
            setObject(datos, clave(kSecValueData))
            setObject(objeto(kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly), clave(kSecAttrAccessible))
        }
        val estado = conConsulta(consulta()) { consulta ->
            conConsulta(atributos) { SecItemUpdate(consulta, it) }
        }
        if (estado == errSecItemNotFound) {
            val nueva = consulta().apply {
                setObject(datos, clave(kSecValueData))
                setObject(objeto(kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly), clave(kSecAttrAccessible))
            }
            check(conConsulta(nueva) { SecItemAdd(it, null) } == errSecSuccess)
        } else check(estado == errSecSuccess)
    }
    override fun leerToken(): String? = memScoped {
        val consulta = consulta().apply {
            setObject(objeto(kCFBooleanTrue), clave(kSecReturnData))
            setObject(objeto(kSecMatchLimitOne), clave(kSecMatchLimit))
        }
        val salida = alloc<CFTypeRefVar>()
        when (val estado = conConsulta(consulta) { SecItemCopyMatching(it, salida.ptr) }) {
            errSecItemNotFound -> null
            errSecSuccess -> {
                val datos = CFBridgingRelease(salida.value) as NSData
                NSString.create(data = datos, encoding = NSUTF8StringEncoding)?.toString()
            }
            else -> error("Keychain no disponible: $estado")
        }
    }
    override fun borrarToken() {
        val estado = conConsulta(consulta()) { SecItemDelete(it) }
        check(estado == errSecSuccess || estado == errSecItemNotFound)
    }
}
