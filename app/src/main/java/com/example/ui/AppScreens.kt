package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.net.Uri
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.example.data.FertilizerType
import com.example.data.SalesRecord
import com.example.data.Farmer
import java.text.SimpleDateFormat
import java.util.*

// --- DATE HELPER UTILITIES ---
fun getTodayDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Calendar.getInstance().time)
}

fun getPastDateString(daysAgo: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(cal.time)
}

fun getThisMonthStartString(): String {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(cal.time)
}

fun formatDisplayDate(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = parser.parse(dateStr)
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        if (date != null) formatter.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}

// Filter logic for Dashboard / Reports / Incentive
fun filterSales(
    sales: List<SalesRecord>,
    filterType: String,
    customStart: String,
    customEnd: String
): List<SalesRecord> {
    val today = getTodayDate()
    return when (filterType) {
        "Today" -> sales.filter { it.date == today }
        "Last 7 Days" -> {
            val limit = getPastDateString(7)
            sales.filter { it.date >= limit }
        }
        "This Month" -> {
            val limit = getThisMonthStartString()
            sales.filter { it.date >= limit }
        }
        "Custom" -> {
            sales.filter { it.date in customStart..customEnd }
        }
        else -> sales // "All"
    }
}


// --- 1. WELCOME SCREEN (HIGH DENSITY COMPACT DASHBOARD) ---
@Composable
fun WelcomeScreen(
    viewModel: MainViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val sales by viewModel.salesRecords.collectAsStateWithLifecycle()
    val types by viewModel.fertilizerTypes.collectAsStateWithLifecycle()

    val today = getTodayDate()
    val todaySalesSum = sales.filter { it.date == today }.sumOf { it.quantity }
    val thisMonthSalesSum = sales.filter { it.date >= getThisMonthStartString() }.sumOf { it.quantity }
    val totalBagsSold = sales.sumOf { it.quantity }

    val formattedTotal = if (totalBagsSold % 1.0 == 0.0) totalBagsSold.toInt().toString() else String.format("%.1f", totalBagsSold)
    val formattedThisMonth = if (thisMonthSalesSum % 1.0 == 0.0) thisMonthSalesSum.toInt().toString() else String.format("%.1f", thisMonthSalesSum)
    val formattedToday = if (todaySalesSum % 1.0 == 0.0) todaySalesSum.toInt().toString() else String.format("%.1f", todaySalesSum)

    // Dynamic Top categories from Catalog
    val firstTypeName = types.getOrNull(0)?.name ?: "DAP"
    val secondTypeName = types.getOrNull(1)?.name ?: "Urea"
    val firstTypeSales = sales.filter { it.fertilizerName == firstTypeName }.sumOf { it.quantity }
    val secondTypeSales = sales.filter { it.fertilizerName == secondTypeName }.sumOf { it.quantity }

    val formattedFirst = if (firstTypeSales % 1.0 == 0.0) firstTypeSales.toInt().toString() else String.format("%.1f", firstTypeSales)
    val formattedSecond = if (secondTypeSales % 1.0 == 0.0) secondTypeSales.toInt().toString() else String.format("%.1f", secondTypeSales)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        // 1. Status Bar / Premium Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CHATTA SEED • INVENTORY MONITOR",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Muhammad Faizan",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            // User Avatar/Circle Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable { viewModel.showMessage("Logged in as ${viewModel.getBackupJson().hashCode().toUInt().toString(16).uppercase()}") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "User Account",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // 2. Dashboard Summary Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Main Stat Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Bags Sold",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Tracked Live",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Text(
                        text = formattedTotal,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "THIS MONTH",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = formattedThisMonth,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Vertical Separator Line
                        Spacer(modifier = Modifier.width(24.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                        )
                        Spacer(modifier = Modifier.width(24.dp))

                        Column {
                            Text(
                                text = "TODAY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = formattedToday,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Quick Stats Grid Row of 2 Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "$firstTypeName Sales",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$formattedFirst Bags",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "$secondTypeName Sales",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$formattedSecond Bags",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        // 3. Quick Actions Grid (4 columns)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Action 1: Add Sale
            QuickActionButton(
                label = "Add Sale",
                icon = Icons.Default.Add,
                backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = { onNavigate(AppScreen.AddSale) }
            )

            // Action 2: Farmers
            QuickActionButton(
                label = "Farmers",
                icon = Icons.Default.Person,
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = { onNavigate(AppScreen.FarmersDetail) }
            )

            // Action 3: Incentive
            QuickActionButton(
                label = "Incentive",
                icon = Icons.Default.CheckCircle, // Using dynamic commission check icon
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = { onNavigate(AppScreen.IncentiveCalculator) }
            )

            // Action 4: Backup
            QuickActionButton(
                label = "Backup",
                icon = Icons.Default.Share,
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = { onNavigate(AppScreen.Reports) }
            )
        }

        // 4. Recent Transactions List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = { onNavigate(AppScreen.Reports) }
                ) {
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            val recentSales = sales.take(3)
            if (recentSales.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No sales records recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    recentSales.forEach { record ->
                        RecentSaleRow(record = record, onClick = { onNavigate(AppScreen.Reports) })
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun RecentSaleRow(
    record: SalesRecord,
    onClick: () -> Unit
) {
    val initials = if (record.fertilizerName.length >= 2) {
        record.fertilizerName.take(2).uppercase()
    } else {
        record.fertilizerName.uppercase()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Circle Badge Prefix
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column {
                Text(
                    text = record.fertilizerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatDisplayDate(record.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "+${if (record.quantity % 1.0 == 0.0) record.quantity.toInt().toString() else record.quantity} Bags",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (record.notes.isNotBlank()) {
                Text(
                    text = record.notes,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 120.dp)
                )
            }
        }
    }
}



// --- 2. ADD & EDIT SALE SCREEN ---
@Composable
fun AddEditSaleScreen(
    viewModel: MainViewModel,
    saleToEdit: SalesRecord? = null,
    onBack: () -> Unit
) {
    val types by viewModel.fertilizerTypes.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedDate by remember { mutableStateOf(saleToEdit?.date ?: getTodayDate()) }
    var selectedType by remember { mutableStateOf(saleToEdit?.fertilizerName ?: "") }
    var quantityStr by remember { mutableStateOf(saleToEdit?.quantity?.toString() ?: "") }
    var notes by remember { mutableStateOf(saleToEdit?.notes ?: "") }

    var expandedDropdown by remember { mutableStateOf(false) }

    // Dropdown initialization
    LaunchedEffect(types, saleToEdit) {
        if (selectedType.isEmpty() && types.isNotEmpty()) {
            selectedType = types.first().name
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(if (saleToEdit == null) "Add Today Sale" else "Edit Sale Record") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // DATE PICKER BUTTON
            OutlinedTextField(
                value = formatDisplayDate(selectedDate),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date sold") },
                trailingIcon = {
                    IconButton(onClick = {
                        val calendar = Calendar.getInstance()
                        if (selectedDate.isNotEmpty()) {
                            try {
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                parserDate(selectedDate)?.let { calendar.time = it }
                            } catch (e: Exception) {}
                        }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) {
                        Icon(Icons.Default.DateRange, "Select Date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // FERTILIZER TYPE DROPDOWN
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fertilizer Bag Type") },
                    trailingIcon = {
                        IconButton(onClick = { expandedDropdown = true }) {
                            Icon(Icons.Default.KeyboardArrowDown, "Select Type")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    if (types.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No types added yet. Press Back and load catalog.") },
                            onClick = { expandedDropdown = false }
                        )
                    } else {
                        types.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type.name
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // QUANTITY TEXT FIELD
            OutlinedTextField(
                value = quantityStr,
                onValueChange = { quantityStr = it },
                label = { Text("Quantity Sold (Bags)") },
                placeholder = { Text("e.g. 25 or 12.5") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
            )

            // OPTIONAL NOTES FIELD
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Optional Notes") },
                placeholder = { Text("Enter supplier, discount details etc.") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SAVE / UPDATE BUTTON
            Button(
                onClick = {
                    val qty = quantityStr.toDoubleOrNull()
                    if (qty == null || qty <= 0.0) {
                        viewModel.showMessage("Please enter a valid positive quantity!")
                        return@Button
                    }
                    if (selectedType.isBlank()) {
                        viewModel.showMessage("Please select a fertilizer bag type!")
                        return@Button
                    }

                    if (saleToEdit == null) {
                        viewModel.addSalesRecord(selectedType, qty, selectedDate, notes)
                    } else {
                        viewModel.updateSalesRecord(
                            saleToEdit.copy(
                                fertilizerName = selectedType,
                                quantity = qty,
                                date = selectedDate,
                                notes = notes
                            )
                        )
                    }
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (saleToEdit == null) "Save Sales Record" else "Update Record Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun parserDate(str: String): Date? {
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(str)
    } catch (e: Exception) {
        null
    }
}


// --- 3. FERTILIZER TYPES MANAGEMENT SCREEN ---
@Composable
fun FertilizerTypesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val types by viewModel.fertilizerTypes.collectAsStateWithLifecycle()
    var newTypeName by remember { mutableStateOf("") }

    // Dialog control for deletes and updates
    var typeToDelete by remember { mutableStateOf<FertilizerType?>(null) }
    var typeToRename by remember { mutableStateOf<FertilizerType?>(null) }
    var renameValue by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Fertilizer Types") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Highlighting safety rule
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Safe Information",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Removing a fertilizer type will not affect previous sales records. Previous transaction details are fully preserved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // ADD TYPE TEXT ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newTypeName,
                    onValueChange = { newTypeName = it },
                    label = { Text("New Fertilizer Name") },
                    placeholder = { Text("e.g. NPK 15, CAN") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (newTypeName.isNotBlank()) {
                            viewModel.addFertilizerType(newTypeName.trim())
                            newTypeName = ""
                        } else {
                            viewModel.showMessage("Name cannot be empty!")
                        }
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                }
            }

            Divider()

            Text(
                text = "Fertilizer Catalog (${types.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // TYPES LIST
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (types.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No fertilizer catalog types configured. Create one above.")
                        }
                    }
                } else {
                    items(types) { type ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = type.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (type.isDefault) {
                                        Text(
                                            text = "System Default",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Row {
                                    IconButton(onClick = {
                                        typeToRename = type
                                        renameValue = type.name
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Create,
                                            contentDescription = "Rename type"
                                        )
                                    }
                                    IconButton(onClick = { typeToDelete = type }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete type",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CONFIRMATION DELETE DIALOG
    if (typeToDelete != null) {
        AlertDialog(
            onDismissRequest = { typeToDelete = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you absolutely sure you want to remove fertilizer type '${typeToDelete?.name}' from current catalog selection? Sales matching this name are not altered.") },
            confirmButton = {
                TextButton(onClick = {
                    typeToDelete?.let { viewModel.deleteFertilizerType(it) }
                    typeToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { typeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // RENAME DIALOG
    if (typeToRename != null) {
        AlertDialog(
            onDismissRequest = { typeToRename = null },
            title = { Text("Rename Fertilizer Type") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("Fertilizer Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameValue.isNotBlank()) {
                        typeToRename?.let {
                            viewModel.updateFertilizerType(it.copy(name = renameValue.trim()))
                        }
                        typeToRename = null
                    } else {
                        viewModel.showMessage("Name cannot be empty!")
                    }
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { typeToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}


// --- 4. DASHBOARD SCREEN WITH CUSTOM CANVAS BAR CHART ---
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEditSale: (SalesRecord) -> Unit
) {
    val sales by viewModel.salesRecords.collectAsStateWithLifecycle()

    var filterType by remember { mutableStateOf("Last 7 Days") }
    var customStartDate by remember { mutableStateOf(getPastDateString(30)) }
    var customEndDate by remember { mutableStateOf(getTodayDate()) }

    val filteredSales = filterSales(sales, filterType, customStartDate, customEndDate)

    val todayDate = getTodayDate()
    val todaySales = sales.filter { it.date == todayDate }.sumOf { it.quantity }

    val currentWeekLimit = getPastDateString(7)
    val weekSales = sales.filter { it.date >= currentWeekLimit }.sumOf { it.quantity }

    val currentMonthLimit = getThisMonthStartString()
    val monthSales = sales.filter { it.date >= currentMonthLimit }.sumOf { it.quantity }

    val totalAllTime = sales.sumOf { it.quantity }

    val context = LocalContext.current

    // Grouping for Type progress chart
    val salesByType = filteredSales.groupBy { it.fertilizerName }
        .mapValues { entry -> entry.value.sumOf { it.quantity } }

    val maxTotalByType = (salesByType.values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)

    // Formatter double representation
    fun formatBags(doubleVal: Double): String {
        return if (doubleVal % 1.0 == 0.0) {
            doubleVal.toInt().toString()
        } else {
            String.format("%.1f", doubleVal)
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Sales Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // FILTER SELECTORS ROW
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Dashboard Filter Duration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Today", "Last 7 Days", "This Month", "All", "Custom").forEach { item ->
                        FilterChip(
                            selected = filterType == item,
                            onClick = { filterType = item },
                            label = { Text(item) }
                        )
                    }
                }
            }

            // CUSTOM DATE ACCORDION
            AnimatedVisibility(visible = filterType == "Custom") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Custom Date Range", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = formatDisplayDate(customStartDate),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Start Date") },
                                modifier = Modifier.weight(1f),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val calendar = Calendar.getInstance()
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                customStartDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                            },
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH),
                                            calendar.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }) {
                                        Icon(Icons.Default.DateRange, "Pick")
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = formatDisplayDate(customEndDate),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("End Date") },
                                modifier = Modifier.weight(1f),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val calendar = Calendar.getInstance()
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                customEndDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                            },
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH),
                                            calendar.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }) {
                                        Icon(Icons.Default.DateRange, "Pick")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // TOTAL METRIC CARDS GRID
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        title = "Today Sales",
                        value = "${formatBags(todaySales)} Bags",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    MetricCard(
                        title = "Last 7 Days",
                        value = "${formatBags(weekSales)} Bags",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        title = "This Month",
                        value = "${formatBags(monthSales)} Bags",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                    MetricCard(
                        title = "Total All Time",
                        value = "${formatBags(totalAllTime)} Bags",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // CHARTS OF PROGRESSION
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SimpleBarChart(sales = filteredSales)
                }
            }

            // SALES BY TYPE breakdown
            Text(
                text = "Sales by Fertilizer Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (salesByType.isEmpty()) {
                        Text("No sales matching criteria found.")
                    } else {
                        salesByType.entries.sortedByDescending { it.value }.forEach { (type, sum) ->
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = type, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text(text = "${formatBags(sum)} Bags", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (sum / maxTotalByType).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }

            // RECENT ENTRIES (Max 5)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Recent Sale Records", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { viewModel.navigateTo(AppScreen.Reports) }) {
                    Text("View All")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val recents = sales.take(5)
                if (recents.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text("No sale records added yet. Add your first record now!", modifier = Modifier.padding(16.dp))
                    }
                } else {
                    recents.forEach { record ->
                        SalesRecordRow(
                            record = record,
                            onEdit = { onEditSale(record) },
                            onDelete = { viewModel.deleteSalesRecord(record) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SimpleBarChart(
    sales: List<SalesRecord>,
    modifier: Modifier = Modifier
) {
    // Group sales by date and sum quantities
    val dailySums = sales.groupBy { it.date }
        .mapValues { entry -> entry.value.sumOf { it.quantity } }
        .toList()
        .sortedBy { it.first }
        .takeLast(7) // show last 7 active transaction days

    if (dailySums.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No sales records in selected date duration to plot.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxVal = (dailySums.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)

    Column(modifier = modifier) {
        Text(
            text = "Daily Progression (Sold Bags)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val bottomPadding = 10f
            val topPadding = 20f
            val usableHeight = canvasHeight - bottomPadding - topPadding

            val barCount = dailySums.size
            val barSpacing = 28f
            val totalSpacing = barSpacing * (barCount + 1)
            val barWidth = (canvasWidth - totalSpacing) / barCount

            // Draw clean Grid lines behind bars
            val gridLines = 3
            for (i in 0..gridLines) {
                val y = topPadding + usableHeight - (usableHeight / gridLines * i)
                drawLine(
                    color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.4f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(canvasWidth, y),
                    strokeWidth = 2f
                )
            }

            dailySums.forEachIndexed { index, (date, total) ->
                val barHeight = ((total / maxVal) * usableHeight).toFloat()
                val x = barSpacing + index * (barWidth + barSpacing)
                val y = canvasHeight - bottomPadding - barHeight

                // Column Rectangle
                drawRoundRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color(0xFF81C784), // Mint Green
                            androidx.compose.ui.graphics.Color(0xFF2E7D32)  // Forest Sage
                        )
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
            }
        }

        // Standard labels to prevent Font draw scale mismatch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dailySums.forEach { (date, total) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = total.let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format("%.1f", it) },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = date.substringAfter('-'), // show MM-DD
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


// --- 5. INCENTIVE CALCULATOR SCREEN ---
@Composable
fun IncentiveCalculatorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val sales by viewModel.salesRecords.collectAsStateWithLifecycle()
    val types by viewModel.fertilizerTypes.collectAsStateWithLifecycle()
    val lastPct by viewModel.lastPercentage.collectAsStateWithLifecycle()

    var pctInput by remember { mutableStateOf(lastPct.toString()) }
    var selectTypeFilter by remember { mutableStateOf("All Fertilizer Types") }

    var filterTypeDate by remember { mutableStateOf("All") }
    var customStartDate by remember { mutableStateOf(getPastDateString(30)) }
    var customEndDate by remember { mutableStateOf(getTodayDate()) }

    var expandedTypeDropdown by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Apply date filter
    val tempSalesDateFiltered = filterSales(sales, filterTypeDate, customStartDate, customEndDate)

    // Apply Fertilizer Type Filter
    val finalFilteredSales = if (selectTypeFilter == "All Fertilizer Types") {
        tempSalesDateFiltered
    } else {
        tempSalesDateFiltered.filter { it.fertilizerName == selectTypeFilter }
    }

    val matchingBagsSum = finalFilteredSales.sumOf { it.quantity }

    val percentageFloat = pctInput.toFloatOrNull() ?: 0.0f

    // Calculate both standard commission payout methods
    val incentiveFormulaProduct = matchingBagsSum * percentageFloat
    val incentiveFormulaAsPercentage = matchingBagsSum * (percentageFloat / 100.0)

    fun formatVal(valDouble: Double): String {
        return if (valDouble % 1.0 == 0.0) {
            valDouble.toInt().toString()
        } else {
            String.format("%.2f", valDouble)
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Incentive Calculator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // INPUT INCENTIVE RATE CARD
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Incentive Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // Percentage Input
                    OutlinedTextField(
                        value = pctInput,
                        onValueChange = { pctInput = it },
                        label = { Text("Incentive value per bag sold (Example: 2, or 1% rate)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        )
                    )

                    Button(
                        onClick = {
                            val value = pctInput.toFloatOrNull()
                            if (value != null && value >= 0f) {
                                viewModel.saveLastPercentage(value)
                                viewModel.showMessage("Last used percentage configuration ($value%) saved locally.")
                            } else {
                                viewModel.showMessage("Please enter a valid rate.")
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Persist Last Used Rate")
                    }
                }
            }

            // CRITERIA FILTER CARD
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Calculation Coverage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // 1. SELECT TYPE DROPDOWN
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectTypeFilter,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Filter Fertilizer Type") },
                            trailingIcon = {
                                IconButton(onClick = { expandedTypeDropdown = true }) {
                                    Icon(Icons.Default.KeyboardArrowDown, "Pick Type")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = expandedTypeDropdown,
                            onDismissRequest = { expandedTypeDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Fertilizer Types") },
                                onClick = {
                                    selectTypeFilter = "All Fertilizer Types"
                                    expandedTypeDropdown = false
                                }
                            )
                            types.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.name) },
                                    onClick = {
                                        selectTypeFilter = t.name
                                        expandedTypeDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. TIMEFRAME SELECTOR
                    Text("Select Timeframe", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Today", "Last 7 Days", "This Month", "Custom").forEach { timeframe ->
                            FilterChip(
                                selected = filterTypeDate == timeframe,
                                onClick = { filterTypeDate = timeframe },
                                label = { Text(timeframe) }
                            )
                        }
                    }

                    // Custom ranges
                    AnimatedVisibility(visible = filterTypeDate == "Custom") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = formatDisplayDate(customStartDate),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Start Date") },
                                    modifier = Modifier.weight(1f),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val c = Calendar.getInstance()
                                            DatePickerDialog(context, { _, y, m, d ->
                                                customStartDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                                            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                                        }) {
                                            Icon(Icons.Default.DateRange, "Pick")
                                        }
                                    }
                                )
                                OutlinedTextField(
                                    value = formatDisplayDate(customEndDate),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("End Date") },
                                    modifier = Modifier.weight(1f),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val c = Calendar.getInstance()
                                            DatePickerDialog(context, { _, y, m, d ->
                                                customEndDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                                            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                                        }) {
                                            Icon(Icons.Default.DateRange, "Pick")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // RESULTS DISPLAY CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Calculation Output Results",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Matching Sold Bags:", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = "${formatVal(matchingBagsSum)} Bags",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Configured Rate Value:", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = "$pctInput Value",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Calculation breakdown Option A: Multiplication direct
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Formula A: Bags × Factor Rate", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${formatVal(matchingBagsSum)} × $percentageFloat = Rs. ${formatVal(incentiveFormulaProduct)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Calculation breakdown Option B: Percentage calculation
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Formula B: Payout as Percentage ($percentageFloat% rate of total bags)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${formatVal(matchingBagsSum)} × ($percentageFloat / 100) = Rs. ${formatVal(incentiveFormulaAsPercentage)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}


// --- 6. REPORTS & EXPORTS / BACKUPS SCREEN ---
@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEditSale: (SalesRecord) -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onImportJson: () -> Unit
) {
    val sales by viewModel.salesRecords.collectAsStateWithLifecycle()
    val types by viewModel.fertilizerTypes.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectFertilizerTypeFilter by remember { mutableStateOf("All Types") }
    var filterTimeframe by remember { mutableStateOf("All Time") }

    var filterStartDate by remember { mutableStateOf(getPastDateString(30)) }
    var filterEndDate by remember { mutableStateOf(getTodayDate()) }

    var expandedTypeChoices by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<SalesRecord?>(null) }

    val context = LocalContext.current

    // Date pre-filtering
    val dateMappedSales = when (filterTimeframe) {
        "Today" -> sales.filter { it.date == getTodayDate() }
        "Last 7 Days" -> {
            val limit = getPastDateString(7)
            sales.filter { it.date >= limit }
        }
        "This Month" -> {
            val limit = getThisMonthStartString()
            sales.filter { it.date >= limit }
        }
        "Custom Date Range" -> {
            sales.filter { it.date in filterStartDate..filterEndDate }
        }
        else -> sales
    }

    // Fertilizer Type filtering
    val typeMappedSales = if (selectFertilizerTypeFilter == "All Types") {
        dateMappedSales
    } else {
        dateMappedSales.filter { it.fertilizerName == selectFertilizerTypeFilter }
    }

    // Search query mapping (case-insensitive search in Notes or Fertilizer name)
    val finalQueryResult = if (searchQuery.isBlank()) {
        typeMappedSales
    } else {
        typeMappedSales.filter {
            it.fertilizerName.contains(searchQuery, ignoreCase = true) ||
            it.notes.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Sales Reports & Storage") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // BACKUP / RESTORE SECTION (TOP LEVEL OF REPORTS ACCORDING TO USER FLOW)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Data Safety & Storage Backups",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Save database to standard documents folder or load existing backups safely. Restore loads data atomically without formatting previous custom entries.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = onExportJson) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Export JSON")
                            Spacer(Modifier.width(4.dp))
                            Text("Export JSON Backup")
                        }
                        Button(onClick = onExportCsv) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Export CSV")
                            Spacer(Modifier.width(4.dp))
                            Text("Export Excel CSV")
                        }
                        Button(
                            onClick = onImportJson,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Import JSON")
                            Spacer(Modifier.width(4.dp))
                            Text("Restore / Import JSON")
                        }
                    }
                }
            }

            // SEARCH FILTERING SEARCH FIELD
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search records") },
                placeholder = { Text("Search fertilizer type or notes text") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search Icon")
                }
            )

            // TWO COLUMN FILTERS BOX (TYPE & TIMEFRAME)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // TYPE FILTER DROPDOWN
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectFertilizerTypeFilter,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type filter") },
                        trailingIcon = {
                            IconButton(onClick = { expandedTypeChoices = true }) {
                                Icon(Icons.Default.KeyboardArrowDown, "Pick")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = expandedTypeChoices,
                        onDismissRequest = { expandedTypeChoices = false },
                        modifier = Modifier.fillMaxWidth(0.45f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Types") },
                            onClick = {
                                selectFertilizerTypeFilter = "All Types"
                                expandedTypeChoices = false
                            }
                        )
                        types.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.name) },
                                onClick = {
                                    selectFertilizerTypeFilter = t.name
                                    expandedTypeChoices = false
                                }
                            )
                        }
                    }
                }

                // DATE TIMEFRAME CHOOSERS
                var dateChoiceExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = filterTimeframe,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Time duration") },
                        trailingIcon = {
                            IconButton(onClick = { dateChoiceExpanded = true }) {
                                Icon(Icons.Default.KeyboardArrowDown, "Pick")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = dateChoiceExpanded,
                        onDismissRequest = { dateChoiceExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.45f)
                    ) {
                        listOf("All Time", "Today", "Last 7 Days", "This Month", "Custom Date Range").forEach { timeframe ->
                            DropdownMenuItem(
                                text = { Text(timeframe) },
                                onClick = {
                                    filterTimeframe = timeframe
                                    dateChoiceExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // CUSTOM RANGE ROW
            AnimatedVisibility(visible = filterTimeframe == "Custom Date Range") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = formatDisplayDate(filterStartDate),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Start") },
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                IconButton(onClick = {
                                    val c = Calendar.getInstance()
                                    DatePickerDialog(context, { _, y, m, d ->
                                        filterStartDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                                }) {
                                    Icon(Icons.Default.DateRange, "Pick")
                                }
                            }
                        )
                        OutlinedTextField(
                            value = formatDisplayDate(filterEndDate),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("End") },
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                IconButton(onClick = {
                                    val c = Calendar.getInstance()
                                    DatePickerDialog(context, { _, y, m, d ->
                                        filterEndDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                                }) {
                                    Icon(Icons.Default.DateRange, "Pick")
                                }
                            }
                        )
                    }
                }
            }

            Text(
                text = "Sales Records List (${finalQueryResult.size})",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            // FILTER RESULT LAZY LIST
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (finalQueryResult.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No matching transactions found with criteria.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(finalQueryResult) { record ->
                        SalesRecordRow(
                            record = record,
                            onEdit = { onEditSale(record) },
                            onDelete = { recordToDelete = record }
                        )
                    }
                }
            }
        }
    }

    // DELETE CONFIRM DIALOG
    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Confirm Deletion of Record") },
            text = { Text("Are you sure you want to permanently remove this transaction (Sold: ${recordToDelete?.quantity} bags of ${recordToDelete?.fertilizerName} on ${recordToDelete?.let { formatDisplayDate(it.date) }})? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    recordToDelete?.let { viewModel.deleteSalesRecord(it) }
                    recordToDelete = null
                }) {
                    Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}


// --- REUSABLE COMPONENTS ---

@Composable
fun SalesRecordRow(
    record: SalesRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.fertilizerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${if (record.quantity % 1.0 == 0.0) record.quantity.toInt().toString() else record.quantity} Bags",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Date icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = formatDisplayDate(record.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (record.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Notes: " + record.notes,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Create, contentDescription = "Edit Transaction")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Transaction",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}


// --- 8. FARMERS DETAIL SCREEN ---
@Composable
fun FarmersDetailScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onAddEditFarmer: (Farmer?) -> Unit
) {
    val farmers by viewModel.allFarmers.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFarmerForModal by remember { mutableStateOf<Farmer?>(null) }

    val filteredFarmers = farmers.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.contact.contains(searchQuery, ignoreCase = true) ||
                it.notes.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Column {
                        Text("Farmer Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "CHATTA SEED • Muhammad Faizan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddEditFarmer(null) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                text = { Text("Add New Farmer") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Farmers (Name / Contact)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredFarmers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Empty icon",
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = if (searchQuery.isEmpty()) "No farmer records yet." else "No farmers matching search query.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredFarmers) { farmer ->
                        FarmerCard(
                            farmer = farmer,
                            onClick = { selectedFarmerForModal = farmer },
                            onEdit = { onAddEditFarmer(farmer) },
                            onDelete = { viewModel.deleteFarmer(farmer) }
                        )
                    }
                }
            }
        }

        // Detail Modal Bottom Sheet dialog
        selectedFarmerForModal?.let { farmer ->
            AlertDialog(
                onDismissRequest = { selectedFarmerForModal = null },
                confirmButton = {
                    TextButton(onClick = { selectedFarmerForModal = null }) {
                        Text("Close")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedFarmerForModal = null
                        onAddEditFarmer(farmer)
                    }) {
                        Text("Edit")
                    }
                },
                title = {
                    Column {
                        Text(text = farmer.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(text = "Chatta Seed Profile", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!farmer.imageUri.isNullOrBlank()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                AsyncImage(
                                    model = Uri.parse(farmer.imageUri),
                                    contentDescription = "Farmer Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AccountBox, "No image", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                                    Text("No receipt/image uploaded", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Info Fields
                        DetailItem(label = "Contact Number", value = farmer.contact)
                        DetailItem(label = "Land Size (Acres)", value = "${farmer.landSizeAcres} Acres")
                        DetailItem(label = "Sale Call status/remark", value = farmer.saleCall)
                        if (farmer.notes.isNotBlank()) {
                            DetailItem(label = "General Notes", value = farmer.notes)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun FarmerCard(
    farmer: Farmer,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Image or Initial Thumbnail circle
                if (!farmer.imageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = Uri.parse(farmer.imageUri),
                        contentDescription = "Thumbnail",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (farmer.name.isNotBlank()) farmer.name.take(1).uppercase() else "F",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = farmer.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Land: ${farmer.landSizeAcres} Acres • Call: ${farmer.saleCall}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = farmer.contact,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick actions buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Create, "Edit Farmer", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete Farmer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(6.dp))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }
}


// --- 9. ADD & EDIT FARMER SCREEN ---
@Composable
fun AddEditFarmerScreen(
    viewModel: MainViewModel,
    farmerToEdit: Farmer?,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(farmerToEdit?.name ?: "") }
    var contact by remember { mutableStateOf(farmerToEdit?.contact ?: "") }
    var landSizeInput by remember { mutableStateOf(farmerToEdit?.landSizeAcres?.toString() ?: "") }
    var saleCall by remember { mutableStateOf(farmerToEdit?.saleCall ?: "") }
    var notes by remember { mutableStateOf(farmerToEdit?.notes ?: "") }
    var selectedImageUriStr by remember { mutableStateOf<String?>(farmerToEdit?.imageUri) }

    val context = LocalContext.current

    // Launcher for selecting an image from the gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Ensure permission persistence
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // ignore permission errors
            }
            selectedImageUriStr = uri.toString()
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(if (farmerToEdit == null) "Add Farmer Entry" else "Edit Farmer Entry") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Form Cards
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Farmer Details & Contact Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Farmer Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text("Contact Number *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        )
                    )

                    OutlinedTextField(
                        value = landSizeInput,
                        onValueChange = { landSizeInput = it },
                        label = { Text("Land Size (Acres) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        )
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Interaction Logs & Remarks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = saleCall,
                        onValueChange = { saleCall = it },
                        label = { Text("Sale Call (Examples: Call Done, Interested, Responded) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("General Notes / Remarks") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }

            // Image picking section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Farmer Receipt / Field Photo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    if (!selectedImageUriStr.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = Uri.parse(selectedImageUriStr),
                                contentDescription = "Selected Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                                Icon(Icons.Default.Refresh, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Change Image")
                            }

                            OutlinedButton(
                                onClick = { selectedImageUriStr = null },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remove")
                            }
                        }
                    } else {
                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AccountBox, "Upload Image", modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Upload image from gallery", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val land = landSizeInput.toDoubleOrNull()
                    if (name.isBlank() || contact.isBlank() || land == null || saleCall.isBlank()) {
                        viewModel.showMessage("Please fill out all mandatory (*) fields with valid parameters.")
                    } else {
                        if (farmerToEdit == null) {
                            viewModel.addFarmer(
                                name = name,
                                contact = contact,
                                landSizeAcres = land,
                                saleCall = saleCall,
                                imageUri = selectedImageUriStr,
                                notes = notes
                            )
                        } else {
                            val updatedFarmer = farmerToEdit.copy(
                                name = name,
                                contact = contact,
                                landSizeAcres = land,
                                saleCall = saleCall,
                                imageUri = selectedImageUriStr,
                                notes = notes
                            )
                            viewModel.updateFarmer(updatedFarmer)
                        }
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (farmerToEdit == null) "Save Farmer Entry" else "Update Farmer Entry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
