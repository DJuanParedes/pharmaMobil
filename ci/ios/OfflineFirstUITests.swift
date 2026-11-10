import XCTest

final class OfflineFirstUITests: XCTestCase {
    private func control(_ ruta: String) throws -> Data {
        let espera = expectation(description: ruta)
        var resultado: Data?
        var fallo: Error?
        URLSession.shared.dataTask(with: URL(string: "http://127.0.0.1:8099/" + ruta)!) { data, _, error in
            resultado = data; fallo = error; espera.fulfill()
        }.resume()
        wait(for: [espera], timeout: 30)
        if let fallo = fallo { throw fallo }
        return try XCTUnwrap(resultado)
    }
    private func captura(_ nombre: String) {
        let adjunto = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        adjunto.name = nombre; adjunto.lifetime = .keepAlways; add(adjunto)
    }
    private func productos(_ app: XCUIApplication) {
        let menu = app.buttons["Abrir menú"]
        XCTAssertTrue(menu.waitForExistence(timeout: 45), app.debugDescription)
        menu.tap(); app.buttons["Productos"].tap()
        XCTAssertTrue(app.staticTexts["Inventario offline-first"].waitForExistence(timeout: 30))
    }
    func testReaperturaConServidorApagado() throws {
        continueAfterFailure = false
        let datos = try JSONSerialization.jsonObject(with: control("credenciales")) as! [String: String]
        let app = XCUIApplication(bundleIdentifier: "pe.edu.upeu.pharmamobil.PharmaMobil")
        app.launch()
        let usuario = app.textFields.firstMatch
        XCTAssertTrue(usuario.waitForExistence(timeout: 30), app.debugDescription)
        usuario.tap(); usuario.typeText("admin")
        let clave = app.secureTextFields.firstMatch
        XCTAssertTrue(clave.waitForExistence(timeout: 10), app.debugDescription)
        clave.tap(); clave.typeText(datos["password"]!)
        app.buttons["Iniciar sesión"].tap()
        productos(app)
        XCTAssertTrue(app.staticTexts["AA S11 iOS persistente"].waitForExistence(timeout: 45), app.debugDescription)
        captura("ios-01-con-red")
        app.terminate()
        _ = try control("offline")
        app.launch()
        productos(app)
        XCTAssertTrue(app.staticTexts["Sin conexión: mostrando datos guardados"].waitForExistence(timeout: 45), app.debugDescription)
        XCTAssertTrue(app.staticTexts["AA S11 iOS persistente"].exists, app.debugDescription)
        captura("ios-02-servidor-apagado-reapertura")
    }
}
