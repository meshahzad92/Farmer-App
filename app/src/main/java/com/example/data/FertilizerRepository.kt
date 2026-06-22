package com.example.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class FertilizerRepository(private val db: AppDatabase) {
    private val fertilizerDao = db.fertilizerDao()
    private val salesDao = db.salesDao()
    private val farmerDao = db.farmerDao()

    val allFertilizerTypes: Flow<List<FertilizerType>> = fertilizerDao.getAllFertilizerTypes()
    val allSalesRecords: Flow<List<SalesRecord>> = salesDao.getAllSales()
    val allFarmers: Flow<List<Farmer>> = farmerDao.getAllFarmers()

    suspend fun insertFarmer(farmer: Farmer): Long {
        return farmerDao.insertFarmer(farmer)
    }

    suspend fun updateFarmer(farmer: Farmer) {
        farmerDao.updateFarmer(farmer)
    }

    suspend fun deleteFarmer(farmer: Farmer) {
        farmerDao.deleteFarmer(farmer)
    }

    suspend fun insertFertilizerType(type: FertilizerType): Long {
        return fertilizerDao.insertFertilizerType(type)
    }

    suspend fun updateFertilizerType(type: FertilizerType) {
        fertilizerDao.updateFertilizerType(type)
    }

    suspend fun deleteFertilizerType(type: FertilizerType) {
        fertilizerDao.deleteFertilizerType(type)
    }

    suspend fun insertSalesRecord(sale: SalesRecord): Long {
        return salesDao.insertSale(sale)
    }

    suspend fun updateSalesRecord(sale: SalesRecord) {
        salesDao.updateSale(sale)
    }

    suspend fun deleteSalesRecord(sale: SalesRecord) {
        salesDao.deleteSale(sale)
    }

    suspend fun restoreBackup(backupData: BackupData) {
        db.withTransaction {
            // Restore new/unique types
            backupData.fertilizerTypes.forEach { type ->
                // Avoid inserting empty types
                if (type.name.isNotBlank()) {
                    fertilizerDao.insertFertilizerType(
                        FertilizerType(id = 0, name = type.name, isDefault = type.isDefault)
                    )
                }
            }

            // Restore sales records
            backupData.salesRecords.forEach { sale ->
                salesDao.insertSale(
                    SalesRecord(
                        id = 0,
                        fertilizerName = sale.fertilizerName,
                        quantity = sale.quantity,
                        date = sale.date,
                        notes = sale.notes,
                        createdTime = sale.createdTime
                    )
                )
            }
        }
    }
}
