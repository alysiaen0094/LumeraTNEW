package com.lumera.app.ui.profiles

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumera.app.ui.settings.ThemeEditorScreen
import com.lumera.app.R
import com.lumera.app.data.model.ProfileEntity
import com.lumera.app.data.model.ThemeEntity
import com.lumera.app.ui.addons.VoidButton
import com.lumera.app.ui.components.CenterCarouselRow
import com.lumera.app.ui.home.DpadRepeatGate
import com.lumera.app.ui.theme.DefaultThemes
import com.lumera.app.ui.theme.LumeraTheme
import com.lumera.app.ui.theme.ThemeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

private const val PROFILE_HORIZONTAL_REPEAT_INTERVAL_MS = 150L

@Composable
fun ProfileScreen(
    profiles: List<ProfileEntity>,
    onProfileSelected: (ProfileEntity) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val wizardStep by viewModel.wizardStep.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // FIX: If loading, just show black background (or nothing)
        // This prevents the "WelcomeView" from flashing briefly.
        if (!isLoading) {
            AnimatedContent(
                targetState = wizardStep,
                transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) },
                label = "ProfileFlow"
            ) { step ->
                when (step) {
                    0 -> {
                        // ZERO STATE or SELECTOR
                        if (profiles.isEmpty()) {
                            WelcomeView(onStart = { viewModel.startWizard() })
                        } else {
                            ProfileSelectorView(
                                profiles = profiles,
                                onSelect = onProfileSelected,
                                onAdd = { viewModel.startWizard() },
                                onEdit = { viewModel.startEditWizard(it) },
                                onDelete = { viewModel.deleteProfile(it.id) },
                                viewModel = viewModel
                            )
                        }
                    }
                    1 -> WizardNameStep(
                        initialName = viewModel.tempName,
                        onNext = { viewModel.setWizardName(it) },
                        onCancel = { viewModel.cancelWizard() }
                    )
                    
                    2 -> WizardAvatarStep(
                        onNext = { viewModel.setWizardAvatar(it) },
                        onBack = { viewModel.goBackStep() }
                    )
                }
            }
        }

    }
}


// --- 1. WELCOME VIEW ---
@Composable
fun WelcomeView(onStart: () -> Unit) {
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(100); requester.requestFocus() }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "WELCOME TO LUMERA",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 4.sp),
            color = Color.White
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Your cinematic universe awaits.",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )
        Spacer(Modifier.height(48.dp))

        VoidButton(
            text = "Create First Profile",
            onClick = onStart,
            isPrimary = true,
            modifier = Modifier.width(250.dp),
            focusRequester = requester
        )
    }
}

