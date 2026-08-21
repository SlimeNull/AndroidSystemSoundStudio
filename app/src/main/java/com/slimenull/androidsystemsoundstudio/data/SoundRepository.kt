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

    fun importSound(uri: Uri, displayName: String, categories: Set<String>): SoundAsset {
        val id = UUID.randomUUID().toString()
        val fileName = "$id.ogg"
        val destination = File(soundDirectory, fileName)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取所选文件" }
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        return SoundAsset(id, displayName.removeSuffix(".ogg"), fileName, categories)
    }

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

    companion object {
        private const val KEY_SOUNDS = "custom_sounds"
        private const val KEY_SELECTIONS = "selections"
    }
}
