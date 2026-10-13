@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package pe.edu.upeu.pharmamobil.data.local

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*

class AlmacenSeguroIos : AlmacenSeguro {
    private fun consulta() = NSMutableDictionary().apply {
        setObject(kSecClassGenericPassword!!, kSecClass as NSCopyingProtocol)
        setObject("pe.edu.upeu.pharmamobil.sesion", kSecAttrService as NSCopyingProtocol)
        setObject("jwt", kSecAttrAccount as NSCopyingProtocol)
    }
    override fun guardarToken(token: String) {
        val datos = (token as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!
        val atributos = NSMutableDictionary().apply {
            setObject(datos, kSecValueData as NSCopyingProtocol)
            setObject(kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly!!, kSecAttrAccessible as NSCopyingProtocol)
        }
        val estado = SecItemUpdate(CFBridgingRetain(consulta()) as CFDictionaryRef,
            CFBridgingRetain(atributos) as CFDictionaryRef)
        if (estado == errSecItemNotFound) {
            val nueva = consulta().apply { addEntriesFromDictionary(atributos) }
            check(SecItemAdd(CFBridgingRetain(nueva) as CFDictionaryRef, null) == errSecSuccess)
        } else check(estado == errSecSuccess)
    }
    override fun leerToken(): String? = memScoped {
        val consulta = consulta().apply {
            setObject(kCFBooleanTrue!!, kSecReturnData as NSCopyingProtocol)
            setObject(kSecMatchLimitOne!!, kSecMatchLimit as NSCopyingProtocol)
        }
        val salida = alloc<CFTypeRefVar>()
        when (val estado = SecItemCopyMatching(CFBridgingRetain(consulta) as CFDictionaryRef, salida.ptr)) {
            errSecItemNotFound -> null
            errSecSuccess -> {
                val datos = CFBridgingRelease(salida.value) as NSData
                NSString.create(data = datos, encoding = NSUTF8StringEncoding)?.toString()
            }
            else -> error("Keychain no disponible: $estado")
        }
    }
    override fun borrarToken() {
        val estado = SecItemDelete(CFBridgingRetain(consulta()) as CFDictionaryRef)
        check(estado == errSecSuccess || estado == errSecItemNotFound)
    }
}
