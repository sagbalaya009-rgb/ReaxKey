package com.reaxkey.keyboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reaxkey.media.SourceDiscoveryEngine
import com.reaxkey.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class DiscoveryUi(val started:Boolean=false,val checking:Boolean=false,val candidates:List<SourceCandidate> = emptyList(),val failed:Boolean=false)
class DiscoveryViewModel(app:Application):AndroidViewModel(app) {
    private val mutable=MutableStateFlow(DiscoveryUi())
    val state=mutable.asStateFlow()
    private var job:Job?=null
    fun find() {
        job?.cancel();mutable.value=DiscoveryUi(started=true,checking=true)
        job=viewModelScope.launch {
            try {SourceDiscoveryEngine(getApplication(),(getApplication<Application>() as ReaxHost).repository).discover().collect {candidate->
                mutable.update {it.copy(candidates=it.candidates.filterNot {old->old.source.id==candidate.source.id}+candidate)}
            }} catch(e:CancellationException){throw e} catch(_:Exception){mutable.update {it.copy(failed=true)}}
            finally {if(isActive) mutable.update {it.copy(checking=false)}}
        }
    }
}
