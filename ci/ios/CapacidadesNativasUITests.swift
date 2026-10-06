import XCTest

final class CapacidadesNativasUITests: XCTestCase {
    func testListadoYCompartir() throws {
        continueAfterFailure = false
        let app = XCUIApplication(bundleIdentifier: "pe.edu.upeu.pharmamobil.PharmaMobil")
        app.launch()
        let productos = app.buttons["Registrar un producto"]
        XCTAssertTrue(productos.waitForExistence(timeout: 30))
        productos.tap()
        XCTAssertTrue(app.staticTexts["Paracetamol 500 mg"].waitForExistence(timeout: 45))
        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@ AND label CONTAINS %@", "S/", "15.50")).firstMatch.exists)
        guardarCaptura("listado-ios")

        let detalle = app.buttons["Ver detalle"].firstMatch
        XCTAssertTrue(detalle.waitForExistence(timeout: 15))
        detalle.tap()
        let compartir = app.buttons["Compartir"]
        XCTAssertTrue(compartir.waitForExistence(timeout: 30))
        guardarCaptura("detalle-ios")
        compartir.tap()
        // La hoja del sistema expone las acciones de compartir/copiar.
        let accionCopiar = app.buttons.matching(NSPredicate(format: "label == 'Copy' OR label == 'Copiar'")).firstMatch
        XCTAssertTrue(accionCopiar.waitForExistence(timeout: 15), app.debugDescription)
        guardarCaptura("compartir-ios")
        accionCopiar.tap()
    }

    private func guardarCaptura(_ nombre: String) {
        let captura = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        captura.name = nombre
        captura.lifetime = .keepAlways
        add(captura)
    }
}
