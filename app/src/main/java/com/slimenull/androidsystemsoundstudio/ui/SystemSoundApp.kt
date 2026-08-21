package com.slimenull.androidsystemsoundstudio.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slimenull.androidsystemsoundstudio.AppViewModel
import com.slimenull.androidsystemsoundstudio.BuildConfig
import com.slimenull.androidsystemsoundstudio.model.SoundAsset
import com.slimenull.androidsystemsoundstudio.model.SoundCatalog
import com.slimenull.androidsystemsoundstudio.model.SoundTarget
import kotlinx.coroutines.launch

private enum class Screen { Home, SoundManager }

private data class SoundSection(val title: String, val targetIds: Set<String>)

private val soundSections = listOf(
    SoundSection("触控与输入", setOf("tick", "keypress", "spacebar", "delete", "return")),
    SoundSection("锁定与电源", setOf("lock", "unlock", "charging", "wireless_charging", "low_battery")),
    SoundSection("相机与录像", setOf("camera", "record_start", "record_stop")),
    SoundSection("底座", setOf("dock", "undock")),
)

@Composable
fun SystemSoundApp(viewModel: AppViewModel) {
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    BackHandler(enabled = screen == Screen.SoundManager) {
        screen = Screen.Home
    }

    SystemSoundTheme {
        when (screen) {
            Screen.Home -> HomeScreen(
                viewModel = viewModel,
                snackbar = snackbar,
                onManage = { screen = Screen.SoundManager },
                onAbout = { showAbout = true },
            )
            Screen.SoundManager -> SoundManagerScreen(
                viewModel = viewModel,
                snackbar = snackbar,
                onBack = { screen = Screen.Home },
            )
        }
        if (showAbout) AboutDialog { showAbout = false }
        if (viewModel.uiState.pendingImport != null || viewModel.uiState.editingSound != null) {
            CategoryDialog(viewModel, snackbar)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    viewModel: AppViewModel,
    snackbar: SnackbarHostState,
    onManage: () -> Unit,
    onAbout: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) {
            viewModel.export(uri).fold(
                onSuccess = { snackbar.launchMessage("已导出模块，共包含 $it 项声音替换") },
                onFailure = { snackbar.launchMessage(it.message ?: "导出失败") },
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("系统声音工坊", fontWeight = FontWeight.SemiBold)
                        Text(
                            "选择并导出系统 UI 音效",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("声音管理") },
                                leadingIcon = { Icon(Icons.Rounded.LibraryMusic, null) },
                                onClick = { menuExpanded = false; onManage() },
                            )
                            DropdownMenuItem(
                                text = { Text("关于") },
                                leadingIcon = { Icon(Icons.Rounded.Info, null) },
                                onClick = { menuExpanded = false; onAbout() },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("导出模块") },
                icon = { Icon(Icons.Rounded.FileDownload, null) },
                onClick = { exportLauncher.launch("SystemSoundStudio-${BuildConfig.VERSION_NAME}.zip") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            soundSections.forEach { section ->
                item(key = "header_${section.title}") {
                    Text(
                        section.title,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
                items(
                    items = SoundCatalog.targets.filter { it.id in section.targetIds },
                    key = { it.id },
                ) { target ->
                    SoundTargetRow(target, viewModel)
                }
            }
        }
    }
}

@Composable
private fun SoundTargetRow(target: SoundTarget, viewModel: AppViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val choices = viewModel.allSounds.filter { target.id in it.categories }
    val selected = choices.firstOrNull { it.id == viewModel.uiState.selections[target.id] }
    ListItem(
        headlineContent = { Text(target.title, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Column {
                Text(target.description)
                Text(
                    "/product/media/audio/ui/${target.fileName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailingContent = {
            Box {
                Surface(
                    modifier = Modifier.width(148.dp).clickable { expanded = true },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        selected?.displayName ?: "默认值",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    SoundChoiceItem("默认值", selected == null) {
                        viewModel.select(target.id, null); expanded = false
                    }
                    choices.forEach { sound ->
                        SoundChoiceItem(sound.displayName, sound.id == selected?.id) {
                            viewModel.select(target.id, sound.id); expanded = false
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun SoundChoiceItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = { if (selected) Icon(Icons.Rounded.Check, null) },
        onClick = onClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundManagerScreen(
    viewModel: AppViewModel,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var deleting by remember { mutableStateOf<SoundAsset?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = context.displayName(uri)
            if (!name.endsWith(".ogg", ignoreCase = true)) {
                snackbar.launchMessage("请选择 OGG 格式的音频文件")
            } else {
                viewModel.beginImport(uri, name)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("声音管理", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { importLauncher.launch(arrayOf("audio/ogg", "application/ogg", "audio/*")) },
                        contentPadding = PaddingValues(horizontal = 14.dp),
                    ) {
                        Icon(Icons.Rounded.FileUpload, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("导入")
                    }
                    Spacer(Modifier.width(8.dp))
                },
            )
        },
    ) { padding ->
        if (viewModel.uiState.customSounds.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Rounded.LibraryMusic,
                    null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(16.dp))
                Text("还没有导入声音", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "导入 OGG 文件后，可在这里维护它所属的声音分类。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(viewModel.uiState.customSounds, key = { it.id }) { sound ->
                    ListItem(
                        headlineContent = { Text(sound.displayName, fontWeight = FontWeight.Medium) },
                        supportingContent = {
                            Text(
                                sound.categories.mapNotNull { id ->
                                    SoundCatalog.targets.firstOrNull { it.id == id }?.title
                                }.joinToString("、"),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = {
                                    viewModel.preview(sound).exceptionOrNull()?.let {
                                        snackbar.launchMessage(it.message ?: "无法播放该声音")
                                    }
                                }) { Icon(Icons.Rounded.PlayArrow, contentDescription = "试听") }
                                IconButton(onClick = { viewModel.editCategories(sound) }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "变更分类")
                                }
                                IconButton(onClick = { deleting = sound }) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    deleting?.let { sound ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            icon = { Icon(Icons.Rounded.Delete, null) },
            title = { Text("删除声音？") },
            text = { Text("“${sound.displayName}”将从应用中移除，使用它的配置会恢复为默认值。") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(sound); deleting = null }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun CategoryDialog(viewModel: AppViewModel, snackbar: SnackbarHostState) {
    val initial = viewModel.uiState.editingSound?.categories.orEmpty()
    var selected by remember(viewModel.uiState.pendingImport, viewModel.uiState.editingSound) {
        mutableStateOf(initial)
    }
    AlertDialog(
        onDismissRequest = viewModel::cancelCategoryEdit,
        title = { Text(if (viewModel.uiState.pendingImport != null) "选择声音分类" else "变更声音分类") },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { selected = SoundCatalog.targets.map { it.id }.toSet() }) {
                        Icon(Icons.Rounded.SelectAll, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("全选")
                    }
                    TextButton(onClick = { selected = emptySet() }) {
                        Icon(Icons.Rounded.Close, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("全不选")
                    }
                }
                LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    items(SoundCatalog.targets, key = { it.id }) { target ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selected = if (target.id in selected) selected - target.id else selected + target.id
                            }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = target.id in selected,
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + target.id else selected - target.id
                                },
                            )
                            Text(target.title)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.confirmCategories(selected).exceptionOrNull()?.let {
                    snackbar.launchMessage(it.message ?: "保存失败")
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = viewModel::cancelCategoryEdit) { Text("取消") } },
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Info, null) },
        title = { Text("关于系统声音工坊") },
        text = {
            Column {
                Text("版本 ${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Medium)
                Text("作者 SlimeNull", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text("为常见 Android 系统 UI 音效生成 Magisk / APatch 模块。导出的模块仅包含已选择的替换项。")
                Spacer(Modifier.height(12.dp))
                Text(
                    "不同 ROM 的声音文件名可能存在差异。安装前建议确认设备使用的是 /product/media/audio/ui 路径。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } },
    )
}

private fun SnackbarHostState.launchMessage(message: String) {
    // The call always originates from composition event callbacks on the main thread.
    kotlinx.coroutines.MainScope().launch { showSnackbar(message) }
}

private fun Context.displayName(uri: Uri): String {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) return cursor.getString(0)
    }
    return uri.lastPathSegment ?: "sound.ogg"
}
