package com.hiresplayer.player

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {
    @Binds
    @Singleton
    abstract fun bindAudioPlayer(player: Media3AudioPlayer): AudioPlayer
}
