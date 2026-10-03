package com.reaxkey.testbed

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.ClipDescription
import android.view.inputmethod.*
import android.widget.*
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import java.util.concurrent.Executors

class ReceiverActivity:Activity() {
    private val executor=Executors.newSingleThreadExecutor()
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        val layout=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(24,48,24,24)}
        layout.addView(TextView(this).apply {setText(R.string.title);textSize=24f})
        layout.addView(TextView(this).apply {setText(R.string.help)})
        val status=TextView(this).apply {setText(R.string.waiting)}
        val fields=listOf(R.string.png_jpeg to arrayOf("image/png","image/jpeg"),R.string.gif to arrayOf("image/gif"),R.string.webp to arrayOf("image/webp"),R.string.all to arrayOf("image/png","image/jpeg","image/gif","image/webp"),R.string.none to emptyArray())
        fields.forEach { (label,mimes) -> layout.addView(ReceiverField(this,mimes){message->status.text=message}.apply {setHint(label);minHeight=96}) }
        layout.addView(EditText(this).apply {
            setHint(R.string.multiline_cursor)
            inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines=4
            gravity=android.view.Gravity.TOP
        })
        layout.addView(status)
        setContentView(ScrollView(this).apply {addView(layout)})
    }
    override fun onDestroy() {executor.shutdownNow();super.onDestroy()}
    inner class ReceiverField(context:Context,private val mimes:Array<String>,private val status:(String)->Unit):EditText(context) {
        override fun onCreateInputConnection(info:EditorInfo):InputConnection? {
            val connection=super.onCreateInputConnection(info) ?: return null
            EditorInfoCompat.setContentMimeTypes(info,mimes)
            return InputConnectionCompat.createWrapper(connection,info) { content,flags,_ ->
                val mime=(0 until content.description.mimeTypeCount).map {content.description.getMimeType(it)}.firstOrNull {candidate->mimes.any {ClipDescription.compareMimeTypes(candidate,it)}}
                if(mime==null || content.contentUri.scheme!="content") false else {
                    try {
                        if(flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION!=0) content.requestPermission()
                        executor.execute {
                            val count=try {contentResolver.openInputStream(content.contentUri)?.use {input->val buffer=ByteArray(8192);var total=0L;while(true){val n=input.read(buffer);if(n<0)break;total+=n;if(total>50L*1024*1024)throw java.io.IOException()};total}} catch(_:Exception){null} finally {content.releasePermission()}
                            runOnUiThread {status(if(count!=null) getString(R.string.received,mime,count) else getString(R.string.rejected))}
                        }
                        true
                    } catch(_:Exception) {content.releasePermission();status(getString(R.string.rejected));false}
                }
            }
        }
    }
}
