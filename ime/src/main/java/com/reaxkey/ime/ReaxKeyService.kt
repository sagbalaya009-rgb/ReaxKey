package com.reaxkey.ime

import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.reaxkey.model.*
import com.reaxkey.ui.R
import java.util.Locale

class ReaxKeyService : InputMethodService() {
    private val state = TypingState()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var root: LinearLayout
    private var popup: PopupWindow? = null
    private var repeating = false
    private var repeatOccurred = false
    private var sensitive = false
    private var numeric = false
    private var languageIndex = 0
    private var emoji = false
    private val packs by lazy {
        listOf("en", "fr", "es", "yo").map { tag -> assets.open("languages/$tag.pack").bufferedReader().use { LanguagePack.parse(it.readText()) } }
    }
    private val pack get() = packs[languageIndex % packs.size]
    private val repeater = object : Runnable {
        override fun run() { if(repeating) { repeatOccurred = true; delete(); handler.postDelayed(this, 65) } }
    }
    override fun onEvaluateFullscreenMode() = false
    override fun onCreateInputView(): View {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(3),dp(5),dp(3),dp(5)) }
        render()
        return root
    }
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute,restarting)
        cancelRepeat(); popup?.dismiss(); state.reset(); emoji=false
        val type=attribute?.inputType ?: 0
        val variation=type and InputType.TYPE_MASK_VARIATION
        numeric=(type and InputType.TYPE_MASK_CLASS) in listOf(InputType.TYPE_CLASS_NUMBER,InputType.TYPE_CLASS_PHONE,InputType.TYPE_CLASS_DATETIME)
        sensitive=(type and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT && variation in listOf(InputType.TYPE_TEXT_VARIATION_PASSWORD,InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) ||
            (type and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) ||
            ((attribute?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0)
        updateCaps()
        if(::root.isInitialized) render()
    }
    override fun onFinishInput() { cancelRepeat(); popup?.dismiss(); state.reset(); super.onFinishInput() }
    override fun onFinishInputView(finishingInput: Boolean) { cancelRepeat(); popup?.dismiss(); super.onFinishInputView(finishingInput) }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); popup?.dismiss(); super.onDestroy() }
    override fun onUpdateSelection(oldSelStart:Int,oldSelEnd:Int,newSelStart:Int,newSelEnd:Int,candidatesStart:Int,candidatesEnd:Int) {
        super.onUpdateSelection(oldSelStart,oldSelEnd,newSelStart,newSelEnd,candidatesStart,candidatesEnd)
        val before=state.shift; updateCaps(); if(::root.isInitialized && before != state.shift) render()
    }
    private fun updateCaps() {
        if(!sensitive && state.layer==Layer.LETTERS) state.autoCap((currentInputConnection?.getCursorCapsMode(currentInputEditorInfo?.inputType ?: 0) ?: 0) != 0)
    }
    private fun render() {
        cancelRepeat(); popup?.dismiss(); root.removeAllViews()
        val dark=resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        root.setBackgroundColor(getColor(if(dark) R.color.keyboard_background_dark else R.color.keyboard_background))
        val rows=when {
            numeric -> listOf(listOf("1","2","3"),listOf("4","5","6"),listOf("7","8","9"),listOf("-","0",pack.decimal))
            emoji -> listOf(listOf("😀","😂","🥹","😍","😎","🤔"),listOf("😭","😡","🥳","🙏","👍","❤️"))
            state.layer==Layer.NUMBERS -> listOf("1234567890","@#£€%&-+()","!?\"':;/.,").map { it.map(Char::toString) }
            state.layer==Layer.SYMBOLS -> listOf("[]{}<>^*_=","\\|~`$¥•…÷×","©®™✓°§±").map { it.map(Char::toString) }
            else -> pack.rows
        }
        rows.forEach { keys ->
            val row=LinearLayout(this)
            keys.forEach { key ->
                val displayed=if(state.shift != Shift.OFF && !numeric) key.uppercase(Locale.forLanguageTag(pack.tag)) else key
                val button=key(row,displayed,displayed,dark) { commit(state.text(key,Locale.forLanguageTag(pack.tag))); updateCaps(); render() }
                pack.alternatives[key]?.let { alternatives -> button.setOnLongClickListener { showAlternatives(button,alternatives,dark); true } }
            }
            root.addView(row)
        }
        val actions=LinearLayout(this)
        if(!numeric) {
            key(actions,when(state.shift) { Shift.OFF -> "⇧"; Shift.ONCE -> "⬆"; Shift.LOCKED -> "⇪" },getString(R.string.key_shift),dark) { state.shift(SystemClock.uptimeMillis()); render() }
            key(actions,if(state.layer==Layer.LETTERS) "123" else "ABC",getString(R.string.key_symbols),dark) { emoji=false; state.layer=if(state.layer==Layer.LETTERS) Layer.NUMBERS else Layer.LETTERS; render() }
            if(state.layer!=Layer.LETTERS) key(actions,"#+=",getString(R.string.more_symbols),dark) { state.layer=if(state.layer==Layer.SYMBOLS) Layer.NUMBERS else Layer.SYMBOLS; render() }
            key(actions,"☺",getString(R.string.emoji),dark) { emoji=!emoji; render() }
        }
        val delete=key(actions,"⌫",getString(R.string.key_delete),dark) { if(!repeatOccurred) delete(); repeatOccurred=false }
        delete.setOnTouchListener { _,event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { repeatOccurred=false; repeating=true; handler.postDelayed(repeater,400) }
                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> cancelRepeat()
            }
            false
        }
        root.addView(actions)
        val bottom=LinearLayout(this)
        key(bottom,"🌐",getString(R.string.switch_keyboard),dark) { getSystemService(InputMethodManager::class.java).showInputMethodPicker() }
        if(!numeric) {
            key(bottom,pack.name,getString(R.string.change_language),dark) { languageIndex=(languageIndex+1)%packs.size; state.reset(); render() }
            val variation=(currentInputEditorInfo?.inputType ?: 0) and InputType.TYPE_MASK_VARIATION
            val punctuation=if(variation==InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS || variation==InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) "@" else if(variation==InputType.TYPE_TEXT_VARIATION_URI) "/" else ","
            key(bottom,punctuation,punctuation,dark) { commit(punctuation) }
            key(bottom,getString(R.string.key_space),getString(R.string.key_space),dark,2f) { commit(" "); updateCaps(); render() }
            key(bottom,".",".",dark) { commit(".") }
        }
        key(bottom,actionLabel(),actionLabel(),dark) { enter() }
        root.addView(bottom)
    }
    private fun key(row:LinearLayout,label:String,description:String,dark:Boolean,weight:Float=1f,action:()->Unit):Button {
        val button=Button(this).apply {
            text=label; contentDescription=description; textSize=if(label.length>3) 12f else 19f
            isAllCaps=false; minWidth=0; minimumWidth=0; setPadding(0,0,0,0)
            setTextColor(getColor(if(dark) R.color.keyboard_text_dark else R.color.keyboard_text))
            background=android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x33426655),GradientDrawable().apply { setColor(getColor(if(dark) R.color.keyboard_key_dark else R.color.keyboard_key)); cornerRadius=dp(7).toFloat() },null)
            setOnClickListener { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); action() }
        }
        row.addView(button,LinearLayout.LayoutParams(0,dp(50),weight).apply { setMargins(dp(2),dp(3),dp(2),dp(3)) })
        return button
    }
    private fun showAlternatives(anchor:View,alternatives:List<String>,dark:Boolean) {
        val row=LinearLayout(this)
        popup=PopupWindow(row,dp(48)*alternatives.size,dp(60),true).apply { setBackgroundDrawable(GradientDrawable().apply { setColor(getColor(if(dark) R.color.keyboard_background_dark else R.color.keyboard_background)) }); elevation=dp(8).toFloat() }
        alternatives.forEach { value -> key(row,value,value,dark) { commit(state.text(value,Locale.forLanguageTag(pack.tag))); popup?.dismiss(); render() } }
        popup?.showAsDropDown(anchor,0,-dp(112))
    }
    private fun commit(text:String) { currentInputConnection?.commitText(text,1) }
    private fun delete() {
        val connection=currentInputConnection ?: return
        // Read only for an explicit deletion; never retain editor text.
        if(!connection.getSelectedText(0).isNullOrEmpty()) connection.commitText("",1)
        else connection.deleteSurroundingTextInCodePoints(1,0)
    }
    private fun enter() {
        val editor=currentInputEditorInfo ?: return
        val action=editor.imeOptions and EditorInfo.IME_MASK_ACTION
        if(editor.actionLabel!=null && editor.actionId!=0) currentInputConnection?.performEditorAction(editor.actionId)
        else if(editor.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION==0 && action !in listOf(EditorInfo.IME_ACTION_NONE,EditorInfo.IME_ACTION_UNSPECIFIED)) currentInputConnection?.performEditorAction(action)
        else commit("\n")
    }
    private fun actionLabel():String {
        val editor=currentInputEditorInfo ?: return getString(R.string.key_enter)
        if(editor.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION!=0) return getString(R.string.key_enter)
        return editor.actionLabel?.toString() ?: getString(when(editor.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_GO -> R.string.action_go
            EditorInfo.IME_ACTION_SEARCH -> R.string.action_search
            EditorInfo.IME_ACTION_SEND -> R.string.action_send
            EditorInfo.IME_ACTION_NEXT -> R.string.action_next
            EditorInfo.IME_ACTION_DONE -> R.string.action_done
            else -> R.string.key_enter
        })
    }
    private fun cancelRepeat() { repeating=false; handler.removeCallbacks(repeater) }
    private fun dp(value:Int)=(value*resources.displayMetrics.density).toInt()
}
