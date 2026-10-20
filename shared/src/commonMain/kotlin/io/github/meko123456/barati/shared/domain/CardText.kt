package io.github.meko123456.barati.shared.domain

/** Text from another app, made fit for the front of a card. */
object CardText {
    /**
     * A phrase picked out or shared in another app, trimmed, with the line breaks and runs of
     * spaces a page's layout leaves inside a selection made single spaces. The no-break spaces
     * pages put between words count too; `\s` alone doesn't match them.
     */
    fun fromSelection(text: String): String = text.replace(spaces, " ").trim()

    private val spaces = Regex("[\\s\\u00A0\\u2007\\u202F\\uFEFF]+")
}
