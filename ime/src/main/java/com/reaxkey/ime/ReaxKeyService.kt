package com.reaxkey.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import com.reaxkey.ui.R

class ReaxKeyService : InputMethodService() {
    override fun onEvaluateFullscreenMode() = false
    override fun onCreateInputView(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        listOf("qwertyuiop", "asdfghjkl", "zxcvbnm").forEach { row ->
            addView(LinearLayout(context).apply {
                row.forEach { character -> addView(Button(context).apply {
                    text = character.toString(); minWidth = 0; minimumWidth = 0; setPadding(0,0,0,0)
                    setOnClickListener { currentInputConnection?.commitText(character.toString(), 1) }
                }, LinearLayout.LayoutParams(0, dp(52), 1f)) }
            })
        }
        addView(LinearLayout(context).apply {
            fun key(label: Int, action: () -> Unit) { addView(Button(context).apply {
                setText(label); setOnClickListener { action() }
            }, LinearLayout.LayoutParams(0, dp(52), 1f)) }
            key(R.string.switch_keyboard) { getSystemService(InputMethodManager::class.java).showInputMethodPicker() }
            key(R.string.key_space) { currentInputConnection?.commitText(" ",1) }
            key(R.string.key_delete) { currentInputConnection?.deleteSurroundingTextInCodePoints(1,0) }
            key(R.string.key_enter) { currentInputConnection?.commitText("\n",1) }
        })
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
