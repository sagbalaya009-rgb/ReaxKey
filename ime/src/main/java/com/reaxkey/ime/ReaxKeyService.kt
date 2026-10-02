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
import com.reaxkey.data.Preferences
import kotlinx.coroutines.*

open class ReaxKeyService : InputMethodService() {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val preferences by lazy {Preferences(this)}
    private val mediaPanel by lazy {MediaPanel(this,scope){sensitive}}
    private var mediaMode=false
    private var searching=false
    private var theme="SYSTEM"
    private var haptics=true
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
    override fun onCreate() {
        super.onCreate()
        scope.launch {preferences.flow.collect {value->
            theme=value.theme;haptics=value.haptics
            languageIndex=packs.indexOfFirst {it.tag==value.language}.coerceAtLeast(0)
            if(::root.isInitialized) render()
        }}
    }
    override fun onEvaluateFullscreenMode() = false
    override fun onCreateInputView(): View {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(2),dp(2),dp(2),dp(4)) }
        render()
        return root
    }
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute,restarting)
        cancelRepeat(); popup?.dismiss(); state.reset(); emoji=false
        mediaPanel.reset();mediaMode=false;searching=false
        val type=attribute?.inputType ?: 0
        val variation=type and InputType.TYPE_MASK_VARIATION
        numeric=(type and InputType.TYPE_MASK_CLASS) in listOf(InputType.TYPE_CLASS_NUMBER,InputType.TYPE_CLASS_PHONE,InputType.TYPE_CLASS_DATETIME)
        sensitive=(type and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT && variation in listOf(InputType.TYPE_TEXT_VARIATION_PASSWORD,InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) ||
            (type and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) ||
            ((attribute?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0)
        updateCaps()
        if(::root.isInitialized) render()
    }
    override fun onFinishInput() { cancelRepeat(); popup?.dismiss(); mediaPanel.reset(); state.reset(); super.onFinishInput() }
    override fun onFinishInputView(finishingInput: Boolean) { cancelRepeat(); popup?.dismiss(); mediaPanel.reset(); super.onFinishInputView(finishingInput) }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); popup?.dismiss(); scope.cancel(); super.onDestroy() }
    override fun onUpdateSelection(oldSelStart:Int,oldSelEnd:Int,newSelStart:Int,newSelEnd:Int,candidatesStart:Int,candidatesEnd:Int) {
        super.onUpdateSelection(oldSelStart,oldSelEnd,newSelStart,newSelEnd,candidatesStart,candidatesEnd)
        val before=state.shift; updateCaps(); if(::root.isInitialized && before != state.shift) render()
    }
    private fun updateCaps() {
        if(!sensitive && !mediaMode && state.layer==Layer.LETTERS) state.autoCap((currentInputConnection?.getCursorCapsMode(currentInputEditorInfo?.inputType ?: 0) ?: 0) != 0)
    }
    private fun render() {
        cancelRepeat(); popup?.dismiss(); root.removeAllViews()
        val dark=theme=="DARK" || (theme=="SYSTEM" && resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        root.setBackgroundColor(getColor(if(dark) R.color.keyboard_background_dark else R.color.keyboard_background))
        if(mediaMode) {
            root.addView(mediaPanel.view(searching,dark,{mediaMode=false;mediaPanel.hide();render()},{searching=!searching;render()}))
            if(!searching) return
        }
        val toolbar=KeyboardRow()
        key(toolbar,"🌐",getString(R.string.switch_keyboard),dark,1f,true) {getSystemService(InputMethodManager::class.java).showInputMethodPicker()}
        key(toolbar,pack.tag.uppercase(Locale.ROOT),getString(R.string.change_language),dark,1f,true) {
            languageIndex=(languageIndex+1)%packs.size;scope.launch {preferences.language(pack.tag)};state.reset();render()
        }
        if(!sensitive) key(toolbar,getString(R.string.media),getString(R.string.media),dark,1.4f,true) {mediaMode=true;searching=false;state.reset();render()}
        root.addView(toolbar)
        val rows=when {
            numeric -> listOf(listOf("1","2","3"),listOf("4","5","6"),listOf("7","8","9"))
            emoji -> listOf(listOf("😀","😂","🥹","😍","😎","🤔"),listOf("😭","😡","🥳","🙏","👍","❤️"))
            state.layer==Layer.NUMBERS -> listOf("1234567890","@#£€%&-+()","!?\"':;/.,").map {it.map(Char::toString)}
            state.layer==Layer.SYMBOLS -> listOf("[]{}<>^*_=","\\|~`$¥•…÷×","©®™✓°§±").map {it.map(Char::toString)}
            else -> pack.rows
        }
        rows.forEachIndexed { index,keys ->
            val row=KeyboardRow()
            if(index==1 && !numeric) row.setPadding(dp(16),0,dp(16),0)
            if(index==2 && !numeric) {
                if(state.layer==Layer.LETTERS && !emoji) key(row,when(state.shift) {Shift.OFF->"⇧";Shift.ONCE->"⬆";Shift.LOCKED->"⇪"},getString(R.string.key_shift),dark,1.4f,true) {state.shift(SystemClock.uptimeMillis());render()}
                else key(row,if(state.layer==Layer.NUMBERS) "=\\<" else "?123",getString(R.string.more_symbols),dark,1.4f,true) {state.layer=if(state.layer==Layer.NUMBERS) Layer.SYMBOLS else Layer.NUMBERS;render()}
            }
            keys.forEach { value ->
                val displayed=if(state.shift!=Shift.OFF && state.layer==Layer.LETTERS && !numeric) value.uppercase(Locale.forLanguageTag(pack.tag)) else value
                val button=key(row,displayed,displayed,dark) {
                    val previousShift=state.shift
                    commit(state.text(value,Locale.forLanguageTag(pack.tag)));updateCaps()
                    if(previousShift!=state.shift) render()
                }
                pack.alternatives[value]?.let { alternatives -> button.setOnLongClickListener {showAlternatives(button,alternatives,dark);true} }
            }
            if(index==2) deleteKey(row,dark)
            root.addView(row)
        }
        val bottom=KeyboardRow()
        if(numeric) {
            key(bottom,"-","-",dark){commit("-")}
            key(bottom,"0","0",dark){commit("0")}
            key(bottom,pack.decimal,pack.decimal,dark){commit(pack.decimal)}
        } else {
            key(bottom,if(state.layer==Layer.LETTERS && !emoji) "?123" else "ABC",getString(R.string.key_symbols),dark,1.3f,true) {
                emoji=false;state.layer=if(state.layer==Layer.LETTERS) Layer.NUMBERS else Layer.LETTERS;render()
            }
            key(bottom,"☺",getString(R.string.emoji),dark,0.9f,true){emoji=!emoji;state.layer=Layer.LETTERS;render()}
            val variation=(currentInputEditorInfo?.inputType ?: 0) and InputType.TYPE_MASK_VARIATION
            val punctuation=if(variation==InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS || variation==InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) "@" else if(variation==InputType.TYPE_TEXT_VARIATION_URI) "/" else ","
            key(bottom,punctuation,punctuation,dark,0.9f){commit(punctuation)}
            key(bottom,getString(R.string.key_space),getString(R.string.key_space),dark,4f){commit(" ");updateCaps()}
            key(bottom,".",".",dark,0.9f){commit(".")}
        }
        key(bottom,actionLabel(),actionLabel(),dark,1.4f,true){enter()}
        root.addView(bottom)
    }
    private fun deleteKey(row:LinearLayout,dark:Boolean) {
        val button=key(row,"⌫",getString(R.string.key_delete),dark,1.4f,true) {if(!repeatOccurred) delete();repeatOccurred=false}
        button.setOnTouchListener { _,event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {repeatOccurred=false;repeating=true;handler.postDelayed(repeater,400)}
                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> cancelRepeat()
            }
            false
        }
    }
    private fun key(row:LinearLayout,label:String,description:String,dark:Boolean,weight:Float=1f,function:Boolean=false,action:()->Unit):Button {
        val button=Button(this).apply {
            text=label; contentDescription=description; textSize=if(label.length>3) 12f else 18f
            isAllCaps=false; minWidth=0; minimumWidth=0; minHeight=0; minimumHeight=0; setPadding(0,0,0,0)
            setTextColor(getColor(if(dark) R.color.keyboard_text_dark else R.color.keyboard_text))
            background=android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x22333333),GradientDrawable().apply {setColor(getColor(when {dark && function->R.color.keyboard_function_dark;dark->R.color.keyboard_key_dark;function->R.color.keyboard_function;else->R.color.keyboard_key}));cornerRadius=dp(5).toFloat()},null)
            setOnClickListener { if(haptics) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); action() }
        }
        row.addView(button,LinearLayout.LayoutParams(0,dp(48),weight).apply {setMargins(dp(1),dp(2),dp(1),dp(2))})
        return button
    }
    /** Only gaps between visible keys are corrected; touches inside a key are never reassigned. */
    private inner class KeyboardRow:LinearLayout(this@ReaxKeyService) {
        private var gapTarget:View?=null
        override fun dispatchTouchEvent(event:MotionEvent):Boolean {
            if(event.actionMasked==MotionEvent.ACTION_DOWN) {
                gapTarget=null
                val hit=(0 until childCount).map(::getChildAt).firstOrNull {event.x>=it.left && event.x<it.right && event.y>=it.top && event.y<it.bottom}
                if(hit==null) {
                    val nearest=(0 until childCount).map(::getChildAt).minByOrNull {child->
                        val dx=event.x-(child.left+child.right)/2f;val dy=event.y-(child.top+child.bottom)/2f;dx*dx+dy*dy
                    }
                    if(nearest!=null) {
                        val dx=if(event.x<nearest.left) nearest.left-event.x else if(event.x>nearest.right) event.x-nearest.right else 0f
                        val dy=if(event.y<nearest.top) nearest.top-event.y else if(event.y>nearest.bottom) event.y-nearest.bottom else 0f
                        if(dx<=dp(8) && dy<=dp(8)) gapTarget=nearest
                    }
                }
            }
            val target=gapTarget ?: return super.dispatchTouchEvent(event)
            val corrected=MotionEvent.obtain(event)
            corrected.offsetLocation((target.width/2f)-event.x,(target.height/2f)-event.y)
            val handled=target.dispatchTouchEvent(corrected);corrected.recycle()
            if(event.actionMasked==MotionEvent.ACTION_UP || event.actionMasked==MotionEvent.ACTION_CANCEL) gapTarget=null
            return handled
        }
    }
    private fun showAlternatives(anchor:View,alternatives:List<String>,dark:Boolean) {
        val row=LinearLayout(this)
        popup=PopupWindow(row,dp(48)*alternatives.size,dp(60),true).apply { setBackgroundDrawable(GradientDrawable().apply { setColor(getColor(if(dark) R.color.keyboard_background_dark else R.color.keyboard_background)) }); elevation=dp(8).toFloat() }
        alternatives.forEach { value -> key(row,value,value,dark) { commit(state.text(value,Locale.forLanguageTag(pack.tag))); popup?.dismiss(); render() } }
        popup?.showAsDropDown(anchor,0,-dp(112))
    }
    private fun commit(text:String) { if(mediaMode) mediaPanel.append(text) else currentInputConnection?.commitText(text,1) }
    private fun delete() {
        if(mediaMode) {mediaPanel.delete();return}
        val connection=currentInputConnection ?: return
        // Read only for an explicit deletion; never retain editor text.
        if(!connection.getSelectedText(0).isNullOrEmpty()) connection.commitText("",1)
        else connection.deleteSurroundingTextInCodePoints(1,0)
    }
    private fun enter() {
        if(mediaMode) {searching=false;render();return}
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
