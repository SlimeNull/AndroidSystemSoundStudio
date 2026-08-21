package com.slimenull.androidsystemsoundstudio.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.slimenull.androidsystemsoundstudio.AppViewModel
import com.slimenull.androidsystemsoundstudio.BuildConfig
import com.slimenull.androidsystemsoundstudio.model.SoundAsset
import com.slimenull.androidsystemsoundstudio.model.SoundCatalog
import com.slimenull.androidsystemsoundstudio.model.SoundTarget
import kotlinx.coroutines.launch

private const val HOME_ROUTE = "home"
private const val SOUND_MANAGER_ROUTE = "sound_manager"
private const val PAGE_TRANSITION_MILLIS = 280

private data class SoundSection(val title: String, val targetIds: Set<String>)

private val soundSections = listOf(
    SoundSection("触控与输入", setOf("tick", "keypress", "spacebar", "delete", "return")),
    SoundSection("锁定与电源", setOf("lock", "unlock", "charging", "wireless_charging", "low_battery")),
    SoundSection("相机与录像", setOf("camera", "record_start", "record_stop")),
    SoundSection("底座", setOf("dock", "undock")),
)

@Composable
fun SystemSoundApp(viewModel: AppViewModel) {
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val navController = rememberNavController()

    SystemSoundTheme {
        NavHost(
            navController = navController,
            startDestination = HOME_ROUTE,
            enterTransition = {
                slideInHorizontally(
                    animationSpec = tween(PAGE_TRANSITION_MILLIS),
                    initialOffsetX = { it / 4 },
                ) + fadeIn(animationSpec = tween(PAGE_TRANSITION_MILLIS))
            },
            exitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(PAGE_TRANSITION_MILLIS),
                    targetOffsetX = { -it / 10 },
                ) + fadeOut(animationSpec = tween(PAGE_TRANSITION_MILLIS / 2))
            },
            popEnterTransition = {
                slideInHorizontally(
                    animationSpec = tween(PAGE_TRANSITION_MILLIS),
                    initialOffsetX = { -it / 10 },
                ) + fadeIn(animationSpec = tween(PAGE_TRANSITION_MILLIS))
            },
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(PAGE_TRANSITION_MILLIS),
                    targetOffsetX = { it / 4 },
                ) + fadeOut(animationSpec = tween(PAGE_TRANSITION_MILLIS / 2))
            },
        ) {
            composable(HOME_ROUTE) {
                HomeScreen(
                    viewModel = viewModel,
                    snackbar = snackbar,
                    onManage = {
                        navController.navigate(SOUND_MANAGER_ROUTE) {
                            launchSingleTop = true
                        }
                    },
                    onAbout = { showAbout = true },
                )
            }
            composable(SOUND_MANAGER_ROUTE) {
                SoundManagerScreen(
                    viewModel = viewModel,
                    snackbar = snackbar,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        if (showAbout) AboutDialog { showAbout = false }
        if (viewModel.uiState.pendingImport != null || viewModel.uiState.editingSound != null) {
            CategoryDialog(viewModel, snackbar)
        }
    }
}

@Preview
@Composable
fun SystemSoundAppPreview() {
    val appViewModel: AppViewModel = viewModel()
    SystemSoundApp(appViewModel)
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
                        Text(
                            "系统声音工坊",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "选择并导出系统 UI 音效",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    Box {
                        FilledTonalIconButton(onClick = { menuExpanded = true }) {
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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 112.dp),
        ) {
            soundSections.forEach { section ->
                item(key = "header_${section.title}") {
                    SoundSectionHeader(section.title)
                }
                items(
                    items = SoundCatalog.targets.filter { it.id in section.targetIds },
                    key = { it.id },
                ) { target ->
                    SoundTargetRow(target, viewModel, snackbar)
                }
            }
        }
    }
}

@Composable
private fun SoundSectionHeader(title: String) {
    Row(
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Apps,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SoundTargetRow(
    target: SoundTarget,
    viewModel: AppViewModel,
    snackbar: SnackbarHostState,
) {
    var expanded by remember { mutableStateOf(false) }
    val choices = viewModel.allSounds.filter { target.id in it.categories }
    val selected = choices.firstOrNull { it.id == viewModel.uiState.selections[target.id] }
    val previewSound = selected ?: choices.firstOrNull { it.builtIn }
    val visual = soundVisual(target.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 128.dp)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(visual.containerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    visual.icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = visual.contentColor,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    target.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    target.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    target.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                SilentPreviewButton(
                    enabled = previewSound != null,
                    containerColor = visual.containerColor,
                    contentColor = visual.contentColor,
                    onClick = {
                        previewSound?.let(viewModel::preview)?.exceptionOrNull()?.let {
                            snackbar.launchMessage(it.message ?: "无法播放该声音")
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.width(104.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                    ) {
                        Text(
                            selected?.displayName ?: "默认值",
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            Icons.Rounded.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        SoundChoiceItem("默认值", selected == null) {
                            viewModel.select(target.id, null)
                            expanded = false
                        }
                        choices.forEach { sound ->
                            SoundChoiceItem(sound.displayName, sound.id == selected?.id) {
                                viewModel.select(target.id, sound.id)
                                expanded = false
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class SoundVisual(
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
)

@Composable
private fun soundVisual(targetId: String): SoundVisual {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val palette = when (targetId) {
        "tick", "camera", "dock" -> if (isDark) Color(0xFF183D70) to Color(0xFFAEC7FF) else Color(0xFFE7EDFF) to Color(0xFF1657B8)
        "keypress", "charging", "record_start" -> if (isDark) Color(0xFF173D2B) to Color(0xFF96D9AD) else Color(0xFFE4F3E9) to Color(0xFF087B3C)
        "spacebar", "wireless_charging" -> if (isDark) Color(0xFF4A3012) to Color(0xFFFFC979) else Color(0xFFFFEFD9) to Color(0xFFC96D00)
        "delete", "low_battery", "record_stop" -> if (isDark) Color(0xFF531F28) to Color(0xFFFFB2BC) else Color(0xFFFCE5E9) to Color(0xFFBD3041)
        else -> if (isDark) Color(0xFF35275E) to Color(0xFFCBBEFF) else Color(0xFFEDE7FF) to Color(0xFF4930B8)
    }
    val icon = when (targetId) {
        "tick" -> Icons.Rounded.TouchApp
        "keypress" -> Icons.Rounded.Keyboard
        "spacebar" -> Icons.Rounded.SpaceBar
        "delete" -> Icons.AutoMirrored.Rounded.Backspace
        "return" -> Icons.AutoMirrored.Rounded.KeyboardReturn
        "lock" -> Icons.Rounded.Lock
        "unlock" -> Icons.Rounded.LockOpen
        "charging", "wireless_charging" -> Icons.Rounded.BatteryChargingFull
        "low_battery" -> Icons.Rounded.BatteryAlert
        "camera" -> Icons.Rounded.CameraAlt
        "record_start" -> Icons.Rounded.Videocam
        "record_stop" -> Icons.Rounded.StopCircle
        "dock", "undock" -> Icons.Rounded.Dock
        else -> Icons.Rounded.LibraryMusic
    }
    return SoundVisual(icon, palette.first, palette.second)
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
            CenterAlignedTopAppBar(
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
                Spacer(Modifier.height(24.dp))
                FilledTonalButton(
                    onClick = { importLauncher.launch(arrayOf("audio/ogg", "application/ogg", "audio/*")) },
                ) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("导入声音")
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(viewModel.uiState.customSounds, key = { it.id }) { sound ->
                    Column {
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SilentPreviewButton(
                                        enabled = true,
                                        onClick = {
                                            viewModel.preview(sound).exceptionOrNull()?.let {
                                                snackbar.launchMessage(it.message ?: "无法播放该声音")
                                            }
                                        },
                                    )
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
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
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
private fun SilentPreviewButton(
    enabled: Boolean,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    onClick: () -> Unit,
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val interactionSource = remember { MutableInteractionSource() }
    val indication = LocalIndication.current
    val pointerModifier = if (enabled) {
        Modifier.pointerInput(interactionSource) {
            detectTapGestures(
                onPress = { position ->
                    val press = PressInteraction.Press(position)
                    interactionSource.emit(press)
                    if (tryAwaitRelease()) {
                        interactionSource.emit(PressInteraction.Release(press))
                    } else {
                        interactionSource.emit(PressInteraction.Cancel(press))
                    }
                },
                onTap = { currentOnClick() },
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (enabled) {
                    containerColor
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
            )
            .indication(interactionSource, indication)
            .then(pointerModifier)
            .semantics {
                role = Role.Button
                contentDescription = if (enabled) "试听" else "默认值无法试听"
                if (enabled) {
                    onClick(label = "试听") {
                        currentOnClick()
                        true
                    }
                } else {
                    disabled()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = if (enabled) {
                contentColor
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
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
