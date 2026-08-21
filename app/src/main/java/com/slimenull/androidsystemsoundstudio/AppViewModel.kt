package com.slimenull.androidsystemsoundstudio

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.slimenull.androidsystemsoundstudio.data.ModuleExporter
import com.slimenull.androidsystemsoundstudio.data.SoundRepository
import com.slimenull.androidsystemsoundstudio.model.SoundAsset
import com.slimenull.androidsystemsoundstudio.model.SoundCatalog
import com.slimenull.androidsystemsoundstudio.model.SoundTarget
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppUiState(
    val customSounds: List<SoundAsset> = emptyList(),
    val selections: Map<String, String> = emptyMap(),
    val pendingImport: PendingImport? = null,
    val editingSound: SoundAsset? = null,
    val isSavingSound: Boolean = false,
    val showUnsupportedSounds: Boolean = false,
)

data class PendingImport(val uri: Uri, val displayName: String)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SoundRepository(application)
    private val exporter = ModuleExporter(application, repository)
    private val deviceSoundFileNames = repository.loadSystemSoundFileNames()
    private var player: MediaPlayer? = null
    private var previewFile: File? = null

    var uiState by mutableStateOf(
        AppUiState(
            customSounds = repository.loadCustomSounds(),
            selections = repository.loadSelections(),
            showUnsupportedSounds = repository.loadShowUnsupportedSounds(),
        ),
    )
        private set

    val allSounds: List<SoundAsset>
        get() = SoundCatalog.builtIns() + uiState.customSounds

    val allTargets: List<SoundTarget>
        get() = SoundCatalog.targetsForDevice(deviceSoundFileNames, includeUnsupported = true)

    val visibleTargets: List<SoundTarget>
        get() = SoundCatalog.targetsForDevice(deviceSoundFileNames, uiState.showUnsupportedSounds)

    val supportsAudioConversion: Boolean
        get() = repository.supportsAudioConversion

    fun select(targetId: String, soundId: String?) {
        val updated = uiState.selections.toMutableMap().apply {
            if (soundId == null) remove(targetId) else put(targetId, soundId)
        }
        uiState = uiState.copy(selections = updated)
        repository.saveSelections(updated)
    }

    fun toggleShowUnsupportedSounds() {
        val show = !uiState.showUnsupportedSounds
        uiState = uiState.copy(showUnsupportedSounds = show)
        repository.saveShowUnsupportedSounds(show)
    }

    fun beginImport(uri: Uri, displayName: String) {
        uiState = uiState.copy(pendingImport = PendingImport(uri, displayName))
    }

    fun isSupportedAudioFile(displayName: String): Boolean =
        SoundRepository.isSupportedAudioFile(displayName)

    fun supportsImport(displayName: String): Boolean = repository.supportsImport(displayName)

    fun cancelCategoryEdit() {
        if (uiState.isSavingSound) return
        uiState = uiState.copy(pendingImport = null, editingSound = null)
    }

    fun editCategories(sound: SoundAsset) {
        uiState = uiState.copy(editingSound = sound)
    }

    fun confirmCategories(categories: Set<String>, onComplete: (Result<Unit>) -> Unit) {
        if (uiState.isSavingSound) return
        if (categories.isEmpty()) {
            onComplete(Result.failure(IllegalArgumentException("请至少选择一个分类")))
            return
        }
        val pending = uiState.pendingImport
        val editing = uiState.editingSound
        if (pending == null && editing == null) {
            onComplete(Result.success(Unit))
            return
        }
        val existingSounds = uiState.customSounds
        uiState = uiState.copy(isSavingSound = true)

        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val updated = when {
                        pending != null -> existingSounds + repository.importSound(
                            pending.uri,
                            pending.displayName,
                            categories,
                        )
                        else -> existingSounds.map {
                            if (it.id == editing?.id) it.copy(categories = categories) else it
                        }
                    }
                    repository.saveSounds(updated)
                    updated
                }
            }
            result.onSuccess { updated ->
                uiState = uiState.copy(
                    customSounds = updated,
                    pendingImport = null,
                    editingSound = null,
                    isSavingSound = false,
                )
            }.onFailure {
                uiState = uiState.copy(isSavingSound = false)
            }
            onComplete(result.map { Unit })
        }
    }

    fun delete(sound: SoundAsset) {
        repository.delete(sound)
        val sounds = uiState.customSounds.filterNot { it.id == sound.id }
        val selections = uiState.selections.filterValues { it != sound.id }
        repository.saveSounds(sounds)
        repository.saveSelections(selections)
        uiState = uiState.copy(customSounds = sounds, selections = selections)
    }

    fun export(uri: Uri): Result<Int> = runCatching {
        exporter.export(uri, uiState.selections, allSounds, allTargets)
    }

    fun preview(sound: SoundAsset): Result<Unit> = playPreview { repository.open(sound) }

    fun canPreview(target: SoundTarget): Boolean =
        target.isAvailableOnDevice ||
            uiState.selections[target.id]?.let { selectedId ->
                allSounds.any { it.id == selectedId && target.id in it.categories }
            } == true ||
            allSounds.any { it.builtIn && target.id in it.categories }

    fun preview(target: SoundTarget): Result<Unit> {
        val selected = uiState.selections[target.id]?.let { selectedId ->
            allSounds.firstOrNull { it.id == selectedId && target.id in it.categories }
        }
        if (selected != null) return preview(selected)

        val builtIn = allSounds.firstOrNull { it.builtIn && target.id in it.categories }
        if (target.isAvailableOnDevice) {
            val systemResult = playPreview { repository.openSystemSound(target.fileName) }
            if (systemResult.isSuccess || builtIn == null) return systemResult
        }
        return builtIn?.let(::preview)
            ?: Result.failure(IllegalStateException("当前声音无法试听"))
    }

    private fun playPreview(openInput: () -> java.io.InputStream): Result<Unit> = runCatching {
        player?.release()
        previewFile?.delete()
        val file = File.createTempFile("preview_", ".ogg", getApplication<Application>().cacheDir)
        openInput().use { input -> file.outputStream().use(input::copyTo) }
        previewFile = file
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener {
                it.release()
                if (player === it) player = null
                file.delete()
            }
            prepare()
            start()
        }
    }

    override fun onCleared() {
        player?.release()
        previewFile?.delete()
    }
}
