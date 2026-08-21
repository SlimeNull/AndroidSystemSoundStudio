package com.slimenull.androidsystemsoundstudio.data

import android.content.Context
import android.net.Uri
import com.slimenull.androidsystemsoundstudio.model.SoundAsset
import com.slimenull.androidsystemsoundstudio.model.SoundCatalog
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class SoundRepository(private val context: Context) {
    private val preferences = context.getSharedPreferences("sound_studio", Context.MODE_PRIVATE)
    private val soundDirectory = File(context.filesDir, "sounds").apply { mkdirs() }
    private val systemSoundDirectory = File(SYSTEM_SOUND_DIRECTORY)

    val supportsAudioConversion: Boolean
        get() = OggAudioTranscoder.isAvailable

    fun loadCustomSounds(): List<SoundAsset> {
        val raw = preferences.getString(KEY_SOUNDS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                repeat(array.length()) { index ->
                    val item = array.getJSONObject(index)
                    val categories = item.getJSONArray("categories")
                    add(
                        SoundAsset(
                            id = item.getString("id"),
                            displayName = item.getString("displayName"),
                            storedFileName = item.getString("storedFileName"),
                            categories = buildSet {
                                repeat(categories.length()) { add(categories.getString(it)) }
                            },
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun loadSelections(): Map<String, String> {
        val raw = preferences.getString(KEY_SELECTIONS, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { json.getString(it) }
        }.getOrDefault(emptyMap())
    }

    fun loadSystemSoundFileNames(): Set<String>? {
        val files = systemSoundDirectory.listFiles() ?: return null
        return files.asSequence()
            .filter(File::isFile)
            .map(File::getName)
            .filter(::isSupportedAudioFile)
            .toSet()
    }

    fun loadShowUnsupportedSounds(): Boolean =
        preferences.getBoolean(KEY_SHOW_UNSUPPORTED_SOUNDS, false)

    fun saveShowUnsupportedSounds(show: Boolean) {
        preferences.edit().putBoolean(KEY_SHOW_UNSUPPORTED_SOUNDS, show).apply()
    }

    fun importSound(uri: Uri, displayName: String, categories: Set<String>): SoundAsset {
        val id = UUID.randomUUID().toString()
        val fileName = "$id.ogg"
        val destination = File(soundDirectory, fileName)
        if (isOggFile(displayName)) {
            copyUri(uri, destination)
        } else {
            check(OggAudioTranscoder.isAvailable) { "当前设备仅支持导入 OGG 音频" }
            val source = File.createTempFile("sound_import_", ".audio", context.cacheDir)
            try {
                copyUri(uri, source)
                OggAudioTranscoder.transcode(source, destination)
            } catch (error: Throwable) {
                destination.delete()
                throw error
            } finally {
                source.delete()
            }
        }
        return SoundAsset(id, displayName.substringBeforeLast('.', displayName), fileName, categories)
    }

    fun supportsImport(displayName: String): Boolean =
        isSupportedAudioFile(displayName) && (isOggFile(displayName) || OggAudioTranscoder.isAvailable)

    fun saveSounds(sounds: List<SoundAsset>) {
        val array = JSONArray()
        sounds.filterNot { it.builtIn }.forEach { sound ->
            array.put(
                JSONObject()
                    .put("id", sound.id)
                    .put("displayName", sound.displayName)
                    .put("storedFileName", sound.storedFileName)
                    .put("categories", JSONArray(sound.categories.toList())),
            )
        }
        preferences.edit().putString(KEY_SOUNDS, array.toString()).apply()
    }

    fun saveSelections(selections: Map<String, String>) {
        preferences.edit().putString(KEY_SELECTIONS, JSONObject(selections).toString()).apply()
    }

    fun delete(sound: SoundAsset) {
        if (!sound.builtIn) File(soundDirectory, sound.storedFileName).delete()
    }

    fun open(sound: SoundAsset) = if (sound.builtIn) {
        context.assets.open("aosp/${sound.storedFileName}")
    } else {
        File(soundDirectory, sound.storedFileName).inputStream()
    }

    fun openSystemSound(fileName: String) =
        File(systemSoundDirectory, fileName).also { file ->
            require(file.name == fileName && file.isFile && file.canRead()) { "无法读取设备系统声音" }
        }.inputStream()

    private fun copyUri(uri: Uri, destination: File) {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取所选文件" }
            destination.outputStream().use { output -> input.copyTo(output) }
        }
    }

    companion object {
        private const val KEY_SOUNDS = "custom_sounds"
        private const val KEY_SELECTIONS = "selections"
        private const val KEY_SHOW_UNSUPPORTED_SOUNDS = "show_unsupported_sounds"
        private const val SYSTEM_SOUND_DIRECTORY = "/product/media/audio/ui"

        private val SUPPORTED_EXTENSIONS = setOf(
            "ogg",
            "oga",
            "opus",
            "mp3",
            "wav",
            "wave",
            "m4a",
            "aac",
            "flac",
        )

        fun isSupportedAudioFile(displayName: String): Boolean =
            displayName.substringAfterLast('.', "").lowercase() in SUPPORTED_EXTENSIONS

        fun isOggFile(displayName: String): Boolean =
            displayName.substringAfterLast('.', "").lowercase() in setOf("ogg", "oga", "opus")
    }
}
