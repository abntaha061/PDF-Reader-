package com.example.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.input.nestedscroll.nestedScroll
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfAppContent(viewModel: PdfViewModel) {
    val currentDoc by viewModel.currentDocument.collectAsState()
    val isLocked by viewModel.isDocumentLocked.collectAsState()
    val rawDocuments by viewModel.documents.collectAsState()
    
    val viewMode by viewModel.viewMode.collectAsState()
    val zoomScale by viewModel.zoomScale.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isReflowMode by viewModel.isReflowMode.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    
    val activeAnnotation by viewModel.activeAnnotationType.collectAsState()
    val signaturePoints by viewModel.signaturePoints.collectAsState()
    
    val selectedTextId by viewModel.selectedTextBlockId.collectAsState()
    val selectedImageId by viewModel.selectedImageBlockId.collectAsState()
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isRegexSearch by viewModel.isRegexSearch.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val currentSearchIdx by viewModel.currentSearchResultIndex.collectAsState()
    
    val geminiResult by viewModel.geminiResult.collectAsState()
    val isGeminiLoading by viewModel.isGeminiLoading.collectAsState()
    
    val speakingBlockId by viewModel.currentReadBlockId.collectAsState()
    val currentPlayingAudio by viewModel.currentPlayingAudio.collectAsState()
    
    val context = LocalContext.current
    val pageListState = rememberLazyListState()
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.openDocumentFromUri(context, uri)
        }
    }
    val activePageIndex by remember(currentDoc) {
        derivedStateOf {
            if (currentDoc != null && currentDoc!!.pages.isNotEmpty()) {
                pageListState.firstVisibleItemIndex.coerceIn(0, currentDoc!!.pages.lastIndex)
            } else {
                0
            }
        }
    }
    var customApiKey by remember { mutableStateOf(SecureApiKeyStorage.getApiKey(context)) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.initTts(context)
        viewModel.initDatabase(context)
    }

    // Modal dialog trigger states
    var showTextEditDialog by remember { mutableStateOf<Pair<Int, PdfTextBlock>?>(null) }
    var showMetadataDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var showTopToolbar by remember { mutableStateOf(true) }
    var showGeminiSheet by remember { mutableStateOf(false) }
    var showSplitDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showConvertDialog by remember { mutableStateOf(false) }
    var showWatermarkDialog by remember { mutableStateOf(false) }
    var showOcrDialog by remember { mutableStateOf(false) }
    var showCompressionDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Navigation drawers state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Permissions system
    val isStorageManagerGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        android.os.Environment.isExternalStorageManager()
    } else {
        true
    }
    
    var showPermissionDialog by remember { mutableStateOf(!isStorageManagerGranted) }

    if (showPermissionDialog) {
        Dialog(onDismissRequest = { }) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E293B) else Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = "Security Permission",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "طلب صلاحية الوصول للملفات",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "يتطلب تطبيق قارئ ومحرر الـ PDF صلاحية \"الوصول إلى جميع الملفات\" (All Files Access) لنتمكن من البحث في ذاكرة الجهاز وعرض مستنداتك وتعديل النصوص وحفظها بسلاسة كما تفعل التطبيقات الكبرى. يرجى تفعيل هذه الصلاحية في الصفحة التالية.",
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPermissionDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.Gray)
                        ) {
                            Text("إلغاء", fontSize = 14.sp)
                        }
                        
                        Button(
                            onClick = {
                                showPermissionDialog = false
                                try {
                                    val intent = android.content.Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION").apply {
                                        addCategory(android.content.Intent.CATEGORY_DEFAULT)
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val intent = android.content.Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION")
                                        context.startActivity(intent)
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                        ) {
                            Text("موافق وتفعيل", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    var activeBottomTab by remember { mutableStateOf("recent") }
    var recentSubTab by remember { mutableStateOf("recent") }

    var isAudioLoading by remember { mutableStateOf(false) }
    var activeAudioPlaying by remember { mutableStateOf<PdfAnnotation.AudioAnnotation?>(null) }
    var isAudioPlayingState by remember { mutableStateOf(false) }
    var audioProgress by remember { mutableStateOf(0f) }
    var audioVolume by remember { mutableStateOf(0.8f) }

    LaunchedEffect(isAudioLoading) {
        if (isAudioLoading) {
            kotlinx.coroutines.delay(1200)
            isAudioLoading = false
        }
    }

    LaunchedEffect(activeAudioPlaying, isAudioPlayingState) {
        if (activeAudioPlaying != null && isAudioPlayingState) {
            audioProgress = 0f
            val durationMs = 4000
            val intervalMs = 100
            val steps = durationMs / intervalMs
            for (i in 1..steps) {
                if (!isAudioPlayingState) break
                kotlinx.coroutines.delay(intervalMs.toLong())
                audioProgress = i.toFloat() / steps
            }
            if (isAudioPlayingState && audioProgress >= 1f) {
                isAudioPlayingState = false
                kotlinx.coroutines.delay(500)
                activeAudioPlaying = null
                viewModel.stopAudioAnnotation()
            }
        }
    }

    if (currentDoc == null) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = activeBottomTab == "recent",
                        onClick = { activeBottomTab = "recent" },
                        icon = { Icon(Icons.Filled.History, contentDescription = "أخير") },
                        label = { Text("أخير", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFC62828),
                            selectedTextColor = Color(0xFFC62828),
                            indicatorColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFFFEBEE)
                        )
                    )
                    NavigationBarItem(
                        selected = activeBottomTab == "files",
                        onClick = { activeBottomTab = "files" },
                        icon = { Icon(Icons.Filled.FolderOpen, contentDescription = "الملفات") },
                        label = { Text("الملفات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1E88E5),
                            selectedTextColor = Color(0xFF1E88E5),
                            indicatorColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFE3F2FD)
                        )
                    )
                    NavigationBarItem(
                        selected = activeBottomTab == "discover",
                        onClick = { activeBottomTab = "discover" },
                        icon = { Icon(Icons.Filled.Explore, contentDescription = "اكتشاف") },
                        label = { Text("اكتشاف", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00897B),
                            selectedTextColor = Color(0xFF00897B),
                            indicatorColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFE0F2F1)
                        )
                    )
                    NavigationBarItem(
                        selected = activeBottomTab == "premium",
                        onClick = { activeBottomTab = "premium" },
                        icon = { Icon(Icons.Filled.OfflineBolt, contentDescription = "Premium") },
                        label = { Text("Premium", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFBC02D),
                            selectedTextColor = Color(0xFFFBC02D),
                            indicatorColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFFFFDE7)
                        )
                    )
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("فتح ملف PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    icon = { Icon(Icons.Filled.FolderOpen, contentDescription = "Open Document", tint = Color.White) },
                    onClick = {
                        try {
                            fileLauncher.launch(arrayOf("application/pdf"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    containerColor = Color(0xFFC62828),
                    modifier = Modifier.padding(bottom = 12.dp, end = 4.dp)
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC))
            ) {
                when (activeBottomTab) {
                    "recent" -> HomeRecentTab(
                        viewModel = viewModel,
                        rawDocuments = rawDocuments,
                        isDarkMode = isDarkMode,
                        recentSubTab = recentSubTab,
                        onSubTabChange = { recentSubTab = it },
                        onFileClick = { viewModel.selectDocument(it) },
                        onCreateClick = { showConvertDialog = true }
                    )
                    "files" -> HomeFilesTab(
                        viewModel = viewModel,
                        rawDocuments = rawDocuments,
                        isDarkMode = isDarkMode,
                        onFileClick = { viewModel.selectDocument(it) }
                    )
                    "discover" -> HomeDiscoverTab(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode,
                        onMergeClick = { showMergeDialog = true },
                        onSplitClick = { showSplitDialog = true },
                        onOcrClick = { showOcrDialog = true },
                        onCompressClick = { showCompressionDialog = true },
                        onWatermarkClick = { showWatermarkDialog = true },
                        onMetadataClick = { showMetadataDialog = true },
                        openBilingualGuide = {
                            val guide = rawDocuments.find { it.name.contains("Bilingual", ignoreCase = true) }
                            if (guide != null) {
                                viewModel.selectDocument(guide)
                            }
                        }
                    )
                    "premium" -> HomePremiumTab(isDarkMode = isDarkMode)
                }
            }
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.width(320.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                // Beautiful Crimson Styled Sidebar Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.MenuBook,
                        contentDescription = "Logo",
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "PDF Reader Pro",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "DOCUMENTS CENTRAL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                val dbSavedFiles by viewModel.dbSavedFiles.collectAsState()
                val dbFavorites by viewModel.dbFavorites.collectAsState()

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    item {
                        Text(
                            text = "ACTIVE DOCUMENTS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    items(rawDocuments) { doc ->
                        val isSelected = currentDoc?.id == doc.id
                        val isFavActive = dbFavorites.any { it.id == doc.id }
                        
                        NavigationDrawerItem(
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = doc.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${doc.pages.size} Pages • ${doc.metadata.author}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    IconButton(onClick = { viewModel.toggleFavoriteInDb(doc.id, isFavActive) }) {
                                        Icon(
                                            imageVector = if (isFavActive) Icons.Filled.Star else Icons.Filled.StarBorder,
                                            tint = if (isFavActive) Color(0xFFFBC02D) else Color.Gray,
                                            contentDescription = "Favorite",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            selected = isSelected,
                            onClick = {
                                viewModel.selectDocument(doc)
                            },
                            icon = {
                                Icon(
                                    imageVector = if (doc.security.isEncrypted) Icons.Filled.Lock else Icons.Filled.PictureAsPdf,
                                    tint = if (isSelected) Color(0xFFC62828) else Color.Gray,
                                    contentDescription = "Doc"
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Color(0xFFFFEBEE),
                                unselectedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    if (dbFavorites.isNotEmpty()) {
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "FAVORITES (المفضلة)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        items(dbFavorites) { fav ->
                            NavigationDrawerItem(
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(fav.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("By: ${fav.author}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        IconButton(onClick = { viewModel.toggleFavoriteInDb(fav.id, true) }) {
                                            Icon(Icons.Filled.Star, contentDescription = "Unstar", tint = Color(0xFFFBC02D), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                selected = currentDoc?.id == fav.id,
                                onClick = {
                                    val matched = rawDocuments.find { it.id == fav.id }
                                    if (matched != null) {
                                        viewModel.selectDocument(matched)
                                    }
                                },
                                icon = { Icon(Icons.Filled.Grade, tint = Color(0xFFFBC02D), contentDescription = null) },
                                colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0xFFFFEBEE), unselectedContainerColor = Color.Transparent),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }

                    if (dbSavedFiles.isNotEmpty()) {
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "RECENT ACTIVITY (سجل الملفات الأخيرة)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE53935),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        items(dbSavedFiles) { rec ->
                            val isFavorited = dbFavorites.any { it.id == rec.id }
                            NavigationDrawerItem(
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(rec.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("Opened recently", fontSize = 9.sp, color = Color.Gray)
                                        }
                                        IconButton(onClick = { viewModel.toggleFavoriteInDb(rec.id, isFavorited) }) {
                                            Icon(
                                                imageVector = if (isFavorited) Icons.Filled.Star else Icons.Filled.StarBorder,
                                                tint = if (isFavorited) Color(0xFFFBC02D) else Color.Gray,
                                                contentDescription = "Star",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                selected = currentDoc?.id == rec.id,
                                onClick = {
                                    val matched = rawDocuments.find { it.id == rec.id }
                                    if (matched != null) {
                                        viewModel.selectDocument(matched)
                                    }
                                },
                                icon = { Icon(Icons.Filled.History, tint = Color.Gray, contentDescription = null) },
                                colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0xFFFFEBEE), unselectedContainerColor = Color.Transparent),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "CONVERT & MANIPULATE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                // Navigation helpers for creating and converting
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Button(
                        onClick = { showConvertDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "New")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create PDF ...", fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { showMergeDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Merge, contentDescription = "Merge", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Merge", fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = { showSplitDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.CallSplit, contentDescription = "Split", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Split", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { showOcrDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFFFEBEE))
                        ) {
                            Icon(Icons.Filled.DocumentScanner, contentDescription = "OCR Scanner", modifier = Modifier.size(15.dp), tint = Color(0xFFC62828))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("OCR", fontSize = 11.sp, color = Color(0xFFC62828))
                        }

                        FilledTonalButton(
                            onClick = { showCompressionDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFFFEBEE))
                        ) {
                            Icon(Icons.Filled.Compress, contentDescription = "Compress API", modifier = Modifier.size(15.dp), tint = Color(0xFFC62828))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compress", fontSize = 11.sp, color = Color(0xFFC62828))
                        }

                        FilledTonalButton(
                            onClick = { showExportDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFFFEBEE))
                        ) {
                            Icon(Icons.Filled.ImportExport, contentDescription = "Export Formats", modifier = Modifier.size(15.dp), tint = Color(0xFFC62828))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", fontSize = 11.sp, color = Color(0xFFC62828))
                        }
                    }
                }

                if (currentDoc != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "PAGE & SHEETS MANAGEMENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )

                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { 
                                    viewModel.rotatePage(0) 
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Filled.RotateLeft, contentDescription = "Rot", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rotate P1", fontSize = 10.sp)
                            }
                            
                            FilledTonalButton(
                                onClick = { viewModel.insertBlankPage(1) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Filled.NoteAdd, contentDescription = "Ins", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Insert Blank", fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { viewModel.deletePage(1) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Filled.DeleteSweep, contentDescription = "Del", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete P2", fontSize = 10.sp)
                            }

                            FilledTonalButton(
                                onClick = { viewModel.movePage(1, -1) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Filled.SwapVert, contentDescription = "Mov", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Move P2 Up", fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { viewModel.splitByBookmark() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Filled.Bookmarks, contentDescription = "Book", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Split Bookmarks", fontSize = 10.sp)
                            }

                            FilledTonalButton(
                                onClick = { viewModel.splitBySize() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Filled.FormatSize, contentDescription = "Size", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Split by Size", fontSize = 10.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            // topBar removed to make the layout immersive under status bar
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .background(if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF3F4F9))
            ) {
                if (isLocked) {
                    // SECURE PASSWORD VERIFICATION BLOCK
                    SecurityPasswordUnlockOverlay(viewModel)
                } else if (currentDoc == null) {
                    // Empty state dashboard
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = "Empty",
                            tint = Color.Gray,
                            modifier = Modifier.size(96.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No PDF Document Loaded",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please select an existing document from the sidebar drawer, or convert a new custom PDF.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    val activeDoc = currentDoc!!
                    val nestedScrollConnection = remember {
                        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                            override fun onPreScroll(
                                available: androidx.compose.ui.geometry.Offset,
                                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
                            ): androidx.compose.ui.geometry.Offset {
                                if (available.y < -15f) {
                                    showTopToolbar = false
                                } else if (available.y > 15f) {
                                    showTopToolbar = true
                                }
                                return androidx.compose.ui.geometry.Offset.Zero
                            }
                        }
                    }
                    
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val parentWidth = maxWidth
                        val parentWidthValue = parentWidth.value
                        val widthDpVal = if (parentWidthValue.isFinite() && parentWidthValue > 0f) parentWidthValue else 400f
                        val availableWidth = (widthDpVal - 32f).coerceIn(100f, 1200f)
                        val firstPage = activeDoc.pages.firstOrNull()
                        val pageStandardWidth = (firstPage?.width ?: 1000).toFloat().coerceAtLeast(1f)
                        val fitScale = availableWidth / pageStandardWidth
                        val relativeScale = fitScale * zoomScale

                        // 1. IMMERSIVE CANVAS SHEET LAYER
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    // Toggling UI controls on empty-canvas clicks!
                                    showTopToolbar = !showTopToolbar
                                }
                        ) {
                            if (isReflowMode) {
                                // REFLOW MODE LAYOUT (Optimized scrolling text reading)
                                ReflowReadingPane(
                                    currentDoc = activeDoc,
                                    speakingBlockId = speakingBlockId,
                                    onSpeakSentence = { block -> viewModel.speakTextBlock(block) },
                                    onStopSpeak = { viewModel.stopTts() },
                                    isDarkMode = isDarkMode
                                )
                            } else {
                                // PHYSICAL GRID CANVAS SHEET
                                LazyColumn(
                                    state = pageListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .nestedScroll(nestedScrollConnection),
                                    contentPadding = PaddingValues(
                                        top = if (showTopToolbar) 310.dp else 40.dp,
                                        bottom = 100.dp,
                                        start = 16.dp,
                                        end = 16.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    items(activeDoc.pages) { page ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            PdfPageCanvasItem(
                                                page = page,
                                                zoomScale = relativeScale,
                                                currentPlayingAudio = activeAudioPlaying ?: currentPlayingAudio,
                                                onPlayAudioAnnotation = { audio ->
                                                     viewModel.playAudioAnnotation(audio)
                                                     isAudioLoading = true
                                                     activeAudioPlaying = audio
                                                     isAudioPlayingState = true
                                                     audioProgress = 0f
                                                 },
                                                isDarkMode = isDarkMode,
                                                isEditMode = isEditMode,
                                                selectedTextId = selectedTextId,
                                                selectedImageId = selectedImageId,
                                                searchResults = searchResults,
                                                currentSearchIdx = currentSearchIdx,
                                                activeAnnotation = activeAnnotation,
                                                signaturePoints = signaturePoints,
                                                onTextBlockClick = { block ->
                                                    if (isEditMode) {
                                                        viewModel.selectTextBlock(block.id)
                                                        showTextEditDialog = Pair(page.index, block)
                                                    } else {
                                                        viewModel.selectTextBlock(null)
                                                        when (activeAnnotation) {
                                                            AnnotationType.HIGHLIGHT -> viewModel.applyHighlightOnText(page.index, block)
                                                            AnnotationType.UNDERLINE -> viewModel.applyUnderlineOnText(page.index, block)
                                                            AnnotationType.STRIKETHROUGH -> viewModel.applyStrikethroughOnText(page.index, block)
                                                            AnnotationType.SQUIGGLY -> viewModel.applySquigglyOnText(page.index, block)
                                                            else -> {}
                                                        }
                                                    }
                                                },
                                                onImageBlockClick = { imageBlock ->
                                                    if (isEditMode) {
                                                        viewModel.selectImageBlock(imageBlock.id)
                                                    }
                                                },
                                                onHighlightWordClick = { block ->
                                                    viewModel.applyHighlightOnText(page.index, block)
                                                },
                                                onPageCanvasInteractiveDrag = { x, y ->
                                                    // Used for signatures
                                                    if (activeAnnotation == AnnotationType.SIGNATURE) {
                                                        viewModel.updateSignatureStroke(signaturePoints + Pair(x, y))
                                                    }
                                                },
                                                onPageDragRelease = {
                                                    if (activeAnnotation == AnnotationType.SIGNATURE) {
                                                        viewModel.applySignatureOnPage(page.index)
                                                    }
                                                },
                                                onPageActionClick = { x, y ->
                                                    when (activeAnnotation) {
                                                        AnnotationType.STAMP -> {
                                                            viewModel.applyStampOnPage(page.index, x, y, "APPROVED")
                                                        }
                                                        AnnotationType.NOTE -> {
                                                            viewModel.applyTextNoteOnPage(page.index, x, y, "Note added on standard canvas.")
                                                        }
                                                        AnnotationType.GEOMETRY -> {
                                                            viewModel.applyGeometryOnPage(page.index, x, y, 160f, 120f)
                                                        }
                                                        AnnotationType.MEASURING -> {
                                                            viewModel.applyMeasuringOnPage(page.index, x, y, x + 180f, y + 120f)
                                                        }
                                                        else -> {}
                                                    }
                                                },
                                                onImageRotateRequested = { blockId ->
                                                    viewModel.manipulateSelectedImage(page.index, blockId, 90f, 1f)
                                                },
                                                onImageScaleRequested = { blockId, up ->
                                                    viewModel.manipulateSelectedImage(page.index, blockId, 0f, if (up) 1.2f else 0.8f)
                                                },
                                                onAddHyperlinkRequested = { label, url, x, y ->
                                                    viewModel.addHyperlink(page.index, label, url, x, y)
                                                },
                                                onFormFieldUpdated = { fieldId, newValue ->
                                                    viewModel.updateFormField(page.index, fieldId, newValue)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. OVERLAY CONTROLS - FLOATING/COLLAPSIBLE SEARCH BAR & TOOLBAR (At Top)
                        AnimatedVisibility(
                            visible = showTopToolbar,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                (if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF3F4F9)).copy(alpha = 0.98f),
                                                (if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF3F4F9)).copy(alpha = 0.95f),
                                                (if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF3F4F9)).copy(alpha = 0.82f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .statusBarsPadding()
                                    .padding(bottom = 12.dp)
                            ) {
                                // Unified Sleek Glassy TopAppBar
                                TopAppBar(
                                    title = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(Color(0xFFC62828), RoundedCornerShape(10.dp))
                                                    .padding(6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.MenuBook,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = currentDoc?.name ?: "PDF Reader",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "PRO SUITE",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isDarkMode) Color.LightGray else Color(0xFF64748B),
                                                    letterSpacing = 1.5.sp
                                                )
                                            }
                                        }
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = {
                                            viewModel.closeDocument()
                                        }) {
                                            Icon(
                                                imageVector = Icons.Filled.ArrowBack,
                                                contentDescription = "رجوع",
                                                tint = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                            )
                                        }
                                    },
                                    actions = {
                                        IconButton(onClick = {
                                            scope.launch {
                                                drawerState.open()
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Filled.Menu,
                                                contentDescription = "سجل الملفات",
                                                tint = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.toggleDarkMode() },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                contentColor = if (isDarkMode) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            Icon(
                                                if (isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                                contentDescription = "Invert Dark Mode"
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.toggleReflowMode() },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                contentColor = if (isReflowMode) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            Icon(Icons.Filled.TextFormat, contentDescription = "Reflow Mode Text")
                                        }

                                        IconButton(
                                            onClick = { viewModel.setEditMode(!isEditMode) },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                contentColor = if (isEditMode) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            Icon(
                                                if (isEditMode) Icons.Filled.EditOff else Icons.Filled.Edit,
                                                contentDescription = "Toggle Edit Content"
                                            )
                                        }

                                        IconButton(onClick = { showMetadataDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Filled.Info,
                                                contentDescription = "Info Metadata",
                                                tint = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                            )
                                        }

                                        IconButton(onClick = { showSecurityDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Filled.Security,
                                                contentDescription = "Security password",
                                                tint = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent,
                                        titleContentColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                    )
                                )

                                ViewControlToolCard(
                                    viewModel = viewModel,
                                    searchQuery = searchQuery,
                                    isRegex = isRegexSearch,
                                    resultsSize = searchResults.size,
                                    currentSearchIdx = currentSearchIdx,
                                    zoomScale = zoomScale,
                                    activeAnnotation = activeAnnotation,
                                    onAddWatermarkClick = { showWatermarkDialog = true }
                                )
                            }
                        }

                        // 3. FLOATING BADGES & TRIGGER CONTROLS (At Bottom Right)
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .navigationBarsPadding(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            // Transparent glassy page status badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${activePageIndex + 1}/${activeDoc.pages.size} صفحة",
                                    fontSize = 12.sp,
                                    color = if (isDarkMode) Color.LightGray else Color(0xFF1E293B),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Immersive full-canvas reader toggle floating button
                            FloatingActionButton(
                                onClick = { showTopToolbar = !showTopToolbar },
                                containerColor = if (isDarkMode) Color(0xFF334155) else Color.White,
                                contentColor = if (isDarkMode) Color.White else Color(0xFF1E293B),
                                modifier = Modifier.size(46.dp),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = if (showTopToolbar) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Toggle UI",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // EXPANDABLE GOOGLE GEMINI AI FAB PANEL TRIGGER
                            ExtendedFloatingActionButton(
                                onClick = { showGeminiSheet = true },
                                containerColor = Color(0xFFC62828),
                                contentColor = Color.White,
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = "AI Co-pilot",
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                text = {
                                    Text("AI Co-Pilot", fontWeight = FontWeight.Bold)
                                }
                            )
                        }

                        // 4. NATIVE MATERIAL 3 MODAL BOTTOM SHEET FOR GEMINI CO-PILOT
                        if (showGeminiSheet) {
                            ModalBottomSheet(
                                onDismissRequest = { showGeminiSheet = false },
                                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                                containerColor = if (isDarkMode) Color(0xFF0F172A) else Color.White,
                                scrimColor = Color.Black.copy(alpha = 0.45f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(bottom = 16.dp)
                                ) {
                                    GeminiAssistantBottomPanel(
                                        viewModel = viewModel,
                                        isGeminiLoading = isGeminiLoading,
                                        geminiResult = geminiResult,
                                        savedApiKey = customApiKey,
                                        activePageIndex = activePageIndex,
                                        onConfigureApiKeyClick = { showApiKeyDialog = true }
                                    )
                                }
                            }
                        }
                    }

                    // --- Black Web-Loading Overlay ---
                    if (isAudioLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.92f))
                                .clickable(enabled = false) {},
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF00B0FF),
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Language,
                                        contentDescription = "Globe",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "جاري التحميل",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "تجهيز النطق ثنائي اللغة (ألماني - عربي)...",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // --- Popup Multimedia Player Dialog ---
                    if (activeAudioPlaying != null && !isAudioLoading) {
                        AudioPlayerDialog(
                            audio = activeAudioPlaying!!,
                            isDarkMode = isDarkMode,
                            isAudioPlayingState = isAudioPlayingState,
                            onPlayToggle = { isPlaying ->
                                isAudioPlayingState = isPlaying
                                if (isPlaying) {
                                    viewModel.playAudioAnnotation(activeAudioPlaying!!)
                                } else {
                                    viewModel.stopTts()
                                }
                            },
                            audioProgress = audioProgress,
                            onProgressChange = { audioProgress = it },
                            audioVolume = audioVolume,
                            onVolumeChange = { audioVolume = it },
                            onDismiss = {
                                viewModel.stopAudioAnnotation()
                                activeAudioPlaying = null
                            }
                        )
                    }
                }
            }
        }
    }
}

    // ============================================
    // MODAL DIALOGS FOR EXTREME PDF OPERATIONS
    // ============================================

    // 1. TextBlock editor (Changing Font name, font sizes, colors, values)
    showTextEditDialog?.let { (pageIdx, block) ->
        var blockContent by remember { mutableStateOf(block.text) }
        var blockFontFamily by remember { mutableStateOf(block.fontName) }
        var blockFontSize by remember { mutableStateOf(block.fontSize) }
        var blockFontColor by remember { mutableStateOf(block.fontColor) }

        Dialog(onDismissRequest = { showTextEditDialog = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Deep Content Text Edit", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = blockContent,
                        onValueChange = { blockContent = it },
                        label = { Text("Text block contents") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Fonts Selector (Font Recognition)
                    Text("Font Family Configuration:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Roboto", "Serif", "Monospace", "SansSerif").forEach { font ->
                            val selected = blockFontFamily == font
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { blockFontFamily = font },
                                color = if (selected) Color(0xFFC62828) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = font,
                                    fontSize = 11.sp,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Size slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Size: ${blockFontSize.roundToInt()}sp", fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Slider(
                            value = blockFontSize,
                            onValueChange = { blockFontSize = it },
                            valueRange = 10f..48f,
                            modifier = Modifier.width(180.dp)
                        )
                    }

                    // Colors Selector
                    Text("Font Color Hex:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("#000000", "#C62828", "#1E88E5", "#43A047", "#5E35B1").forEach { hex ->
                            val selected = blockFontColor.uppercase() == hex.uppercase()
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(
                                        width = if (selected) 3.dp else 0.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { blockFontColor = hex }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showTextEditDialog = null }) {
                            Text("Discard")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.updateTextBlock(
                                    pageIdx = pageIdx,
                                    blockId = block.id,
                                    newText = blockContent,
                                    fontColor = blockFontColor,
                                    fontSize = blockFontSize,
                                    fontName = blockFontFamily
                                )
                                showTextEditDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) {
                            Text("Apply Changes")
                        }
                    }
                }
            }
        }
    }

    // 2. Metadata View/Editor Dialog
    if (showMetadataDialog && currentDoc != null) {
        val meta = currentDoc!!.metadata
        var titleEdit by remember { mutableStateOf(meta.title) }
        var authorEdit by remember { mutableStateOf(meta.author) }
        var subjectEdit by remember { mutableStateOf(meta.subject) }
        var keywordEdit by remember { mutableStateOf(meta.keywords) }

        Dialog(onDismissRequest = { showMetadataDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Edit Document Metadata", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = titleEdit,
                        onValueChange = { titleEdit = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = authorEdit,
                        onValueChange = { authorEdit = it },
                        label = { Text("Author") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = subjectEdit,
                        onValueChange = { subjectEdit = it },
                        label = { Text("Subject / Vibe") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = keywordEdit,
                        onValueChange = { keywordEdit = it },
                        label = { Text("Keywords Comma Separated") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showMetadataDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.updateMetadata(titleEdit, authorEdit, subjectEdit, keywordEdit)
                                showMetadataDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) {
                            Text("Save Info")
                        }
                    }
                }
            }
        }
    }

    // 3. Security Password Dialog (Locking)
    if (showSecurityDialog && currentDoc != null) {
        var newPassword by remember { mutableStateOf("") }
        val activeDoc = currentDoc!!

        Dialog(onDismissRequest = { showSecurityDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Document Encryption & Security", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    
                    Text(
                        text = if (activeDoc.security.isEncrypted) 
                            "This document is currently password protected. Enter a blank password below and click Save to unlock security parameters permanently." 
                            else "Secure your document against printing, viewing, or editing inputs by establishing a Master Key.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Secure Password Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSecurityDialog = false }) {
                            Text("Close")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.changeSecurityPassword(newPassword)
                                showSecurityDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) {
                            Text(if (activeDoc.security.isEncrypted && newPassword.isEmpty()) "Decrypt Legal File" else "Encrypt PDF")
                        }
                    }
                }
            }
        }
    }

    // 4. Merge PDFs Dialog
    if (showMergeDialog) {
        var mergeTitle by remember { mutableStateOf("Merged_Portfolio_Doc.pdf") }
        Dialog(onDismissRequest = { showMergeDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Merge Multiple PDF Documents", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "This will merge all indexable documents currently in our central registry database into a single cohesive portfolio flow.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = mergeTitle,
                        onValueChange = { mergeTitle = it },
                        label = { Text("Merged document name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showMergeDialog = false }) {
                            Text("Discard")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.performMergeAction(mergeTitle)
                                showMergeDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) {
                            Text("Perform Merge")
                        }
                    }
                }
            }
        }
    }

    // 5. Split PDF Dialog
    if (showSplitDialog) {
        var splitTitle by remember { mutableStateOf("Extracted_Excerpt_Sheet.pdf") }
        Dialog(onDismissRequest = { showSplitDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Extract Pages & Split PDF", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Extract specified pages of the active document into a brand new standalone document.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = splitTitle,
                        onValueChange = { splitTitle = it },
                        label = { Text("Extracted document name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSplitDialog = false }) {
                            Text("Discard")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.performSplitAction(splitTitle, listOf(0)) // Splits and takes first page
                                showSplitDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) {
                            Text("Extract Page 1 Only")
                        }
                    }
                }
            }
        }
    }

    // 6. Add Watermark Dialog
    if (showWatermarkDialog) {
        var wmText by remember { mutableStateOf("CONFIDENTIAL") }
        Dialog(onDismissRequest = { showWatermarkDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Add Semi-Transparent Watermark", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    
                    OutlinedTextField(
                        value = wmText,
                        onValueChange = { wmText = it },
                        label = { Text("Watermark Label") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showWatermarkDialog = false }) {
                            Text("Discard")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.applyWatermarkAction(wmText)
                                showWatermarkDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) {
                            Text("Watermark File")
                        }
                    }
                }
            }
        }
    }

    // 7. Create/Convert PDF Dialog (Word, Excel, PowerPoint, HTML, Text To PDF Converter)
    if (showConvertDialog) {
        var convertTitle by remember { mutableStateOf("Form_Compiled_File") }
        var inputFormat by remember { mutableStateOf("TEXT") } // TEXT, HTML, WORD, EXCEL, PPT
        var bodyContent by remember { mutableStateOf("Write content here...") }

        Dialog(onDismissRequest = { showConvertDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("تجميع مستند PDF جديد", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF0F172A))

                    OutlinedTextField(
                        value = convertTitle,
                        onValueChange = { convertTitle = it },
                        label = { Text("اسم ملف الـ PDF الناتج") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("صيغة المدخلات المكتوبة لإنشاء الـ PDF:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("TEXT" to "نص عادي", "HTML" to "HTML", "WORD" to "Word", "EXCEL" to "Excel CSV", "PPT" to "PowerPoint Slides").forEach { (fmt, label) ->
                            val s = inputFormat == fmt
                            InputChip(
                                selected = s,
                                onClick = { 
                                    inputFormat = fmt 
                                    bodyContent = when (fmt) {
                                        "HTML" -> "<h1>عنوان التقرير</h1>\n<p>محتوى التقرير التلقائي...</p>"
                                        "WORD" -> "المستند النصي المكتوب لبرنامج وورد\nالسطر الثاني من المستند\nالسطر الثالث للمستند"
                                        "EXCEL" -> "الاسم,العمر,المهنة,البلد\nمحمد,29,مهندس,مصر\nسارة,24,طبيبة,الأردن\nخالد,32,مبرمج,السعودية"
                                        "PPT" -> "العنوان: الشريحة الأولى لموضوعنا\nتفاصيل نقطة 1 للتقديم\n---\nالعنوان: الشريحة الثانية لموضوعنا\nشرح موجز للمخرجات الذكية"
                                        else -> "اكتب هنا البيانات النصية العادية لإنشاء صفحات PDF..."
                                    }
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = InputChipDefaults.inputChipColors(selectedContainerColor = Color(0xFFFFEBEE))
                            )
                        }
                    }

                    OutlinedTextField(
                        value = bodyContent,
                        onValueChange = { bodyContent = it },
                        label = { Text("محتوى المستند للتجميع") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showConvertDialog = false }) {
                            Text("إلغاء")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                when (inputFormat) {
                                    "HTML" -> viewModel.convertHtmlToNewPdf(convertTitle, bodyContent)
                                    "WORD" -> viewModel.convertWordToNewPdf(convertTitle, bodyContent)
                                    "EXCEL" -> viewModel.convertExcelToNewPdf(convertTitle, bodyContent)
                                    "PPT" -> viewModel.convertPptToNewPdf(convertTitle, bodyContent)
                                    else -> viewModel.convertTextToNewPdf(convertTitle, bodyContent)
                                }
                                showConvertDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) {
                            Text("تجميع وتحميل")
                        }
                    }
                }
            }
        }
    }

    // 8. PDF Compression Dialog
    if (showCompressionDialog) {
        var compressionResult by remember { mutableStateOf<String?>(null) }
        var selectedProfile by remember { mutableStateOf("MEDIUM") }
        
        Dialog(onDismissRequest = { 
            showCompressionDialog = false 
            compressionResult = null
        }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("أداة ضغط مستندات PDF", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                    Text("اختر مستوى الضغط المطلوب لتقليل حجم الملف مع مراعاة الجودة:", fontSize = 13.sp, color = Color.Gray)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedProfile = "HIGH" }) {
                            RadioButton(selected = selectedProfile == "HIGH", onClick = { selectedProfile = "HIGH" }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFC62828)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ضغط عالي (توفر ~ 65% من الحجم - جودة منخفضة)", fontSize = 13.sp, color = if (isDarkMode) Color.LightGray else Color.Black)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedProfile = "MEDIUM" }) {
                            RadioButton(selected = selectedProfile == "MEDIUM", onClick = { selectedProfile = "MEDIUM" }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFC62828)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ضغط متوازن (توفر ~ 40% من الحجم - جودة جيدة)", fontSize = 13.sp, color = if (isDarkMode) Color.LightGray else Color.Black)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedProfile = "LOW" }) {
                            RadioButton(selected = selectedProfile == "LOW", onClick = { selectedProfile = "LOW" }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFC62828)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ضغط خفيف (توفر ~ 15% من الحجم - جودة ممتازة)", fontSize = 13.sp, color = if (isDarkMode) Color.LightGray else Color.Black)
                        }
                    }

                    if (compressionResult != null) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text(
                                text = compressionResult!!,
                                fontSize = 13.sp,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { 
                            showCompressionDialog = false 
                            compressionResult = null
                        }) {
                            Text("إغلاق")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (compressionResult == null) {
                            Button(
                                onClick = {
                                    compressionResult = viewModel.compressCurrentDocument(selectedProfile)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                            ) {
                                Text("ضغط وتحسين")
                            }
                        }
                    }
                }
            }
        }
    }

    // 9. PDF Export / Conversion Dialog
    if (showExportDialog) {
        var exportedText by remember { mutableStateOf<String?>(null) }
        var exportFormatName by remember { mutableStateOf("") }
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        
        Dialog(onDismissRequest = { 
            showExportDialog = false 
            exportedText = null
        }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White),
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("تصدير وتحويل مستند PDF", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                    Text("اختر الصيغة المستهدفة لاستخراج محتويات ملف PDF الحالي وتحويله إليها:", fontSize = 13.sp, color = Color.Gray)
                    
                    if (exportedText == null) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { 
                                        exportedText = viewModel.runExportToWord() 
                                        exportFormatName = "Microsoft Word (.docx)"
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Word")
                                }
                                
                                Button(
                                    onClick = { 
                                        exportedText = viewModel.runExportToExcel() 
                                        exportFormatName = "Microsoft Excel (.xlsx)"
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Excel")
                                }
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { 
                                        exportedText = viewModel.runExportToPPT() 
                                        exportFormatName = "PowerPoint Presentation (.pptx)"
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Slideshow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PPT")
                                }
                                
                                Button(
                                    onClick = { 
                                        exportedText = viewModel.runExportToHtml() 
                                        exportFormatName = "HTML Page (.html)"
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Html, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("HTML")
                                }
                            }

                            Button(
                                onClick = { 
                                    exportedText = viewModel.runExportToText() 
                                    exportFormatName = "Plain Text (.txt)"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.TextFormat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصدير كنص عادي (Plain Text)")
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("مخرجات تحويل الملف إلى: $exportFormatName", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = exportedText!!,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isDarkMode) Color.LightGray else Color.DarkGray
                                )
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(exportedText!!))
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("نسخ الحافظة")
                                }
                                
                                Button(
                                    onClick = {
                                        exportedText = null
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("تحويل آخر")
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { 
                            showExportDialog = false 
                            exportedText = null
                        }) {
                            Text("إغلاق")
                        }
                    }
                }
            }
        }
    }

    // 10. OCR / AI Scanner Dialog
    if (showOcrDialog) {
        Dialog(onDismissRequest = { showOcrDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White),
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("التعرف البصري والرقمنة (OCR / AI Scanner)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                    Text("تقوم هذه الميزة بتحويل المستندات الممسوحة ضوئياً أو الصور إلى نصوص رقمية قابلة للبحث والتحرير الكامل باستخدام محرك Gemini AI.", fontSize = 13.sp, color = Color.Gray)

                    if (isGeminiLoading) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFC62828))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("جاري استخراج النصوص الرقمية وتحليلها...", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else if (geminiResult != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("النص الرقمي المستخرج المكتوب:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .background(if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(geminiResult!!, fontSize = 12.sp, color = if (isDarkMode) Color.LightGray else Color.DarkGray)
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.convertOcrTextToPdf("Digitized_OCR_Result", geminiResult!!)
                                        showOcrDialog = false
                                        viewModel.dismissGeminiPanel()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حفظ كـ PDF رقمي", fontSize = 11.sp)
                                }
                                
                                Button(
                                    onClick = {
                                        viewModel.dismissGeminiPanel()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(0.6f)
                                ) {
                                    Text("مسح آخر", fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.askGeminiToOcr(context, customApiKey, activePageIndex, null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFFFEBEE))
                            ) {
                                Icon(Icons.Filled.DocumentScanner, contentDescription = null, tint = Color(0xFFC62828))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مسح الصفحة الحالية بالـ OCR", color = Color(0xFFC62828))
                            }
                            
                            FilledTonalButton(
                                onClick = {
                                    val b = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
                                    viewModel.askGeminiToOcr(context, customApiKey, activePageIndex, b)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFFFEBEE))
                            ) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = Color(0xFFC62828))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("رفع صورة ممسوحة/كاميرا للتحليل", color = Color(0xFFC62828))
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { 
                            showOcrDialog = false 
                            viewModel.dismissGeminiPanel()
                        }) {
                            Text("إغلاق")
                        }
                    }
                }
            }
        }
    }

    // 10.1 Gemini API Key Config Dialog
    if (showApiKeyDialog) {
        var tempKeyText by remember { mutableStateOf(customApiKey) }
        var isPasswordVisible by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showApiKeyDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White),
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.VpnKey, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "إعداد مفتاح Gemini API Key",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                    }

                    Text(
                        "برجاء إدخال مفتاح Google Gemini API الخاص بك لتتمكن من استخدام ميزات الذكاء الاصطناعي الفعلي (OCR، الترجمة والتلخيص) بشكل مباشر غير محدود وبسرية تامة.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = tempKeyText,
                        onValueChange = { tempKeyText = it },
                        label = { Text("Gemini API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("AIzaSy...") },
                        visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle Visibility"
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFC62828),
                            focusedLabelColor = Color(0xFFC62828)
                        )
                    )

                    Text(
                        "• يتم تشفير وحفظ المفتاح محلياً على جهازك بشكل آمن مشفر (AES-128) ولا يتم مشاركته مع أي خوادم خارجية.\n• إذا تركته فارغاً، سيحاول التطبيق تشغيل المفتاح المجاني الافتراضي التابع لـ AI Studio.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = { 
                            showApiKeyDialog = false 
                        }) {
                            Text("إلغاء", color = Color.Gray)
                        }

                        Button(
                            onClick = {
                                SecureApiKeyStorage.saveApiKey(context, tempKeyText)
                                customApiKey = tempKeyText.trim()
                                showApiKeyDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("حفظ المفتاح بأمان", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// COMPRESSED BEAUTIFUL SUB-COMPONENTS
// ============================================

@Composable
fun SecurityPasswordUnlockOverlay(viewModel: PdfViewModel) {
    var codeText by remember { mutableStateOf("") }
    var keyErrorState by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = "Lock icon",
            tint = Color(0xFFC62828),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Password Protected File",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Please enter the unlock key to index pages of: ${viewModel.lockedDocumentPending.value?.name}",
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = codeText,
            onValueChange = {
                codeText = it
                keyErrorState = false
            },
            isError = keyErrorState,
            label = { Text("Input Pin Key (Hint: 123)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.width(240.dp),
            singleLine = true
        )

        if (keyErrorState) {
            Text("Incorrect Password! Access Denied.", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val ok = viewModel.unlockDocument(codeText)
                if (!ok) {
                    keyErrorState = true
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
            modifier = Modifier.width(180.dp)
        ) {
            Text("Unlock PDF Content")
        }
    }
}

@Composable
fun ViewControlToolCard(
    viewModel: PdfViewModel,
    searchQuery: String,
    isRegex: Boolean,
    resultsSize: Int,
    currentSearchIdx: Int,
    zoomScale: Float,
    activeAnnotation: AnnotationType?,
    onAddWatermarkClick: () -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.94f) else Color.White.copy(alpha = 0.94f)
        ),
        border = BorderStroke(
            1.dp,
            if (isDarkMode) Color(0xFF334155).copy(alpha = 0.8f) else Color(0xFFE2E8F0).copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            
            // Search Input Row (Word & Regex matching options)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it, isRegex) },
                    label = { Text("Search word or regex expression...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "S", tint = Color(0xFFC62828)) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Regex",
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp),
                                color = if (isRegex) Color(0xFFC62828) else Color.Gray,
                                fontWeight = if (isRegex) FontWeight.Bold else FontWeight.Normal
                            )
                            Checkbox(
                                checked = isRegex,
                                onCheckedChange = { viewModel.setSearchQuery(searchQuery, it) },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFC62828)),
                                modifier = Modifier.scaleScaleReduction()
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFC62828),
                        unfocusedBorderColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0),
                        focusedLabelColor = Color(0xFFC62828),
                        unfocusedLabelColor = Color.Gray
                    ),
                    singleLine = true
                )

                if (resultsSize > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${currentSearchIdx + 1}/$resultsSize",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                    IconButton(onClick = { viewModel.navigateSearchResult(-1) }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Prev")
                    }
                    IconButton(onClick = { viewModel.navigateSearchResult(1) }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Next")
                    }
                }
            }

            // Annotation Drawers selector (Highlights, Note, Stamp, Signature brush, shape vectors, measurements)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Annotation Tools:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.LightGray else Color(0xFF475569)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnnotationChip(
                        label = "Highlight",
                        icon = Icons.Filled.BorderColor,
                        selected = activeAnnotation == AnnotationType.HIGHLIGHT,
                        onClick = {
                            viewModel.setAnnotationType(
                                if (activeAnnotation == AnnotationType.HIGHLIGHT) null else AnnotationType.HIGHLIGHT
                            )
                        }
                    )

                    AnnotationChip(
                        label = "Underline",
                        icon = Icons.Filled.FormatUnderlined,
                        selected = activeAnnotation == AnnotationType.UNDERLINE,
                        onClick = {
                            viewModel.setAnnotationType(
                                if (activeAnnotation == AnnotationType.UNDERLINE) null else AnnotationType.UNDERLINE
                            )
                        }
                    )

                    AnnotationChip(
                        label = "Strike",
                        icon = Icons.Filled.FormatStrikethrough,
                        selected = activeAnnotation == AnnotationType.STRIKETHROUGH,
                        onClick = {
                            viewModel.setAnnotationType(
                                if (activeAnnotation == AnnotationType.STRIKETHROUGH) null else AnnotationType.STRIKETHROUGH
                            )
                        }
                    )

                    AnnotationChip(
                        label = "Squiggly",
                        icon = Icons.Filled.Waves,
                        selected = activeAnnotation == AnnotationType.SQUIGGLY,
                        onClick = {
                            viewModel.setAnnotationType(
                                if (activeAnnotation == AnnotationType.SQUIGGLY) null else AnnotationType.SQUIGGLY
                            )
                        }
                    )

                    AnnotationChip(
                        label = "Note",
                        icon = Icons.Filled.SpeakerNotes,
                        selected = activeAnnotation == AnnotationType.NOTE,
                        onClick = {
                            viewModel.setAnnotationType(
                                if (activeAnnotation == AnnotationType.NOTE) null else AnnotationType.NOTE
                            )
                        }
                    )

                    AnnotationChip(
                        label = "Stamp",
                        icon = Icons.Filled.Approval,
                        selected = activeAnnotation == AnnotationType.STAMP,
                        onClick = {
                            viewModel.setAnnotationType(
                                if (activeAnnotation == AnnotationType.STAMP) null else AnnotationType.STAMP
                            )
                        }
                    )

                    AnnotationChip(
                        label = "Signature",
                        icon = Icons.Filled.Gesture,
                        selected = activeAnnotation == AnnotationType.SIGNATURE,
                        onClick = {
                            viewModel.setAnnotationType(
                                if (activeAnnotation == AnnotationType.SIGNATURE) null else AnnotationType.SIGNATURE
                            )
                        }
                    )

                    AnnotationChip(
                        label = "Shapes",
                        icon = Icons.Filled.Category,
                        selected = activeAnnotation == AnnotationType.GEOMETRY,
                        onClick = {
                            viewModel.setAnnotationType(
                                if (activeAnnotation == AnnotationType.GEOMETRY) null else AnnotationType.GEOMETRY
                            )
                        }
                    )

                    AnnotationChip(
                        label = "Measure",
                        icon = Icons.Filled.SquareFoot,
                        selected = activeAnnotation == AnnotationType.MEASURING,
                        onClick = {
                            viewModel.setAnnotationType(
                                if (activeAnnotation == AnnotationType.MEASURING) null else AnnotationType.MEASURING
                            )
                        }
                    )
                }
            }

            // Zoom Slide & Watermark Row
            HorizontalDivider(color = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom", tint = Color(0xFFC62828))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Zoom: ${(zoomScale * 100).roundToInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )
                Slider(
                    value = zoomScale,
                    onValueChange = { viewModel.setZoomScale(it) },
                    valueRange = 0.5f..2.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFC62828),
                        activeTrackColor = Color(0xFFC62828)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                FilledTonalButton(
                    onClick = onAddWatermarkClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isDarkMode) Color(0xFF3F1F1F) else Color(0xFFFFEBEE)
                    )
                ) {
                    Icon(
                        Icons.Filled.BrandingWatermark,
                        contentDescription = "W",
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Watermark", fontSize = 11.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AnnotationChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    Surface(
        onClick = onClick,
        color = if (selected) Color(0xFFC62828) else (if (dark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
        border = BorderStroke(
            1.dp,
            if (selected) Color(0xFFC62828) else (if (dark) Color(0xFF334155) else Color(0xFFE2E8F0))
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(12.dp),
                tint = if (selected) Color.White else (if (dark) Color.White else Color(0xFF0F172A))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else (if (dark) Color.White else Color(0xFF0F172A))
            )
        }
    }
}

// Scale reduction helper for small check buttons
@Composable
fun Modifier.scaleScaleReduction(): Modifier = this.size(36.dp)

@Composable
fun ReflowReadingPane(
    currentDoc: PdfDocumentState,
    speakingBlockId: String?,
    onSpeakSentence: (PdfTextBlock) -> Unit,
    onStopSpeak: () -> Unit,
    isDarkMode: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Text Reflow Read Pane",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFC62828)
            )

            if (speakingBlockId != null) {
                Button(
                    onClick = onStopSpeak,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Filled.VolumeOff, contentDescription = "Stop TTS")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mute Speaker", fontSize = 11.sp)
                }
            }
        }

        currentDoc.pages.forEach { page ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Page ${page.index + 1}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )

                    page.textBlocks.forEach { block ->
                        val isReading = speakingBlockId == block.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isReading) Color(0xFFFFEBEE) else Color.Transparent)
                                .clickable { onSpeakSentence(block) }
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Filled.VolumeUp,
                                    contentDescription = "Speak",
                                    tint = if (isReading) Color(0xFFC62828) else Color.Gray,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = block.text,
                                    fontSize = 16.sp,
                                    lineHeight = 22.sp,
                                    color = if (isReading) Color(0xFFB71C1C) else if (isDarkMode) Color.White else Color.Black,
                                    fontWeight = if (isReading) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfPageCanvasItem(
    page: PdfPage,
    zoomScale: Float,
    isDarkMode: Boolean,
    isEditMode: Boolean,
    selectedTextId: String?,
    selectedImageId: String?,
    searchResults: List<SearchResult>,
    currentSearchIdx: Int,
    activeAnnotation: AnnotationType?,
    signaturePoints: List<Pair<Float, Float>>,
    onTextBlockClick: (PdfTextBlock) -> Unit,
    onImageBlockClick: (PdfImageBlock) -> Unit,
    onHighlightWordClick: (PdfTextBlock) -> Unit,
    onPageCanvasInteractiveDrag: (Float, Float) -> Unit,
    onPageDragRelease: () -> Unit,
    onPageActionClick: (Float, Float) -> Unit,
    onImageRotateRequested: (String) -> Unit,
    onImageScaleRequested: (String, Boolean) -> Unit,
    onAddHyperlinkRequested: (String, String, Float, Float) -> Unit,
    onFormFieldUpdated: (String, String) -> Unit = { _, _ -> },
    currentPlayingAudio: PdfAnnotation.AudioAnnotation? = null,
    onPlayAudioAnnotation: (PdfAnnotation.AudioAnnotation) -> Unit = {}
) {
    val baseWidth = page.width * zoomScale
    val baseHeight = page.height * zoomScale

    Card(
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF262626) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .width(baseWidth.dp)
            .height(baseHeight.dp)
            .pointerInput(activeAnnotation) {
                detectDragGestures(
                    onDragStart = {},
                    onDragEnd = { onPageDragRelease() },
                    onDragCancel = {},
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onPageCanvasInteractiveDrag(change.position.x, change.position.y)
                    }
                )
            }
            .pointerInput(activeAnnotation) {
                detectTapGestures { offset ->
                    onPageActionClick(offset.x, offset.y)
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // BACKGROUND INK LAYER (Night contrast inverse representation)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        if (isDarkMode) {
                            // Dark mode inverted document color draw
                            drawRect(color = Color(0xFF1E1E1E))
                        }
                    }
            )            // DRAWN ANNOTATION HISTORIES IN REALTIME
            page.annotations.forEach { ann ->
                when (ann) {
                    is PdfAnnotation.Highlight -> {
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (ann.x * zoomScale).dp,
                                    y = (ann.y * zoomScale).dp
                                )
                                .width((ann.width * zoomScale).dp)
                                .height((ann.height * zoomScale).dp)
                                .background(Color(0x73FFEB3B))
                        )
                    }
                    is PdfAnnotation.Underline -> {
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (ann.x * zoomScale).dp,
                                    y = ((ann.y + ann.height) * zoomScale).dp
                                )
                                .width((ann.width * zoomScale).dp)
                                .height((2 * zoomScale).dp)
                                .background(Color(0xFFC62828))
                        )
                    }
                    is PdfAnnotation.Strikethrough -> {
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (ann.x * zoomScale).dp,
                                    y = ((ann.y + ann.height / 2f) * zoomScale).dp
                                )
                                .width((ann.width * zoomScale).dp)
                                .height((1.5 * zoomScale).dp)
                                .background(Color.Black)
                        )
                    }
                    is PdfAnnotation.Squiggly -> {
                        Canvas(
                            modifier = Modifier
                                .offset(x = (ann.x * zoomScale).dp, y = (ann.y * zoomScale).dp)
                                .width((ann.width * zoomScale).dp)
                                .height((ann.height * zoomScale).dp)
                        ) {
                            val path = Path()
                            var currentX = 0f
                            val squigglyY = size.height - 2f
                            val waveLength = (8f * zoomScale).coerceAtLeast(1f)
                            val amplitude = 2f * zoomScale
                            path.moveTo(currentX, squigglyY)
                            var toggle = true
                            var safetyCounter = 0
                            while (currentX < size.width && safetyCounter < 1000) {
                                safetyCounter++
                                val nextX = (currentX + waveLength).coerceAtMost(size.width)
                                val targetY = if (toggle) squigglyY - amplitude else squigglyY + amplitude
                                path.lineTo(nextX, targetY)
                                currentX = nextX
                                toggle = !toggle
                            }
                            drawPath(path = path, color = Color(0xFFE53935), style = Stroke(width = 1.5f * zoomScale))
                        }
                    }
                    is PdfAnnotation.Geometry -> {
                        Canvas(
                            modifier = Modifier
                                .offset(x = (ann.x * zoomScale).dp, y = (ann.y * zoomScale).dp)
                                .width((ann.width * zoomScale).dp)
                                .height((ann.height * zoomScale).dp)
                        ) {
                            val strokeW = ann.strokeWidth * zoomScale
                            val pColor = try { Color(android.graphics.Color.parseColor(ann.color)) } catch(e: Exception) { Color.Red }
                            when (ann.type) {
                                PdfAnnotation.GeometryType.ROUND_RECT -> {
                                    drawRoundRect(color = pColor, style = Stroke(width = strokeW), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f * zoomScale))
                                }
                                PdfAnnotation.GeometryType.OVAL -> {
                                    drawOval(color = pColor, style = Stroke(width = strokeW))
                                }
                                PdfAnnotation.GeometryType.ARROW -> {
                                    drawLine(color = pColor, start = Offset(0f, size.height), end = Offset(size.width, 0f), strokeWidth = strokeW)
                                    val angle = kotlin.math.atan2(-size.height, size.width)
                                    val arrowLength = 16f * zoomScale
                                    val arrowAngle = Math.PI / 6
                                    val x1 = size.width - arrowLength * kotlin.math.cos(angle - arrowAngle).toFloat()
                                    val y1 = 0f - arrowLength * kotlin.math.sin(angle - arrowAngle).toFloat()
                                    val x2 = size.width - arrowLength * kotlin.math.cos(angle + arrowAngle).toFloat()
                                    val y2 = 0f - arrowLength * kotlin.math.sin(angle + arrowAngle).toFloat()
                                    
                                    val path = Path().apply {
                                        moveTo(size.width, 0f)
                                        lineTo(x1, y1)
                                        lineTo(x2, y2)
                                        close()
                                    }
                                    drawPath(path = path, color = pColor)
                                }
                                PdfAnnotation.GeometryType.LINE -> {
                                    drawLine(color = pColor, start = Offset(0f, 0f), end = Offset(size.width, size.height), strokeWidth = strokeW)
                                }
                            }
                        }
                    }
                    is PdfAnnotation.Measuring -> {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val pColor = try { Color(android.graphics.Color.parseColor(ann.color)) } catch(e: Exception) { Color(0xFF1E88E5) }
                            val start = Offset(ann.x * zoomScale, ann.y * zoomScale)
                            val end = Offset(ann.toX * zoomScale, ann.toY * zoomScale)
                            drawLine(color = pColor, start = start, end = end, strokeWidth = 3f * zoomScale)
                            
                            val angle = kotlin.math.atan2(end.y - start.y, end.x - start.x)
                            val orthoAngle = angle + Math.PI / 2
                            val tickLen = 8f * zoomScale
                            val cosO = kotlin.math.cos(orthoAngle).toFloat()
                            val sinO = kotlin.math.sin(orthoAngle).toFloat()
                            
                            drawLine(color = pColor, start = start - Offset(cosO*tickLen, sinO*tickLen), end = start + Offset(cosO*tickLen, sinO*tickLen), strokeWidth = 2f * zoomScale)
                            drawLine(color = pColor, start = end - Offset(cosO*tickLen, sinO*tickLen), end = end + Offset(cosO*tickLen, sinO*tickLen), strokeWidth = 2f * zoomScale)
                        }
                        
                        val dx = (ann.toX - ann.x) * zoomScale
                        val dy = (ann.toY - ann.y) * zoomScale
                        val distancePx = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat() / zoomScale
                        val measureText = "${(distancePx * ann.scaleRatio).roundToInt()} ${ann.unit}"
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (((ann.x + ann.toX) / 2f) * zoomScale - 25f).dp,
                                    y = (((ann.y + ann.toY) / 2f) * zoomScale - 20f).dp
                                )
                                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, Color.Gray, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(measureText, fontSize = (10 * zoomScale).sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
                        }
                    }
                    is PdfAnnotation.Stamp -> {
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (ann.x * zoomScale).dp,
                                    y = (ann.y * zoomScale).dp
                                )
                                .border(width = 2.dp, color = Color(0xFFC62828), shape = RoundedCornerShape(4.dp))
                                .background(Color(0x1FFF1744))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                ann.label,
                                fontSize = (14 * zoomScale).sp,
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    is PdfAnnotation.TextNote -> {
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (ann.x * zoomScale).dp,
                                    y = (ann.y * zoomScale).dp
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF9C4))
                                .border(1.dp, Color(0xFFFBC02D), shape = RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.SpeakerNotes, contentDescription = "note", tint = Color(0xFFF57F17), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(ann.text, fontSize = (11 * zoomScale).sp, color = Color.Black)
                            }
                        }
                    }
                    is PdfAnnotation.Signature -> {
                        // Custom signature renderer block
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val path = Path()
                            val pts = ann.strokePoints
                            if (pts.size > 1) {
                                path.moveTo(pts[0].first * zoomScale, pts[0].second * zoomScale)
                                for (k in 1 until pts.size) {
                                    path.lineTo(pts[k].first * zoomScale, pts[k].second * zoomScale)
                                }
                                drawPath(
                                    path = path,
                                    color = Color.Blue,
                                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                                )
                            }
                        }
                    }
                    is PdfAnnotation.AudioAnnotation -> {
                        val isCurrentPlaying = currentPlayingAudio?.id == ann.id
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (ann.x * zoomScale).dp,
                                    y = (ann.y * zoomScale).dp
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCurrentPlaying) Color(0xFFE0F2F1) else Color(0xFFF1F5F9)
                                )
                                .border(
                                    width = if (isCurrentPlaying) 2.dp else 1.dp,
                                    color = if (isCurrentPlaying) Color(0xFF00796B) else Color.LightGray,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onPlayAudioAnnotation(ann) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCurrentPlaying) Icons.Filled.VolumeUp else Icons.Filled.VolumeMute,
                                    contentDescription = "Audio Playback",
                                    tint = if (isCurrentPlaying) Color(0xFF00796B) else Color.DarkGray,
                                    modifier = Modifier.size((16 * zoomScale).dp)
                                )
                                Text(
                                    text = ann.label,
                                    fontSize = (11 * zoomScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentPlaying) Color(0xFF004D40) else Color.Black
                                )
                                if (isCurrentPlaying) {
                                    Text(
                                        text = " ♪",
                                        fontSize = (11 * zoomScale).sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF00796B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // INTERACTIVE ACROFORMS RENDERING ENGINE
            page.formFields.forEach { field ->
                Box(
                    modifier = Modifier
                        .offset(
                            x = (field.x * zoomScale).dp,
                            y = (field.y * zoomScale).dp
                        )
                        .width((field.width * zoomScale).dp)
                        .height((field.height * zoomScale).dp)
                ) {
                    when (field.type) {
                        FormFieldType.TEXT -> {
                            var textVal by remember(field.value) { mutableStateOf(field.value) }
                            OutlinedTextField(
                                value = textVal,
                                onValueChange = {
                                    textVal = it
                                    onFormFieldUpdated(field.id, it)
                                },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = (13 * zoomScale).sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFC62828),
                                    unfocusedBorderColor = Color.Gray,
                                    focusedContainerColor = Color(0xFFF1F5F9),
                                    unfocusedContainerColor = Color(0xFFF1F5F9),
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.fillMaxSize(),
                                singleLine = true,
                                label = { Text(field.name, fontSize = (9 * zoomScale).sp) }
                            )
                        }
                        FormFieldType.CHECKBOX -> {
                            val checked = field.value == "true"
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                    .clickable {
                                        onFormFieldUpdated(field.id, if (checked) "false" else "true")
                                    }
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        onFormFieldUpdated(field.id, if (it) "true" else "false")
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFC62828))
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = field.name,
                                    fontSize = (11 * zoomScale).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        FormFieldType.DROPDOWN -> {
                            var expanded by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color(0xFFC62828), RoundedCornerShape(4.dp))
                                    .clickable { expanded = true }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${field.name}: ${field.value}",
                                        fontSize = (11 * zoomScale).sp,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "dropdown", tint = Color.Black)
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    field.options.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt) },
                                            onClick = {
                                                onFormFieldUpdated(field.id, opt)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        FormFieldType.RADIO -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(field.name + ":", fontSize = (10 * zoomScale).sp, color = Color.Gray)
                                field.options.forEach { opt ->
                                    val isSel = field.value == opt
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            onFormFieldUpdated(field.id, opt)
                                        }
                                    ) {
                                        RadioButton(
                                            selected = isSel,
                                            onClick = {
                                                onFormFieldUpdated(field.id, opt)
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFC62828))
                                        )
                                        Text(opt, fontSize = (10 * zoomScale).sp, color = Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // IMAGES RENDERING ENGINE
            page.imageBlocks.forEach { imgBlock ->
                val isSelected = selectedImageId == imgBlock.id
                
                // Draw mock / placeholder image vectors smoothly or custom bitmap
                Box(
                    modifier = Modifier
                        .offset(
                            x = (imgBlock.x * zoomScale).dp,
                            y = (imgBlock.y * zoomScale).dp
                        )
                        .width((imgBlock.width * zoomScale).dp)
                        .height((imgBlock.height * zoomScale).dp)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color(0xFFC62828) else Color.LightGray
                        )
                        .clickable { onImageBlockClick(imgBlock) }
                        .background(Color(0xFFEFEFEF))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = "PDF Asset",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("PDF Page Image Block", fontSize = 11.sp, color = Color.Gray)
                        
                        if (isSelected && isEditMode) {
                            // Image modifier triggers
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.8f))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                IconButton(onClick = { onImageRotateRequested(imgBlock.id) }) {
                                    Icon(Icons.Filled.RotateRight, contentDescription = "R", tint = Color.White)
                                }
                                IconButton(onClick = { onImageScaleRequested(imgBlock.id, true) }) {
                                    Icon(Icons.Filled.ZoomIn, contentDescription = "Z", tint = Color.White)
                                }
                                IconButton(onClick = { onImageScaleRequested(imgBlock.id, false) }) {
                                    Icon(Icons.Filled.ZoomOut, contentDescription = "Z", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // TEXT CONTENT GRAPHICS LAYER
            page.textBlocks.forEach { textBlock ->
                val isSelected = selectedTextId == textBlock.id
                val searchResultActive = searchResults.getOrNull(currentSearchIdx)
                val isCurrentSearchResult = searchResultActive != null && 
                                            searchResultActive.pageIndex == page.index && 
                                            searchResultActive.textBlockId == textBlock.id
                
                val parsedColor = try {
                    Color(android.graphics.Color.parseColor(textBlock.fontColor))
                } catch (e: Exception) {
                    if (isDarkMode) Color.White else Color.Black
                }

                val customFamily = when(textBlock.fontName.lowercase()) {
                    "serif" -> FontFamily.Serif
                    "monospace" -> FontFamily.Monospace
                    "sansserif" -> FontFamily.SansSerif
                    else -> FontFamily.Default
                }

                Box(
                    modifier = Modifier
                        .offset(
                            x = (textBlock.x * zoomScale).dp,
                            y = (textBlock.y * zoomScale).dp
                        )
                        .width((textBlock.width * zoomScale).dp)
                        .border(
                            width = if (isSelected) 2.dp else if (isCurrentSearchResult) 3.dp else 0.dp,
                            color = if (isSelected) Color(0xFFC62828) else if (isCurrentSearchResult) Color(0xFFFF9800) else Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .background(
                            if (isCurrentSearchResult) Color(0x66FF9800) else Color.Transparent
                        )
                        .clickable { onTextBlockClick(textBlock) }
                        .padding(2.dp)
                ) {
                    Text(
                        text = textBlock.text,
                        fontSize = (textBlock.fontSize * zoomScale).sp,
                        color = if (isDarkMode && parsedColor == Color.Black) Color.White else parsedColor,
                        fontFamily = customFamily,
                        lineHeight = (textBlock.fontSize * zoomScale * 1.3f).sp
                    )

                    if (isEditMode) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit Block",
                            tint = Color(0xFFC62828),
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }

            // HYPERLINKS SHIELD
            page.hyperlinks.forEach { link ->
                Surface(
                    onClick = {
                        // Quick leap
                    },
                    color = Color(0x330288D1),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .offset(
                            x = (link.x * zoomScale).dp,
                            y = (link.y * zoomScale).dp
                        )
                        .width((link.width * zoomScale).dp)
                        .height((link.height * zoomScale).dp)
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Link, contentDescription = "Link", tint = Color(0xFF0288D1), modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = link.url ?: "Page Jump",
                            fontSize = (10 * zoomScale).sp,
                            color = Color(0xFF0288D1),
                            textDecoration = TextDecoration.Underline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // RENDERING FLOATING BRUSH STROKE (Drawn while mouse drag is active)
            if (activeAnnotation == AnnotationType.SIGNATURE && signaturePoints.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path()
                    path.moveTo(signaturePoints[0].first, signaturePoints[0].second)
                    for (m in 1 until signaturePoints.size) {
                        path.lineTo(signaturePoints[m].first, signaturePoints[m].second)
                    }
                    drawPath(
                        path = path,
                        color = Color.Blue,
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun GeminiAssistantBottomPanel(
    viewModel: PdfViewModel,
    isGeminiLoading: Boolean,
    geminiResult: String?,
    savedApiKey: String,
    activePageIndex: Int,
    onConfigureApiKeyClick: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dark) Color(0xFF1E293B) else Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (dark) Color(0xFF334155) else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = "Gemini",
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Gemini PDF Co-Pilot",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (dark) Color.White else Color(0xFF0F172A)
                )
                if (savedApiKey.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(Active Custom Key)",
                        fontSize = 10.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onConfigureApiKeyClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Configure API Key",
                        tint = if (dark) Color.LightGray else Color(0xFF475569),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // Buttons list (mockup style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Action 1: Summarize
                Button(
                    onClick = { viewModel.askGeminiToSummarize(savedApiKey, activePageIndex) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (dark) Color(0xFF121B2A) else Color(0xFFF1F5F9),
                        contentColor = if (dark) Color(0xFF38BDF8) else Color(0xFF1E3A8A)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (dark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFF1E3A8A).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Summarize,
                            contentDescription = null,
                            tint = if (dark) Color(0xFF38BDF8) else Color(0xFF1E3A8A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Summarize", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Action 2: Translate
                Button(
                    onClick = { viewModel.askGeminiToTranslate(savedApiKey, "Arabic", activePageIndex) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (dark) Color(0xFF2E1B4E) else Color(0xFFFAF5FF),
                        contentColor = if (dark) Color(0xFFC084FC) else Color(0xFF5B21B6)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (dark) Color(0xFF5B21B6).copy(alpha = 0.5f) else Color(0xFF5B21B6).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Translate,
                            contentDescription = null,
                            tint = if (dark) Color(0xFFC084FC) else Color(0xFF5B21B6),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Translate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Action 3: Optical OCR
                Button(
                    onClick = { viewModel.askGeminiToOcr(context, savedApiKey, activePageIndex, null) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (dark) Color(0xFF3B1E1E) else Color(0xFFFFF1F2),
                        contentColor = if (dark) Color(0xFFF43F5E) else Color(0xFF9F1239)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (dark) Color(0xFF9F1239).copy(alpha = 0.5f) else Color(0xFF9F1239).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DocumentScanner,
                            contentDescription = null,
                            tint = if (dark) Color(0xFFF43F5E) else Color(0xFF9F1239),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Optical OCR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Results summary with loading panel
            AnimatedVisibility(visible = isGeminiLoading || geminiResult != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            if (dark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            if (dark) Color(0xFF334155) else Color(0xFFE2E8F0),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {
                    if (isGeminiLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Gemini is analyzing document contents and structure...",
                                fontSize = 12.sp,
                                color = if (dark) Color.LightGray else Color(0xFF475569)
                            )
                        }
                    } else if (geminiResult != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Co-Pilot Output Results:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (dark) Color.LightGray else Color(0xFF475569)
                            )
                            IconButton(
                                onClick = { viewModel.dismissGeminiPanel() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Close",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (dark) Color.White else Color.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SelectionContainer {
                            Text(
                                text = geminiResult,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = if (dark) Color.White else Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// HIGH FIDELITY BOTTOM NAVIGATION TAB DASHBOARDS (Recent, Files, Discover, Premium)
// =========================================================================

@Composable
fun HomeRecentTab(
    viewModel: PdfViewModel,
    rawDocuments: List<PdfDocumentState>,
    isDarkMode: Boolean,
    recentSubTab: String,
    onSubTabChange: (String) -> Unit,
    onFileClick: (PdfDocumentState) -> Unit,
    onCreateClick: () -> Unit
) {
    val dbSavedFiles by viewModel.dbSavedFiles.collectAsState()
    val dbFavorites by viewModel.dbFavorites.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // 1. Search Bar with Profile Icon (Avatar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEEF2F6),
                    RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFC62828)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "بحث عن الملفات والأدوات",
                color = if (isDarkMode) Color.LightGray else Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Filled.Smartphone,
                    contentDescription = "Device info",
                    tint = if (isDarkMode) Color.White else Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Tab selection ("أخير" vs "مميز بنجمة") + Filter Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    modifier = Modifier.clickable { onSubTabChange("recent") },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "أخير",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (recentSubTab == "recent") Color(0xFFC62828) else Color.Gray
                    )
                    if (recentSubTab == "recent") {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .width(30.dp)
                                .height(3.dp)
                                .background(Color(0xFFC62828), RoundedCornerShape(1.5.dp))
                        )
                    }
                }

                Column(
                    modifier = Modifier.clickable { onSubTabChange("starred") },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "مميز بنجمة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (recentSubTab == "starred") Color(0xFFC62828) else Color.Gray
                    )
                    if (recentSubTab == "starred") {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .width(50.dp)
                                .height(3.dp)
                                .background(Color(0xFFC62828), RoundedCornerShape(1.5.dp))
                        )
                    }
                }
            }

            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = "Filter",
                    tint = if (isDarkMode) Color.White else Color.Black
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))

        // 3. List of files
        Box(modifier = Modifier.weight(1f)) {
            val displayedFiles = remember(recentSubTab, rawDocuments, dbSavedFiles, dbFavorites) {
                if (recentSubTab == "starred") {
                    rawDocuments.filter { doc -> dbFavorites.any { it.id == doc.id } }
                } else {
                    rawDocuments
                }
            }

            if (displayedFiles.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (recentSubTab == "starred") Icons.Filled.StarBorder else Icons.Filled.HistoryToggleOff,
                        contentDescription = "Empty",
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (recentSubTab == "starred") "لا توجد ملفات مميزة بنجمة" else "قائمة الملفات الأخيرة فارغة",
                        fontSize = 15.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (recentSubTab == "recent") {
                        item {
                            RecentMockFileItem(
                                name = "proguard-rules.txt",
                                subtitle = "٢٧-٠٥ • تنزيل",
                                fileType = "TXT",
                                isDarkMode = isDarkMode,
                                onClick = {
                                    val match = rawDocuments.find { it.name.contains("Manual", ignoreCase = true) }
                                    if (match != null) onFileClick(match)
                                }
                            )
                        }
                        item {
                            RecentMockFileItem(
                                name = "build.gradle.kts",
                                subtitle = "ملفات من Download",
                                fileType = "TXT",
                                isDarkMode = isDarkMode,
                                onClick = {
                                    val match = rawDocuments.find { it.name.contains("Agreement", ignoreCase = true) }
                                    if (match != null) onFileClick(match)
                                }
                            )
                        }
                        item {
                            RecentMockFileItem(
                                name = "cookies (1).txt",
                                subtitle = "٢٣-٠٥ • تنزيل",
                                fileType = "TXT",
                                isDarkMode = isDarkMode,
                                onClick = {
                                    val match = rawDocuments.find { it.name.contains("Manual", ignoreCase = true) }
                                    if (match != null) onFileClick(match)
                                }
                            )
                        }
                    }

                    items(displayedFiles) { doc ->
                        val isFav = dbFavorites.any { it.id == doc.id }
                        var showDropdown by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isDarkMode) Color(0xFF1E293B) else Color.White,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onFileClick(doc) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        if (doc.security.isEncrypted) Color(0xFFFFCC80) else Color(0xFFFFEBEE),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (doc.security.isEncrypted) Icons.Filled.Lock else Icons.Filled.PictureAsPdf,
                                    tint = if (doc.security.isEncrypted) Color(0xFFE65100) else Color(0xFFC62828),
                                    modifier = Modifier.size(24.dp),
                                    contentDescription = "PDF Icon"
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "٢٢-٠٥ • ${doc.pages.size} صفحة • هذا الجهاز",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Box {
                                IconButton(onClick = { showDropdown = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = "Options",
                                        tint = if (isDarkMode) Color.White else Color.Black
                                    )
                                }

                                DropdownMenu(
                                    expanded = showDropdown,
                                    onDismissRequest = { showDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("مشاركة الملف") },
                                        onClick = { showDropdown = false },
                                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (isFav) "إلغاء التمييز بنجمة" else "تمييز بنجمة") },
                                        onClick = {
                                            viewModel.toggleFavoriteInDb(doc.id, isFav)
                                            showDropdown = false
                                        },
                                        leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("معلومات الملف") },
                                        onClick = { showDropdown = false },
                                        leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ExtendedFloatingActionButton(
                text = { Text("إنشاء", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Create", tint = Color.White) },
                onClick = { onCreateClick() },
                containerColor = Color(0xFF2979FF),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 4.dp)
            )
        }
    }
}

@Composable
fun RecentMockFileItem(
    name: String,
    subtitle: String,
    fileType: String,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isDarkMode) Color(0xFF1E293B) else Color.White,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Color(0xFFECEFF1), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fileType,
                color = Color(0xFF455A64),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null,
                tint = if (isDarkMode) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun HomeFilesTab(
    viewModel: PdfViewModel,
    rawDocuments: List<PdfDocumentState>,
    isDarkMode: Boolean,
    onFileClick: (PdfDocumentState) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEEF2F6),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFC62828)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("M", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "بحث عن الملفات والأدوات",
                    color = if (isDarkMode) Color.LightGray else Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Smartphone,
                        contentDescription = "Device info",
                        tint = if (isDarkMode) Color.White else Color.Black
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isDarkMode) Color(0xFF1E293B) else Color.White,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CategoryGridItem(name = "PDF", color = Color(0xFFEF5350), icon = Icons.Filled.PictureAsPdf, onClick = {
                        val match = rawDocuments.find { it.name.contains("Bilingual", ignoreCase = true) }
                        if (match != null) onFileClick(match)
                    })
                    CategoryGridItem(name = "XLS", color = Color(0xFF66BB6A), icon = Icons.Filled.Article, onClick = {
                        val match = rawDocuments.find { it.name.contains("Manual", ignoreCase = true) }
                        if (match != null) onFileClick(match)
                    })
                    CategoryGridItem(name = "PPT", color = Color(0xFFFF7043), icon = Icons.Filled.Slideshow, onClick = {
                        val match = rawDocuments.find { it.name.contains("Agreement", ignoreCase = true) }
                        if (match != null) onFileClick(match)
                    })
                    CategoryGridItem(name = "DOC", color = Color(0xFF42A5F5), icon = Icons.Filled.Description, onClick = {
                        val match = rawDocuments.find { it.name.contains("Manual", ignoreCase = true) }
                        if (match != null) onFileClick(match)
                    })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CategoryGridItem(name = "TXT", color = Color(0xFF78909C), icon = Icons.Filled.Assignment, onClick = {
                        val match = rawDocuments.find { it.name.contains("Manual", ignoreCase = true) }
                        if (match != null) onFileClick(match)
                    })
                    CategoryGridItem(name = "الصور", color = Color(0xFFAB47BC), icon = Icons.Filled.Image, onClick = {
                        val match = rawDocuments.find { it.name.contains("Agreement", ignoreCase = true) }
                        if (match != null) onFileClick(match)
                    })
                    CategoryGridItem(name = "أخرى", color = Color(0xFF26A69A), icon = Icons.Filled.GridOn, onClick = {})
                    CategoryGridItem(name = "سحابي", color = Color(0xFF26C6DA), icon = Icons.Filled.Cloud, onClick = {})
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isDarkMode) Color(0xFF1E293B) else Color.White,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(vertical = 8.dp)
            ) {
                StoragePathRow(label = "هذا الجهاز", sub = "G ٢٤,٣", icon = Icons.Filled.Smartphone, iconColor = Color(0xFF42A5F5), isDarkMode = isDarkMode)
                StoragePathRow(label = "تم الإرسال من تطبيقات أخرى", sub = "الملفات النشطة من WhatsApp أو Telegram", icon = Icons.Filled.Share, iconColor = Color(0xFF66BB6A), badgeCount = 3, isDarkMode = isDarkMode)
                StoragePathRow(label = "تنزيل", sub = "ملفات الـ Download والإنترنت", icon = Icons.Filled.ArrowCircleDown, iconColor = Color(0xFFAB47BC), isDarkMode = isDarkMode)
                StoragePathRow(label = "مستنداتي", sub = "مسار التخزين الافتراضي للمستندات", icon = Icons.Filled.Folder, iconColor = Color(0xFFFF9100), isDarkMode = isDarkMode)
            }
        }

        item {
            Text(
                text = "الخدمات السحابية",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isDarkMode) Color(0xFF1E293B) else Color.White,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(vertical = 8.dp)
            ) {
                StoragePathRow(label = "WPS Cloud", sub = "مساحة سحابية آمنة ومجانية", icon = Icons.Filled.Cloud, iconColor = Color(0xFF00B0FF), isDarkMode = isDarkMode)
                StoragePathRow(label = "OneDrive", sub = "ربط ومزامنة مستندات Microsoft", icon = Icons.Filled.Backup, iconColor = Color(0xFF114285), isDarkMode = isDarkMode)
                StoragePathRow(label = "Evernote", sub = "استيراد الملاحظات والملفات والمسودات", icon = Icons.Filled.EventNote, iconColor = Color(0xFF4CAF50), isDarkMode = isDarkMode)
                StoragePathRow(label = "إضافة WebDAV / FTP", sub = "الاتصال بالخوادم المحلية والبعيدة", icon = Icons.Filled.AddCircle, iconColor = Color(0xFF1E88E5), isDarkMode = isDarkMode)
            }
        }
    }
}

@Composable
fun CategoryGridItem(
    name: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(60.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(color, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StoragePathRow(
    label: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    badgeCount: Int? = null,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
            )
            Text(
                text = sub,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        if (badgeCount != null) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(Color(0xFF2979FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun HomeDiscoverTab(
    viewModel: PdfViewModel,
    isDarkMode: Boolean,
    onMergeClick: () -> Unit,
    onSplitClick: () -> Unit,
    onOcrClick: () -> Unit,
    onCompressClick: () -> Unit,
    onWatermarkClick: () -> Unit,
    onMetadataClick: () -> Unit,
    openBilingualGuide: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "اكتشاف الأدوات الذكية",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
        )
        Text(
            text = "مجموعة متكاملة من أدوات تعديل وإنشاء مستندات PDF الاحترافية",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE0F2F1)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openBilingualGuide() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFF00796B), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔊", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "القارئ الصوتي التفاعلي ثنائي اللغة (Bilingual Reading)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF004D40)
                        )
                        Text(
                            text = "يدعم دمج نطق صوتي تفاعلي لكل فقرة، مثالي لتعلم اللغة الألمانية والعربية.",
                            fontSize = 11.sp,
                            color = if (isDarkMode) Color.LightGray else Color(0xFF004D40).copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            DiscoverToolItem(
                title = "محرك الدمج السريع (Merge PDF)",
                description = "دمج عدة صفحات ومستندات في ملف واحد متكامل.",
                emoji = "📂",
                toolColor = Color(0xFF42A5F5),
                isDarkMode = isDarkMode,
                onClick = onMergeClick
            )

            DiscoverToolItem(
                title = "محرك التقسيم الذكي (Split PDF)",
                description = "تفتيت المستند المفتوح بناءً على أرقام الصفحات أو بحجم الملف.",
                emoji = "✂️",
                toolColor = Color(0xFFEF5350),
                isDarkMode = isDarkMode,
                onClick = onSplitClick
            )

            DiscoverToolItem(
                title = "مسح ضوئي ذكي AI OCR Scanner",
                description = "استخراج النصوص من الصور وتلخيصها أو ترجمتها عبر Gemini AI.",
                emoji = "🔍",
                toolColor = Color(0xFFAB47BC),
                isDarkMode = isDarkMode,
                onClick = onOcrClick
            )

            DiscoverToolItem(
                title = "تقليص وضغط المستند (Compress PDF)",
                description = "تطبيق خوارزميات الضغط لتقليل الحجم مع الحفاظ على جودة النصوص.",
                emoji = "⚡",
                toolColor = Color(0xFF66BB6A),
                isDarkMode = isDarkMode,
                onClick = onCompressClick
            )

            DiscoverToolItem(
                title = "تحرير العلامات المائية والحماية (Watermarking)",
                description = "إضافة نصوص وأختام حماية لمنع نسخ وتداول الملف.",
                emoji = "🛡️",
                toolColor = Color(0xFFFF7043),
                isDarkMode = isDarkMode,
                onClick = onWatermarkClick
            )

            DiscoverToolItem(
                title = "محدد البيانات الفوقية (PDF Metadata Control)",
                description = "تعديل اسم الكاتب، العنوان، والكلمات المفتاحية في ثوانٍ.",
                emoji = "✍️",
                toolColor = Color(0xFF26A69A),
                isDarkMode = isDarkMode,
                onClick = onMetadataClick
            )
        }
    }
}

@Composable
fun DiscoverToolItem(
    title: String,
    description: String,
    emoji: String,
    toolColor: Color,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(toolColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun HomePremiumTab(isDarkMode: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E293B) else Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFFFBC02D).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.OfflineBolt,
                        contentDescription = "Premium",
                        tint = Color(0xFFFBC02D),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "WPS Office Premium",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    text = "قم بالتصفح السحابي ومضاعفة إنتاجيتك الآن",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "الترقية إلى الباقة الاحترافية",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "فقط 4.99$ شهرياً - إلغاء في أي وقت",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "المزايا الحصرية المتضمنة",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color.White else Color(0xFF0F172A),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        PremiumBenefitRow(title = "سعة سحابية بمساحة 20 جيجابايت لتخزين ملفاتك ومزامنتها", emoji = "☁️", isDarkMode = isDarkMode)
        PremiumBenefitRow(title = "إزالة تامة لجميع الإعلانات المنبثقة للتصفح الهادئ", emoji = "🚫", isDarkMode = isDarkMode)
        PremiumBenefitRow(title = "تحرير النصوص وتعديل الموضع المتقدم للـ PDF", emoji = "📝", isDarkMode = isDarkMode)
        PremiumBenefitRow(title = "محرك النطق الآلي ثنائي اللغة المتميز Pro TTS", emoji = "🌍", isDarkMode = isDarkMode)
        PremiumBenefitRow(title = "حزمة فك وتعديل كلمات المرور الأمنية للمستندات", emoji = "🔒", isDarkMode = isDarkMode)
    }
}

@Composable
fun PremiumBenefitRow(title: String, emoji: String, isDarkMode: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkMode) Color.White else Color(0xFF1E293B)
        )
    }
}

@Composable
fun AudioPlayerDialog(
    audio: PdfAnnotation.AudioAnnotation,
    isDarkMode: Boolean,
    isAudioPlayingState: Boolean,
    onPlayToggle: (Boolean) -> Unit,
    audioProgress: Float,
    onProgressChange: (Float) -> Unit,
    audioVolume: Float,
    onVolumeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(
                                    if (isDarkMode) Color(0xFF334155) else Color(0xFFE0F2F1),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (audio.language == "de") "🇩🇪" else "🇸🇦", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (audio.language == "de") "النطق الألماني (German Speech)" else "النطق العربي (Arabic Speech)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00796B)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Player",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                IconButton(
                    onClick = { onPlayToggle(!isAudioPlayingState) },
                    modifier = Modifier
                        .size(76.dp)
                        .background(Color(0xFFE0F2F1), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isAudioPlayingState) Icons.Filled.PauseCircleFilled else Icons.Filled.PlayCircleFilled,
                        contentDescription = "Toggle Speech",
                        tint = Color(0xFF00796B),
                        modifier = Modifier.size(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = audio.textToSpeak,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkMode) Color.White else Color(0xFF022C22),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = audio.label,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Slider(
                    value = audioProgress,
                    onValueChange = onProgressChange,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFF00796B),
                        thumbColor = Color(0xFF004D40)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val progressSec = (audioProgress * 4f).roundToInt()
                    Text("0:0$progressSec", fontSize = 11.sp, color = Color.Gray)
                    Text("0:04", fontSize = 11.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (audioVolume == 0f) Icons.Filled.VolumeMute else Icons.Filled.VolumeUp,
                        contentDescription = "Volume",
                        tint = Color(0xFF00796B),
                        modifier = Modifier.size(18.dp)
                    )
                    Slider(
                        value = audioVolume,
                        onValueChange = onVolumeChange,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.LightGray,
                            thumbColor = Color(0xFF004D40)
                        )
                    )
                }
            }
        }
    }
}
