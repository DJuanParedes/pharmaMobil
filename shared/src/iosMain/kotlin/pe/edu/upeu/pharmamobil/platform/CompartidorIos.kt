package pe.edu.upeu.pharmamobil.platform

import kotlinx.cinterop.ExperimentalForeignApi
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UINavigationController
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class)
class CompartidorIos : Compartidor {
    override fun compartir(texto: String) {
        // El evento Compose se ejecuta en el hilo principal. Usar la escena
        // activa evita depender de UIApplication.keyWindow, obsoleta en iOS.
        val ventana = UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .filter { it.activationState == UISceneActivationStateForegroundActive }
            .flatMap { it.windows.filterIsInstance<UIWindow>() }
            .firstOrNull { it.isKeyWindow() }
        val raiz = checkNotNull(ventana?.rootViewController) {
            "No hay una ventana activa para compartir el producto."
        }
        val presentador = controladorVisible(raiz)
        val controlador = UIActivityViewController(
            activityItems = listOf(texto),
            applicationActivities = null,
        )
        // UIActivityViewController necesita un ancla de popover en iPad.
        controlador.popoverPresentationController?.let { popover ->
            popover.sourceView = presentador.view
            popover.sourceRect = presentador.view.bounds
        }
        presentador.presentViewController(controlador, true, null)
    }

    private fun controladorVisible(raiz: UIViewController): UIViewController {
        raiz.presentedViewController?.let { return controladorVisible(it) }
        if (raiz is UINavigationController) {
            raiz.visibleViewController?.let { return controladorVisible(it) }
        }
        if (raiz is UITabBarController) {
            raiz.selectedViewController?.let { return controladorVisible(it) }
        }
        return raiz
    }
}
