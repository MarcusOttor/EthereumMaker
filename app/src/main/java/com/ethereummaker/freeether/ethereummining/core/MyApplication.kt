package com.ethereummaker.freeether.ethereummining.core

import android.app.Application
import android.content.Context
import android.support.multidex.MultiDex
import android.support.multidex.MultiDexApplication
import com.ethereummaker.freeether.ethereummining.core.advertisements.AdvertisementManager
import com.ethereummaker.freeether.ethereummining.core.managers.CoinsManager
import com.ethereummaker.freeether.ethereummining.core.managers.PreferencesManager
import com.ethereummaker.freeether.ethereummining.core.managers.RetrofitManager
import com.ethereummaker.freeether.ethereummining.core.services.MiningService
import com.ethereummaker.freeether.ethereummining.inject.AppModule
import com.ethereummaker.freeether.ethereummining.inject.DaggerAppComponent
import com.ethereummaker.freeether.ethereummining.inject.MainModule
import com.yandex.metrica.YandexMetrica
import com.yandex.metrica.YandexMetricaConfig
import io.realm.Realm
import uk.co.chrisjenx.calligraphy.CalligraphyConfig
import javax.inject.Inject

class MyApplication : MultiDexApplication() {

    @Inject lateinit var calligraphy: CalligraphyConfig
    @Inject lateinit var coinsManager: CoinsManager
    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var metrica: YandexMetricaConfig.Builder

    @Inject lateinit var retrofit: RetrofitManager

    lateinit var mainModule: MainModule

    var advertisement: AdvertisementManager? = null

    override fun onCreate() {
        super.onCreate()

        mainModule = MainModule(this)

        DaggerAppComponent.builder()
                .appModule(AppModule(applicationContext))
                .mainModule(mainModule)
                .build().inject(this)

        CalligraphyConfig.initDefault(calligraphy)

        Realm.init(this)

        advertisement = AdvertisementManager(preferencesManager, coinsManager, applicationContext)

        if (!MiningService.isTimerRunning) {
            preferencesManager.put(PreferencesManager.MINING_PAUSED, true)
        }

        if (!preferencesManager.get(PreferencesManager.FIRST_LAUNCH, true)) {
            metrica.handleFirstActivationAsUpdate(true)
        }

        var extended = metrica.build()
        YandexMetrica.activate(applicationContext, extended)
        YandexMetrica.enableActivityAutoTracking(this)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}
