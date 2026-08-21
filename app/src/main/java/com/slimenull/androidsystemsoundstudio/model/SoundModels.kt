package com.slimenull.androidsystemsoundstudio.model

data class SoundTarget(
    val id: String,
    val title: String,
    val description: String,
    val fileName: String,
)

data class SoundAsset(
    val id: String,
    val displayName: String,
    val storedFileName: String,
    val categories: Set<String>,
    val builtIn: Boolean = false,
)

object SoundCatalog {
    val targets = listOf(
        SoundTarget("tick", "触摸提示音", "轻触屏幕控件时播放", "Effect_Tick.ogg"),
        SoundTarget("keypress", "按键音", "输入普通字符时播放", "KeypressStandard.ogg"),
        SoundTarget("spacebar", "空格键音", "按下空格键时播放", "KeypressSpacebar.ogg"),
        SoundTarget("delete", "删除键音", "按下删除键时播放", "KeypressDelete.ogg"),
        SoundTarget("return", "回车键音", "按下回车键时播放", "KeypressReturn.ogg"),
        SoundTarget("lock", "锁屏声音", "设备锁定时播放", "Lock.ogg"),
        SoundTarget("unlock", "解锁声音", "设备解锁时播放", "Unlock.ogg"),
        SoundTarget("charging", "有线充电声音", "连接充电器时播放", "ChargingStarted.ogg"),
        SoundTarget("wireless_charging", "无线充电声音", "开始无线充电时播放", "WirelessChargingStarted.ogg"),
        SoundTarget("camera", "相机快门声", "拍摄照片时播放", "camera_click.ogg"),
        SoundTarget("record_start", "录像开始声音", "开始录像时播放", "VideoRecord.ogg"),
        SoundTarget("record_stop", "录像结束声音", "结束录像时播放", "VideoStop.ogg"),
        SoundTarget("low_battery", "低电量提示音", "电量不足时播放", "LowBattery.ogg"),
        SoundTarget("dock", "连接底座声音", "设备连接底座时播放", "Dock.ogg"),
        SoundTarget("undock", "移除底座声音", "设备离开底座时播放", "Undock.ogg"),
    )

    fun builtIns(): List<SoundAsset> = targets.map { target ->
        SoundAsset(
            id = "aosp_${target.id}",
            displayName = "AOSP · ${target.title}",
            storedFileName = target.fileName,
            categories = setOf(target.id),
            builtIn = true,
        )
    }
}
