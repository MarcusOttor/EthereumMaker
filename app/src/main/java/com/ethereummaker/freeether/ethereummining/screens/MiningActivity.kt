package com.ethereummaker.freeether.ethereummining.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.OnClick
import com.ethereummaker.freeether.ethereummining.AppTools
import com.ethereummaker.freeether.ethereummining.R
import com.ethereummaker.freeether.ethereummining.core.data.Thread
import com.ethereummaker.freeether.ethereummining.core.managers.PreferencesManager
import com.ethereummaker.freeether.ethereummining.core.services.MiningService
import com.vicpin.krealmextensions.deleteAll
import com.vicpin.krealmextensions.queryAll
import com.vicpin.krealmextensions.save
import com.vicpin.krealmextensions.saveAll
import kotlinx.android.synthetic.main.activity_mining.*
import kotlinx.android.synthetic.main.content_mining.*
import kotlinx.android.synthetic.main.thread_item.view.*

class MiningActivity : BaseActivity(), Runnable {

    private var boostersHandler = Handler()

    private var threads: MutableList<Thread>? = null
    private var adapter: ThreadAdapter? = null

    private var currentPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mining)

        emailTextMain.text = preferencesManager.get(PreferencesManager.USERNAME, "")

        if (preferencesManager.get(PreferencesManager.FIRST_LAUNCH, true)) {
            preferencesManager.put(PreferencesManager.CORES, AppTools.coresCount())
            if (Thread().queryAll().isEmpty()) {
                Thread(0, 5, 1).save()
            }
        }
        preferencesManager.put(PreferencesManager.FIRST_LAUNCH, false)

        bindCoinView()
        bind()

        setUpMining()
        setSpeedLayout()

        startMining()

        boostersHandler.post(this)

        checkBonusSatoshi()

    }

    private fun startMining() {
        if (!MiningService.isTimerRunning) {
            preferencesManager.put(PreferencesManager.MINING_PAUSED, false)
            startService()
        }
    }

    @OnClick(R.id.speed1, R.id.speed3, R.id.speed5)
    fun speedClick(v: View) {
        when (v.id) {
            R.id.speed1 -> {
                if (threads !![currentPosition].speed != 1) {
                    dialogsManager.showAlertDialog(supportFragmentManager,
                            "Not available!", { interstitial?.show() })
                }
            }
            R.id.speed3 -> {
                if (threads !![currentPosition].speed > 3) {
                    dialogsManager.showAlertDialog(supportFragmentManager,
                            "Not available!", { interstitial?.show() })
                } else if (threads !![currentPosition].speed < 3) {
                    dialogsManager.showAdvAlertDialog(supportFragmentManager,
                            "Do you want to increase speed for this thread for ${(currentPosition + 1) * 3 * 1000} Szabo?",
                            {
                                if (coinsManager.getCoins() >= ((currentPosition + 1) * 3 * 1000)) {
                                    coinsManager.subtractCoins((currentPosition + 1) * 3 * 1000)
                                    updateCoins()
                                    threads !![currentPosition].speed = 3
                                    Thread().deleteAll()
                                    threads?.saveAll()
                                    adapter?.notifyDataSetChanged()
                                    updateSpeed()
                                    setSpeedLayout()
                                } else {
                                    dialogsManager.showAlertDialog(supportFragmentManager,
                                            "Not enough Szabo!", { interstitial?.show() })
                                }
                            }, { interstitial?.show() })
                }
            }
            R.id.speed5 -> {
                if (threads !![currentPosition].speed < 5) {
                    dialogsManager.showAdvAlertDialog(supportFragmentManager,
                            "Do you want to increase speed for this thread for ${(currentPosition + 1) * 5 * 1000} Szabo?",
                            {
                                if (coinsManager.getCoins() >= ((currentPosition + 1) * 5 * 1000)) {
                                    coinsManager.subtractCoins((currentPosition + 1) * 5 * 1000)
                                    updateCoins()
                                    threads !![currentPosition].speed = 5
                                    Thread().deleteAll()
                                    threads?.saveAll()
                                    adapter?.notifyDataSetChanged()
                                    updateSpeed()
                                    setSpeedLayout()
                                } else {
                                    dialogsManager.showAlertDialog(supportFragmentManager,
                                            "Not enough Szabo!", { interstitial?.show() })
                                }
                            }, { interstitial?.show() })
                }
            }
        }
    }

    private fun setSpeedLayout() {
        var speed = threads !![currentPosition].speed
        threadSpeedText.text = "${threads!![currentPosition].satoshi * threads!![currentPosition].speed} SZABO/MIN"
        when (speed) {
            1 -> {
                speed1.setBackgroundColor(resources.getColor(R.color.colorPrimary))
                speed3.setBackgroundColor(resources.getColor(R.color.colorAccentLight))
                speed5.setBackgroundColor(resources.getColor(R.color.colorAccentLight))
                speed1.setTextColor(resources.getColor(R.color.colorAccent))
                speed3.setTextColor(resources.getColor(android.R.color.white))
                speed5.setTextColor(resources.getColor(android.R.color.white))
            }
            3 -> {
                speed1.setBackgroundColor(resources.getColor(R.color.colorAccentLight))
                speed3.setBackgroundColor(resources.getColor(R.color.colorPrimary))
                speed5.setBackgroundColor(resources.getColor(R.color.colorAccentLight))
                speed1.setTextColor(resources.getColor(android.R.color.white))
                speed3.setTextColor(resources.getColor(R.color.colorAccent))
                speed5.setTextColor(resources.getColor(android.R.color.white))
            }
            5 -> {
                speed1.setBackgroundColor(resources.getColor(R.color.colorAccentLight))
                speed3.setBackgroundColor(resources.getColor(R.color.colorAccentLight))
                speed5.setBackgroundColor(resources.getColor(R.color.colorPrimary))
                speed1.setTextColor(resources.getColor(android.R.color.white))
                speed3.setTextColor(resources.getColor(android.R.color.white))
                speed5.setTextColor(resources.getColor(R.color.colorAccent))
            }
        }
    }

    private fun setUpMining() {
        threads = Thread().queryAll().toMutableList()
        adapter = ThreadAdapter()
        threadsRecycler.setHasFixedSize(true)
        threadsRecycler.layoutManager = LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false)
        threadsRecycler.adapter = adapter
        adapter?.notifyDataSetChanged()
        updateSpeed()
    }

    private fun updateSpeed() {
        miningSpeedText.text = threads!!.indices.sumBy { threads!![it].satoshi * threads!![it].speed }.toString()
    }

    @OnClick(R.id.addCore)
    fun coreAdd() {
        if (preferencesManager.get(PreferencesManager.CORES, 1) > threads !!.size) {
            dialogsManager.showAdvAlertDialog(supportFragmentManager,
                    "Do you want to purchase a new thread for ${5000 * threads?.size !!} Szabo?",
                    {
                        if (coinsManager.getCoins() >= (5000 * threads?.size !!)) {
                            coinsManager.subtractCoins((5000 * threads?.size !!))
                            updateCoins()
                            Thread(threads?.size !!, 5, 1).save()
                            threads = Thread().queryAll().toMutableList()
                            adapter?.notifyDataSetChanged()
                            currentPosition = threads?.size !! - 1
                            threadsText.text = "#${currentPosition + 1}"
                            threadSpeedText.text = "${threads!![currentPosition].satoshi * threads!![currentPosition].speed} SZABO/MIN"
                            updateSpeed()
                        } else {
                            dialogsManager.showAlertDialog(supportFragmentManager, "Not enough Szabo!", {
                                interstitial?.show()
                            })
                        }
                    }, { interstitial?.show() })
        } else {
            dialogsManager.showAlertDialog(supportFragmentManager, "${threads !!.size} threads is max for your device!", {
                interstitial?.show()
            })
        }
    }

    @OnClick(R.id.redeem, R.id.logout, R.id.free, R.id.menu,
            R.id.share, R.id.rateus, R.id.claiming, R.id.tickets, R.id.freeBtn)
    fun controls(v: View) {
        when (v.id) {
            R.id.redeem -> {
                startActivity(Intent(this, WithdrawActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
            R.id.logout -> {
                dialogsManager.showAdvAlertDialog(supportFragmentManager, "Are you sure?", {
                    preferencesManager.deleteAll()
                    coinsManager.deleteall()
                    Thread().deleteAll()
                    startActivity(Intent(this, LoginSignupActivity::class.java)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    finish()
                }, {})
            }
            R.id.free -> {
                startActivity(Intent(this, OffersActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
            R.id.menu -> {
                drawer_layout.openDrawer(nav_view)
            }
            R.id.share -> {
                var mess = "I'am using this app to get free Ethereum: \"https://play.google.com/store/apps/details?id=" +
                        packageName + "\"" + " Here is my invite code: " +
                        preferencesManager.get(PreferencesManager.INVITE_CODE, "") +
                        " Install an app and enter this code to get 1000 Szabo!"
                try {
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, mess), "Share"))
                } catch (ex: Exception) {
                }
            }
            R.id.rateus -> {
                dialogsManager.showAlertDialog(supportFragmentManager, "Ple".plus("ase, ").plus("rat").plus("e us")
                        .plus(" 5").plus(" sta").plus("rs!"), {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)))
                    } catch (ex: Exception) {
                    }
                })
            }
            R.id.claiming -> {
                startActivity(Intent(this, ClaimActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
            R.id.tickets -> {
                startActivity(Intent(this, GameActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
            R.id.freeBtn -> {
                startActivity(Intent(this, OffersActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
        }
    }

    override fun onBackPressed() {
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            dialogsManager.showAdvAlertDialog(supportFragmentManager, "Do you really want to exit?",
                    { finish() }, {})
        }
    }

    private fun checkBonusSatoshi() {
        if (preferencesManager.get(PreferencesManager.LAST_CHECKED, 0L) <= System.currentTimeMillis()) {
            preferencesManager.put(PreferencesManager.LAST_CHECKED, (System.currentTimeMillis() + (60 * 60 * 1000)))
            retrofitManager.invitesatoshi(preferencesManager.get(PreferencesManager.USERNAME, ""),
                    preferencesManager.get(PreferencesManager.PASSWORD, ""), { Szabo ->
                if (Szabo > 0) {
                    coinsManager.addCoins(Szabo)
                    updateCoins()
                    dialogsManager.showAlertDialog(supportFragmentManager,
                            "Someone entered your invite code! You got $Szabo Szabo!", {})
                }
            }, {})
        }
    }

    override fun run() {
        updateCoins()
        updateSpeed()
        boostersHandler.postDelayed(this, 5000)
    }

    override fun onDestroy() {
        boostersHandler.removeCallbacks(this)
        super.onDestroy()
    }

    inner class ThreadAdapter : RecyclerView.Adapter<ThreadAdapter.ThreadsHolder>() {

        override fun getItemCount(): Int = threads?.size !!

        override fun onBindViewHolder(holder: ThreadsHolder?, position: Int) {
            holder?.itemView?.coreText?.text = "#${threads !![position].index + 1}"
            holder?.itemView?.coreText?.setBackgroundResource(R.drawable.plate)
            holder?.itemView?.coreText?.setOnClickListener {
                currentPosition = position
                threadsText.text = "#${position + 1}"
                threadSpeedText.text = "${threads!![position].satoshi * threads!![position].speed} SZABO/MIN"
                updateSpeed()
                setSpeedLayout()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ThreadsHolder {
            return ThreadsHolder(LayoutInflater.from(parent?.context).inflate(R.layout.thread_item, parent, false))
        }

        inner class ThreadsHolder(itemView: View?) : RecyclerView.ViewHolder(itemView)
    }
}
