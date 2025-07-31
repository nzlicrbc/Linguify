package com.example.linguify.data.di

import android.content.Context
import com.example.linguify.data.manager.LoginPreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLoginPreferencesManager(
        @ApplicationContext context: Context
    ): LoginPreferencesManager = LoginPreferencesManager(context)
}