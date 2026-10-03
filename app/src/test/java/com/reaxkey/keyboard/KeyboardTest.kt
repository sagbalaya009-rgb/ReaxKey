package com.reaxkey.keyboard

import android.text.*
import android.view.*
import android.view.inputmethod.*
import android.widget.Button
import androidx.test.core.app.ApplicationProvider
import com.reaxkey.ime.ReaxKeyService
import com.reaxkey.ime.TypingIntelligence
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows
import java.time.Duration

class KeyboardHarness:ReaxKeyService() {
    val editor=EditorInfo().apply {inputType=InputType.TYPE_CLASS_TEXT}
    val buffer=SpannableStringBuilder()
    var connected=true
    var nativeCursorSupported=true
    var capsQueries=0
    var action=-1
    private val connection=object:BaseInputConnection(View(ApplicationProvider.getApplicationContext()),true) {
        override fun getEditable():Editable=buffer
        override fun getCursorCapsMode(reqModes:Int):Int {capsQueries++;return 0}
        override fun performEditorAction(actionCode:Int):Boolean {action=actionCode;return true}
        override fun getExtractedText(request:ExtractedTextRequest?,flags:Int):ExtractedText = ExtractedText().apply {
            text=buffer.toString();startOffset=0
            selectionStart=Selection.getSelectionStart(buffer).coerceAtLeast(0)
            selectionEnd=Selection.getSelectionEnd(buffer).coerceAtLeast(0)
        }
        override fun sendKeyEvent(event:KeyEvent):Boolean {
            if(!nativeCursorSupported) return false
            if(event.action!=KeyEvent.ACTION_DOWN) return true
            val position=Selection.getSelectionStart(buffer).coerceAtLeast(0)
            val target=when(event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> (position-1).coerceAtLeast(0)
                KeyEvent.KEYCODE_DPAD_RIGHT -> (position+1).coerceAtMost(buffer.length)
                KeyEvent.KEYCODE_DPAD_UP -> buffer.lastIndexOf('\n', (position-1).coerceAtLeast(0)).coerceAtLeast(0)
                KeyEvent.KEYCODE_DPAD_DOWN -> buffer.indexOf('\n',position).let {if(it<0) position else (it+1).coerceAtMost(buffer.length)}
                else -> return super.sendKeyEvent(event)
            }
            Selection.setSelection(buffer,target);return true
        }
    }
    override fun getCurrentInputConnection():InputConnection?=if(connected) connection else null
    override fun getCurrentInputEditorInfo():EditorInfo=editor
}
class MissingConnectionHarness:ReaxKeyService() {
    val editor=EditorInfo().apply {inputType=InputType.TYPE_CLASS_TEXT}
    override fun getCurrentInputConnection():InputConnection?=null
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
    @Test fun correctionUndoAndPunctuationStayLocalToCurrentWord() {
        val controller=Robolectric.buildService(KeyboardHarness::class.java).create();val service=controller.get()
        try {
            service.onStartInput(service.editor,false);val view=service.onCreateInputView()
            listOf("t","e","h","Space").forEach {label->buttons(view).first {it.text==label}.performClick()}
            assertEquals("the ",service.buffer.toString())
            buttons(view).first {it.contentDescription=="Delete"}.performClick()
            assertEquals("teh",service.buffer.toString())
            buttons(view).first {it.text=="."}.performClick()
            assertEquals("the.",service.buffer.toString())
        } finally {controller.destroy()}
    }
    @Test fun suggestionCompletesPrefixWithoutDuplicatingIt() {
        val controller=Robolectric.buildService(KeyboardHarness::class.java).create();val service=controller.get()
        try {
            service.onStartInput(service.editor,false);val view=service.onCreateInputView()
            buttons(view).first {it.text=="t"}.performClick();buttons(view).first {it.text=="h"}.performClick()
            buttons(view).first {it.text=="the"}.performClick()
            assertEquals("the ",service.buffer.toString())
        } finally {controller.destroy()}
    }
    @Test fun sensitiveFieldHidesSuggestionsAndDoesNotCorrect() {
        val controller=Robolectric.buildService(KeyboardHarness::class.java).create();val service=controller.get()
        try {
            service.editor.inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            service.onStartInput(service.editor,false);val view=service.onCreateInputView()
            assertTrue(buttons(view).none {it.contentDescription?.startsWith("Suggestion")==true})
            listOf("t","e","h","Space").forEach {label->buttons(view).first {it.text==label}.performClick()}
            assertEquals("teh ",service.buffer.toString())
        } finally {controller.destroy()}
    }
    @Test fun editorNoSuggestionsFlagIsRespected() {
        val controller=Robolectric.buildService(KeyboardHarness::class.java).create();val service=controller.get()
        try {
            service.editor.inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            service.onStartInput(service.editor,false);val view=service.onCreateInputView()
            assertTrue(buttons(view).none {it.contentDescription?.startsWith("Suggestion")==true})
            listOf("t","e","h","Space").forEach {label->buttons(view).first {it.text==label}.performClick()}
            assertEquals("teh ",service.buffer.toString())
        } finally {controller.destroy()}
    }
    @Test fun spaceTapAndHoldAreMutuallyExclusive() {
        val controller=Robolectric.buildService(KeyboardHarness::class.java).create();val service=controller.get()
        try {
            service.onStartInput(service.editor,false);val view=service.onCreateInputView()
            view.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.AT_MOST))
            view.layout(0,0,1080,view.measuredHeight)
            var space=buttons(view).first {it.text=="Space"}
            touch(space,MotionEvent.ACTION_DOWN,20f,20f);touch(space,MotionEvent.ACTION_MOVE,22f,21f);touch(space,MotionEvent.ACTION_UP,22f,21f)
            assertEquals(" ",service.buffer.toString())
            space=buttons(view).first {it.text=="Space"}
            val gestureField=ReaxKeyService::class.java.getDeclaredField("spaceGesture").apply {isAccessible=true}
            assertNotNull(gestureField.get(service))
            val gesture=gestureField.get(service)
            touch(space,MotionEvent.ACTION_DOWN,20f,20f)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(350))
            assertSame(gesture,gestureField.get(service))
            assertEquals("Cursor mode",space.contentDescription)
            assertEquals(" ",service.buffer.toString())
            touch(space,MotionEvent.ACTION_UP,20f,20f)
            assertEquals(" ",service.buffer.toString())
        } finally {controller.destroy()}
    }
    @Test fun heldSpaceMovesCursorInTwoDimensionsAndCancelInsertsNothing() {
        val controller=Robolectric.buildService(KeyboardHarness::class.java).create();val service=controller.get()
        try {
            service.onStartInput(service.editor,false);val view=service.onCreateInputView()
            view.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.AT_MOST))
            view.layout(0,0,1080,view.measuredHeight)
            service.buffer.append("abc\ndef");Selection.setSelection(service.buffer,service.buffer.length)
            val space=buttons(view).first {it.text=="Space"}
            touch(space,MotionEvent.ACTION_DOWN,80f,30f)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(350))
            touch(space,MotionEvent.ACTION_MOVE,45f,5f)
            assertTrue(Selection.getSelectionStart(service.buffer)<service.buffer.length)
            touch(space,MotionEvent.ACTION_CANCEL,45f,5f)
            assertEquals("abc\ndef",service.buffer.toString())
            assertEquals("Space",space.text)
        } finally {controller.destroy()}
    }
    @Test fun cursorModeCancelsIfEditorConnectionDisappears() {
        val controller=Robolectric.buildService(KeyboardHarness::class.java).create();val service=controller.get()
        try {
            service.onStartInput(service.editor,false);val view=service.onCreateInputView()
            view.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.AT_MOST))
            view.layout(0,0,1080,view.measuredHeight)
            val space=buttons(view).first {it.text=="Space"}
            touch(space,MotionEvent.ACTION_DOWN,80f,30f)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(350))
            assertEquals("Cursor mode",space.contentDescription)
            service.connected=false
            touch(space,MotionEvent.ACTION_MOVE,40f,15f)
            assertEquals("Space",space.text)
            touch(space,MotionEvent.ACTION_UP,40f,15f)
        } finally {controller.destroy()}
    }
    @Test fun logicalLineFallbackAndUnicodeHorizontalFallback() {
        val controller=Robolectric.buildService(KeyboardHarness::class.java).create();val service=controller.get()
        try {
            service.nativeCursorSupported=false
            service.onStartInput(service.editor,false);val view=service.onCreateInputView()
            view.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.AT_MOST))
            view.layout(0,0,1080,view.measuredHeight)
            service.buffer.append("abc\ndef");Selection.setSelection(service.buffer,service.buffer.length)
            val space=buttons(view).first {it.text=="Space"}
            touch(space,MotionEvent.ACTION_DOWN,80f,30f)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(350))
            touch(space,MotionEvent.ACTION_MOVE,80f,-30f)
            assertEquals(3,Selection.getSelectionStart(service.buffer))
            touch(space,MotionEvent.ACTION_MOVE,80f,30f)
            assertEquals(7,Selection.getSelectionStart(service.buffer))
            touch(space,MotionEvent.ACTION_UP,80f,30f)
            service.buffer.clear();service.buffer.append("A😀B");Selection.setSelection(service.buffer,3)
            touch(space,MotionEvent.ACTION_DOWN,80f,30f)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(350))
            touch(space,MotionEvent.ACTION_MOVE,65f,30f)
            assertEquals(1,Selection.getSelectionStart(service.buffer))
            touch(space,MotionEvent.ACTION_UP,65f,30f)
        } finally {controller.destroy()}
    }
    @Test fun suggestionPreservesFollowingPunctuation() {
        val controller=Robolectric.buildService(KeyboardHarness::class.java).create();val service=controller.get()
        try {
            service.onStartInput(service.editor,false);val view=service.onCreateInputView()
            service.buffer.append("th,");Selection.setSelection(service.buffer,2)
            service.onUpdateSelection(0,0,2,2,0,0)
            buttons(view).first {it.text=="the"}.performClick()
            assertEquals("the,",service.buffer.toString())
        } finally {controller.destroy()}
    }
    @Test fun offlineModelCorrectsOnlyKnownTyposAndLearnsAfterRepetition() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        TypingIntelligence.clear(context)
        try {
            val model=TypingIntelligence(context)
            assertEquals("the",model.correction("teh"))
            assertEquals("The",model.correction("Teh"))
            assertNull(model.correction("orisim"))
            assertNull(model.correction("there"))
            repeat(2) {model.observeDeliberateWord("OriSim")}
            TypingIntelligence.clear(context)
            model.observeDeliberateWord("OriSim")
            assertTrue(model.suggestions("ori","").none {it.equals("orisim",true)})
            repeat(2) {model.observeDeliberateWord("OriSim")}
            assertTrue(model.suggestions("ori","").any {it.equals("orisim",true)})
        } finally {TypingIntelligence.clear(context)}
    }
    @Test fun spaceHoldWithoutConnectionCancelsSafely() {
        val controller=Robolectric.buildService(MissingConnectionHarness::class.java).create();val service=controller.get()
        try {
            service.onStartInput(service.editor,false);val view=service.onCreateInputView()
            view.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.AT_MOST))
            view.layout(0,0,1080,view.measuredHeight)
            val space=buttons(view).first {it.text=="Space"}
            touch(space,MotionEvent.ACTION_DOWN,20f,20f)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(350))
            assertEquals("Space",space.text)
            touch(space,MotionEvent.ACTION_UP,20f,20f)
        } finally {controller.destroy()}
    }
    private fun touch(view:View,action:Int,x:Float,y:Float) {
        val event=MotionEvent.obtain(0,android.os.SystemClock.uptimeMillis(),action,x,y,0)
        view.dispatchTouchEvent(event);event.recycle()
    }
}
