package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FertilizerDao {
    @Query("SELECT * FROM fertilizer_types ORDER BY name ASC")
    fun getAllFertilizerTypes(): Flow<List<FertilizerType>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFertilizerType(type: FertilizerType): Long

    @Update
    suspend fun updateFertilizerType(type: FertilizerType)

    @Delete
    suspend fun deleteFertilizerType(type: FertilizerType)

    @Query("SELECT COUNT(*) FROM fertilizer_types")
    suspend fun getCount(): Int
}

@Dao
interface SalesDao {
    @Query("SELECT * FROM sales_records ORDER BY date DESC, createdTime DESC")
    fun getAllSales(): Flow<List<SalesRecord>>

    @Query("SELECT * FROM sales_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getSalesInDateRange(startDate: String, endDate: String): Flow<List<SalesRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SalesRecord): Long

    @Update
    suspend fun updateSale(sale: SalesRecord)

    @Delete
    suspend fun deleteSale(sale: SalesRecord)
}

@Dao
interface FarmerDao {
    @Query("SELECT * FROM farmers ORDER BY name ASC")
    fun getAllFarmers(): Flow<List<Farmer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarmer(farmer: Farmer): Long

    @Update
    suspend fun updateFarmer(farmer: Farmer)

    @Delete
    suspend fun deleteFarmer(farmer: Farmer)
}

