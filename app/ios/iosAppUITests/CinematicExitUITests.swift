import XCTest

/// The way out of cinematic mode has to clear the notch and answer taps in every orientation.
/// The JVM test covers its colour over the letterbox, but has no real insets. Keeps screenshots in the test result.
final class CinematicExitUITests: XCTestCase {
    private let app = XCUIApplication()
    private let timeout: TimeInterval = 15

    override func setUpWithError() throws {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
        app.launch()
    }

    override func tearDownWithError() throws {
        XCUIDevice.shared.orientation = .portrait
    }

    func testExitClearsTheInsetsAndWorksInEveryOrientation() {
        let orientations: [(String, UIDeviceOrientation)] = [
            ("portrait", .portrait), ("landscape-left", .landscapeLeft), ("landscape-right", .landscapeRight),
        ]
        for (name, orientation) in orientations {
            XCUIDevice.shared.orientation = orientation
            element("Enter cinematic mode").tapWhenReady(timeout)
            let exit = element("Exit cinematic mode")
            XCTAssertTrue(exit.waitForExistence(timeout: timeout), "\(name): no exit button")
            // Past the toggle's fade and the rotation.
            Thread.sleep(forTimeInterval: 1.5)

            let shot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
            shot.name = "cinematic-\(name)"
            shot.lifetime = .keepAlways
            add(shot)

            let window = app.windows.firstMatch.frame
            let frame = exit.frame
            print("cinematic-\(name): window \(window), exit \(frame)")
            XCTAssertTrue(exit.isHittable, "\(name): exit button not hittable")
            // The sensor housing and status bar take at least this much on the phones and tablets with insets.
            if UIDevice.current.userInterfaceIdiom == .phone {
                if orientation == .portrait {
                    XCTAssertGreaterThanOrEqual(frame.minY, 54, "\(name): under the status bar")
                } else {
                    XCTAssertLessThanOrEqual(frame.maxX, window.maxX - 54, "\(name): under the notch")
                }
            }

            exit.tap()
            XCTAssertTrue(element("Enter cinematic mode").waitForExistence(timeout: timeout), "\(name): tap didn't leave cinematic mode")
        }
    }

    private func element(_ text: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", text)).firstMatch
    }
}

private extension XCUIElement {
    func tapWhenReady(_ timeout: TimeInterval) {
        XCTAssertTrue(waitForExistence(timeout: timeout), "Not on screen: \(label)")
        tap()
    }
}
