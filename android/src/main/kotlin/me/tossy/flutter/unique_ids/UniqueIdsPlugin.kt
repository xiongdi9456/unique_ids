package me.tossy.flutter.unique_ids

import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.ads.identifier.AdvertisingIdClient.Info
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.net.NetworkInterface
import java.net.SocketException
import java.security.MessageDigest
import java.util.*

class UniqueIdsPlugin(private val registrar: Registrar) : MethodCallHandler {

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "unique_ids")
            channel.setMethodCallHandler(UniqueIdsPlugin(registrar))
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {

        when {
            call.method == "adId" -> {
                AdIdTask(result).execute(registrar.context())
            }
            call.method == "uuid" -> result.success(UUID.randomUUID().toString().replace("-", ""))
            call.method == "realDeviceId" -> {
                RealDeviceIdTask(result).execute(registrar.context())
            }
            else -> result.notImplemented()
        }
    }
}

private class AdIdTask(private val result: Result) : AsyncTask<Context, Void, String>() {
    override fun doInBackground(vararg params: Context?): String? {
        var adInfo: Info? = null
        try {
            adInfo = AdvertisingIdClient.getAdvertisingIdInfo(params[0])
        } catch (e: IOException) {
            // Unrecoverable error connecting to Google Play services (e.g.,
            // the old version of the service doesn't support getting AdvertisingId).
            return "";
        } catch (e: GooglePlayServicesNotAvailableException) {
            // Google Play services is not available entirely.
            return "";
        }
        var id = ""
        adInfo?.let {
            id = adInfo.id
        }
        return id
    }

    override fun onPostExecute(id: String?) {
        super.onPostExecute(id)
        result.success(id)
    }
}

private class RealDeviceIdTask(private val result: Result) : AsyncTask<Context, Void, String>() {
    
    override fun doInBackground(vararg params: Context?): String? {
        //获取设备Android
        return getUniquePsuedoID(params[0])
        // var uuidStr = Settings.Secure.getString(params[0].contentResolver, Settings.Secure.ANDROID_ID)
        // //getUniquePsuedoID
        // if (isInvalidId(uuidStr)) {
        //     uuidStr = getUniquePsuedoID(params[0])
        // }
        // //为了统一格式对设备的唯一标识进行md5加密 最终生成32位字符串
        // // val md5RealDeviceId = getMD5(uuidStr, false)
        // // return md5RealDeviceId
        // return uuidStr
    }

    override fun onPostExecute(md5RealDeviceId: String?) {
        super.onPostExecute(md5RealDeviceId)
        result.success(md5RealDeviceId)
    }

    //获得独一无二的Psuedo ID
    private fun getUniquePsuedoID(context: Context?): String {
        var serial: String? = null
        //使用硬件信息拼凑出来的15位号码
        val mSzDevIDShort = "35" + Build.BOARD.length % 10 + Build.BRAND.length % 10 + Build.CPU_ABI.length % 10 + Build.DEVICE.length % 10 + Build.DISPLAY.length % 10 + Build.HOST.length % 10 + Build.ID.length % 10 + Build.MANUFACTURER.length % 10 + Build.MODEL.length % 10 + Build.PRODUCT.length % 10 + Build.TAGS.length % 10 + Build.TYPE.length % 10 + Build.USER.length % 10 //13 位
        serial = deviceSerial
        if (isInvalidId(serial)) {
            serial = getDeviceAndroidId(context)
        }
        return UUID(mSzDevIDShort.hashCode().toLong(), serial.hashCode().toLong()).toString()
    }

    private fun getDeviceAndroidId(context: Context?): String {
        var androidId = ""
        if (context != null) {
            androidId = Settings.Secure.getString(context?.contentResolver, Settings.Secure.ANDROID_ID)
        }
        return if (isInvalidId(androidId)) UUID.randomUUID().toString().replace("-", "") else androidId // 随便一个初始化
    }

    //Build.VERSION.SDK_INT>=29会得到unknown(需要配置READ_PHONE危险权限)
    private val deviceSerial: String?
        private get() {
            var str: String? = ""
            if (Build.VERSION.SDK_INT < 26) {
                str = Build.SERIAL
            } else if (Build.VERSION.SDK_INT >= 26) {
                try {
                    val clazz = Class.forName("android.os.Build")
                    val method = clazz.getMethod("getSerial", *arrayOfNulls(0))
                    str = method.invoke(clazz, *arrayOfNulls(0)) as String
                } catch (throwable: Throwable) {
                }
            }
            //Build.VERSION.SDK_INT>=29会得到unknown(需要配置READ_PHONE危险权限)
            return str
        }

    private fun isInvalidId(str: CharSequence?): Boolean {
        return str == null || str.isEmpty() || str == "unknown"
    }
    

    private fun getMD5(message: String, upperCase: Boolean): String {
        var md5str = ""
        try {
            val md = MessageDigest.getInstance("MD5")
            val input = message.toByteArray()
            val buff = md.digest(input)
            md5str = bytesToHex(buff, upperCase)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return md5str
    }

    private fun bytesToHex(bytes: ByteArray, upperCase: Boolean): String {
        val md5str = StringBuffer()
        var digital: Int
        for (i in bytes.indices) {
            digital = bytes[i].toInt()

            if (digital < 0) {
                digital += 256
            }
            if (digital < 16) {
                md5str.append("0")
            }
            md5str.append(Integer.toHexString(digital))
        }
        return if (upperCase) {
            md5str.toString().toUpperCase()
        } else md5str.toString().toLowerCase()
    }
}