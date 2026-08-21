package com.slimenull.androidsystemsoundstudio.data

import android.content.Context
import android.net.Uri
import com.slimenull.androidsystemsoundstudio.model.SoundAsset
import com.slimenull.androidsystemsoundstudio.model.SoundCatalog
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ModuleExporter(
    private val context: Context,
    private val repository: SoundRepository,
) {
    fun export(uri: Uri, selections: Map<String, String>, sounds: List<SoundAsset>): Int {
        val selectedTargets = resolveSelections(selections, sounds)
        require(selectedTargets.isNotEmpty()) { "请先为至少一个项目选择声音" }

        context.contentResolver.openOutputStream(uri, "w").use { rawOutput ->
            requireNotNull(rawOutput) { "无法创建导出文件" }
            ZipOutputStream(rawOutput.buffered()).use { zip ->
                zip.textEntry(
                    "module.prop",
                    """id=system_sound_studio
                        |name=系统声音工坊
                        |version=1.0
                        |versionCode=1
                        |author=SlimeNull
                        |description=由系统声音工坊生成的系统 UI 音效替换模块
                        |""".trimMargin(),
                )
                zip.textEntry(
                    "customize.sh",
                    """SKIPMOUNT=false
                        |PROPFILE=false
                        |POSTFSDATA=false
                        |LATESTARTSERVICE=false
                        |ui_print "- 正在安装系统声音替换模块"
                        |set_perm_recursive ${'$'}MODPATH 0 0 0755 0644
                        |""".trimMargin(),
                )
                zip.textEntry("system.prop", "")
                selectedTargets.forEach { (target, sound) ->
                    zip.putNextEntry(ZipEntry("system/product/media/audio/ui/${target.fileName}"))
                    repository.open(sound).use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        return selectedTargets.size
    }

    private fun ZipOutputStream.textEntry(name: String, text: String) {
        putNextEntry(ZipEntry(name))
        write(text.toByteArray())
        closeEntry()
    }
}

internal fun resolveSelections(
    selections: Map<String, String>,
    sounds: List<SoundAsset>,
) = SoundCatalog.targets.mapNotNull { target ->
    val soundId = selections[target.id] ?: return@mapNotNull null
    sounds.firstOrNull { it.id == soundId && target.id in it.categories }?.let { target to it }
}
