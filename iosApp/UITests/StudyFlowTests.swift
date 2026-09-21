import XCTest

/// Proves the app works, not merely that it compiles.
///
/// Every decision this app makes is made in Kotlin — which cards are due, what a grade does to a
/// schedule, what a deck looks like after an edit — so a build that succeeds says the two languages
/// agree about types and says nothing at all about whether a tap reaches `DeckRepository` and the
/// answer comes back. These drive that chain end to end: SM-2 scheduling, due-card selection, deck
/// and card editing, and the round trip through `UserDefaults`.
final class StudyFlowTests: XCTestCase {

    private var app: XCUIApplication!
    private var suite: String!

    override func setUp() {
        continueAfterFailure = false
        // A suite per test. This app remembers everything, so tests that share a store are not
        // independent: the samples seed on first launch only, and a graded card stays graded for
        // a day, which would make each of these pass once and then fail until tomorrow.
        suite = "uitests-\(UUID().uuidString)"
        app = XCUIApplication()
    }

    override func tearDown() {
        app = nil
        suite = nil
    }

    // MARK: - Tests

    func testGradingACardTakesItOutOfTodaysQueue() {
        launchFresh()

        // Five due, and none of them has ever been seen. That number is the shared `Scheduler`'s
        // answer rather than a card count: a card with no review row counts as due.
        let due = app.staticTexts["due-Kotlin basics"]
        XCTAssertTrue(due.waitForExistence(timeout: 60), "the seeded deck never reached the list")
        XCTAssertEqual(due.label, "5 due")

        tap(app.buttons["deck-Kotlin basics"], "the Kotlin basics row")

        // The same five, reached by a different call on the other side of the bridge: `dueCards`
        // returning a list rather than `dueCount` returning an Int.
        let progress = app.staticTexts["studyProgress"]
        XCTAssertTrue(progress.waitForExistence(timeout: 15), "the study queue never appeared")
        XCTAssertEqual(progress.label, "Card 1 of 5")

        tap(app.buttons["showAnswer"], "Show answer")
        tap(app.buttons["grade-Good"], "the Good button")
        expect(progress, toHaveLabel: "Card 2 of 5")

        back()

        // Good on a never-seen card is SM-2 repetition 1 with a one-day interval, so the card is
        // due tomorrow and today's queue is one shorter. Nothing in Swift knows that rule; if the
        // grade had not crossed the bridge this would still read "5 due".
        XCTAssertTrue(due.waitForExistence(timeout: 15), "the deck list did not come back")
        expect(due, toHaveLabel: "4 due")
    }

    func testACardAddedToADeckIsDueTheSameDay() {
        launchFresh()

        createDeck(named: "Swift basics")

        // Created and empty. Two claims, split so a failure names one of them: the deck reached
        // the repository, and an empty deck has nothing to study.
        let cards = app.staticTexts["cards-Swift basics"]
        XCTAssertTrue(cards.waitForExistence(timeout: 30), "the new deck never reached the list")
        XCTAssertEqual(cards.label, "0 cards")
        XCTAssertFalse(app.staticTexts["due-Swift basics"].exists, "an empty deck reported cards due")

        openEditor(forDeck: "Swift basics")
        addCard(front: "What does ?? do?", back: "Nil-coalescing — supplies a value when the left side is nil.")

        // The editor lists the deck's cards straight off the repository, so this is the first
        // proof that the card was stored rather than only typed.
        XCTAssertTrue(app.staticTexts["What does ?? do?"].waitForExistence(timeout: 15),
                      "the card was not in the deck the repository handed back")

        back()

        // And the payoff: a brand-new card is due today, because `Scheduler` counts a card with no
        // review history as due. The count went from absent to one without the app restarting.
        XCTAssertTrue(cards.waitForExistence(timeout: 15), "the deck list did not come back")
        expect(cards, toHaveLabel: "1 card")
        expect(app.staticTexts["due-Swift basics"], toHaveLabel: "1 due")
    }

