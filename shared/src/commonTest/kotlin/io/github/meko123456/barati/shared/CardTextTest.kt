package io.github.meko123456.barati.shared

import io.github.meko123456.barati.shared.domain.CardText
import kotlin.test.Test
import kotlin.test.assertEquals

class CardTextTest {

    @Test
    fun aSelectionLosesTheSpacesAroundIt() {
        assertEquals("la casa", CardText.fromSelection("  la casa \n"))
    }

    @Test
    fun aPhraseBrokenOverLinesBecomesOneLine() {
        assertEquals("to look forward to", CardText.fromSelection("to look\nforward  to"))
        assertEquals("to look forward to", CardText.fromSelection("to look\r\n\tforward to"))
    }

    @Test
    fun noBreakSpacesAndAByteOrderMarkCountAsSpaces() {
        assertEquals("la casa", CardText.fromSelection("la casa"))
        assertEquals("10 000 words", CardText.fromSelection("10 000 words"))
        assertEquals("word", CardText.fromSelection("﻿word"))
    }

    @Test
    fun onlySpacesGiveNothing() {
        assertEquals("", CardText.fromSelection(" \n "))
    }
}
