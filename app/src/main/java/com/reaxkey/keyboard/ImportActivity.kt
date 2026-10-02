package com.reaxkey.keyboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.reaxkey.media.MediaAccess
import com.reaxkey.model.ReaxHost
import com.reaxkey.ui.R
import com.reaxkey.ui.ReaxTheme
import kotlinx.coroutines.*
import coil3.load
import android.widget.ImageView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.reaxkey.ui.ReaxButton

class ImportActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        val uris=when(intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(androidx.core.content.IntentCompat.getParcelableExtra(intent,Intent.EXTRA_STREAM,Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE -> androidx.core.content.IntentCompat.getParcelableArrayListExtra(intent,Intent.EXTRA_STREAM,Uri::class.java)?.toList() ?: emptyList()
            else -> emptyList()
        }.filter {it.scheme=="content"}.take(100)
        setContent {ReaxTheme {Surface {Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            var busy by remember {mutableStateOf(false)}; var result by remember {mutableStateOf<String?>(null)}
            Text(stringResource(R.string.share_confirm),style=MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.reaction_count,uris.size))
            LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) {items(uris) {uri -> AndroidView(factory={context->ImageView(context).apply {scaleType=ImageView.ScaleType.FIT_CENTER}},update={it.load(uri){size(320,320)}},modifier=Modifier.size(if(uris.size==1) 240.dp else 120.dp))}}
            Text(result ?: stringResource(if(busy) R.string.importing else R.string.import_body))
            ReaxButton(stringResource(if(uris.size==1) R.string.add_one else R.string.add_all),enabled=!busy && uris.isNotEmpty() && result==null,onClick={busy=true;lifecycleScope.launch {
                val media=MediaAccess(this@ImportActivity,(application as ReaxHost).repository); var count=0
                for(uri in uris) try {media.importCopy(uri);count++} catch(e:CancellationException){throw e} catch(_:Exception) { }
                result=getString(R.string.import_complete,count,uris.size);busy=false
            }})
            TextButton(onClick={finish()},enabled=!busy){Text(stringResource(R.string.close))}
        }}}}
    }
}
