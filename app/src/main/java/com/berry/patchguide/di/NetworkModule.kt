package com.berry.patchguide.di

import com.berry.patchguide.data.remote.api.BerryPatchApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // ─── Constants ───────────────────────────────────────────────────────────
    // TODO: Render 배포 완료 후 실제 클라우드 URL로 교체
    private const val LOCAL_BASE_URL = "http://172.30.1.79:8000/"
    private const val CLOUD_BASE_URL = "https://berrypatchguide.onrender.com/"

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    // ─── Local Retrofit ──────────────────────────────────────────────────────
    @Provides
    @Singleton
    @Named("local")
    fun provideLocalRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl(LOCAL_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
    }

    @Provides
    @Singleton
    @Named("local")
    fun provideLocalApi(@Named("local") retrofit: Retrofit): BerryPatchApi {
        return retrofit.create(BerryPatchApi::class.java)
    }

    // ─── Cloud Retrofit ──────────────────────────────────────────────────────
    @Provides
    @Singleton
    @Named("cloud")
    fun provideCloudRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl(CLOUD_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
    }

    @Provides
    @Singleton
    @Named("cloud")
    fun provideCloudApi(@Named("cloud") retrofit: Retrofit): BerryPatchApi {
        return retrofit.create(BerryPatchApi::class.java)
    }
}
