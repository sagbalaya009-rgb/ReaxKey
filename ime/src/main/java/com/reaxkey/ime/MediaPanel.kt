package com.reaxkey.ime

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.inputmethod.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.allowHardware
import com.reaxkey.media.MediaAccess
import com.reaxkey.model.*
import com.reaxkey.ui.R
import kotlinx.coroutines.*

/** Owns only library query text. It never reads or writes the host editor for search. */
class MediaPanel(private val service:InputMethodService,private val scope:CoroutineScope,private val privacy:()->Boolean) {
    private val repo get()=(service.application as ReaxHost).repository
    private val access by lazy {MediaAccess(service,repo)}
    var query=""
        private set
    private var filter="All"
    private var offset=0
    private var queryJob:Job?=null
    private var sendJob:Job?=null
    private var session=0L
    private var grid:RecyclerView?=null
    private var label:TextView?=null
    private var importButton:Button?=null
    private var popup:PopupWindow?=null
    fun reset() {session++;query="";offset=0;queryJob?.cancel();sendJob?.cancel();popup?.dismiss();grid=null;label=null;importButton=null}
    fun hide() {queryJob?.cancel();popup?.dismiss();grid=null;label=null;importButton=null}
    fun append(value:String) {query=(query+value).take(200);offset=0;refresh()}
    fun delete() {if(query.isNotEmpty()) query=query.substring(0,query.offsetByCodePoints(query.length,-1));offset=0;refresh()}
    fun view(search:Boolean,dark:Boolean,onBack:()->Unit,onSearch:()->Unit):View {
        val column=LinearLayout(service).apply {orientation=LinearLayout.VERTICAL}
        val controls=LinearLayout(service)
        fun button(parent:LinearLayout,text:String,action:()->Unit) {parent.addView(Button(service).apply {this.text=text;isAllCaps=false;setOnClickListener {action()}},LinearLayout.LayoutParams(0,dp(48),1f))}
        button(controls,service.getString(R.string.key_letters),onBack)
        button(controls,service.getString(R.string.action_search),onSearch)
        button(controls,service.getString(R.string.clear_search)) {query="";offset=0;refresh()}
        column.addView(controls)
        label=TextView(service).apply {setTextColor(service.getColor(if(dark) R.color.keyboard_text_dark else R.color.keyboard_text));text=query.ifBlank {service.getString(R.string.media_search)};setPadding(dp(8),dp(4),dp(8),dp(4));setOnClickListener {onSearch()};contentDescription=service.getString(R.string.media_search)}
        column.addView(label)
        val tabs=LinearLayout(service)
        listOf("All" to R.string.all,"Stickers" to R.string.stickers,"GIFs" to R.string.gifs,"Recent" to R.string.recent,"Favorites" to R.string.favorites).forEach { (key,title) ->button(tabs,service.getString(title)){filter=key;offset=0;refresh()} }
        column.addView(HorizontalScrollView(service).apply {addView(tabs,ViewGroup.LayoutParams(dp(510),dp(48)))})
        grid=RecyclerView(service).apply {layoutManager=GridLayoutManager(service,4);adapter=Items(emptyList())}
        column.addView(grid,LinearLayout.LayoutParams(-1,dp(if(search) 100 else 210)))
        importButton=Button(service).apply {
            text=service.getString(R.string.import_stickers);isAllCaps=false;visibility=View.GONE
            setOnClickListener {service.packageManager.getLaunchIntentForPackage(service.packageName)?.let {intent->service.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}
        }
        column.addView(importButton)
        if(!search) {val pages=LinearLayout(service);button(pages,service.getString(R.string.previous_page)){offset=(offset-80).coerceAtLeast(0);refresh()};button(pages,service.getString(R.string.next_page)){offset+=80;refresh()};column.addView(pages)}
        refresh();return column
    }
    private fun refresh() {
        label?.text=query.ifBlank {service.getString(R.string.media_search)}
        queryJob?.cancel()
        queryJob=scope.launch {
            delay(100)
            try { repo.observe(MediaQuery(query,filter,limit=80,offset=offset)).collect {items ->
                grid?.adapter=Items(items)
                if(items.isEmpty()) label?.text=service.getString(R.string.empty_library)
                importButton?.visibility=if(items.isEmpty()) View.VISIBLE else View.GONE
            }} catch(e:CancellationException){throw e} catch(_:Exception){label?.setText(R.string.operation_failed)}
        }
    }
    private inner class Holder(val image:ImageView):RecyclerView.ViewHolder(image)
    private inner class Items(val items:List<MediaItem>):RecyclerView.Adapter<Holder>() {
        override fun getItemCount()=items.size
        override fun onCreateViewHolder(parent:ViewGroup,type:Int)=Holder(ImageView(service).apply {layoutParams=ViewGroup.LayoutParams(-1,dp(82));scaleType=ImageView.ScaleType.FIT_CENTER;setPadding(dp(4),dp(4),dp(4),dp(4))})
        override fun onBindViewHolder(holder:Holder,position:Int) {
            val item=items[position];holder.image.contentDescription=item.label.ifBlank {item.name}
            holder.image.load(item.uri) {size(160,160);allowHardware(false)}
            holder.image.setOnClickListener {send(item)}
            holder.image.setOnLongClickListener {
                menu(holder.image,item.name,listOf(service.getString(if(item.favorite) R.string.unfavorite else R.string.favorite) to {scope.launch {repo.favorite(item.id,!item.favorite)}},service.getString(R.string.library) to {
                    service.packageManager.getLaunchIntentForPackage(service.packageName)?.let {service.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
                }));true
            }
        }
        override fun onViewRecycled(holder:Holder) {holder.image.setImageDrawable(null)}
    }
    private fun send(item:MediaItem) {
        val editor=service.currentInputEditorInfo ?: return
        val connection=service.currentInputConnection ?: return
        val expectedSession=session
        if(!MediaRules.accepts(item.mime,EditorInfoCompat.getContentMimeTypes(editor).toList())) {fallback(item);return}
        sendJob?.cancel();sendJob=scope.launch {
            try {
                val uri=access.sendUri(item)
                if(expectedSession!=session || service.currentInputConnection!==connection) return@launch
                val accepted=RichContentSender.commit(connection,editor,uri,item.mime,item.name)
                if(accepted) {if(!privacy()) repo.recordUse(item.id)} else fallback(item)
            } catch(e:CancellationException){throw e} catch(_:Exception){Toast.makeText(service,R.string.missing_media,Toast.LENGTH_LONG).show()}
        }
    }
    private fun fallback(item:MediaItem) {
        val anchor=grid ?: return
        menu(anchor,service.getString(R.string.unsupported_field),listOf(service.getString(R.string.share) to {scope.launch {
            try {
                val uri=access.sendUri(item)
                val intent=Intent(Intent.ACTION_SEND).setType(item.mime).putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply {clipData=ClipData.newRawUri(item.name,uri)}
                service.startActivity(Intent.createChooser(intent,service.getString(R.string.share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch(e:CancellationException){throw e} catch(_:Exception){Toast.makeText(service,R.string.missing_media,Toast.LENGTH_LONG).show()}
        }}))
    }
    private fun menu(anchor:View,title:String,actions:List<Pair<String,()->Unit>>) {
        popup?.dismiss()
        val column=LinearLayout(service).apply {orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(12),dp(12),dp(12));setBackgroundColor(service.getColor(R.color.keyboard_key))}
        column.addView(TextView(service).apply {text=title;setTextColor(service.getColor(R.color.keyboard_text))})
        actions.forEach { (text,action) ->column.addView(Button(service).apply {this.text=text;setOnClickListener {popup?.dismiss();action()}}) }
        popup=PopupWindow(column,dp(300),-2,true).apply {setBackgroundDrawable(android.graphics.drawable.ColorDrawable(service.getColor(R.color.keyboard_key)));elevation=dp(8).toFloat();showAtLocation(anchor,android.view.Gravity.CENTER,0,0)}
    }
    private fun dp(value:Int)=(value*service.resources.displayMetrics.density).toInt()
}
