package com.ethereummaker.freeether.ethereummining.screens.dialogs

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import butterknife.ButterKnife
import butterknife.OnClick
import com.ethereummaker.freeether.ethereummining.AppTools
import com.ethereummaker.freeether.ethereummining.R
import com.ethereummaker.freeether.ethereummining.core.MyApplication
import com.ethereummaker.freeether.ethereummining.core.managers.CoinsManager
import com.ethereummaker.freeether.ethereummining.core.managers.DialogsManager
import com.ethereummaker.freeether.ethereummining.core.managers.PreferencesManager
import com.ethereummaker.freeether.ethereummining.core.managers.RetrofitManager
import com.ethereummaker.freeether.ethereummining.inject.AppModule
import com.ethereummaker.freeether.ethereummining.inject.DaggerAppComponent
import kotlinx.android.synthetic.main.promocode_dialog.*
import javax.inject.Inject

class PromocodeDialog(private var done: () -> Unit) : DialogFragment() {

    @Inject lateinit var retrofitManager: RetrofitManager
    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var coinsManager: CoinsManager
    @Inject lateinit var dialogsManager: DialogsManager

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog.window.requestFeature(Window.FEATURE_NO_TITLE)

        DaggerAppComponent.builder()
                .appModule(AppModule(context))
                .mainModule((activity.application as MyApplication).mainModule)
                .build().inject(this)

        var view = inflater?.inflate(R.layout.promocode_dialog, container, false)

        ButterKnife.bind(this, view!!)

        return view
    }

    @OnClick(R.id.enter)
    fun invite() {
        if (AppTools.isNetworkAvaliable(context)) {
            var progress = dialogsManager.showProgressDialog(activity.supportFragmentManager)
            retrofitManager.checkinvite(promocodeTxt.text.toString().trim(), {
                progress.dismiss()
                coinsManager.addCoins(1000)
                dialogsManager.showAlertDialog(activity.supportFragmentManager, "You got 1000 Szabo for invite code!", {
                    dismiss()
                    done()
                })
            }, {
                progress.dismiss()
                dialogsManager.showAlertDialog(activity.supportFragmentManager, "Invite code is wrong!", {})
            })
        } else {
            dialogsManager.showAlertDialog(activity.supportFragmentManager,
                    "Sorry, no internet connection!", {})
        }
    }

    @OnClick(R.id.skip)
    fun skip() {
        dismiss()
        done()
    }

    fun root(): View {
        return view?.rootView!!
    }
}
