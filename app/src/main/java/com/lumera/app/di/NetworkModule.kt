package com.lumera.app.di

import com.lumera.app.data.remote.StremioApiService
import com.lumera.app.data.remote.IntroDbService
import com.lumera.app.data.remote.TmdbApiService
import com.lumera.app.data.remote.TraktApiService
import com.lumera.app.data.remote.TraktSyncApiService
import com.lumera.app.data.trakt.TraktAuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TmdbRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TraktRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TraktAuthenticatedRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val dispatcher = okhttp3.Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 12
        }
    
        return OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(
                okhttp3.ConnectionPool(
                    maxIdleConnections = 8,
                    keepAliveDuration = 90,
                    timeUnit = TimeUnit.SECONDS
                )
            )
    
            // Keep Cloudflare DNS if it is working for you.
            // If timeout still happens after this change, remove this line and use system DNS.
            .dns(com.lumera.app.network.CloudflareDns.create())
    
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
    
            // Most important fix: prevents total request from hanging forever.
            .callTimeout(35, TimeUnit.SECONDS)
    
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Lumera_VHfbFBaqTXRpIJIA")
                    .build()
            
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://stremio-addons.netlify.app/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @TmdbRetrofit
    fun provideTmdbRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideStremioApi(retrofit: Retrofit): StremioApiService {
        return retrofit.create(StremioApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideIntroDbService(retrofit: Retrofit): IntroDbService {
        return retrofit.create(IntroDbService::class.java)
    }

    @Provides
    @Singleton
    fun provideTmdbApiService(@TmdbRetrofit retrofit: Retrofit): TmdbApiService {
        return retrofit.create(TmdbApiService::class.java)
    }

    @Provides
    @Singleton
    @TraktRetrofit
    fun provideTraktRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTraktApiService(@TraktRetrofit retrofit: Retrofit): TraktApiService {
        return retrofit.create(TraktApiService::class.java)
    }

    @Provides
    @Singleton
    @TraktAuthenticatedRetrofit
    fun provideTraktAuthenticatedRetrofit(
        okHttpClient: OkHttpClient,
        traktAuthInterceptor: TraktAuthInterceptor
    ): Retrofit {
        val authenticatedClient = okHttpClient.newBuilder()
            .addInterceptor(traktAuthInterceptor)
            .build()
        return Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(authenticatedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTraktSyncApiService(@TraktAuthenticatedRetrofit retrofit: Retrofit): TraktSyncApiService {
        return retrofit.create(TraktSyncApiService::class.java)
    }
}
