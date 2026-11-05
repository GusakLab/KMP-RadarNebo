import XCTest

/// Drives the app from the outside through the accessibility tree, like the Android UI tests. Needs no server.
final class AppUITests: XCTestCase {
    private let app = XCUIApplication()
    private let timeout: TimeInterval = 15

    override func setUpWithError() throws {
        continueAfterFailure = false
        app.launch()
        need("Settings")
    }

    func testLaunchShowsTheMapAndItsControls() {
        need("Map")
        need("Favorites")
        need("Settings")
        need("Enter cinematic mode")
        need("Show tracked targets")
    }

    func testFavoritesOpensAndCloses() {
        tap("Favorites")
        need("Favorite places")
        // Compose joins the button's description with its glyph: "Close, ✕".
        let close = app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Close'")).firstMatch
        XCTAssertTrue(close.waitForExistence(timeout: timeout), "No close button")
        close.tap()
        gone("Favorite places")
        need("Show tracked targets")
    }

    func testACityFoundBySearchCanBeAddedAndRemoved() {
        tap("Favorites")
        // Compose exposes its text fields as text views.
        let search = app.textViews.firstMatch
        XCTAssertTrue(search.waitForExistence(timeout: timeout), "No search field")
        search.tap()
        search.typeText("Lviv")
        // "+" only marks a search suggestion, so this can't be satisfied by the text just typed into the field.
        tap("+")
        gone("+")
        need("Lviv")
        tap("Remove")
        gone("Lviv")
    }

    func testSettingsChangesTheTheme() {
        tap("Settings")
        need("APPEARANCE")
        for theme in ["Dark", "Light", "System"] {
            tap(theme)
            let button = element(theme)
            let selected = NSPredicate(format: "isSelected == true OR value == '1' OR value == 'selected'")
            XCTAssertEqual(
                XCTWaiter.wait(for: [expectation(for: selected, evaluatedWith: button)], timeout: timeout),
                .completed,
                "\(theme) should be selected",
            )
        }
    }

    func testTheAppSurvivesARotation() {
        XCUIDevice.shared.orientation = .landscapeLeft
        defer { XCUIDevice.shared.orientation = .portrait }
        need("Settings")
        tap("Favorites")
        need("Favorite places")
    }

    func testTappingTheScrimCloses() {
        tap("Favorites")
        need("Favorite places")
        // Just below the status bar, which takes the touches on it; the panel keeps a margin from there.
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.078)).tap()
        gone("Favorite places")
    }

    func testTheNavigationBarStaysUsableWithADialogOpen() {
        tap("Favorites")
        need("Favorite places")
        tap("Settings")
        need("APPEARANCE")
        gone("Favorite places")
        tap("Map")
        gone("APPEARANCE")
    }

    /// The scrim's reach is pixels, so this keeps screenshots in the test result to review by eye.
    func testADialogDimsTheWholeScreen() {
        XCUIDevice.shared.orientation = .portrait
        tap("Favorites")
        need("Favorite places")
        attachScreenshot("favorites-portrait")
        tap("Add by coordinates")
        need("Cancel")
        attachScreenshot("coordinates-portrait")
        tap("Cancel")
        gone("Cancel")
        tap("Settings")
        need("APPEARANCE")
        attachScreenshot("settings-portrait")
        XCUIDevice.shared.orientation = .landscapeLeft
        defer { XCUIDevice.shared.orientation = .portrait }
        need("Settings")
        attachScreenshot("settings-landscape")
    }

    private func attachScreenshot(_ name: String) {
        // Past the scrim's fade and any rotation.
        Thread.sleep(forTimeInterval: 1)
        let shot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        shot.name = name
        shot.lifetime = .keepAlways
        add(shot)
    }

    private func element(_ text: String) -> XCUIElement {
        app.descendants(matching: .any)
            .matching(NSPredicate(format: "label == %@ OR value == %@", text, text))
            .firstMatch
    }

    @discardableResult
    private func need(_ text: String, file: StaticString = #filePath, line: UInt = #line) -> XCUIElement {
        let found = element(text)
        if !found.waitForExistence(timeout: timeout) {
            XCTFail("Not on screen: \(text)\n\(app.debugDescription)", file: file, line: line)
        }
        return found
    }

    private func tap(_ text: String, file: StaticString = #filePath, line: UInt = #line) {
        need(text, file: file, line: line).tap()
    }

    private func gone(_ text: String, file: StaticString = #filePath, line: UInt = #line) {
        let missing = NSPredicate(format: "exists == false")
        let result = XCTWaiter.wait(for: [expectation(for: missing, evaluatedWith: element(text))], timeout: timeout)
        XCTAssertEqual(result, .completed, "Should be gone: \(text)", file: file, line: line)
    }
}
