package com.ethereummaker.freeether.ethereummining.inject

import com.ethereummaker.freeether.ethereummining.core.MyApplication
import com.ethereummaker.freeether.ethereummining.core.services.ClaimService
import com.ethereummaker.freeether.ethereummining.core.services.MiningService
import com.ethereummaker.freeether.ethereummining.screens.BaseActivity
import com.ethereummaker.freeether.ethereummining.screens.dialogs.LoginDialog
import com.ethereummaker.freeether.ethereummining.screens.dialogs.PromocodeDialog
import com.ethereummaker.freeether.ethereummining.screens.dialogs.SignupDialog
import dagger.Component

@Component(modules = arrayOf(AppModule::class, MainModule::class))
interface AppComponent {

    fun inject(screen: BaseActivity)
    fun inject(app: MyApplication)
    fun inject(dialog: LoginDialog)
    fun inject(dialog: SignupDialog)
    fun inject(dialog: PromocodeDialog)
    fun inject(service: ClaimService)
    fun inject(service: MiningService)
}
