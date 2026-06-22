package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AppScreen {
    object Welcome : AppScreen
    object AddSale : AppScreen
    data class EditSale(val sale: SalesRecord) : AppScreen
    object Dashboard : AppScreen
    object FertilizerTypes : AppScreen
    object IncentiveCalculator : AppScreen
    object Reports : AppScreen
    object FarmersDetail : AppScreen
    data class AddEditFarmer(val farmer: Farmer?) : AppScreen
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FertilizerRepository
    private val sharedPrefs = application.getSharedPreferences("fertilizer_prefs", Context.MODE_PRIVATE)

    init {
        val database = AppDatabase.getInstance(application)
        repository = FertilizerRepository(database)
    }

    // Screen State
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Welcome)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    // Fertilizer Types Flow
    val fertilizerTypes: StateFlow<List<FertilizerType>> = repository.allFertilizerTypes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sales Records Flow
    val salesRecords: StateFlow<List<SalesRecord>> = repository.allSalesRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Farmers Flow
    val allFarmers: StateFlow<List<Farmer>> = repository.allFarmers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Last used percentage state (default 1%)
    private val _lastPercentage = MutableStateFlow(sharedPrefs.getFloat("last_pct", 1.0f))
    val lastPercentage: StateFlow<Float> = _lastPercentage.asStateFlow()

    fun saveLastPercentage(pct: Float) {
        _lastPercentage.value = pct
        sharedPrefs.edit().putFloat("last_pct", pct).apply()
    }

    // UI Feedback Message Flow
    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage = _uiMessage.asSharedFlow()

    fun showMessage(msg: String) {
        viewModelScope.launch {
            _uiMessage.emit(msg)
        }
    }

    // Operations for Fertilizer Types
    fun addFertilizerType(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertFertilizerType(FertilizerType(name = name))
            showMessage("Type '$name' added.")
        }
    }

    fun updateFertilizerType(type: FertilizerType) {
        if (type.name.isBlank()) return
        viewModelScope.launch {
            repository.updateFertilizerType(type)
            showMessage("Type updated to '${type.name}'.")
        }
    }

    fun deleteFertilizerType(type: FertilizerType) {
        viewModelScope.launch {
            repository.deleteFertilizerType(type)
            showMessage("Type '${type.name}' removed.")
        }
    }

    // Operations for Sales Records
    fun addSalesRecord(fertilizerName: String, quantity: Double, date: String, notes: String) {
        viewModelScope.launch {
            val record = SalesRecord(
                fertilizerName = fertilizerName,
                quantity = quantity,
                date = date,
                notes = notes
            )
            repository.insertSalesRecord(record)
            showMessage("Sales record added successfully.")
        }
    }

    fun updateSalesRecord(sale: SalesRecord) {
        viewModelScope.launch {
            repository.updateSalesRecord(sale)
            showMessage("Sales record updated.")
        }
    }

    fun deleteSalesRecord(sale: SalesRecord) {
        viewModelScope.launch {
            repository.deleteSalesRecord(sale)
            showMessage("Sales record deleted.")
        }
    }

    // Operations for Farmer Details
    fun addFarmer(name: String, contact: String, landSizeAcres: Double, saleCall: String, imageUri: String?, notes: String) {
        viewModelScope.launch {
            val farmer = Farmer(
                name = name,
                contact = contact,
                landSizeAcres = landSizeAcres,
                saleCall = saleCall,
                imageUri = imageUri,
                notes = notes
            )
            repository.insertFarmer(farmer)
            showMessage("Farmer details saved!")
        }
    }

    fun updateFarmer(farmer: Farmer) {
        viewModelScope.launch {
            repository.updateFarmer(farmer)
            showMessage("Farmer details updated.")
        }
    }

    fun deleteFarmer(farmer: Farmer) {
        viewModelScope.launch {
            repository.deleteFarmer(farmer)
            showMessage("Farmer entry deleted.")
        }
    }

    // Export Helpers
    fun getBackupJson(): String {
        return BackupHelper.exportToJson(fertilizerTypes.value, salesRecords.value)
    }

    fun getBackupCsv(): String {
        return BackupHelper.exportToCsv(salesRecords.value)
    }

    fun restoreBackupFromJson(jsonString: String): Boolean {
        val backup = BackupHelper.importFromJson(jsonString)
        return if (backup != null) {
            viewModelScope.launch {
                repository.restoreBackup(backup)
                showMessage("Backup restored successfully. ${backup.salesRecords.size} records imported.")
            }
            true
        } else {
            false
        }
    }
}
