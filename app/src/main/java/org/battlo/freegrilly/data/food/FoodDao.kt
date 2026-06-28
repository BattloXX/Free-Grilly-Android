package org.battlo.freegrilly.data.food

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM custom_foods ORDER BY isFavorite DESC, nameDe ASC")
    fun getAll(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM custom_foods WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<FoodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(food: FoodEntity)

    @Delete
    suspend fun delete(food: FoodEntity)

    @Query("UPDATE custom_foods SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: String, fav: Boolean)

    @Query("DELETE FROM custom_foods WHERE id = :id")
    suspend fun deleteById(id: String)
}
