package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fertilizer_types")
data class FertilizerType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "sales_records")
data class SalesRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fertilizerName: String,
    val quantity: Double,
    val date: String, // "YYYY-MM-DD"
    val notes: String = "",
    val createdTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "farmers")
data class Farmer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val contact: String,
    val landSizeAcres: Double,
    val saleCall: String,
    val imageUri: String? = null,
    val notes: String = "",
    val createdTime: Long = System.currentTimeMillis()
)

