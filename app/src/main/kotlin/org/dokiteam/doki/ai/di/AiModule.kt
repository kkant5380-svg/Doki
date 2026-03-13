package org.dokiteam.doki.ai.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * AiModule — all AI classes use @Inject constructor + @Singleton so Hilt
 * discovers and provides them automatically. No manual @Provides needed.
 */
@Module
@InstallIn(SingletonComponent::class)
object AiModule
