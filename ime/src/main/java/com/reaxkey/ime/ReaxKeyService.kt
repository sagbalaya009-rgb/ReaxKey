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
    private var suggestionsEnabled=true
    private var autocorrectEnabled=true
    private var learningEnabled=true
    private var trackpadEnabled=true
    private val intelligence by lazy { TypingIntelligence(this) }
    private var suggestionButtons:List<Button> = emptyList()
    private var spaceGesture:SpaceCursorGesture?=null
    private var renderAfterGesture=false
    private data class CorrectionUndo(val original:String,val replacement:String,val separator:String)
    private var correctionUndo:CorrectionUndo?=null
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
            val nextLanguage=packs.indexOfFirst {it.tag==value.language}.coerceAtLeast(0)
            val changed=theme!=value.theme || haptics!=value.haptics || suggestionsEnabled!=value.suggestions ||
                autocorrectEnabled!=value.autocorrect || learningEnabled!=value.learnWords || trackpadEnabled!=value.spaceTrackpad || languageIndex!=nextLanguage
            theme=value.theme;haptics=value.haptics; suggestionsEnabled=value.suggestions
            autocorrectEnabled=value.autocorrect;learningEnabled=value.learnWords;trackpadEnabled=value.spaceTrackpad
            languageIndex=nextLanguage
            if(changed && ::root.isInitialized) {if(spaceGesture?.isInteracting==true) renderAfterGesture=true else render()}
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
        cancelRepeat();spaceGesture?.cancel();correctionUndo=null; popup?.dismiss(); state.reset(); emoji=false
        mediaPanel.reset();mediaMode=false;searching=false
        val type=attribute?.inputType ?: 0
        val variation=type and InputType.TYPE_MASK_VARIATION
        numeric=(type and InputType.TYPE_MASK_CLASS) in listOf(InputType.TYPE_CLASS_NUMBER,InputType.TYPE_CLASS_PHONE,InputType.TYPE_CLASS_DATETIME)
        sensitive=(type and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT && variation in listOf(InputType.TYPE_TEXT_VARIATION_PASSWORD,InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) ||
            (type and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) ||
            ((attribute?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0)
        if(attribute?.hintText?.contains(Regex("(?i)\\b(otp|one.time|verification code|security code|cvv|cvc|pin)\\b")) == true) sensitive=true
        updateCaps()
        if(::root.isInitialized) render()
    }
    override fun onFinishInput() { cancelRepeat();spaceGesture?.cancel();correctionUndo=null; popup?.dismiss(); mediaPanel.reset(); state.reset(); super.onFinishInput() }
    override fun onFinishInputView(finishingInput: Boolean) { cancelRepeat();spaceGesture?.cancel();correctionUndo=null; popup?.dismiss(); mediaPanel.reset(); super.onFinishInputView(finishingInput) }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); popup?.dismiss(); scope.cancel(); super.onDestroy() }
    override fun onUpdateSelection(oldSelStart:Int,oldSelEnd:Int,newSelStart:Int,newSelEnd:Int,candidatesStart:Int,candidatesEnd:Int) {
        super.onUpdateSelection(oldSelStart,oldSelEnd,newSelStart,newSelEnd,candidatesStart,candidatesEnd)
        val before=state.shift; updateCaps()
        if(::root.isInitialized && before != state.shift) {
            if(spaceGesture?.isInteracting==true) renderAfterGesture=true else render()
        } else refreshSuggestions()
    }
    private fun updateCaps() {
        if(!sensitive && !mediaMode && state.layer==Layer.LETTERS) state.autoCap((currentInputConnection?.getCursorCapsMode(currentInputEditorInfo?.inputType ?: 0) ?: 0) != 0)
    }
    private fun render() {
        cancelRepeat();spaceGesture?.cancel();spaceGesture=null;renderAfterGesture=false; popup?.dismiss(); root.removeAllViews(); suggestionButtons=emptyList()
        val dark=theme=="DARK" || (theme=="SYSTEM" && resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        root.setBackgroundColor(getColor(if(dark) R.color.keyboard_background_dark else R.color.keyboard_background))
        if(mediaMode) {
            root.addView(mediaPanel.view(searching,dark,{mediaMode=false;mediaPanel.hide();render()},{searching=!searching;render()}))
            if(!searching) return
        }
        val toolbar=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            background=GradientDrawable().apply {setColor(if(dark) 0xAA454A50.toInt() else 0xCCFFFFFF.toInt());cornerRadius=dp(24).toFloat();setStroke(dp(1),if(dark) 0x55FFFFFF else 0x55FFFFFF)}
        }
        control(toolbar,"🌐",getString(R.string.switch_keyboard),dark) {getSystemService(InputMethodManager::class.java).showInputMethodPicker()}
        separator(toolbar,dark)
        control(toolbar,pack.tag.uppercase(Locale.ROOT)+" ▾",getString(R.string.change_language),dark) {
            languageIndex=(languageIndex+1)%packs.size;scope.launch {preferences.language(pack.tag)};state.reset();render()
        }
        if(!sensitive) {
            separator(toolbar,dark)
            control(toolbar,getString(R.string.media),getString(R.string.media),dark) {mediaMode=true;searching=false;state.reset();render()}
        }
        root.addView(toolbar,LinearLayout.LayoutParams(-1,dp(42)).apply {setMargins(dp(5),dp(2),dp(5),dp(3))})
        if(typingIntelligenceAllowed() && suggestionsEnabled) {
            val strip=LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL
                background=GradientDrawable().apply {setColor(if(dark) 0x443D4247 else 0x99FFFFFF.toInt());cornerRadius=dp(12).toFloat()}
            }
            suggestionButtons=(0..2).map { index ->
                Button(this).apply {
                    isAllCaps=false;textSize=14f;minHeight=0;minimumHeight=0;minWidth=0;minimumWidth=0
                    setPadding(0,0,0,0);setTextColor(getColor(if(dark) R.color.keyboard_text_dark else R.color.keyboard_text))
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setOnClickListener { acceptSuggestion(text.toString()) }
                }.also { strip.addView(it,LinearLayout.LayoutParams(0,-1,1f));if(index<2) separator(strip,dark) }
            }
            root.addView(strip,LinearLayout.LayoutParams(-1,dp(34)).apply {setMargins(dp(5),0,dp(5),dp(2))});refreshSuggestions()
        }
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
            key(bottom,punctuation,punctuation,dark,0.9f){commitBoundary(punctuation)}
            val space=key(bottom,getString(R.string.key_space),getString(R.string.key_space),dark,4f){commitBoundary(" ");updateCaps()}
            if(trackpadEnabled && !mediaMode) {
                spaceGesture=SpaceCursorGesture(space,handler,resources.displayMetrics.density,{currentInputConnection},
                    {commitBoundary(" ");updateCaps()},{haptics},{correctionUndo=null},{if(renderAfterGesture) render()})
                space.setOnTouchListener(spaceGesture)
            }
            key(bottom,".",".",dark,0.9f){commitBoundary(".")}
        }
        key(bottom,actionLabel(),actionLabel(),dark,1.4f,true){enter()}
        root.addView(bottom)
        refreshSuggestions()
    }
    private fun control(row:LinearLayout,label:String,description:String,dark:Boolean,action:()->Unit) {
        val button=Button(this).apply {text=label;contentDescription=description;isAllCaps=false;textSize=14f
            minHeight=0;minimumHeight=0;minWidth=0;minimumWidth=0;setPadding(0,0,0,0)
            setTextColor(getColor(if(dark) R.color.keyboard_text_dark else R.color.keyboard_text));setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setOnClickListener {action()}}
        row.addView(button,LinearLayout.LayoutParams(0,-1,1f))
    }
    private fun separator(row:LinearLayout,dark:Boolean) {row.addView(View(this).apply {setBackgroundColor(if(dark) 0x55FFFFFF else 0x33000000)},LinearLayout.LayoutParams(dp(1),dp(22)).apply {gravity=Gravity.CENTER_VERTICAL})}
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
            background=android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x22333333),GradientDrawable().apply {setColor(getColor(when {dark && function->R.color.keyboard_function_dark;dark->R.color.keyboard_key_dark;function->R.color.keyboard_function;else->R.color.keyboard_key}));cornerRadius=dp(9).toFloat();setStroke(dp(1),if(dark) 0x22FFFFFF else 0x55FFFFFF)},null)
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
    private fun commit(text:String) { if(mediaMode) mediaPanel.append(text) else {correctionUndo=null;currentInputConnection?.commitText(text,1);refreshSuggestions()} }
    private fun typingIntelligenceAllowed():Boolean {
        if(sensitive || numeric || mediaMode || searching || state.layer!=Layer.LETTERS || emoji || pack.tag!="en") return false
        val inputType=currentInputEditorInfo?.inputType ?: return false
        if(inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0 || inputType and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE != 0) return false
        val variation=inputType and InputType.TYPE_MASK_VARIATION
        return variation !in listOf(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI,InputType.TYPE_TEXT_VARIATION_FILTER,InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT)
    }
    private fun currentWord():String {
        if(!currentInputConnection?.getSelectedText(0).isNullOrEmpty()) return ""
        val before=currentInputConnection?.getTextBeforeCursor(64,0)?.toString() ?: return ""
        return before.takeLastWhile {it.isLetter() || it=='\''}.takeLast(32)
    }
    private fun refreshSuggestions() {
        if(suggestionButtons.isEmpty() || !typingIntelligenceAllowed()) return
        val ic=currentInputConnection ?: return
        val before=ic.getTextBeforeCursor(96,0)?.toString().orEmpty()
        val prefix=if(ic.getSelectedText(0).isNullOrEmpty()) before.takeLastWhile {it.isLetter() || it=='\''}.takeLast(32) else ""
        val previous=before.dropLast(prefix.length).trimEnd().takeLastWhile {it.isLetter() || it=='\''}
        val candidates=intelligence.suggestions(prefix,previous)
        suggestionButtons.forEachIndexed { index,button ->
            button.text=candidates.getOrNull(index).orEmpty()
            button.isEnabled=index<candidates.size
            button.contentDescription=if(button.isEnabled) getString(R.string.suggestion)+" "+button.text else ""
        }
    }
    private fun acceptSuggestion(candidate:String) {
        if(!typingIntelligenceAllowed() || !suggestionsEnabled || candidate.isBlank()) return
        val ic=currentInputConnection ?: return
        val prefix=currentWord()
        val next=ic.getTextAfterCursor(1,0)?.toString().orEmpty()
        val separator=if(next.firstOrNull()?.let {it.isWhitespace() || it in ",.!?;:"}==true) "" else " "
        ic.beginBatchEdit()
        try {if(prefix.isNotEmpty()) ic.deleteSurroundingTextInCodePoints(prefix.codePointCount(0,prefix.length),0);ic.commitText(candidate+separator,1)}
        finally {ic.endBatchEdit()}
        correctionUndo=null;refreshSuggestions()
    }
    private fun commitBoundary(separator:String) {
        if(mediaMode) {mediaPanel.append(separator);return}
        val ic=currentInputConnection ?: return
        val original=if(typingIntelligenceAllowed()) currentWord() else ""
        val replacement=if(autocorrectEnabled && original.isNotBlank()) intelligence.correction(original) else null
        if(replacement!=null) {
            ic.beginBatchEdit()
            try {ic.deleteSurroundingTextInCodePoints(original.codePointCount(0,original.length),0);ic.commitText(replacement+separator,1)}
            finally {ic.endBatchEdit()}
            correctionUndo=CorrectionUndo(original,replacement,separator)
        } else {
            if(learningEnabled && original.isNotBlank() && typingIntelligenceAllowed()) intelligence.observeDeliberateWord(original)
            ic.commitText(separator,1);correctionUndo=null
        }
        refreshSuggestions()
    }
    private fun delete() {
        if(mediaMode) {mediaPanel.delete();return}
        val connection=currentInputConnection ?: return
        correctionUndo?.let { undo ->
            val expected=undo.replacement+undo.separator
            if(connection.getTextBeforeCursor(expected.length,0)?.toString()==expected) {
                connection.beginBatchEdit()
                try {connection.deleteSurroundingTextInCodePoints(expected.codePointCount(0,expected.length),0);connection.commitText(undo.original,1)}
                finally {connection.endBatchEdit()}
                if(learningEnabled && typingIntelligenceAllowed()) intelligence.observeDeliberateWord(undo.original)
                correctionUndo=null;refreshSuggestions();return
            }
        }
        correctionUndo=null
        // Read only for an explicit deletion; never retain editor text.
        if(!connection.getSelectedText(0).isNullOrEmpty()) connection.commitText("",1)
        else connection.deleteSurroundingTextInCodePoints(1,0)
        refreshSuggestions()
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
