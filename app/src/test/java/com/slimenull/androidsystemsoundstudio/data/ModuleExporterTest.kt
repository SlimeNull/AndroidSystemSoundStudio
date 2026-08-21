package com.slimenull.androidsystemsoundstudio.data

import com.slimenull.androidsystemsoundstudio.model.SoundAsset
import com.slimenull.androidsystemsoundstudio.model.SoundCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleExporterTest {
    @Test
    fun defaultSelectionsProduceNoFiles() {
        assertTrue(resolveSelections(emptyMap(), SoundCatalog.builtIns()).isEmpty())
    }

    @Test
    fun builtInSoundsOnlyBelongToTheirOwnCategory() {
        val builtIns = SoundCatalog.builtIns()

        assertTrue(builtIns.all { it.categories.size == 1 })
        assertEquals(SoundCatalog.targets.map { it.id }.toSet(), builtIns.flatMap { it.categories }.toSet())
    }

    @Test
    fun crossCategoryOrMissingSelectionsAreIgnored() {
        val tick = SoundAsset("tick_sound", "Tick", "tick.ogg", setOf("tick"))
        val resolved = resolveSelections(
            selections = mapOf("tick" to tick.id, "lock" to tick.id, "unlock" to "missing"),
            sounds = listOf(tick),
        )

        assertEquals(listOf("tick"), resolved.map { it.first.id })
    }

    @Test
    fun customSoundCanResolveForMultipleCategories() {
        val custom = SoundAsset("custom", "Custom", "custom.ogg", setOf("tick", "lock"))
        val resolved = resolveSelections(
            selections = mapOf("tick" to custom.id, "lock" to custom.id),
            sounds = listOf(custom),
        )

        assertEquals(setOf("tick", "lock"), resolved.map { it.first.id }.toSet())
    }
}