    func testReviewProgressAndNewDecksSurviveARelaunch() {
        launchFresh()

        createDeck(named: "Persisted deck")
        XCTAssertTrue(app.staticTexts["cards-Persisted deck"].waitForExistence(timeout: 30),
                      "the new deck never reached the list")

        let due = app.staticTexts["due-Kotlin basics"]
        XCTAssertTrue(due.waitForExistence(timeout: 15))
        tap(app.buttons["deck-Kotlin basics"], "the Kotlin basics row")
        tap(app.buttons["showAnswer"], "Show answer")
        tap(app.buttons["grade-Easy"], "the Easy button")
        back()
        expect(due, toHaveLabel: "4 due")

        relaunchWithoutClearingTheStore()

        // Two stores, two different keys, one relaunch. The deck came back from `DeckStore`, which
        // also means the samples did not re-seed over it; the count came back from `ReviewStore`,
        // which is the one that is easy to lose because it is written on every grade.
        XCTAssertTrue(app.staticTexts["cards-Persisted deck"].waitForExistence(timeout: 60),
                      "the deck did not survive the relaunch")
        expect(app.staticTexts["due-Kotlin basics"], toHaveLabel: "4 due")
    }

    // MARK: - Driving the app

    /// Launches against an empty store, so the repository seeds the samples and no earlier test's
    /// grades are in the way.
    private func launchFresh() {
        app.launchEnvironment["UITEST_DEFAULTS_SUITE"] = suite
        app.launchEnvironment["UITEST_DEFAULTS_RESET"] = "1"
        app.launch()
    }

    /// Restarts against the same store. The reset flag is separate from the suite name precisely
    /// so this can exist.
    private func relaunchWithoutClearingTheStore() {
        app.terminate()
        app.launchEnvironment["UITEST_DEFAULTS_RESET"] = "0"
        app.launch()
    }

    /// Taps once the element is genuinely hittable, not merely present.
    ///
    /// `waitForExistence` proves an element is in the accessibility tree and nothing more. Tapping
    /// one whose frame has not settled — the app still being brought to the front, a sheet still
    /// animating — synthesises the event at hit point {-1, -1}, which silently does nothing and
    /// then surfaces as a timeout somewhere else entirely.
    private func tap(
        _ element: XCUIElement,
        _ what: String,
        timeout: TimeInterval = 30,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        let hittable = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "isHittable == true"),
            object: element
        )
        XCTAssertEqual(XCTWaiter().wait(for: [hittable], timeout: timeout), .completed,
                       "\(what) never became tappable", file: file, line: line)
        element.tap()
    }

    private func createDeck(named name: String) {
        tap(app.buttons["newDeck"], "the new-deck button")
        let alert = app.alerts.firstMatch
        XCTAssertTrue(alert.waitForExistence(timeout: 15), "the new-deck prompt never opened")
        let field = alert.textFields.firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        field.typeText(name)
        alert.buttons["Create"].tap()
    }

    /// The editor is behind a trailing swipe, next to a destructive Delete — which is why both
    /// swipe buttons carry identifiers rather than being picked out by their glyph.
    private func openEditor(forDeck name: String) {
        app.buttons["deck-\(name)"].swipeLeft()
        let edit = app.buttons["editDeck-\(name)"]
        XCTAssertTrue(edit.waitForExistence(timeout: 10), "the swipe did not reveal the editor button")
        tap(edit, "the editor button")
    }

    private func addCard(front: String, back: String) {
        let add = app.buttons["addCard"]
        XCTAssertTrue(add.waitForExistence(timeout: 15), "the deck editor never opened")
        tap(add, "the add-card button")

        let alert = app.alerts.firstMatch
        XCTAssertTrue(alert.waitForExistence(timeout: 15), "the add-card prompt never opened")
        let frontField = alert.textFields.element(boundBy: 0)
        XCTAssertTrue(frontField.waitForExistence(timeout: 10))
        frontField.tap()
        frontField.typeText(front)
        let backField = alert.textFields.element(boundBy: 1)
        backField.tap()
        backField.typeText(back)
        alert.buttons["Add"].tap()
    }

    private func back() {
        tap(app.navigationBars.buttons.firstMatch, "the back button")
    }

    // MARK: - Assertions

    /// Waits for a label rather than reading one. SwiftUI redraws after the repository answers, so
    /// reading immediately after a tap races the redraw and fails for the wrong reason.
    private func expect(
        _ element: XCUIElement,
        toHaveLabel expected: String,
        timeout: TimeInterval = 15,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        let matched = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "label == %@", expected),
            object: element
        )
        let outcome = XCTWaiter().wait(for: [matched], timeout: timeout)
        let seen = element.exists ? "\"\(element.label)\"" : "no such element"
        XCTAssertEqual(outcome, .completed, "expected \"\(expected)\", saw \(seen)", file: file, line: line)
    }
}