// --- 2. SELECTOR VIEW ---
@Composable
fun ProfileSelectorView(
    profiles: List<ProfileEntity>,
    onSelect: (ProfileEntity) -> Unit,
    onAdd: () -> Unit,
    onEdit: (ProfileEntity) -> Unit,
    onDelete: (ProfileEntity) -> Unit,
    viewModel: ProfileViewModel
) {
    var activeProfileForOptions by remember { mutableStateOf<ProfileEntity?>(null) }
    var setupTargetProfile by remember { mutableStateOf<ProfileEntity?>(null) }
    var showSetupChoiceDialog by remember { mutableStateOf(false) }
    var showCopyFromDialog by remember { mutableStateOf(false) }
    var showScratchConfirmDialog by remember { mutableStateOf(false) }
    val isInitializingProfile by viewModel.isInitializingProfile.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "WHO IS WATCHING?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        Spacer(Modifier.height(50.dp))

        // Static centered row (no scrolling)
        val focusRequesters = remember(profiles.size) { 
            List(profiles.size) { FocusRequester() } 
        }
        
        // Request focus on first profile
        LaunchedEffect(profiles.size, activeProfileForOptions) {
            if (profiles.isNotEmpty() && activeProfileForOptions == null) {
                delay(100)
                focusRequesters[0].requestFocus()
            }
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            profiles.forEachIndexed { index, profile ->
                ProfileCard(
                    profile = profile,
                    onClick = {
                        if (isInitializingProfile) return@ProfileCard
                    
                        if (viewModel.needsInitialSetup(profile.id)) {
                            val sourceProfile = profiles.firstOrNull { it.id != profile.id }
                    
                            if (sourceProfile != null) {
                                viewModel.initializeProfileByCopy(profile.id, sourceProfile.id) {
                                    onSelect(profile)
                                }
                            } else {
                                viewModel.initializeProfileFromScratch(profile.id) {
                                    onSelect(profile)
                                }
                            }
                        } else {
                            onSelect(profile)
                        }
                    },
                    onEdit = { activeProfileForOptions = profile },
                    focusRequester = focusRequesters[index]
                )
            }

            // Single-profile Troy build: do not allow adding more profiles.
            if (profiles.size < 6) {
                AddProfileCard(onClick = onAdd)
            }
        }
    }

    if (activeProfileForOptions != null) {
        ProfileOptionsDialog(
            profile = activeProfileForOptions!!,
            onDismiss = { activeProfileForOptions = null },
            onEdit = {
                onEdit(activeProfileForOptions!!)
                activeProfileForOptions = null
            },
            onDelete = {
                onDelete(activeProfileForOptions!!)
                activeProfileForOptions = null
            }
        )
    }

    val setupTarget = setupTargetProfile
    if (showSetupChoiceDialog && setupTarget != null) {
        val sourceProfiles = profiles.filter { it.id != setupTarget.id }
        ProfileInitialSetupDialog(
            profileName = setupTarget.name,
            canCopy = sourceProfiles.isNotEmpty(),
            isLoading = isInitializingProfile,
            onCopy = {
                showSetupChoiceDialog = false
                showCopyFromDialog = true
            },
            onStartScratch = {
                showSetupChoiceDialog = false
                showScratchConfirmDialog = true
            },
            onDismiss = {
                showSetupChoiceDialog = false
                setupTargetProfile = null
            }
        )
    }

    if (showCopyFromDialog && setupTarget != null) {
        val sourceProfiles = profiles.filter { it.id != setupTarget.id }
        CopyProfileSelectionDialog(
            targetProfileName = setupTarget.name,
            sourceProfiles = sourceProfiles,
            isLoading = isInitializingProfile,
            onSelectProfile = { sourceProfile ->
                viewModel.initializeProfileByCopy(setupTarget.id, sourceProfile.id) {
                    showCopyFromDialog = false
                    setupTargetProfile = null
                    onSelect(setupTarget)
                }
            },
            onBack = {
                showCopyFromDialog = false
                showSetupChoiceDialog = true
            },
            onDismiss = {
                showCopyFromDialog = false
                setupTargetProfile = null
            }
        )
    }

    if (showScratchConfirmDialog && setupTarget != null) {
        ScratchConfirmDialog(
            profileName = setupTarget.name,
            isLoading = isInitializingProfile,
            onConfirm = {
                viewModel.initializeProfileFromScratch(setupTarget.id) {
                    showScratchConfirmDialog = false
                    setupTargetProfile = null
                    onSelect(setupTarget)
                }
            },
            onBack = {
                showScratchConfirmDialog = false
                showSetupChoiceDialog = true
            },
            onDismiss = {
                showScratchConfirmDialog = false
                setupTargetProfile = null
            }
        )
    }
}

