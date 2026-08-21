# 系统声音工坊

一个使用 Jetpack Compose 编写的 Android 应用，用于选择系统 UI 音效并导出可由 Magisk / APatch 安装的替换模块。

## 功能

- 配置 AOSP 常见的触摸、按键、锁屏、充电、相机和录像等系统声音
- 内置 AOSP 音效，也可通过系统文件选择器导入 OGG
- 自定义声音支持多分类、重新分类、试听和删除
- 仅为非“默认值”的项目生成模块文件
- 导出标准 ZIP 模块，无需存储权限

## 构建

```powershell
.\gradlew.bat assembleDebug
```

需要 Android SDK 36 和 JDK 17 或更新版本。调试 APK 位于 `app/build/outputs/apk/debug/`。
