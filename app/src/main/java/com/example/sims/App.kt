package com.example.sims

import android.app.Application
import android.content.Context

class App : Application() {

    companion object {
        private lateinit var instance: App

        fun instance(): App {
            return instance
        }

        fun getContext(): Context {
            return instance.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