@Composable
private fun ProfileInitialSetupDialog(
    profileName: String,
    canCopy: Boolean,
    isLoading: Boolean,
    onCopy: () -> Unit,
    onStartScratch: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .border(2.dp, Color(0xFF333333), RoundedCornerShape(24.dp))
                .padding(32.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Set Up \"$profileName\"",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Choose how this profile should start.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(Modifier.height(24.dp))

                VoidButton(
                    text = if (canCopy) "Copy From Another Profile" else "No Profile Available To Copy",
                    onClick = onCopy,
                    enabled = canCopy && !isLoading,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                VoidButton(
                    text = "Start From Scratch",
                    onClick = onStartScratch,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                VoidButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CopyProfileSelectionDialog(
    targetProfileName: String,
    sourceProfiles: List<ProfileEntity>,
    isLoading: Boolean,
    onSelectProfile: (ProfileEntity) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(560.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .border(2.dp, Color(0xFF333333), RoundedCornerShape(24.dp))
                .padding(32.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Copy Configuration",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Select the profile to copy into \"$targetProfileName\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(Modifier.height(20.dp))

                if (sourceProfiles.isEmpty()) {
                    Text(
                        text = "No available profiles to copy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                } else {
                    sourceProfiles.forEach { sourceProfile ->
                        VoidButton(
                            text = sourceProfile.name,
                            onClick = { onSelectProfile(sourceProfile) },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                VoidButton(
                    text = "Back",
                    onClick = onBack,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ScratchConfirmDialog(
    profileName: String,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .border(2.dp, Color(0xFF333333), RoundedCornerShape(24.dp))
                .padding(32.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Start From Scratch?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "\"$profileName\" will start with no addons, no integrations, and a fresh home setup.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(Modifier.height(24.dp))
                VoidButton(
                    text = "Yes, Start Fresh",
                    onClick = onConfirm,
                    enabled = !isLoading,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                VoidButton(
                    text = "Back",
                    onClick = onBack,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ProfileOptionsDialog(
    profile: ProfileEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val editRequester = remember { FocusRequester() }

    var areButtonsReady by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        editRequester.requestFocus()
        delay(400)
        areButtonsReady = true
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(400.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .border(2.dp, Color(0xFF333333), RoundedCornerShape(24.dp))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val avatarSource = ProfileAssets.getAvatarSource(profile.avatarRef)

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarSource)
                        .size(300, 300)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = profile.name.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )

                Spacer(Modifier.height(32.dp))

                VoidButton(
                    text = "Edit Profile",
                    onClick = {
                        if (areButtonsReady) onEdit()
                    },
                    isPrimary = false,
                    modifier = Modifier.fillMaxWidth(),
                    focusRequester = editRequester
                )

                Spacer(Modifier.height(12.dp))

                VoidButton(
                    text = "Delete Profile",
                    onClick = {
                        if (areButtonsReady) showDeleteConfirm = true
                    },
                    isPrimary = false,
                    isDestructive = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                VoidButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    isPrimary = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            profileName = profile.name,
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = {
                showDeleteConfirm = false
            }
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    profileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val noRequester = remember { FocusRequester() }
    var isReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        noRequester.requestFocus()
        isReady = true
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(400.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .border(2.dp, Color(0xFF333333), RoundedCornerShape(24.dp))
                .padding(32.dp)
                .alpha(if (isReady) 1f else 0f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Delete Profile?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Are you sure you want to delete \"${profileName}\"?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(Modifier.height(32.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    VoidButton(
                        text = "Yes",
                        onClick = onConfirm,
                        isPrimary = false,
                        isDestructive = true,
                        modifier = Modifier.weight(1f)
                    )
                    VoidButton(
                        text = "No",
                        onClick = onDismiss,
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        focusRequester = noRequester
                    )
                }
            }
        }
    }
}

// --- 3. WIZARD STEPS ---

@Composable
fun WizardNameStep(initialName: String, onNext: (String) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(initialName.take(18)) }
    val firstKeyRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(120)
        firstKeyRequester.requestFocus()
    }

    BackHandler { onCancel() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Name Your Profile",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 28.sp
            ),
            color = Color.White.copy(alpha = 0.94f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Use your remote to enter a profile name",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp
            ),
            color = Color.White.copy(alpha = 0.42f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(30.dp))

        ProfileNameInput(value = name)

        Spacer(Modifier.height(34.dp))

        ProfileNameKeyboard(
            value = name,
            firstKeyRequester = firstKeyRequester,
            onValueChange = { name = it.take(18) },
            onDone = {
                val cleanName = name.trim()
                if (cleanName.isNotEmpty()) {
                    onNext(cleanName)
                }
            }
        )
    }
}

@Composable
private fun ProfileNameInput(value: String) {
    Box(
        modifier = Modifier
            .width(440.dp)
            .height(66.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.ifBlank { "PROFILE NAME" },
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 25.sp,
                letterSpacing = if (value.isBlank()) 1.4.sp else 0.4.sp
            ),
            color = if (value.isBlank()) {
                Color.White.copy(alpha = 0.28f)
            } else {
                Color.White.copy(alpha = 0.96f)
            },
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun ProfileNameKeyboard(
    value: String,
    firstKeyRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("Z", "X", "C", "V", "B", "N", "M", "⌫"),
        listOf("SPACE", "DONE")
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEachIndexed { index, key ->
                    val isBackspace = key == "⌫"
                    val isSpace = key == "SPACE"
                    val isDone = key == "DONE"

                    ProfileKeyboardKey(
                        text = key,
                        enabled = when {
                            isBackspace -> value.isNotEmpty()
                            isDone -> value.trim().isNotEmpty()
                            else -> true
                        },
                        width = when {
                            isSpace -> 180.dp
                            isDone -> 150.dp
                            isBackspace -> 72.dp
                            else -> 47.dp
                        },
                        height = 44.dp,
                        focusRequester = if (rowIndex == 0 && index == 0) firstKeyRequester else null,
                        onClick = {
                            when {
                                isBackspace -> onValueChange(value.dropLast(1))
                                isSpace -> {
                                    if (value.isNotBlank() && !value.endsWith(" ")) {
                                        onValueChange((value + " ").take(18))
                                    }
                                }
                                isDone -> onDone()
                                else -> onValueChange((value + key).take(18))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileKeyboardKey(
    text: String,
    enabled: Boolean,
    width: Dp,
    height: Dp,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        label = "profileKeyScale"
    )

    val background by animateColorAsState(
        targetValue = when {
            !enabled -> Color.White.copy(alpha = 0.035f)
            isFocused -> Color.White.copy(alpha = 0.96f)
            else -> Color.White.copy(alpha = 0.11f)
        },
        label = "profileKeyBackground"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.White.copy(alpha = 0.18f)
            isFocused -> Color.Black.copy(alpha = 0.94f)
            else -> Color.White.copy(alpha = 0.88f)
        },
        label = "profileKeyContent"
    )

    val requesterModifier = if (focusRequester != null) {
        Modifier.focusRequester(focusRequester)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .scale(scale)
            .then(requesterModifier)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .onPreviewKeyEvent { event ->
                if (
                    enabled &&
                    event.type == KeyEventType.KeyDown &&
                    (
                        event.key == Key.Enter ||
                            event.key == Key.DirectionCenter ||
                            event.key == Key.NumPadEnter
                    )
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .focusable(enabled = enabled, interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = when (text) {
                    "SPACE", "DONE" -> 14.sp
                    "⌫" -> 18.sp
                    else -> 15.sp
                },
                letterSpacing = 0.4.sp
            ),
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun WizardAvatarStep(onNext: (String) -> Unit, onBack: () -> Unit) {
    val avatars = ProfileAssets.AVATAR_MAP.toList()
    val initialIndex = avatars.size / 2
    val listState = rememberLazyListState()
    val horizontalRepeatGate = remember {
        DpadRepeatGate(horizontalRepeatIntervalMs = PROFILE_HORIZONTAL_REPEAT_INTERVAL_MS)
    }
    val focusRequesters = remember(avatars.size) { List(avatars.size) { FocusRequester() } }
    
    LaunchedEffect(Unit) {
        listState.scrollToItem(initialIndex)
        delay(100)
        focusRequesters[initialIndex].requestFocus()
    }
    BackHandler { onBack() }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Choose an Avatar", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(16.dp))
        Text("Select an avatar that represents you.", color = Color.Gray)

        Spacer(Modifier.height(40.dp))

        CenterCarouselRow(
            itemWidth = 120.dp,
            itemSpacing = 24.dp,
            state = listState,
            modifier = Modifier.onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)
                ) {
                    horizontalRepeatGate.shouldConsume(event)
                } else {
                    false
                }
            }
        ) {
            items(avatars.size) { index ->
                val (key, resId) = avatars[index]
                AvatarGridItem(
                    resId = resId,
                    onClick = { onNext(key) },
                    focusRequester = focusRequesters[index],
                    modifier = Modifier.size(120.dp)
                )
            }
        }        
    }
}

@Composable
private fun UploadAvatarButton(
    onClick: () -> Unit,
    focusRequester: FocusRequester
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f)
    
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isFocused) MaterialTheme.colorScheme.primary
                else Color.White.copy(alpha = 0.1f)
            )
            .border(
                width = 2.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .focusRequester(focusRequester)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Upload Your Own",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (isFocused) Color.Black else Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun WizardThemeStep(onFinish: (String) -> Unit, onBack: () -> Unit) {
    val themeManager: ThemeManager = hiltViewModel()
    val themes = DefaultThemes.ALL
    val initialIndex = themes.size / 2
    var previewTheme by remember { mutableStateOf(themes[initialIndex]) }
    val listState = rememberLazyListState()
    val horizontalRepeatGate = remember {
        DpadRepeatGate(horizontalRepeatIntervalMs = PROFILE_HORIZONTAL_REPEAT_INTERVAL_MS)
    }
    val focusRequesters = remember(themes.size) { List(themes.size) { FocusRequester() } }
    val createButtonRequester = remember { FocusRequester() }
    
    var showThemeEditor by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        listState.scrollToItem(initialIndex)
        delay(100)
        focusRequesters[initialIndex].requestFocus()
    }
    BackHandler { onBack() }

    LumeraTheme(theme = previewTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Offset for theme name labels below circles (not present in avatar selector)
            Spacer(Modifier.height(32.dp))
            
            Text("Choose a Theme", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(16.dp))
            Text("Select a color scheme for your experience.", color = Color.Gray)

            Spacer(Modifier.height(40.dp))

            CenterCarouselRow(
                itemWidth = 120.dp,
                itemSpacing = 24.dp,
                state = listState,
                modifier = Modifier.onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)
                    ) {
                        horizontalRepeatGate.shouldConsume(event)
                    } else {
                        false
                    }
                }
            ) {
                items(themes.size) { index ->
                    val theme = themes[index]
                    ThemePickItem(
                        theme = theme,
                        onClick = { onFinish(theme.id) },
                        focusRequester = focusRequesters[index],
                        onFocused = { previewTheme = theme }
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            Text(
                "Or",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            
            Spacer(Modifier.height(16.dp))
            
            CreateThemeButton(
                onClick = { showThemeEditor = true },
                focusRequester = createButtonRequester
            )
        }
    }
    
    // Theme Editor Dialog
    if (showThemeEditor) {
        Dialog(
            onDismissRequest = { showThemeEditor = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(32.dp)
            ) {
                ThemeEditorScreen(
                    editingTheme = null,
                    onSave = { name, primary, background ->
                        val newThemeId = themeManager.createCustomTheme(
                            name = name,
                            primaryColor = primary,
                            backgroundColor = background
                        )
                        showThemeEditor = false
                        onFinish(newThemeId)
                    },
                    onCancel = { showThemeEditor = false }
                )
            }
        }
    }
}

@Composable
private fun CreateThemeButton(
    onClick: () -> Unit,
    focusRequester: FocusRequester
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f)
    
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isFocused) MaterialTheme.colorScheme.primary
                else Color.White.copy(alpha = 0.1f)
            )
            .border(
                width = 2.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .focusRequester(focusRequester)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Create Your Own",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (isFocused) Color.Black else Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun ThemePickItem(
    theme: ThemeEntity,
    onClick: () -> Unit,
    focusRequester: FocusRequester?,
    onFocused: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.1f else 1f)

    LaunchedEffect(isFocused) {
        if (isFocused) onFocused?.invoke()
    }

    val focusModifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .then(focusModifier)
                .clickable(interactionSource = interactionSource, indication = null) { onClick() }
                .focusable(interactionSource = interactionSource),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(Color(theme.backgroundColor.toInt()))
                    .border(
                        3.dp,
                        if (isFocused) Color.White else Color.Transparent,
                        CircleShape
                    )
                    .padding(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(theme.primaryColor.toInt()))
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = theme.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (isFocused) Color.White else Color.Gray
        )
    }
}

@Composable
fun ProfileCard(
    profile: ProfileEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardFocused by cardInteractionSource.collectIsFocusedAsState()
    
    val editInteractionSource = remember { MutableInteractionSource() }
    val isEditFocused by editInteractionSource.collectIsFocusedAsState()
    
    // Pencil is visible when either card or pencil is focused
    val isPencilVisible = isCardFocused || isEditFocused
    
    val context = LocalContext.current

    val scale by animateFloatAsState(if (isCardFocused) 1.1f else 1f)
    val borderAlpha by animateFloatAsState(if (isCardFocused || isEditFocused) 1f else 0f)

    // CONVERT STRING ("avatar_5") -> Source (Int or File)
    val avatarSource = ProfileAssets.getAvatarSource(profile.avatarRef)

    val focusModifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1A))
                .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha), CircleShape)
                .then(focusModifier)
                .onPreviewKeyEvent { event ->
                    val isConfirm = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
                    if (isConfirm && event.type == KeyEventType.KeyUp) {
                        onClick()
                        true
                    } else {
                        false
                    }
                }
                .focusable(interactionSource = cardInteractionSource)
                .clickable(
                    interactionSource = cardInteractionSource,
                    indication = null,
                    onClick = onClick
                )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(avatarSource)
                    .size(300, 300)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = profile.name.uppercase(),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = if(isCardFocused || isEditFocused) Color.White else Color.Gray
        )
        
        Spacer(Modifier.height(8.dp))
        
        // Pencil Edit Icon - always in composition but visibility controlled by alpha
        val editScale by animateFloatAsState(if (isEditFocused) 1.2f else 1f)
        val pencilAlpha by animateFloatAsState(if (isPencilVisible) 1f else 0f)
        
        Box(
            modifier = Modifier
                .size(32.dp)
                .scale(editScale)
                .alpha(pencilAlpha)
                .clip(CircleShape)
                .background(if (isEditFocused) Color.White else Color.Transparent)
                .clickable(
                    interactionSource = editInteractionSource,
                    indication = null,
                    onClick = onEdit
                )
                .focusable(interactionSource = editInteractionSource),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Profile",
                tint = if (isEditFocused) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun AddProfileCard(onClick: () -> Unit, focusRequester: FocusRequester? = null) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.1f else 1f)

    val focusModifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.1f))
                .border(2.dp, if (isFocused) Color.White else Color.Transparent, CircleShape)
                .then(focusModifier)
                .onPreviewKeyEvent { event ->
                    val isConfirm =
                        event.key == Key.DirectionCenter ||
                            event.key == Key.Enter ||
                            event.key == Key.NumPadEnter

                    if (isConfirm && event.type == KeyEventType.KeyUp) {
                        onClick()
                        true
                    } else {
                        false
                    }
                }
                .clickable(interactionSource = interactionSource, indication = null) {
                    onClick()
                }
                .focusable(interactionSource = interactionSource),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Profile",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "ADD PROFILE",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = if (isFocused) Color.White else Color.Gray
        )

        Spacer(Modifier.height(8.dp))
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun AvatarGridItem(
    resId: Int, 
    onClick: () -> Unit, 
    focusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
    onFocused: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.15f else 1f)

    LaunchedEffect(isFocused) {
        if (isFocused) onFocused?.invoke()
    }

    val focusModifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
    val context = LocalContext.current

    Box(
        modifier = modifier
            .then(focusModifier)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .border(3.dp, if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
        ) {
            AsyncImage(
            model = ImageRequest.Builder(context)
                .data(resId)
                .size(300, 300)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
}
