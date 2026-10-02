package org.battlo.freegrilly.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.battlo.freegrilly.data.security.AndroidKeystoreOtaPasswordStore
import org.battlo.freegrilly.data.security.OtaPasswordStore

@Module
@InstallIn(SingletonComponent::class)
abstract class OtaPasswordStoreModule {
    @Binds
    abstract fun bindOtaPasswordStore(impl: AndroidKeystoreOtaPasswordStore): OtaPasswordStore
}
