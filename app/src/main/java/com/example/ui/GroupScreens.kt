package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.ripple.rememberRipple
import com.example.data.db.*
import com.example.ui.theme.SimChatPrimary
import com.example.viewmodel.SimChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.Serializable
import java.util.UUID

// REUSABLE PREMIUM GLASSMORPHISM LAYER
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
        ),
        border = BorderStroke(1.2.dp, Brush.linearGradient(
            colors = listOf(
                SimChatPrimary.copy(alpha = 0.35f),
                SimChatPrimary.copy(alpha = 0.08f)
            )
        )),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            content()
        }
    }
}

// PREMIUM GRADIENT PRESET PROFILE AVATARS
val AvatarPresets = listOf(
    "🏆" to Color(0xFFFFD54F),
    "💡" to Color(0xFF4FC3F7),
    "🔥" to Color(0xFFFF8A65),
    "🎪" to Color(0xFFBA68C8),
    "🎯" to Color(0xFFE57373),
    "🚀" to Color(0xFF81C784),
    "💻" to Color(0xFF90A4AE),
    "📣" to Color(0xFFA1887F)
)

@Composable
fun AvatarPresetSelector(
    selectedAvatar: String,
    onAvatarSelected: (String) -> Unit
) {
    Column {
        Text(
            "Select Group Icon Emoji Profile",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = SimChatPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AvatarPresets.forEach { (emoji, color) ->
                val isSelected = selectedAvatar == emoji
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) SimChatPrimary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onAvatarSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 28.sp)
                }
            }
        }
    }
}

