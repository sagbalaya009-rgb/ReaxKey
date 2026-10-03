package com.reaxkey.model

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class TypingStateTest {
    @Test fun singleShiftIsConsumed() {
        val state = TypingState(); state.shift(1000)
        assertEquals("A",state.text("a",Locale.ENGLISH)); assertEquals("b",state.text("b",Locale.ENGLISH))
    }
    @Test fun doubleTapLocksAndThirdUnlocks() {
        val state=TypingState(); state.shift(1000); state.shift(1200)
        assertEquals("A",state.text("a",Locale.ENGLISH)); assertEquals("B",state.text("b",Locale.ENGLISH))
        state.shift(1500); assertEquals(Shift.OFF,state.shift)
    }
    @Test fun autoCapDoesNotCancelCapsLock() {
        val state=TypingState(); state.shift(1000); state.shift(1100); state.autoCap(false)
        assertEquals(Shift.LOCKED,state.shift)
    }
    @Test fun casingUsesLanguageLocale() {
        val state=TypingState(); state.shift(1000)
        assertEquals("İ",state.text("i",Locale.forLanguageTag("tr")))
    }
    @Test fun resetClearsEditorState() {
        val state=TypingState(); state.shift(1); state.layer=Layer.SYMBOLS; state.reset()
        assertEquals(Shift.OFF,state.shift); assertEquals(Layer.LETTERS,state.layer)
    }
    @Test fun languagePackSupportsMultiCodePointKeys() {
        val pack=LanguagePack.parse("en\tEnglish\t.\na b\nc d\ne f\na=á à")
        assertEquals(listOf("á","à"),pack.alternatives["a"])
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsInvalidPack() { LanguagePack.parse("broken") }
}
