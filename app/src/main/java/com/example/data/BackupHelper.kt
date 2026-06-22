package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonClass
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class BackupData(
    val fertilizerTypes: List<FertilizerType>,
    val salesRecords: List<SalesRecord>
)

object BackupHelper {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(BackupData::class.java)

    fun exportToJson(types: List<FertilizerType>, sales: List<SalesRecord>): String {
        return adapter.toJson(BackupData(types, sales))
    }

    fun importFromJson(jsonString: String): BackupData? {
        return try {
            adapter.fromJson(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportToCsv(sales: List<SalesRecord>): String {
        val sb = java.lang.StringBuilder()
        sb.append("ID,Date,Fertilizer Name,Quantity,Notes,Created Time\n")
        for (sale in sales) {
            val noteEscaped = sale.notes.replace("\"", "\"\"")
            sb.append("${sale.id},${sale.date},\"${sale.fertilizerName}\",${sale.quantity},\"$noteEscaped\",${sale.createdTime}\n")
        }
        return sb.toString()
    }
}