// CREATE GROUP SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    viewModel: SimChatViewModel,
    onBack: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var groupName by remember { mutableStateOf("") }
    var groupDesc by remember { mutableStateOf("") }
    var groupCategory by remember { mutableStateOf("Chat") } // Chat, Social, Office, Study, Announcement
    var groupType by remember { mutableStateOf("Private") } // Private, Public, Community, Announcement
    var selectedEmojiAvatar by remember { mutableStateOf("💡") }
    
    // Privacy and admin settings states
    var hiddenMembers by remember { mutableStateOf(false) }
    var adminOnlyMessage by remember { mutableStateOf(false) }
    var adminOnlyAnnounce by remember { mutableStateOf(false) }
    var requireApproval by remember { mutableStateOf(false) }
    var isEncrypted by remember { mutableStateOf(true) }
    
    // Profile members selection
    val contacts by viewModel.contacts.collectAsState(initial = emptyList())
    var selectedContacts by remember { mutableStateOf<Set<ContactEntity>>(emptySet()) }
    var contactQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Premium Group", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            SimChatPrimary.copy(alpha = 0.05f)
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // GLASSMORPHIC BUILDER COVER
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Big icon display
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                AvatarPresets
                                    .firstOrNull { it.first == selectedEmojiAvatar }?.second
                                    ?: Color(0xFF81C784)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(selectedEmojiAvatar, fontSize = 40.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        TextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            placeholder = { Text("Group Name...", fontSize = 16.sp) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = SimChatPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextField(
                            value = groupDesc,
                            onValueChange = { groupDesc = it },
                            placeholder = { Text("Description...", fontSize = 13.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = SimChatPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                AvatarPresetSelector(
                    selectedAvatar = selectedEmojiAvatar,
                    onAvatarSelected = { selectedEmojiAvatar = it }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // CATEGORY & TYPE SEGMENTED ROW
            Text("Group Classification", fontWeight = FontWeight.Bold, color = SimChatPrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Category", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Chat", "Office", "Announcement").forEach { cat ->
                                OutlinedButton(
                                    onClick = { groupCategory = cat },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (groupCategory == cat) SimChatPrimary.copy(alpha = 0.15f) else Color.Transparent
                                    ),
                                    border = BorderStroke(1.dp, if (groupCategory == cat) SimChatPrimary else Color.LightGray),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(cat, fontSize = 11.sp, color = if (groupCategory == cat) SimChatPrimary else Color.DarkGray)
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Group Type", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Private", "Public", "Announcement").forEach { type ->
                                OutlinedButton(
                                    onClick = { 
                                        groupType = type 
                                        if (type == "Announcement") {
                                            adminOnlyMessage = true
                                            adminOnlyAnnounce = true
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (groupType == type) SimChatPrimary.copy(alpha = 0.15f) else Color.Transparent
                                    ),
                                    border = BorderStroke(1.dp, if (groupType == type) SimChatPrimary else Color.LightGray),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(type, fontSize = 11.sp, color = if (groupType == type) SimChatPrimary else Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ADVANCED PRIVACY TOGGLES
            Text("Advanced Privacy & Security", fontWeight = FontWeight.Bold, color = SimChatPrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Hidden Member List", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Only admins can see member registries", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = hiddenMembers,
                            onCheckedChange = { hiddenMembers = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = SimChatPrimary)
                        )
                    }

                    HorizontalDivider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Admin-Only Messaging", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Restrict message sending to admins only", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = adminOnlyMessage,
                            onCheckedChange = { adminOnlyMessage = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = SimChatPrimary)
                        )
                    }

                    HorizontalDivider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Join Request Approval", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Requires Admin authorization in invite links", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = requireApproval,
                            onCheckedChange = { requireApproval = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = SimChatPrimary)
                        )
                    }

                    HorizontalDivider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("End-to-End Encrypted", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Secure Cloud messaging channel", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isEncrypted,
                            onCheckedChange = { isEncrypted = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = SimChatPrimary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // CONTACTS MULTI-SELECT REGISTER
            Text("Add Group Members (${selectedContacts.size} Selected)", fontWeight = FontWeight.Bold, color = SimChatPrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = contactQuery,
                    onValueChange = { contactQuery = it },
                    placeholder = { Text("Search contact catalog...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                val filteredContacts = contacts.filter { 
                    it.name.contains(contactQuery, ignoreCase = true) || it.phone.contains(contactQuery) 
                }

                if (filteredContacts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No contacts found", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredContacts.take(5).forEach { contact ->
                            val isSelected = selectedContacts.contains(contact)
                            Row(
                                modifier = Modifier
                                    .fillModifierClickable {
                                        selectedContacts = if (isSelected) {
                                            selectedContacts - contact
                                        } else {
                                            selectedContacts + contact
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F5E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        contact.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = SimChatPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(contact.phone, fontSize = 11.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        if (contact.isCloud) {
                                            Badge(containerColor = SimChatPrimary.copy(alpha = 0.15f), contentColor = SimChatPrimary) { Text("Cloud", fontSize = 8.sp) }
                                        } else {
                                            Badge(containerColor = Color.LightGray.copy(alpha = 0.3f), contentColor = Color.DarkGray) { Text("SMS Fallback Eligible", fontSize = 8.sp) }
                                        }
                                    }
                                }
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        selectedContacts = if (isSelected) {
                                            selectedContacts - contact
                                        } else {
                                            selectedContacts + contact
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = SimChatPrimary)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    if (groupName.isEmpty()) {
                        Toast.makeText(context, "Group Name is required!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        val gId = viewModel.repository.createPremiumGroup(
                            name = groupName,
                            desc = groupDesc,
                            avatar = selectedEmojiAvatar,
                            category = groupCategory,
                            groupType = groupType,
                            memberList = selectedContacts.toList(),
                            hiddenMembers = hiddenMembers,
                            adminOnlyMessage = adminOnlyMessage,
                            adminOnlyAnnounce = adminOnlyAnnounce,
                            requireApproval = requireApproval,
                            isEncrypted = isEncrypted
                        )
                        Toast.makeText(context, "Premium Group Setup Finalized!", Toast.LENGTH_SHORT).show()
                        onGroupCreated(gId)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("submit_premium_group_button"),
                colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Generate Group Ecosystem", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// INVITE QR CODE CORE CANVAS RENDERER
@Composable
fun GroupQrCodeRenderer(groupId: String) {
    Canvas(
        modifier = Modifier
            .size(160.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(2.dp, SimChatPrimary, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        val size = size.width
        val squareCount = 10
        val cellSize = size / squareCount
        val dynamicSeed = groupId.hashCode()

        // Draw Outer Position Anchors (Top-Left, Top-Right, Bottom-Left)
        drawRect(color = SimChatPrimary, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(cellSize * 3, cellSize * 3))
        drawRect(color = Color.White, topLeft = Offset(cellSize, cellSize), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))

        drawRect(color = SimChatPrimary, topLeft = Offset(size - cellSize * 3, 0f), size = androidx.compose.ui.geometry.Size(cellSize * 3, cellSize * 3))
        drawRect(color = Color.White, topLeft = Offset(size - cellSize * 2, cellSize), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))

        drawRect(color = SimChatPrimary, topLeft = Offset(0f, size - cellSize * 3), size = androidx.compose.ui.geometry.Size(cellSize * 3, cellSize * 3))
        drawRect(color = Color.White, topLeft = Offset(cellSize, size - cellSize * 2), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))

        // Populate inner cells with reproducible seed logic representing the hash URL
        for (row in 0 until squareCount) {
            for (col in 0 until squareCount) {
                // Skip the corners
                if ((row < 3 && col < 3) || (row < 3 && col >= squareCount - 3) || (row >= squareCount - 3 && col < 3)) {
                    continue
                }
                val index = row * squareCount + col
                val filled = (dynamicSeed xor index) % 3 == 0 || (dynamicSeed xor (index * 7)) % 5 == 0
                if (filled) {
                    drawRect(
                        color = Color(0xFF141714),
                        topLeft = Offset(col * cellSize, row * cellSize),
                        size = androidx.compose.ui.geometry.Size(cellSize - 1f, cellSize - 1f)
                    )
                }
            }
        }
    }
}

// SMART ANALYTICS CHART SEGMENT
@Composable
fun SmartEngagementAnalyticsChart() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        val width = size.width
        val height = size.height
        val barCount = 7
        val textHeaderHeight = 24f
        val chartHeight = height - textHeaderHeight
        val barWidth = (width / barCount) * 0.65f
        val gapWidth = (width / barCount) * 0.35f
        
        // Mock analytics data heights
        val activityPoints = listOf(0.4f, 0.75f, 0.6f, 0.95f, 0.5f, 0.85f, 0.9f)
        
        activityPoints.forEachIndexed { idx, value ->
            val cx = idx * (barWidth + gapWidth) + (gapWidth / 2)
            val barHeight = chartHeight * value
            val rectTop = chartHeight - barHeight + textHeaderHeight
            
            // Draw gradient analytics bars
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SimChatPrimary,
                        SimChatPrimary.copy(alpha = 0.25f)
                    )
                ),
                topLeft = Offset(cx, rectTop),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
        }
        
        // Horizontal reference indicator line
        drawLine(
            color = Color.LightGray.copy(alpha = 0.4f),
            start = Offset(0f, chartHeight * 0.5f + textHeaderHeight),
            end = Offset(width, chartHeight * 0.5f + textHeaderHeight),
            strokeWidth = 1.dp.toPx()
        )
    }
}

// GROUP DETAILS PROFILE & MANAGEMENT CONSOLE
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    viewModel: SimChatViewModel,
    groupId: String,
    onBack: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Core data bindings
    var group by remember { mutableStateOf<GroupEntity?>(null) }
    val members by viewModel.getMembersForGroup(groupId).collectAsState(initial = emptyList())
    val polls by viewModel.getPollsForGroup(groupId).collectAsState(initial = emptyList())
    val events by viewModel.getEventsForGroup(groupId).collectAsState(initial = emptyList())
    val logs by viewModel.getNotificationsForGroup(groupId).collectAsState(initial = emptyList())

    // Admin privileges confirmation
    val meEmail = viewModel.sessionManager.userEmail ?: "me@simchat.com"
    val iAmOwnerOrAdmin = members.any { m -> 
        m.memberPhone == meEmail && (m.role == "OWNER" || m.role == "ADMIN") 
    }

    // Modal drawer states
    var showPollCreator by remember { mutableStateOf(false) }
    var showEventCreator by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        group = viewModel.repository.getGroupById(groupId)
    }

    if (group == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SimChatPrimary)
        }
        return
    }

    val activeGroup = group!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Control Deck", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToChat) {
                        Icon(imageVector = Icons.Filled.Chat, contentDescription = "Go to Chat", tint = SimChatPrimary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            SimChatPrimary.copy(alpha = 0.05f)
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            
            // GLASSMORPHIC BRAND HEADLINE
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(
                                AvatarPresets.firstOrNull { it.first == activeGroup.avatarUrl }?.second
                                    ?: Color(0xFF81C784)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(activeGroup.avatarUrl.ifEmpty { "👥" }, fontSize = 54.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(activeGroup.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(activeGroup.description.ifEmpty { "No description declared." }, fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Badge(containerColor = SimChatPrimary.copy(alpha = 0.15f), contentColor = SimChatPrimary) {
                            Text(activeGroup.groupType, fontWeight = FontWeight.Bold)
                        }
                        
                        Badge(containerColor = Color.LightGray.copy(alpha = 0.4f), contentColor = Color.DarkGray) {
                            Text(activeGroup.category)
                        }
                        
                        if (activeGroup.isEncrypted) {
                            Badge(containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF2E7D32)) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Secured End-to-End", fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ECOSYSTEM STATS PANEL
            Text("Community Smart Metrics", fontWeight = FontWeight.Bold, color = SimChatPrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${members.size}", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = SimChatPrimary)
                        Text("Active Members", fontSize = 11.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val activeCount = maxOf(1, (members.size * 0.75).toInt())
                        Text("$activeCount", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFF0288D1))
                        Text("Online Now", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Ecosystem Message Traffic Analytics", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp), color = Color.Gray)
                SmartEngagementAnalyticsChart()
            }

            Spacer(modifier = Modifier.height(20.dp))

            // HYBRID FALLBACK SMS ANALYZER
            Text("Hybrid Fallback Routing Engine", fontWeight = FontWeight.Bold, color = SimChatPrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Fallback Engine translates global Announcement notifications into local carrier SMS broadcasts for members without active high-speed cloud connections.",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                    
                    val fallbackTargets = members.filter { !it.memberPhone.contains("@") }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Eligible Fallback Recipients:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Badge(containerColor = Color(0xFFFFF3E0), contentColor = Color(0xFFE65100)) {
                            Text("${fallbackTargets.size} SMS Targets")
                        }
                    }
                    
                    if (fallbackTargets.isEmpty()) {
                        Text("No contacts are registered as fallback targets in this group.", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            fallbackTargets.forEach { ft ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(ft.memberName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("📡 FALLBACK SMS: ${ft.memberPhone}", fontSize = 11.sp, color = SimChatPrimary)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // DYNAMIC POLL DETAILS CONSOLE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Realtime Polls (${polls.size})", fontWeight = FontWeight.Bold, color = SimChatPrimary, fontSize = 14.sp)
                if (iAmOwnerOrAdmin) {
                    TextButton(onClick = { showPollCreator = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create Poll", fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (polls.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("No active polls in this ecosystem.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                polls.forEach { poll ->
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    poll.question,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (poll.isAnonymous) {
                                    Badge(containerColor = Color.LightGray.copy(alpha = 0.3f), contentColor = Color.DarkGray) {
                                        Text("Anonymous", fontSize = 9.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Created by ${poll.creatorName}", fontSize = 10.sp, color = Color.Gray)
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            val options = poll.optionsJson.split("|")
                            val votes = poll.votesJson.split("|").map { it.toIntOrNull() ?: 0 }
                            val totalVotes = votes.sum().coerceAtLeast(1)

                            options.forEachIndexed { index, option ->
                                val voteCount = votes.getOrElse(index) { 0 }
                                val percentage = voteCount.toFloat() / totalVotes.toFloat()
                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .fillModifierClickable {
                                            // Vote interaction
                                            val updatedVotes = votes.toMutableList()
                                            updatedVotes[index] = updatedVotes[index] + 1
                                            val newVotesJson = updatedVotes.joinToString("|")
                                            viewModel.savePoll(poll.copy(votesJson = newVotesJson))
                                            Toast.makeText(context, "Vote Recorded!", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(option, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text("$voteCount votes (${(percentage * 100).toInt()}%)", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Animated progress bar
                                    LinearProgressIndicator(
                                        progress = { percentage },
                                        color = SimChatPrimary,
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SCHEDULED EVENTS SEGMENT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ecosystem Milestones & Events (${events.size})", fontWeight = FontWeight.Bold, color = SimChatPrimary, fontSize = 14.sp)
                if (iAmOwnerOrAdmin) {
                    TextButton(onClick = { showEventCreator = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Event", fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (events.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("No upcoming events listed.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                events.forEach { event ->
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(event.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Badge(containerColor = Color(0xFFE0F7FA), contentColor = Color(0xFF006064)) {
                                    Text(event.eventDateText, fontSize = 10.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(event.description, fontSize = 12.sp, color = Color.DarkGray)
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            // RSVP actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("RSVP Status Check:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Gray)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            val rsvps = event.rsvpYesList.split(",").filter { it.isNotEmpty() }.toMutableSet()
                                            rsvps.add("You")
                                            viewModel.saveEvent(event.copy(rsvpYesList = rsvps.joinToString(",")))
                                            Toast.makeText(context, "RSVP Recieved: YES", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF2E7D32)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("RSVP YES (${event.rsvpYesList.split(",").filter { it.isNotEmpty() }.size})", fontSize = 10.sp)
                                    }

                                    Button(
                                        onClick = {
                                            val rsvps = event.rsvpNoList.split(",").filter { it.isNotEmpty() }.toMutableSet()
                                            rsvps.add("You")
                                            viewModel.saveEvent(event.copy(rsvpNoList = rsvps.joinToString(",")))
                                            Toast.makeText(context, "RSVP Recieved: NO", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color(0xFFC62828)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("NO (${event.rsvpNoList.split(",").filter { it.isNotEmpty() }.size})", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SHARED MEDIA EXTRAS PANEL
            Text("Shared Multimedia & Documents", fontWeight = FontWeight.Bold, color = SimChatPrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Default.InsertDriveFile, contentDescription = null, tint = SimChatPrimary)
                        Text("3 Documents", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Default.Photo, contentDescription = null, tint = Color(0xFF0288D1))
                        Text("12 Media Images", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = Color(0xFFE91E63))
                        Text("8 Links Shared", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // INVITE LINK & CANVAS QR DISPLAY
            Text("Invite System Portal", fontWeight = FontWeight.Bold, color = SimChatPrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Generate QR Code or Share Link directly to external carriers via fallback SMS.",
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Canvas drawn QR Code Anchor
                    GroupQrCodeRenderer(groupId)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("SimChat Group Invite", activeGroup.inviteUrl)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Invitation link copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = SimChatPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            activeGroup.inviteUrl,
                            fontWeight = FontWeight.Bold,
                            color = SimChatPrimary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = SimChatPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // MEMBER ROLES & PRIVILEGES PANEL
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Group Membership Registry (${members.size})", fontWeight = FontWeight.Bold, color = SimChatPrimary, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    members.forEach { m ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(m.memberName.take(1).uppercase(), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(m.memberName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(containerColor = if (m.role == "OWNER") Color(0xFFFFEBEE) else Color(0xFFE8F5E9), contentColor = if (m.role == "OWNER") Color(0xFFC62828) else Color(0xFF2E7D32)) {
                                        Text(m.role, fontSize = 8.sp)
                                    }
                                }
                                Text(m.memberPhone, fontSize = 11.sp, color = Color.Gray)
                            }
                            
                            // Multi-role admin management triggers
                            if (iAmOwnerOrAdmin && m.memberPhone != meEmail) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Promote Admin
                                    if (m.role == "MEMBER") {
                                        IconButton(onClick = {
                                            viewModel.promoteMember(groupId, m.memberPhone, m.memberName, "ADMIN")
                                            Toast.makeText(context, "${m.memberName} Promoted to Admin!", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = "Make Admin", tint = Color(0xFF0288D1), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    
                                    // Mute Member
                                    IconButton(onClick = {
                                        val mutedValue = !m.isMuted
                                        viewModel.muteMember(groupId, m.memberPhone, mutedValue)
                                        Toast.makeText(context, if (mutedValue) "${m.memberName} Muted!" else "${m.memberName} Unmuted!", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(
                                            imageVector = if (m.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                            contentDescription = "Mute",
                                            tint = if (m.isMuted) Color.Red else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Remove Member
                                    IconButton(onClick = {
                                        scope.launch {
                                            viewModel.removeGroupMember(groupId, m.memberPhone, m.memberName)
                                            Toast.makeText(context, "${m.memberName} Removed!", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(imageVector = Icons.Default.RemoveCircle, contentDescription = "Kick", tint = Color.Red, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ECOSYSTEM DESTRUCTION BUTTON
            Button(
                onClick = {
                    scope.launch {
                        viewModel.leaveGroup(groupId)
                        Toast.makeText(context, "You left $groupId", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Leave & Purge Group Data", fontWeight = FontWeight.Bold)
            }
        }
    }

    // DRAW POLL CREATOR CARD MODAL
    if (showPollCreator) {
        Dialog(onDismissRequest = { showPollCreator = false }) {
            var q by remember { mutableStateOf("") }
            var o1 by remember { mutableStateOf("") }
            var o2 by remember { mutableStateOf("") }
            var anon by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📊 Start A Group Poll", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SimChatPrimary)
                    
                    OutlinedTextField(
                        value = q,
                        onValueChange = { q = it },
                        label = { Text("Poll Question") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = o1,
                        onValueChange = { o1 = it },
                        label = { Text("Option 1") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = o2,
                        onValueChange = { o2 = it },
                        label = { Text("Option 2") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Anonymous Voting", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Switch(checked = anon, onCheckedChange = { anon = it }, colors = SwitchDefaults.colors(checkedThumbColor = SimChatPrimary))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showPollCreator = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (q.isNotEmpty() && o1.isNotEmpty() && o2.isNotEmpty()) {
                                    viewModel.createGroupPoll(groupId, q, listOf(o1, o2), anon)
                                    showPollCreator = false
                                    Toast.makeText(context, "Ecosystem Poll Dispatched!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary)
                        ) {
                            Text("Launch")
                        }
                    }
                }
            }
        }
    }

    // DRAW EVENT CREATOR MODAL
    if (showEventCreator) {
        Dialog(onDismissRequest = { showEventCreator = false }) {
            var evTitle by remember { mutableStateOf("") }
            var evDesc by remember { mutableStateOf("") }
            var evDate by remember { mutableStateOf("") }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🗓️ Schedule Group Event", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SimChatPrimary)
                    
                    OutlinedTextField(
                        value = evTitle,
                        onValueChange = { evTitle = it },
                        label = { Text("Event Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = evDesc,
                        onValueChange = { evDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = evDate,
                        onValueChange = { evDate = it },
                        label = { Text("Date (e.g., June 15, 2026)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showEventCreator = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (evTitle.isNotEmpty() && evDate.isNotEmpty()) {
                                    viewModel.createGroupEvent(groupId, evTitle, evDesc, evDate)
                                    showEventCreator = false
                                    Toast.makeText(context, "Ecosystem Event Slotted!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SimChatPrimary)
                        ) {
                            Text("Schedule")
                        }
                    }
                }
            }
        }
    }
}

// Inline helper extension for clickable modifiers with ripple effects
fun Modifier.fillModifierClickable(onClick: () -> Unit): Modifier {
    return this.then(
        Modifier.clickable(onClick = onClick)
    )
}
