import XCTest

/// Proves the app works, not merely that it compiles.
///
/// Every decision this app makes is made in Kotlin — which cards are due, what a grade does to a
/// schedule, what a deck looks like after an edit — so a build that succeeds says the two languages
/// agree about types and says nothing at all about whether a tap reaches `DeckRepository` and the
/// answer comes back. These drive that chain end to end: SM-2 scheduling, due-card selection, deck
/// and card editing, and the round trip through `UserDefaults`.
final class StudyFlowTests: XCTestCase {

    /// How long to wait for the app to answer. A wait returns the moment it is met, so a generous
    /// one costs nothing on a quick run. 15 seconds was not enough on a CI runner on 16 October
    /// 2026: finding the deck row alone took 14, and the deck list was already right ("expected
    /// "5 cards, nothing due", saw "5 cards, nothing due"").
    static let wait: TimeInterval = 30


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
        let deck = app.buttons["deck-Kotlin basics"]
        XCTAssertTrue(deck.waitForExistence(timeout: 60), "the seeded deck never reached the list")
        // One element per row, read the way VoiceOver reads it: the name, then the counts.
        XCTAssertEqual(deck.label, "Kotlin basics")
        XCTAssertEqual(deck.value as? String, "5 cards, 5 due")

        tap(deck, "the Kotlin basics row")

        // The same five, reached by a different call on the other side of the bridge: `dueCards`
        // returning a list rather than `dueCount` returning an Int.
        let progress = app.staticTexts["studyProgress"]
        XCTAssertTrue(progress.waitForExistence(timeout: Self.wait), "the study queue never appeared")
        XCTAssertEqual(progress.label, "Card 1 of 5")

        tap(app.buttons["showAnswer"], "Show answer")
        tap(app.buttons["grade-Good"], "the Good button")
        expect(progress, toHaveLabel: "Card 2 of 5")

        back()

