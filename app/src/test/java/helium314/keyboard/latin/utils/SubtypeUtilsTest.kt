package helium314.keyboard.latin.utils

import android.view.inputmethod.InputMethodSubtype
import helium314.keyboard.latin.RichInputMethodManager
import helium314.keyboard.latin.common.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class SubtypeUtilsTest {

    @Test
    fun testLocale_fromLanguageTag() {
        val subtype = InputMethodSubtype.InputMethodSubtypeBuilder()
            .setLanguageTag("fr-CA")
            .build()
        assertEquals(Locale("fr", "CA"), subtype.locale())
    }

    @Test
    fun testLocale_fromSubtypeLocale() {
        val subtype = InputMethodSubtype.InputMethodSubtypeBuilder()
            .setSubtypeLocale("de_DE")
            .build()
        assertEquals(Locale("de", "DE"), subtype.locale())
    }

    @Test
    fun testMainLayoutName() {
        val subtype = InputMethodSubtype.InputMethodSubtypeBuilder()
            .setSubtypeExtraValue("${Constants.Subtype.ExtraValue.KEYBOARD_LAYOUT_SET}=MAIN:qwerty")
            .build()
        assertEquals("qwerty", subtype.mainLayoutName())
        assertEquals("qwerty", subtype.mainLayoutNameOrQwerty())
    }

    @Test
    fun testMainLayoutName_missing() {
        val subtype = InputMethodSubtype.InputMethodSubtypeBuilder()
            .setSubtypeExtraValue("SomeOtherValue=test")
            .build()
        assertNull(subtype.mainLayoutName())
        assertEquals("qwerty", subtype.mainLayoutNameOrQwerty())
    }

    @Test
    fun testGetSecondaryLocales() {
        val extraValue = "${Constants.Subtype.ExtraValue.SECONDARY_LOCALES}=en_US:fr:de_DE"
        val locales = getSecondaryLocales(extraValue)
        assertEquals(3, locales.size)
        assertEquals(Locale("en", "US"), locales[0])
        assertEquals(Locale("fr"), locales[1])
        assertEquals(Locale("de", "DE"), locales[2])
    }

    @Test
    fun testGetSecondaryLocales_empty() {
        val extraValue = "SomeOtherValue=test"
        val locales = getSecondaryLocales(extraValue)
        assertEquals(0, locales.size)
    }

    private val english = subtype("en-US", "qwerty")
    private val englishDvorak = subtype("en-US", "dvorak")
    private val spanish = subtype("es", "spanish")
    private val enabledSubtypes = listOf(english, englishDvorak, spanish)

    private fun subtype(languageTag: String, layout: String) = InputMethodSubtype.InputMethodSubtypeBuilder()
        .setLanguageTag(languageTag)
        .setSubtypeExtraValue("${Constants.Subtype.ExtraValue.KEYBOARD_LAYOUT_SET}=MAIN:$layout")
        .build()

    private fun detectingSubtypeState() = SubtypeState {}.apply { startLanguageDetection(true) }

    private fun SubtypeState.commitWord(detectedLocale: Locale?, currentSubtype: InputMethodSubtype = english) =
        getSubtypeForDetectedLocale(detectedLocale, currentSubtype, enabledSubtypes)

    private fun SubtypeState.subtypeToRestore() =
        getSubtypeForLocales(mock(RichInputMethodManager::class.java), mutableListOf(), null)

    @Test
    fun testDetectedLanguage_noSwitchBeforeMinimumWords() {
        assertNull(detectingSubtypeState().commitWord(Locale("es")))
    }

    @Test
    fun testDetectedLanguage_switchOncePerField() {
        val state = detectingSubtypeState()
        state.commitWord(null)
        assertEquals(spanish, state.commitWord(Locale("es")))
        assertNull(state.commitWord(Locale("en"), spanish))
    }

    @Test
    fun testDetectedLanguage_currentSubtypeWinsSameLanguage() {
        val state = detectingSubtypeState()
        state.commitWord(null, englishDvorak)
        assertNull(state.commitWord(Locale("en", "US"), englishDvorak))
    }

    @Test
    fun testDetectedLanguage_noEnabledSubtype() {
        val state = detectingSubtypeState()
        state.commitWord(null)
        assertNull(state.commitWord(Locale("de")))
    }

    @Test
    fun testDetectedLanguage_stopAfterMaximumWords() {
        val state = detectingSubtypeState()
        repeat(SubtypeState.MAX_WORDS_TO_DETECT_LANGUAGE) { state.commitWord(null) }
        assertNull(state.commitWord(Locale("es")))
    }

    @Test
    fun testDetectedLanguage_disabled() {
        val state = SubtypeState {}.apply { startLanguageDetection(false) }
        state.commitWord(null)
        assertNull(state.commitWord(Locale("es")))
    }

    @Test
    fun testDetectedLanguage_userSwitchStopsDetection() {
        val state = detectingSubtypeState()
        state.commitWord(null)
        state.onSubtypeChanged(english, englishDvorak)
        assertNull(state.commitWord(Locale("es"), englishDvorak))
    }

    @Test
    fun testDetectedLanguage_restoredInNextField() {
        val state = detectingSubtypeState()
        state.commitWord(null)
        state.onSubtypeChanged(english, state.commitWord(Locale("es")))
        assertEquals(english, state.subtypeToRestore())
    }

    @Test
    fun testDetectedLanguage_notRestoredAfterUserSwitch() {
        val state = detectingSubtypeState()
        state.commitWord(null)
        state.onSubtypeChanged(english, state.commitWord(Locale("es")))
        state.onSubtypeChanged(spanish, englishDvorak)
        assertNull(state.subtypeToRestore())
    }
}
