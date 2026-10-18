package com.biowellness.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.biowellness.app.ui.theme.*
import com.biowellness.app.utils.ChatMessage
import com.biowellness.app.viewmodel.WellnessViewModel

@Composable
fun AiChatScreen(nav: NavHostController, vm: WellnessViewModel) {
    val messages     by vm.chatMessages.collectAsState()
    val isLoading    by vm.chatLoading.collectAsState()
    val liveAnalysis by vm.liveAnalysis.collectAsState()
    val report       by vm.report.collectAsState()

    var inputText    by remember { mutableStateOf("") }
    val listState    = rememberLazyListState()
    val keyboardCtrl = LocalSoftwareKeyboardController.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val suggestions = listOf(
        "How do I do a proper squat?",
        "Build me a 3-day beginner workout",
        "What does my stress score mean?",
        "Explain my HRV result",
        "Is my resting heart rate normal?",
        "How should I warm up before lifting?",
        "What should I eat after a workout?",
        "How much sleep do I need to recover?",
        "How can I reduce muscle soreness?",
        "Why can't this app measure blood pressure?"
    )

    Column(modifier = Modifier.fillMaxSize().background(color = BackgroundDark)) {

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(listOf(Color(0xFF061428), BackgroundDark)))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextSecondary)
            }
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp)
                        .background(color = CyanPrimary.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI",
                        tint = CyanPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "BioWellness AI Assistant",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "● Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = GreenGood
                    )
                }
            }
            if (messages.isNotEmpty()) {
                IconButton(onClick = { vm.clearChat() }) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear", tint = TextMuted)
                }
            }
        }

        // Context banner
        if (liveAnalysis.wellnessScore > 0 || report != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CyanPrimary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(width = 1.dp, color = CyanPrimary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Context",
                        tint = CyanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val parts = mutableListOf<String>()
                    if (liveAnalysis.wellnessScore > 0) {
                        parts.add("Wellness: ${liveAnalysis.wellnessScore}")
                        parts.add("Stress: ${liveAnalysis.stressLevel}")
                    }
                    Text(
                        text = "AI has your live data: ${parts.joinToString(" · ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanPrimary
                    )
                }
            }
        }

        // Messages or welcome
        if (messages.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier.size(80.dp).background(
                        brush = Brush.radialGradient(listOf(CyanPrimary.copy(alpha = 0.2f), CyanPrimary.copy(alpha = 0.04f))),
                        shape = CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI",
                        tint = CyanPrimary, modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "How can I help you?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ask me anything about your results,\nhow features work, or what your readings mean.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "SUGGESTED QUESTIONS",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                suggestions.forEach { s ->
                    AiSuggestionChip(text = s, onClick = { vm.sendChatMessage(s) })
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(count = messages.size) { i -> AiChatBubble(message = messages[i]) }
            }
        }

        // Input section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = CardDark)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (messages.isNotEmpty() && !isLoading) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    val quick = listOf("Explain this", "What should I do?", "Is this normal?", "How to improve?")
                    items(count = quick.size) { i ->
                        Box(
                            modifier = Modifier
                                .background(color = CyanPrimary.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp))
                                .border(width = 1.dp, color = CyanPrimary.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp))
                                .clickable { vm.sendChatMessage(quick[i]) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = quick[i], color = CyanPrimary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(text = "Ask anything about your health...", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = CyanPrimary,
                        unfocusedBorderColor    = Color(0xFF1A3A5C),
                        focusedTextColor        = TextPrimary,
                        unfocusedTextColor      = TextPrimary,
                        cursorColor             = CyanPrimary,
                        focusedContainerColor   = Color(0xFF071325),
                        unfocusedContainerColor = Color(0xFF071325)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && !isLoading) {
                                vm.sendChatMessage(inputText); inputText = ""; keyboardCtrl?.hide()
                            }
                        }
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                val canSend = inputText.isNotBlank() && !isLoading
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(color = if (canSend) CyanPrimary else CardDarker, shape = CircleShape)
                        .clickable(enabled = canSend) {
                            vm.sendChatMessage(inputText); inputText = ""; keyboardCtrl?.hide()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = CyanPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send",
                            tint = if (canSend) BackgroundDark else TextMuted, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AiChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(modifier = Modifier.size(32.dp)
                .background(color = CyanPrimary.copy(alpha = 0.15f), shape = CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI",
                    tint = CyanPrimary, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        val bubbleShape = RoundedCornerShape(
            topStart    = if (isUser) 16.dp else 4.dp,
            topEnd      = if (isUser) 4.dp else 16.dp,
            bottomStart = 16.dp, bottomEnd = 16.dp
        )
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    color = when {
                        message.isError -> RedAlert.copy(alpha = 0.12f)
                        isUser          -> CyanPrimary.copy(alpha = 0.15f)
                        else            -> CardDark
                    },
                    shape = bubbleShape
                )
                .border(width = 1.dp,
                    color = when {
                        message.isError -> RedAlert.copy(alpha = 0.3f)
                        isUser          -> CyanPrimary.copy(alpha = 0.3f)
                        else            -> Color(0xFF1A3A5C)
                    },
                    shape = bubbleShape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (message.isLoading) {
                AiTypingIndicator()
            } else {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        message.isError -> RedAlert
                        isUser          -> TextPrimary
                        else            -> TextSecondary
                    },
                    lineHeight = 20.sp
                )
            }
        }
        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(32.dp)
                .background(color = CyanPrimary.copy(alpha = 0.15f), shape = CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.Person, contentDescription = "User",
                    tint = CyanPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun AiTypingIndicator() {
    val inf = rememberInfiniteTransition(label = "typing")
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        for (i in 0..2) {
            val alpha by inf.animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = i * 150),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$i"
            )
            Box(modifier = Modifier.size(8.dp)
                .background(color = CyanPrimary.copy(alpha = alpha), shape = CircleShape))
        }
    }
}

@Composable
fun AiSuggestionChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(color = CardDark, shape = RoundedCornerShape(12.dp))
            .border(width = 1.dp, color = Color(0xFF1A3A5C), shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Ask",
                tint = CyanPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        }
    }
}