package com.reaxkey.ime

import android.content.ClipDescription
import android.net.Uri
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.view.inputmethod.*
import com.reaxkey.model.MediaRules

object RichContentSender {
    fun commit(connection:InputConnection,editor:EditorInfo,uri:Uri,mime:String,label:String):Boolean {
        if(uri.scheme!="content" || !MediaRules.accepts(mime,EditorInfoCompat.getContentMimeTypes(editor).toList())) return false
        return InputConnectionCompat.commitContent(connection,editor,
            InputContentInfoCompat(uri,ClipDescription(label,arrayOf(mime)),null),
            InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,null)
    }
}
