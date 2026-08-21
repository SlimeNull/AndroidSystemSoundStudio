package com.slimenull.androidsystemsoundstudio.data

import com.slimenull.androidsystemsoundstudio.model.SoundAsset
import com.slimenull.androidsystemsoundstudio.model.SoundCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleExporterTest {
    @Test
    fun defaultSelectionsProduceNoFiles() {
        assertTrue(resolveSelections(emptyMap(), SoundCatalog.builtIns(), SoundCatalog.mappings).isEmpty())
    }

    @Test
    fun builtInSoundsOnlyBelongToTheirOwnCategory() {
        val builtIns = SoundCatalog.builtIns()

        assertTrue(builtIns.all { it.categories.size == 1 })
        assertEquals(
            SoundCatalog.mappings.filter { it.builtInAssetName != null }.map { it.id }.toSet(),
            builtIns.flatMap { it.categories }.toSet(),
        )
    }

    @Test
    fun crossCategoryOrMissingSelectionsAreIgnored() {
        val tick = SoundAsset("tick_sound", "Tick", "tick.ogg", setOf("tick"))
        val resolved = resolveSelections(
            selections = mapOf("tick" to tick.id, "lock" to tick.id, "unlock" to "missing"),
            sounds = listOf(tick),
            targets = SoundCatalog.mappings,
        )

        assertEquals(listOf("tick"), resolved.map { it.first.id })
    }

    @Test
    fun customSoundCanResolveForMultipleCategories() {
        val custom = SoundAsset("custom", "Custom", "custom.ogg", setOf("tick", "lock"))
        val resolved = resolveSelections(
            selections = mapOf("tick" to custom.id, "lock" to custom.id),
            sounds = listOf(custom),
            targets = SoundCatalog.mappings,
        )

        assertEquals(setOf("tick", "lock"), resolved.map { it.first.id }.toSet())
    }

    @Test
    fun unavailableMappingsAreHiddenByDefaultAndIncludedOnRequest() {
        val deviceFiles = setOf("Effect_Tick.ogg", "VideoRecord.ogg")

        val visible = SoundCatalog.targetsForDevice(deviceFiles, includeUnsupported = false)
        val withUnsupported = SoundCatalog.targetsForDevice(deviceFiles, includeUnsupported = true)

        assertEquals(setOf("tick", "record_start"), visible.map { it.id }.toSet())
        assertTrue(withUnsupported.first { it.id == "tick" }.isAvailableOnDevice)
        assertFalse(withUnsupported.first { it.id == "record_stop" }.isAvailableOnDevice)
    }

    @Test
    fun unknownDeviceFilesUseStablePlaceholderAndRemainExportable() {
        val fileName = "VendorSpecial.ogg"
        val target = SoundCatalog.targetsForDevice(setOf(fileName), includeUnsupported = false)
            .single { it.fileName == fileName }
        val custom = SoundAsset("custom", "Custom", "custom.ogg", setOf(target.id))

        assertEquals("未知系统声音", target.title)
        assertFalse(target.isMapped)
        assertTrue(target.isAvailableOnDevice)
        assertEquals("system:vendorspecial.ogg", target.id)
        assertEquals(
            listOf(target),
            resolveSelections(mapOf(target.id to custom.id), listOf(custom), listOf(target)).map { it.first },
        )
    }

    @Test
    fun referenceDeviceFilesAreCoveredByMappings() {
        val referenceFiles = setOf(
            "Camera_Timer.ogg",
            "Camera_Timer_2sec.ogg",
            "Dock.ogg",
            "Effect_Tick.ogg",
            "InCallNotification.ogg",
            "KeypressDelete.ogg",
            "KeypressInvalid.ogg",
            "KeypressReturn.ogg",
            "KeypressSpacebar.ogg",
            "KeypressStandard.ogg",
            "Lock.ogg",
            "LowBattery.ogg",
            "Screenshots.ogg",
            "Undock.ogg",
            "Unlock.ogg",
            "VideoRecord.ogg",
            "camera_click.ogg",
            "camera_click_start.ogg",
            "camera_click_stop.ogg",
            "camera_focus.ogg",
            "camera_shutter.ogg",
            "charging.ogg",
        )
        val mappings = SoundCatalog.mappings.associateBy { it.fileName.lowercase() }

        referenceFiles.forEach { assertNotNull("Missing mapping for $it", mappings[it.lowercase()]) }
    }
}
