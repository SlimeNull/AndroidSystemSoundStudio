package com.slimenull.androidsystemsoundstudio.model

import java.util.Locale

enum class SoundGroup(val title: String) {
    TOUCH_INPUT("触控与输入"),
    LOCK_POWER("锁定与电源"),
    CAMERA_MEDIA("相机与媒体"),
    CONNECTIVITY("连接与通知"),
    OTHER("其他声音"),
}

enum class SoundIcon {
    TOUCH,
    KEYBOARD,
    SPACE,
    BACKSPACE,
    RETURN,
    LOCK,
    UNLOCK,
    CHARGING,
    BATTERY,
    CAMERA,
    TIMER,
    SCREENSHOT,
    VIDEO,
    STOP,
    DOCK,
    CALL,
    NFC,
    GENERIC,
}

enum class SoundColor {
    BLUE,
    GREEN,
    ORANGE,
    RED,
    PURPLE,
    TEAL,
    NEUTRAL,
}

data class SoundTarget(
    val id: String,
    val title: String,
    val description: String,
    val fileName: String,
    val group: SoundGroup,
    val icon: SoundIcon,
    val color: SoundColor,
    val builtInAssetName: String? = null,
    val isAvailableOnDevice: Boolean = true,
    val isMapped: Boolean = true,
)

data class SoundAsset(
    val id: String,
    val displayName: String,
    val storedFileName: String,
    val categories: Set<String>,
    val builtIn: Boolean = false,
)

object SoundCatalog {
    val defaultPlaceholder = SoundTarget(
        id = "placeholder",
        title = "未知系统声音",
        description = "设备中的未映射系统声音",
        fileName = "unknown.ogg",
        group = SoundGroup.OTHER,
        icon = SoundIcon.GENERIC,
        color = SoundColor.NEUTRAL,
        isMapped = false,
    )

    val mappings = listOf(
        mapping("tick", "Effect_Tick.ogg", "触摸提示音", "轻触屏幕控件时播放", SoundGroup.TOUCH_INPUT, SoundIcon.TOUCH, SoundColor.BLUE, "Effect_Tick.ogg"),
        mapping("keypress", "KeypressStandard.ogg", "按键音", "输入普通字符时播放", SoundGroup.TOUCH_INPUT, SoundIcon.KEYBOARD, SoundColor.GREEN, "KeypressStandard.ogg"),
        mapping("spacebar", "KeypressSpacebar.ogg", "空格键音", "按下空格键时播放", SoundGroup.TOUCH_INPUT, SoundIcon.SPACE, SoundColor.ORANGE, "KeypressSpacebar.ogg"),
        mapping("delete", "KeypressDelete.ogg", "删除键音", "按下删除键时播放", SoundGroup.TOUCH_INPUT, SoundIcon.BACKSPACE, SoundColor.RED, "KeypressDelete.ogg"),
        mapping("return", "KeypressReturn.ogg", "回车键音", "按下回车键时播放", SoundGroup.TOUCH_INPUT, SoundIcon.RETURN, SoundColor.PURPLE, "KeypressReturn.ogg"),
        mapping("keypress_invalid", "KeypressInvalid.ogg", "无效按键音", "按下无效按键时播放", SoundGroup.TOUCH_INPUT, SoundIcon.KEYBOARD, SoundColor.RED),

        mapping("lock", "Lock.ogg", "锁屏声音", "设备锁定时播放", SoundGroup.LOCK_POWER, SoundIcon.LOCK, SoundColor.PURPLE, "Lock.ogg"),
        mapping("unlock", "Unlock.ogg", "解锁声音", "设备解锁时播放", SoundGroup.LOCK_POWER, SoundIcon.UNLOCK, SoundColor.BLUE, "Unlock.ogg"),
        mapping("low_battery", "LowBattery.ogg", "低电量提示音", "电量不足时播放", SoundGroup.LOCK_POWER, SoundIcon.BATTERY, SoundColor.RED, "LowBattery.ogg"),
        mapping("charging", "ChargingStarted.ogg", "有线充电声音", "连接充电器时播放", SoundGroup.LOCK_POWER, SoundIcon.CHARGING, SoundColor.GREEN, "ChargingStarted.ogg"),
        mapping("wireless_charging", "WirelessChargingStarted.ogg", "无线充电声音", "开始无线充电时播放", SoundGroup.LOCK_POWER, SoundIcon.CHARGING, SoundColor.TEAL, "WirelessChargingStarted.ogg"),
        mapping("charging_oem", "charging.ogg", "充电提示音", "设备开始充电时播放", SoundGroup.LOCK_POWER, SoundIcon.CHARGING, SoundColor.GREEN),
        mapping("reverse_charging", "ChargingReverse.ogg", "反向充电声音", "开始反向充电时播放", SoundGroup.LOCK_POWER, SoundIcon.CHARGING, SoundColor.ORANGE),
        mapping("trusted", "Trusted.ogg", "可信设备提示音", "设备进入可信状态时播放", SoundGroup.LOCK_POWER, SoundIcon.UNLOCK, SoundColor.TEAL),

        mapping("camera", "camera_click.ogg", "相机拍照音", "拍摄照片时播放", SoundGroup.CAMERA_MEDIA, SoundIcon.CAMERA, SoundColor.BLUE, "camera_click.ogg"),
        mapping("camera_click_start", "camera_click_start.ogg", "相机操作开始音", "相机开始拍摄操作时播放", SoundGroup.CAMERA_MEDIA, SoundIcon.CAMERA, SoundColor.GREEN),
        mapping("camera_click_stop", "camera_click_stop.ogg", "相机操作结束音", "相机结束拍摄操作时播放", SoundGroup.CAMERA_MEDIA, SoundIcon.CAMERA, SoundColor.RED),
        mapping("camera_focus", "camera_focus.ogg", "相机对焦音", "相机完成对焦时播放", SoundGroup.CAMERA_MEDIA, SoundIcon.CAMERA, SoundColor.TEAL),
        mapping("camera_shutter", "camera_shutter.ogg", "相机快门声", "触发相机快门时播放", SoundGroup.CAMERA_MEDIA, SoundIcon.CAMERA, SoundColor.PURPLE),
        mapping("camera_timer", "Camera_Timer.ogg", "相机倒计时声音", "相机倒计时期间播放", SoundGroup.CAMERA_MEDIA, SoundIcon.TIMER, SoundColor.ORANGE),
        mapping("camera_timer_2sec", "Camera_Timer_2sec.ogg", "相机两秒倒计时声音", "相机两秒倒计时期间播放", SoundGroup.CAMERA_MEDIA, SoundIcon.TIMER, SoundColor.ORANGE),
        mapping("record_start", "VideoRecord.ogg", "录像开始声音", "开始录像时播放", SoundGroup.CAMERA_MEDIA, SoundIcon.VIDEO, SoundColor.GREEN, "VideoRecord.ogg"),
        mapping("record_stop", "VideoStop.ogg", "录像停止声音", "停止录像时播放", SoundGroup.CAMERA_MEDIA, SoundIcon.STOP, SoundColor.RED, "VideoStop.ogg"),
        mapping("screenshots", "Screenshots.ogg", "截屏声音", "保存屏幕截图时播放", SoundGroup.CAMERA_MEDIA, SoundIcon.SCREENSHOT, SoundColor.BLUE),
        mapping("screenshot", "screenshot.ogg", "截屏声音", "保存屏幕截图时播放", SoundGroup.CAMERA_MEDIA, SoundIcon.SCREENSHOT, SoundColor.BLUE),

        mapping("dock", "Dock.ogg", "连接底座声音", "设备连接底座时播放", SoundGroup.CONNECTIVITY, SoundIcon.DOCK, SoundColor.BLUE, "Dock.ogg"),
        mapping("undock", "Undock.ogg", "移除底座声音", "设备离开底座时播放", SoundGroup.CONNECTIVITY, SoundIcon.DOCK, SoundColor.PURPLE, "Undock.ogg"),
        mapping("incall_notification", "InCallNotification.ogg", "通话内通知音", "通话期间收到通知时播放", SoundGroup.CONNECTIVITY, SoundIcon.CALL, SoundColor.TEAL),
        mapping("nfc_failure", "NFCFailure.ogg", "NFC 失败提示音", "NFC 操作失败时播放", SoundGroup.CONNECTIVITY, SoundIcon.NFC, SoundColor.RED),
        mapping("nfc_initiated", "NFCInitiated.ogg", "NFC 开始提示音", "NFC 操作开始时播放", SoundGroup.CONNECTIVITY, SoundIcon.NFC, SoundColor.BLUE),
        mapping("nfc_success", "NFCSuccess.ogg", "NFC 成功提示音", "NFC 操作成功时播放", SoundGroup.CONNECTIVITY, SoundIcon.NFC, SoundColor.GREEN),
        mapping("nfc_transfer_complete", "NFCTransferComplete.ogg", "NFC 传输完成音", "NFC 传输完成时播放", SoundGroup.CONNECTIVITY, SoundIcon.NFC, SoundColor.GREEN),
        mapping("nfc_transfer_initiated", "NFCTransferInitiated.ogg", "NFC 传输开始音", "NFC 传输开始时播放", SoundGroup.CONNECTIVITY, SoundIcon.NFC, SoundColor.BLUE),
    )

