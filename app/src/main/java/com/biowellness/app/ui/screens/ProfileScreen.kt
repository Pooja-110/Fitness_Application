@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.biowellness.app.ui.screens
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.biowellness.app.data.models.UserProfile
import com.biowellness.app.ui.theme.*
import com.biowellness.app.viewmodel.WellnessViewModel


@Composable
fun ProfileScreen(
    nav: NavHostController,
    vm: WellnessViewModel
) {
    val saved by vm.savedProfile.collectAsState()

    var age by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var activity by rememberSaveable { mutableStateOf("") }
    var sleep by rememberSaveable { mutableStateOf("") }
    var stress by rememberSaveable { mutableStateOf("") }
    var smoking by rememberSaveable { mutableStateOf("") }
    var alcohol by rememberSaveable { mutableStateOf("") }
    var medical by rememberSaveable { mutableStateOf("") }

    var initialized by rememberSaveable { mutableStateOf(false) }

    var ageError by rememberSaveable { mutableStateOf("") }
    var showUnsaved by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showNewProf by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    /*
     * Load saved profile into the form once.
     *
     * Without this, remember { mutableStateOf(saved.xxx) }
     * can cause stale form values when savedProfile changes.
     */
    LaunchedEffect(saved) {
        if (!initialized) {
            age = saved.age
            gender = saved.gender
            height = saved.height
            weight = saved.weight
            activity = saved.activityLevel
            sleep = saved.sleepHours
            stress = saved.stressLevel
            smoking = saved.smokingStatus
            alcohol = saved.alcoholUse
            medical = saved.medicalHistory

            initialized = true
        }
    }

    val hasChanges =
        age != saved.age ||
                gender != saved.gender ||
                height != saved.height ||
                weight != saved.weight ||
                activity != saved.activityLevel ||
                sleep != saved.sleepHours ||
                stress != saved.stressLevel ||
                smoking != saved.smokingStatus ||
                alcohol != saved.alcoholUse ||
                medical != saved.medicalHistory

    fun currentProfile(): UserProfile {
        return UserProfile(
            age = age.trim(),
            gender = gender,
            height = height.trim(),
            weight = weight.trim(),
            activityLevel = activity,
            sleepHours = sleep.trim(),
            stressLevel = stress,
            smokingStatus = smoking,
            alcoholUse = alcohol,
            medicalHistory = medical.trim()
        )
    }

    fun validate(): Boolean {
        ageError = ""

        val ageValue = age.trim().toIntOrNull()

        if (age.isNotBlank() &&
            (ageValue == null || ageValue !in 1..120)
        ) {
            ageError = "Enter a valid age (1–120)"
            return false
        }

        return true
    }

    fun saveProfile(): Boolean {
        if (!validate()) {
            return false
        }

        vm.saveProfile(currentProfile())
        showSuccess = true

        return true
    }

    /*
     * Reset the form to the currently saved profile.
     */
    fun resetForm() {
        age = saved.age
        gender = saved.gender
        height = saved.height
        weight = saved.weight
        activity = saved.activityLevel
        sleep = saved.sleepHours
        stress = saved.stressLevel
        smoking = saved.smokingStatus
        alcohol = saved.alcoholUse
        medical = saved.medicalHistory

        ageError = ""
    }

    /*
     * Clear everything for creating a completely new profile.
     */
    fun clearForm() {
        age = ""
        gender = ""
        height = ""
        weight = ""
        activity = ""
        sleep = ""
        stress = ""
        smoking = ""
        alcohol = ""
        medical = ""

        ageError = ""
    }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            kotlinx.coroutines.delay(2200L)
            showSuccess = false
        }
    }

    BackHandler(enabled = hasChanges) {
        showUnsaved = true
    }


    // ---------------------------------------------------------
    // UNSAVED CHANGES DIALOG
    // ---------------------------------------------------------

    if (showUnsaved) {
        AlertDialog(
            onDismissRequest = {
                showUnsaved = false
            },
            containerColor = CardDark,
            title = {
                Text(
                    text = "Unsaved Changes",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "You have unsaved changes. What would you like to do?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (saveProfile()) {
                            showUnsaved = false
                            nav.popBackStack()
                        }
                    }
                ) {
                    Text(
                        text = "Save & Exit",
                        color = CyanPrimary
                    )
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showUnsaved = false
                        }
                    ) {
                        Text(
                            text = "Keep Editing",
                            color = TextSecondary
                        )
                    }

                    TextButton(
                        onClick = {
                            showUnsaved = false
                            nav.popBackStack()
                        }
                    ) {
                        Text(
                            text = "Discard",
                            color = RedAlert
                        )
                    }
                }
            }
        )
    }


    // ---------------------------------------------------------
    // DELETE DIALOG
    // ---------------------------------------------------------

    if (showDelete) {
        AlertDialog(
            onDismissRequest = {
                showDelete = false
            },
            containerColor = CardDark,
            title = {
                Text(
                    text = "Delete Profile",
                    color = RedAlert,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This permanently deletes all saved profile data. The app will behave as if launched for the first time.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteProfile()
                        clearForm()
                        showDelete = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedAlert
                    )
                ) {
                    Text(
                        text = "Delete",
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                    }
                ) {
                    Text(
                        text = "Cancel",
                        color = TextSecondary
                    )
                }
            }
        )
    }


    // ---------------------------------------------------------
    // NEW PROFILE DIALOG
    // ---------------------------------------------------------

    if (showNewProf) {
        AlertDialog(
            onDismissRequest = {
                showNewProf = false
            },
            containerColor = CardDark,
            title = {
                Text(
                    text = "Create New Profile",
                    color = CyanPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This clears the form. Your currently saved profile remains unchanged until you press Save.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearForm()
                        showNewProf = false
                    }
                ) {
                    Text(
                        text = "Clear Form",
                        color = CyanPrimary
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNewProf = false
                    }
                ) {
                    Text(
                        text = "Cancel",
                        color = TextSecondary
                    )
                }
            }
        )
    }


    // ---------------------------------------------------------
    // MAIN SCREEN
    // ---------------------------------------------------------

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            TopBar(
                title = "Profile",
                onBack = {
                    if (hasChanges) {
                        showUnsaved = true
                    } else {
                        nav.popBackStack()
                    }
                },
                actions = {

                    if (!saved.isEmpty) {

                        IconButton(
                            onClick = {
                                showDelete = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Delete",
                                tint = RedAlert
                            )
                        }

                    } else {

                        Box(
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            )


            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {

                // -------------------------------------------------
                // TOP ACTIONS
                // -------------------------------------------------

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    OutlinedButton(
                        onClick = {
                            showNewProf = true
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = CyanPrimary.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "New",
                            tint = CyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text(
                            text = "New Profile",
                            color = CyanPrimary,
                            fontSize = 12.sp
                        )
                    }


                    OutlinedButton(
                        onClick = {
                            resetForm()
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = OrangeWarn.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = OrangeWarn,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text(
                            text = "Reset",
                            color = OrangeWarn,
                            fontSize = 12.sp
                        )
                    }
                }


                // -------------------------------------------------
                // UNSAVED WARNING
                // -------------------------------------------------

                if (hasChanges) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = OrangeWarn.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = OrangeWarn.copy(alpha = 0.3f)
                        )
                    ) {

                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = OrangeWarn,
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Text(
                                text = "Unsaved changes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OrangeWarn
                            )
                        }
                    }
                }


                Spacer(
                    modifier = Modifier.height(12.dp)
                )


                // -------------------------------------------------
                // BASIC INFORMATION
                // -------------------------------------------------

                ProfHeader("BASIC INFORMATION")

                ProfCard {

                    Column {

                        Text(
                            text = "Age",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        ProfField(
                            value = age,
                            onValueChange = {
                                if (
                                    it.length <= 3 &&
                                    it.all(Char::isDigit)
                                ) {
                                    age = it
                                    ageError = ""
                                }
                            },
                            label = "Age (years)",
                            isError = ageError.isNotEmpty(),
                            errorMsg = ageError
                        )
                    }
                }


                Spacer(
                    modifier = Modifier.height(10.dp)
                )


                // -------------------------------------------------
                // GENDER
                // -------------------------------------------------

                ProfCard {

                    Column {

                        Text(
                            text = "Gender",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        ProfChips(
                            options = listOf(
                                "Male",
                                "Female",
                                "Non-Binary",
                                "Prefer not to say"
                            ),
                            selected = gender,
                            onSelect = {
                                gender = it
                            }
                        )
                    }
                }


                Spacer(
                    modifier = Modifier.height(10.dp)
                )


                // -------------------------------------------------
                // HEIGHT / WEIGHT
                // -------------------------------------------------

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    ProfCard(
                        modifier = Modifier.weight(1f)
                    ) {

                        Column {

                            Text(
                                text = "Height",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            ProfField(
                                value = height,
                                onValueChange = {
                                    if (
                                        it.length <= 3 &&
                                        it.all(Char::isDigit)
                                    ) {
                                        height = it
                                    }
                                },
                                label = "cm"
                            )
                        }
                    }


                    ProfCard(
                        modifier = Modifier.weight(1f)
                    ) {

                        Column {

                            Text(
                                text = "Weight",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            ProfField(
                                value = weight,
                                onValueChange = {
                                    if (
                                        it.length <= 3 &&
                                        it.all(Char::isDigit)
                                    ) {
                                        weight = it
                                    }
                                },
                                label = "kg"
                            )
                        }
                    }
                }


                Spacer(
                    modifier = Modifier.height(10.dp)
                )


                // -------------------------------------------------
                // ACTIVITY & LIFESTYLE
                // -------------------------------------------------

                ProfHeader("ACTIVITY & LIFESTYLE")

                ProfCard {

                    Column {

                        Text(
                            text = "Activity Level",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        ProfChips(
                            options = listOf(
                                "Sedentary",
                                "Light",
                                "Moderate",
                                "Active",
                                "Athletic"
                            ),
                            selected = activity,
                            onSelect = {
                                activity = it
                            }
                        )
                    }
                }


                Spacer(
                    modifier = Modifier.height(10.dp)
                )


                ProfCard {

                    Column {

                        Text(
                            text = "Average Sleep Hours",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        ProfField(
                            value = sleep,
                            onValueChange = {
                                sleep = it
                            },
                            label = "e.g. 7.5"
                        )
                    }
                }


                Spacer(
                    modifier = Modifier.height(10.dp)
                )


                ProfCard {

                    Column {

                        Text(
                            text = "Lifestyle Stress",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        ProfChips(
                            options = listOf(
                                "Low",
                                "Moderate",
                                "High",
                                "Very High"
                            ),
                            selected = stress,
                            onSelect = {
                                stress = it
                            }
                        )
                    }
                }


                Spacer(
                    modifier = Modifier.height(10.dp)
                )


                // -------------------------------------------------
                // HEALTH HISTORY
                // -------------------------------------------------

                ProfHeader("HEALTH HISTORY")

                ProfCard {

                    Column {

                        Text(
                            text = "Smoking Status",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        ProfChips(
                            options = listOf(
                                "Non-Smoker",
                                "Ex-Smoker",
                                "Smoker"
                            ),
                            selected = smoking,
                            onSelect = {
                                smoking = it
                            }
                        )
                    }
                }


                Spacer(
                    modifier = Modifier.height(10.dp)
                )


                ProfCard {

                    Column {

                        Text(
                            text = "Alcohol Use",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        ProfChips(
                            options = listOf(
                                "None",
                                "Occasional",
                                "Moderate",
                                "Heavy"
                            ),
                            selected = alcohol,
                            onSelect = {
                                alcohol = it
                            }
                        )
                    }
                }


                Spacer(
                    modifier = Modifier.height(10.dp)
                )


                // -------------------------------------------------
                // MEDICAL HISTORY
                // -------------------------------------------------

                ProfCard {

                    Column {

                        Text(
                            text = "Medical History",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Optional — personalises recommendations only",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        OutlinedTextField(
                            value = medical,
                            onValueChange = {
                                medical = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    text = "e.g. Hypertension, None",
                                    color = TextMuted
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor =
                                    TextMuted.copy(alpha = 0.3f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = CyanPrimary
                            ),
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }


                Spacer(
                    modifier = Modifier.height(24.dp)
                )


                // -------------------------------------------------
                // SAVE
                // -------------------------------------------------

                Button(
                    onClick = {
                        saveProfile()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {

                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save",
                        modifier = Modifier.size(20.dp),
                        tint = BackgroundDark
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(
                        text = "SAVE PROFILE",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = BackgroundDark
                    )
                }


                Spacer(
                    modifier = Modifier.height(24.dp)
                )
            }
        }


        // ---------------------------------------------------------
        // SUCCESS MESSAGE
        // ---------------------------------------------------------

        if (showSuccess) {

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = GreenGood
                ),
                shape = RoundedCornerShape(12.dp)
            ) {

                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = BackgroundDark,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Text(
                        text = "Profile saved successfully!",
                        color = BackgroundDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


// =============================================================
// PROFILE HEADER
// =============================================================

@Composable
fun ProfHeader(text: String) {

    Text(
        text = text,
        modifier = Modifier.padding(vertical = 10.dp),
        style = MaterialTheme.typography.labelLarge,
        color = TextMuted,
        letterSpacing = 2.sp
    )
}


// =============================================================
// PROFILE CARD
// =============================================================

@Composable
fun ProfCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardDark
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFF1A3A5C)
        )
    ) {

        Box(
            modifier = Modifier.padding(14.dp)
        ) {
            content()
        }
    }
}


// =============================================================
// PROFILE FIELD
// =============================================================

@Composable
fun ProfField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    errorMsg: String = ""
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),

        label = {
            Text(
                text = label,
                color = if (isError) RedAlert else TextMuted
            )
        },

        isError = isError,

        supportingText = if (errorMsg.isNotEmpty()) {
            {
                Text(
                    text = errorMsg,
                    color = RedAlert,
                    fontSize = 11.sp
                )
            }
        } else {
            null
        },

        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanPrimary,
            unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = CyanPrimary
        ),

        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}


// =============================================================
// PROFILE CHIPS
// =============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        options.forEach { option ->

            val isSelected = option == selected

            FilterChip(
                selected = isSelected,

                onClick = {
                    onSelect(
                        if (isSelected) "" else option
                    )
                },

                label = {
                    Text(
                        text = option,
                        fontSize = 12.sp
                    )
                },

                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor =
                        CyanPrimary.copy(alpha = 0.15f),

                    selectedLabelColor = CyanPrimary,

                    containerColor = CardDarker,

                    labelColor = TextSecondary
                )
            )
        }
    }
}