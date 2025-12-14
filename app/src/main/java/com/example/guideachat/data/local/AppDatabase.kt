package com.example.guideachat.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.guideachat.data.model.VoitureEntity
import com.example.guideachat.data.model.MoteursCarburant
import com.example.guideachat.data.model.BilanFiabilite
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// 1. Le DAO (Data Access Object)
@Dao
interface CarDao {
    @Query("SELECT * FROM voitures WHERE id_modele = :id")
    suspend fun getVoiture(id: String): VoitureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiture(voiture: VoitureEntity)

    // Pour l'auto-complétion
    @Query("SELECT nom_modele FROM voitures WHERE nom_modele LIKE :query || '%' LIMIT 5")
    fun getSuggestions(query: String): Flow<List<String>>
}

// 2. Les Convertisseurs (JSON <-> String BDD)
class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromListInt(list: List<Int>): String = json.encodeToString(list)
    @TypeConverter
    fun toListInt(data: String): List<Int> = json.decodeFromString(data)

    @TypeConverter
    fun fromListString(list: List<String>): String = json.encodeToString(list)
    @TypeConverter
    fun toListString(data: String): List<String> = json.decodeFromString(data)

    @TypeConverter
    fun fromMoteurs(moteurs: MoteursCarburant): String = json.encodeToString(moteurs)
    @TypeConverter
    fun toMoteurs(data: String): MoteursCarburant = json.decodeFromString(data)

    @TypeConverter
    fun fromBilan(bilan: BilanFiabilite): String = json.encodeToString(bilan)
    @TypeConverter
    fun toBilan(data: String): BilanFiabilite = json.decodeFromString(data)
}

// 3. La Database
@Database(entities = [VoitureEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "guide_auto_db"
                ).build().also { instance = it }
            }
    }
}