    private val mappingsByFileName = mappings.associateBy { normalizeFileName(it.fileName) }

    fun targetsForDevice(
        deviceFileNames: Set<String>?,
        includeUnsupported: Boolean,
    ): List<SoundTarget> {
        if (deviceFileNames == null) return mappings

        val actualNamesByLowercase = deviceFileNames.associateBy(::normalizeFileName)
        val mappedTargets = mappings.mapNotNull { mapping ->
            val actualName = actualNamesByLowercase[normalizeFileName(mapping.fileName)]
            when {
                actualName != null -> mapping.copy(fileName = actualName, isAvailableOnDevice = true)
                includeUnsupported -> mapping.copy(isAvailableOnDevice = false)
                else -> null
            }
        }
        val unknownTargets = deviceFileNames
            .filterNot { mappingsByFileName.containsKey(normalizeFileName(it)) }
            .sortedBy(::normalizeFileName)
            .map(::unknownTarget)
        return mappedTargets + unknownTargets
    }

    fun builtIns(): List<SoundAsset> = mappings.mapNotNull { target ->
        val assetName = target.builtInAssetName ?: return@mapNotNull null
        SoundAsset(
            id = "aosp_${target.id}",
            displayName = "AOSP · ${target.title}",
            storedFileName = assetName,
            categories = setOf(target.id),
            builtIn = true,
        )
    }

    private fun mapping(
        id: String,
        fileName: String,
        title: String,
        description: String,
        group: SoundGroup,
        icon: SoundIcon,
        color: SoundColor,
        builtInAssetName: String? = null,
    ) = SoundTarget(
        id = id,
        title = title,
        description = description,
        fileName = fileName,
        group = group,
        icon = icon,
        color = color,
        builtInAssetName = builtInAssetName,
    )

    private fun unknownTarget(fileName: String) = defaultPlaceholder.copy(
        id = "system:${normalizeFileName(fileName)}",
        fileName = fileName,
    )

    private fun normalizeFileName(fileName: String) = fileName.lowercase(Locale.ROOT)
}
