package com.netspeed.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SpeedService.createChannel(this)

        val service = Intent(this, SpeedService::class.java)
        requireViewById<Button>(R.id.start).setOnClickListener {
            startForegroundService(service)
        }
        requireViewById<Button>(R.id.stop).setOnClickListener {
            // Deliver Stop after any pending start so the service can enter the foreground first.
            startService(Intent(service).setAction(SpeedService.ACTION_STOP))
        }
    }
}
