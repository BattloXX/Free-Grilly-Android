package org.battlo.freegrilly.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.battlo.freegrilly.data.food.FoodDatabase
import org.battlo.freegrilly.data.history.HistoryDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideFoodDatabase(@ApplicationContext context: Context): FoodDatabase =
        Room.databaseBuilder(context, FoodDatabase::class.java, "food_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideFoodDao(db: FoodDatabase) = db.foodDao()

    @Provides
    @Singleton
    fun provideHistoryDatabase(@ApplicationContext context: Context): HistoryDatabase =
        Room.databaseBuilder(context, HistoryDatabase::class.java, "history_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideHistoryDao(db: HistoryDatabase) = db.historyDao()
}