        // Good on a never-seen card is SM-2 repetition 1 with a one-day interval, so the card is
        // due tomorrow and today's queue is one shorter. Nothing in Swift knows that rule; if the
        // grade had not crossed the bridge this would still read "5 due".
        XCTAssertTrue(deck.waitForExistence(timeout: Self.wait), "the deck list did not come back")
        expect(deck, toHaveValue: "5 cards, 4 due")
    }

    func testACardGradedAgainComesBackBeforeTheSittingEnds() {
        launchFresh()
        tap(app.buttons["deck-Kotlin basics"], "the Kotlin basics row")
        let progress = app.staticTexts["studyProgress"]
        XCTAssertTrue(progress.waitForExistence(timeout: Self.wait), "the study queue never appeared")

        tap(app.buttons["showAnswer"], "Show answer")
        tap(app.buttons["grade-Again"], "the Again button")
        for position in 2...5 {
            expect(progress, toHaveLabel: "Card \(position) of 5")
            tap(app.buttons["showAnswer"], "Show answer")
            tap(app.buttons["grade-Good"], "the Good button")
        }

        // The forgotten card comes round again before the sitting ends. `StudySession`, in Kotlin,
        // put it at the back of the line; Swift only shows what it says is next.
        expect(progress, toHaveLabel: "Once more · 1 left")
        tap(app.buttons["showAnswer"], "Show answer")
        tap(app.buttons["grade-Good"], "the Good button")
        XCTAssertTrue(app.staticTexts["Session complete"].waitForExistence(timeout: Self.wait), "the sitting did not end")

        back()
        // Every card was graded across the bridge, the forgotten one for tomorrow.
        expect(app.buttons["deck-Kotlin basics"], toHaveValue: "5 cards, nothing due")
    }

    func testACardAddedToADeckIsDueTheSameDay() {
        launchFresh()

        createDeck(named: "Swift basics")

        // Created and empty. Two claims, split so a failure names one of them: the deck reached
        // the repository, and an empty deck has nothing to study.
        let deck = app.buttons["deck-Swift basics"]
        XCTAssertTrue(deck.waitForExistence(timeout: Self.wait), "the new deck never reached the list")
        XCTAssertEqual(deck.value as? String, "0 cards, nothing due", "an empty deck reported cards due")

        openEditor(forDeck: "Swift basics")
        addCard(front: "What does ?? do?", back: "Nil-coalescing — supplies a value when the left side is nil.")

        // The editor lists the deck's cards straight off the repository, so this is the first
        // proof that the card was stored rather than only typed.
        XCTAssertTrue(app.staticTexts["What does ?? do?"].waitForExistence(timeout: Self.wait),
                      "the card was not in the deck the repository handed back")

        back()

        // And the payoff: a brand-new card is due today, because `Scheduler` counts a card with no
        // review history as due. The count went from nothing to one without the app restarting.
        XCTAssertTrue(deck.waitForExistence(timeout: Self.wait), "the deck list did not come back")
        expect(deck, toHaveValue: "1 card, 1 due")
    }

    func testASittingOfOneCardCountsOneCard() {
        launchFresh()
        createDeck(named: "Swift basics")
        let deck = app.buttons["deck-Swift basics"]
        XCTAssertTrue(deck.waitForExistence(timeout: Self.wait), "the new deck never reached the list")
        openEditor(forDeck: "Swift basics")
        addCard(front: "What does ?? do?", back: "Nil-coalescing.")
        XCTAssertTrue(app.staticTexts["What does ?? do?"].waitForExistence(timeout: Self.wait),
                      "the card was not in the deck the repository handed back")
        back()
        expect(deck, toHaveValue: "1 card, 1 due")

        tap(deck, "the Swift basics row")
        tap(app.buttons["showAnswer"], "Show answer")
        tap(app.buttons["grade-Good"], "the Good button")
        XCTAssertTrue(app.staticTexts["Session complete"].waitForExistence(timeout: Self.wait), "the sitting did not end")
        // One card, so "card": the summary said "1 cards reviewed".
        XCTAssertTrue(app.staticTexts["1 card reviewed"].exists, "the summary did not count one card as one")
    }

    func testReviewProgressAndNewDecksSurviveARelaunch() {
        launchFresh()

        createDeck(named: "Persisted deck")
        XCTAssertTrue(app.buttons["deck-Persisted deck"].waitForExistence(timeout: Self.wait),
                      "the new deck never reached the list")

        let deck = app.buttons["deck-Kotlin basics"]
        XCTAssertTrue(deck.waitForExistence(timeout: Self.wait))
        tap(deck, "the Kotlin basics row")
        tap(app.buttons["showAnswer"], "Show answer")
        tap(app.buttons["grade-Easy"], "the Easy button")
        back()
        expect(deck, toHaveValue: "5 cards, 4 due")

        relaunchWithoutClearingTheStore()

        // Two stores, two different keys, one relaunch. The deck came back from `DeckStore`, which
        // also means the samples did not re-seed over it; the count came back from `ReviewStore`,
        // which is the one that is easy to lose because it is written on every grade.
        XCTAssertTrue(app.buttons["deck-Persisted deck"].waitForExistence(timeout: 60),
                      "the deck did not survive the relaunch")
        expect(app.buttons["deck-Kotlin basics"], toHaveValue: "5 cards, 4 due")
    }

    func testDeletingADeckAsksFirst() {
        launchFresh()

        // The trash glyph deleted the whole deck at one tap. Now it asks, and Cancel keeps the deck.
        openEditor(forDeck: "Kotlin basics")
        tap(app.buttons["deleteDeck"], "the delete-deck button")
        let alert = app.alerts["Delete “Kotlin basics”?"]
        XCTAssertTrue(alert.waitForExistence(timeout: Self.wait), "deleting a deck did not ask first")
        XCTAssertTrue(alert.staticTexts["Its 5 cards go with it."].exists, "the question did not say what goes")
        tap(alert.buttons["Cancel"], "Cancel")
        back()
        let deck = app.buttons["deck-Kotlin basics"]
        XCTAssertTrue(deck.waitForExistence(timeout: Self.wait), "Cancel deleted the deck anyway")

        // And confirming does delete it.
        openEditor(forDeck: "Kotlin basics")
        tap(app.buttons["deleteDeck"], "the delete-deck button")
        XCTAssertTrue(alert.waitForExistence(timeout: Self.wait), "the second delete did not ask either")
        tap(alert.buttons["Delete"], "Delete")
        XCTAssertTrue(deck.waitForNonExistence(timeout: Self.wait), "the confirmed delete left the deck in the list")
    }

    func testSwipingToDeleteADeckAsksFirst() {
        launchFresh()

        // The editor's trash button asked, but the list's swipe action deleted the deck outright.
        let deck = app.buttons["deck-Kotlin basics"]
        XCTAssertTrue(deck.waitForExistence(timeout: Self.wait), "the sample deck never appeared")
        deck.swipeLeft()
        tap(app.buttons["deleteDeck-Kotlin basics"], "the swipe's Delete")
        let alert = app.alerts["Delete “Kotlin basics”?"]
        XCTAssertTrue(alert.waitForExistence(timeout: Self.wait), "the swipe deleted the deck without asking")
        XCTAssertTrue(alert.staticTexts["Its 5 cards go with it."].exists, "the question did not say what goes")
        tap(alert.buttons["Cancel"], "Cancel")
        XCTAssertTrue(deck.waitForExistence(timeout: Self.wait), "Cancel deleted the deck anyway")

        deck.swipeLeft()
        tap(app.buttons["deleteDeck-Kotlin basics"], "the swipe's Delete")
        XCTAssertTrue(alert.waitForExistence(timeout: Self.wait), "the second swipe did not ask either")
        tap(alert.buttons["Delete"], "Delete")
        XCTAssertTrue(deck.waitForNonExistence(timeout: Self.wait), "the confirmed delete left the deck in the list")
    }

    func testASwipedAwayCardCanBeUndone() {
        launchFresh()

        // On Android a deleted card comes with Undo; on iOS the swipe was the end of it.
        openEditor(forDeck: "Kotlin basics")
        let card = app.staticTexts["sealed class?"]
        XCTAssertTrue(card.waitForExistence(timeout: Self.wait), "the deck's cards are not listed")
        card.swipeLeft()
        tap(app.buttons["Delete"], "the swipe's Delete")
        XCTAssertTrue(card.waitForNonExistence(timeout: Self.wait), "the swipe did not delete the card")

        tap(app.buttons["undoCardDelete"], "Undo")
        XCTAssertTrue(card.waitForExistence(timeout: Self.wait), "Undo did not bring the card back")
        back()
        expect(app.buttons["deck-Kotlin basics"], toHaveValue: "5 cards, 5 due")
    }

    func testToolbarButtonsSayWhatTheyDo() {
        launchFresh()

        // Glyph-only buttons: VoiceOver reads the label, so it has to name the action and its object.
        let newDeck = app.buttons["newDeck"]
        XCTAssertTrue(newDeck.waitForExistence(timeout: Self.wait))
        XCTAssertEqual(newDeck.label, "New deck")

        openEditor(forDeck: "Kotlin basics")
        XCTAssertTrue(app.buttons["deleteDeck"].waitForExistence(timeout: Self.wait), "the deck editor never opened")
        XCTAssertEqual(app.buttons["renameDeck"].label, "Rename deck")
        XCTAssertEqual(app.buttons["deleteDeck"].label, "Delete deck")
        XCTAssertEqual(app.buttons["addCard"].label, "Add card")
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
        XCTAssertTrue(alert.waitForExistence(timeout: Self.wait), "the new-deck prompt never opened")
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
        XCTAssertTrue(add.waitForExistence(timeout: Self.wait), "the deck editor never opened")
        tap(add, "the add-card button")

        let alert = app.alerts.firstMatch
        XCTAssertTrue(alert.waitForExistence(timeout: Self.wait), "the add-card prompt never opened")
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
        timeout: TimeInterval = StudyFlowTests.wait,
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

    /// The same wait for an accessibility value: a deck row's counts, read as one string.
    private func expect(
        _ element: XCUIElement,
        toHaveValue expected: String,
        timeout: TimeInterval = StudyFlowTests.wait,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        let matched = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "value == %@", expected),
            object: element
        )
        let outcome = XCTWaiter().wait(for: [matched], timeout: timeout)
        let seen = element.exists ? "\"\(element.value as? String ?? "")\"" : "no such element"
        XCTAssertEqual(outcome, .completed, "expected \"\(expected)\", saw \(seen)", file: file, line: line)
    }
}
