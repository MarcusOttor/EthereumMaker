package com.ethereummaker.freeether.ethereummining.core.data

import io.realm.RealmObject

open class Thread() : RealmObject() {

    var index: Int = 0
    var satoshi: Int = 0
    var speed: Int = 0

    constructor(index: Int, satoshi: Int, speed: Int) : this() {
        this.satoshi = satoshi
        this.index = index
        this.speed = speed
    }
}
