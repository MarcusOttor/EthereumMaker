package com.ethereummaker.freeether.ethereummining.screens

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import butterknife.ButterKnife
import com.ethereummaker.freeether.ethereummining.AppTools
import com.ethereummaker.freeether.ethereummining.R
import com.ethereummaker.freeether.ethereummining.core.MyApplication
import com.ethereummaker.freeether.ethereummining.core.advertisements.FyberInterstitial
import com.ethereummaker.freeether.ethereummining.core.data.Thread
import com.ethereummaker.freeether.ethereummining.core.managers.*
import com.ethereummaker.freeether.ethereummining.core.receiver.GameCooldownReceiver
import com.ethereummaker.freeether.ethereummining.core.services.ClaimService
import com.ethereummaker.freeether.ethereummining.core.services.MiningService
import com.ethereummaker.freeether.ethereummining.inject.AppModule
import com.ethereummaker.freeether.ethereummining.inject.DaggerAppComponent
import com.vicpin.krealmextensions.queryAll
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var coinsManager: CoinsManager
    @Inject lateinit var retrofitManager: RetrofitManager
    @Inject lateinit var dialogsManager: DialogsManager
    @Inject lateinit var animationsManager: AnimationsManager

    protected lateinit var coinsView: TextView

    private var handler = Handler()

    var interstitial: FyberInterstitial? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inject()
        startService()
        startMiningService()
        saveData()

        if ((application as MyApplication).advertisement != null) {
            (application as MyApplication).advertisement?.init(this)
        }
        
        interstitial = FyberInterstitial(this)
    }

    fun runBasicHandler() {
        handler.post(updater)
    }

    private var updater = Runnable {
        updateCoins()
        reschedule()
    }

    private fun reschedule() {
        handler.postDelayed(updater, 5000)
    }

    fun bindCoinView() {
        try {
            coinsView = findViewById<View>(R.id.coinsView) as TextView
        } catch (ex: Exception) {}
    }

    fun startService() {
        if (!ClaimService.isTimerRunning) {
            startService(Intent(this, ClaimService::class.java))
        }
    }

    fun startMiningService() {
        if (!MiningService.isTimerRunning) {
            if (!preferencesManager.get(PreferencesManager.MINING_PAUSED, true)) {
                startService(Intent(this, MiningService::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateCoins()
        (application as MyApplication).advertisement?.onResume(this, true)
        if (this !is LoginSignupActivity) {
            if (!AppTools.isNetworkAvaliable(this)) {
                dialogsManager.showAlertDialog(supportFragmentManager, "No internet connection! Mining stopped!", {
                    if (AppTools.isNetworkAvaliable(applicationContext)) {
                        onRestart()
                    } else {
                        finish()
                    }
                })
            }
        }
    }

    fun bind() {
        ButterKnife.bind(this)
    }

    fun updateCoins() {
        try {
            coinsView.text = coinsManager.getCoins().toString()
        } catch (ex: Exception) {}
    }

    fun inject() {
        DaggerAppComponent.builder()
                .appModule(AppModule(applicationContext))
                .mainModule((application as MyApplication).mainModule)
                .build().inject(this)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

    fun layoutBack() {
        (findViewById<View>(R.id.menu) as ImageView)
                .setImageDrawable(resources.getDrawable(R.drawable.arrow_back))
    }

    fun scheduleAlarm() {
        var intent = Intent(this, GameCooldownReceiver::class.java)
        var pi = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        var am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            am.setExact(AlarmManager.RTC_WAKEUP, preferencesManager.get(PreferencesManager.TICKETS_TIME, 0L), pi)
        } else {
            am.set(AlarmManager.RTC_WAKEUP, preferencesManager.get(PreferencesManager.TICKETS_TIME, 0L), pi)
        }
    }

    private fun saveData() {
        if ((preferencesManager.get(PreferencesManager.LAST_SAVED, 0L) < System.currentTimeMillis())
                and (preferencesManager.get(PreferencesManager.LAST_SAVED, 0L) != 0L)) {

            preferencesManager.put(PreferencesManager.LAST_SAVED, System.currentTimeMillis() + (5 * 60 * 1000))

            var data = ""
            var boosters = Thread().queryAll()
            data += "s=${coinsManager.getCoins()}|"
            for (i in boosters.indices) {
                data += "${boosters[i].speed}${if (i < boosters.size - 1) {
                    "@"
                } else {
                    ""
                }}"
            }

            retrofitManager.savedata(preferencesManager.get(PreferencesManager.USERNAME, ""),
                    preferencesManager.get(PreferencesManager.PASSWORD, ""), data, {}, {})

        } else if (preferencesManager.get(PreferencesManager.LAST_SAVED, 0L) == 0L) {
            preferencesManager.put(PreferencesManager.LAST_SAVED, System.currentTimeMillis() + (1 * 60 * 1000))
        }
    }
}
