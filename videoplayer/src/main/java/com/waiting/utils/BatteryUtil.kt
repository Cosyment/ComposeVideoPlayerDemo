package com.waiting.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log

/**
 * Author: HeChao
 * Date: 2022/3/25 11:51
 * Description:
 */
object BatteryUtil {

    @SuppressLint("PrivateApi")
    private fun getBatteryCapacity(context: Context): String {
        val mPowerProfile: Any
        var batteryCapacity = 0.0
        val powerProfileClass = "com.android.internal.os.PowerProfile"
        try {
            Class.forName(powerProfileClass)
                .getConstructor(Context::class.java)
                .newInstance(context).also { mPowerProfile = it }
            batteryCapacity = Class.forName(powerProfileClass)
                .getMethod("getBatteryCapacity")
                .invoke(mPowerProfile) as Double
        } catch (e: Exception) {
            Log.e("TAG", e.toString());
        }
        return batteryCapacity.toString() + "mAh"
    }

    fun getBattery(context: Context): BatteryBean? {
        var batteryBean: BatteryBean? = null
        try {
            val batteryStatus = context.registerReceiver(null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                var batteryLevel = -1.0
                if (level != -1 && scale != -1) {
                    batteryLevel = level.toDouble() / scale
                }
                // unknown=1, charging=2, discharging=3, not charging=4, full=5
                val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                batteryBean = BatteryBean(status = status, battery = batteryLevel)
                // ac=1, usb=2, wireless=4
                val plugState = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                // unknown=1, good=2, overheat=3, dead=4, over voltage=5, unspecified failure=6, cold=7
                val health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
                val present = batteryStatus.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false)
                val technology = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
                val temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                val voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return batteryBean
    }

    private fun batteryStatus(status: Int): String {
        var healthBat = ""
        when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> healthBat = "charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> healthBat = "disCharging"
            BatteryManager.BATTERY_STATUS_FULL -> healthBat = "full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> healthBat = "notCharging"
            BatteryManager.BATTERY_STATUS_UNKNOWN -> healthBat = "unknown"
            else -> {}
        }
        return healthBat
    }
}

data class BatteryBean(var status: Int, var battery: Double) {
    fun isCharging(): Boolean = status == 2
}