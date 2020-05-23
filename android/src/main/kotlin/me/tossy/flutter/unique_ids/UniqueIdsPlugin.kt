package me.tossy.flutter.unique_ids

import android.content.Context
import android.os.AsyncTask
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.ads.identifier.AdvertisingIdClient.Info
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.IOException
import java.util.*
import android.os.Build
import java.net.NetworkInterface
import java.net.SocketException
import java.security.MessageDigest
import java.util.UUID

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
        } catch (e: GooglePlayServicesNotAvailableException) {
            // Google Play services is not available entirely.
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
        //获取设备ID
        var uuidStr = getDeviceMacAddress(context)
        if (isInvalidId(uuidStr)) {
            uuidStr = getUniquePsuedoID(context)
        }
        //为了统一格式对设备的唯一标识进行md5加密 最终生成32位字符串
        val md5RealDeviceId = getMD5(uuidStr, false)
        return md5RealDeviceId
    }

    override fun onPostExecute(md5RealDeviceId: String?) {
        super.onPostExecute(md5RealDeviceId)
        result.success(md5RealDeviceId)
    }

    //获得独一无二的Psuedo ID
    private fun getUniquePsuedoID(context: Context): String {
        var serial: String? = null
        //使用硬件信息拼凑出来的15位号码
        val mSzDevIDShort = "35" + Build.BOARD.length % 10 + Build.BRAND.length % 10 + Build.CPU_ABI.length % 10 + Build.DEVICE.length % 10 + Build.DISPLAY.length % 10 + Build.HOST.length % 10 + Build.ID.length % 10 + Build.MANUFACTURER.length % 10 + Build.MODEL.length % 10 + Build.PRODUCT.length % 10 + Build.TAGS.length % 10 + Build.TYPE.length % 10 + Build.USER.length % 10 //13 位
        serial = deviceSerial
        if (isInvalidId(serial)) {
            serial = getDeviceAndroidId(context)
        }
        return UUID(mSzDevIDShort.hashCode().toLong(), serial.hashCode().toLong()).toString()
    }

    private fun getDeviceAndroidId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return if (isInvalidId(androidId)) UUID.randomUUID().toString().replace("-", "") else androidId // 随便一个初始化
    }

    //Build.VERSION.SDK_INT>=29会得到unknown(需要配置READ_PHONE危险权限)
    private val deviceSerial: String?
        private get() {
            var str: String? = ""
            if (Build.VERSION.SDK_INT >= 9 && Build.VERSION.SDK_INT < 26) {
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
    /**
     * 获取设备MAC地址
     *
     * @param context
     * @return Mac地址
     */
    private fun getDeviceMacAddress(context: Context): String {
        val mac: String = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> {
                getMacDefault(context)
            }
            Build.VERSION.SDK_INT < Build.VERSION_CODES.N -> {
                macAddressM
            }
            else -> {
                macFromHardware
            }
        }
        return if (mac == "02:00:00:00:00:00") "" else mac
    }

    /**
     * Android  6.0 之前（不包括6.0）
     * 必须的权限  <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"></uses-permission>
     * @param context
     * @return String
     */
    private fun getMacDefault(context: Context?): String {
        var mac = "02:00:00:00:00:00"
        if (context == null) {
            return mac
        }
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                ?: return mac
        var info: WifiInfo? = null
        try {
            info = wifi.connectionInfo
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (info == null) {
            return mac
        }
        mac = info.macAddress
        if (!TextUtils.isEmpty(mac)) {
            mac = mac.toUpperCase(Locale.ENGLISH)
        }
        return mac
    }

    /**
     * Android 6.0（包括） - Android 7.0（不包括）
     * @return String
     */
    private val macAddressM: String
        private get() {
            var wifiAddress = "02:00:00:00:00:00"
            try {
                wifiAddress = BufferedReader(FileReader(File("/sys/class/net/wlan0/address"))).readLine()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return wifiAddress
        }

    /**
     * 遍历循环所有的网络接口，找到接口是 wlan0
     * 必须的权限 <uses-permission android:name="android.permission.INTERNET"></uses-permission>
     * *type = eth0,获取有线mac
     * *type = wlan0,获取无线mac
     * @return String
     */
    private val macFromHardware: String
        private get() {
            try {
                val all: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (nif in all) {
                    if (!nif.name.equals("wlan0", ignoreCase = true) && !nif.name.equals("eth0", ignoreCase = true)) {
                        continue
                    }
                    val macBytes = nif.hardwareAddress
                    if (macBytes == null || macBytes.isEmpty()) {
                        return ""
                    }
                    val res1 = StringBuilder()
                    for (b in macBytes) {
                        res1.append(String.format("%02X:", b))
                    }
                    if (res1.isNotEmpty()) {
                        res1.deleteCharAt(res1.length - 1)
                    }
                    return res1.toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return "02:00:00:00:00:00"
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