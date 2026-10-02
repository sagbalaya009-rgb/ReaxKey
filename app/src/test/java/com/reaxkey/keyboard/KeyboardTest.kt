package com.reaxkey.keyboard

import android.text.*
import android.view.*
import android.view.inputmethod.*
import android.widget.Button
import androidx.test.core.app.ApplicationProvider
import com.reaxkey.ime.ReaxKeyService
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class KeyboardHarness:ReaxKeyService() {
    val editor=EditorInfo().apply {inputType=InputType.TYPE_CLASS_TEXT}
    val buffer=SpannableStringBuilder()
    var capsQueries=0
    var action=-1
    private val connection=object:BaseInputConnection(View(ApplicationProvider.getApplicationContext()),true) {
        override fun getEditable():Editable=buffer
        override fun getCursorCapsMode(reqModes:Int):Int {capsQueries++;return 0}
        override fun performEditorAction(actionCode:Int):Boolean {action=actionCode;return true}
    }
    override fun getCurrentInputConnection():InputConnection=connection
    override fun getCurrentInputEditorInfo():EditorInfo=editor
}

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class KeyboardTest {
    @Test fun ordinaryTypingRetainsKeyViewsForRapidTaps() {
        val controller=Robolectric.buildService(KeyboardHarness::class.java).create();val service=controller.get()
        try {
            service.onStartInput(service.editor,false);val view=service.onCreateInputView()
            val a=buttons(view).first {it.text=="a"}
            repeat(3) {a.performClick()}
            assertEquals("aaa",service.buffer.toString())
            assertSame(a,buttons(view).first {it.text=="a"})
        } finally {controller.destroy()}
    }
    private fun buttons(view:View):List<Button> = if(view is Button) listOf(view) else if(view is ViewGroup) (0 until view.childCount).flatMap {buttons(view.getChildAt(it))} else emptyList()
    @Test fun mediaSearchCannotTypeIntoHost() {
        val controller=Robolectric.buildService(KeyboardHarness::class.java).create();val service=controller.get()
        try {
            service.onStartInput(service.editor,false);val view=service.onCreateInputView()
            buttons(view).first {it.text=="q"}.performClick();assertEquals("q",service.buffer.toString())
            buttons(view).first {it.text=="Media"}.performClick();buttons(view).first {it.text=="Search"}.performClick()
            buttons(view).first {it.text=="a"}.performClick();buttons(view).first {it.text=="Space"}.performClick()
            assertEquals("q",service.buffer.toString())
        } finally {controller.destroy()}
    }
    @Test fun deletionDoesNotSplitSurrogatePairsAndReplacesSelections() {
        val controller=Robolectric.buildService(KeyboardHarness::class.java).create();val service=controller.get()
        try {
            service.onStartInput(service.editor,false);val view=service.onCreateInputView()
            service.buffer.append("A😀");Selection.setSelection(service.buffer,service.buffer.length)
            buttons(view).first {it.contentDescription=="Delete"}.performClick();assertEquals("A",service.buffer.toString())
            Selection.setSelection(service.buffer,0,1);buttons(view).first {it.contentDescription=="Delete"}.performClick();assertEquals("",service.buffer.toString())
        } finally {controller.destroy()}
    }
    @Test fun passwordSkipsCapsQueriesAndEnterUsesEditorAction() {
        val controller=Robolectric.buildService(KeyboardHarness::class.java).create();val service=controller.get()
        try {
            service.editor.inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            service.editor.imeOptions=EditorInfo.IME_ACTION_DONE
            service.onStartInput(service.editor,false);val view=service.onCreateInputView()
            assertTrue(buttons(view).none {it.text=="Media"})
            buttons(view).first {it.text=="a"}.performClick();buttons(view).first {it.text=="Done"}.performClick()
            assertEquals(0,service.capsQueries);assertEquals(EditorInfo.IME_ACTION_DONE,service.action)
        } finally {controller.destroy()}
    }
    @Test fun familiarRowsAndSymbolPagesReturnToLetters() {
        val controller=Robolectric.buildService(KeyboardHarness::class.java).create();val service=controller.get()
        try {
            service.onStartInput(service.editor,false);val view=service.onCreateInputView()
            val letters=buttons(view)
            assertSame(letters.first {it.text=="z"}.parent,letters.first {it.contentDescription=="Shift"}.parent)
            assertSame(letters.first {it.text=="m"}.parent,letters.first {it.contentDescription=="Delete"}.parent)
            letters.first {it.text=="?123"}.performClick()
            assertTrue(buttons(view).any {it.text=="1"})
            buttons(view).first {it.text=="=\\<"}.performClick()
            assertTrue(buttons(view).any {it.text=="©"})
            buttons(view).first {it.text=="ABC"}.performClick()
            assertTrue(buttons(view).any {it.text=="q"})
        } finally {controller.destroy()}
    }
    @Test fun mediaPanelReturnsToTypingWithoutChangingHostText() {
        val controller=Robolectric.buildService(KeyboardHarness::class.java).create();val service=controller.get()
        try {
            service.onStartInput(service.editor,false);val view=service.onCreateInputView()
            buttons(view).first {it.text=="Media"}.performClick()
            buttons(view).first {it.text=="ABC"}.performClick()
            buttons(view).first {it.text=="h"}.performClick()
            assertEquals("h",service.buffer.toString())
        } finally {controller.destroy()}
    }
}
