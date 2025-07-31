package com.example.linguify.data.di

import com.example.linguify.data.repositories.AuthRepository
import com.example.linguify.data.repositories.PexelsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository = AuthRepository(firebaseAuth, firestore)

    @Provides
    @Singleton
    fun providePexelsRepository(): PexelsRepository {
        return PexelsRepository()
    }
}