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
            call.method == "uuid" -> result.success(UUID.randomUUID().toString())
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
    private
    val uniquePsuedoID: String
        get() {
            var serial: String? = null

            val m_szDevIDShort = "35" +
                    Build.BOARD.length % 10 + Build.BRAND.length % 10 +

                    Build.CPU_ABI.length % 10 + Build.DEVICE.length % 10 +

                    Build.DISPLAY.length % 10 + Build.HOST.length % 10 +

                    Build.ID.length % 10 + Build.MANUFACTURER.length % 10 +

                    Build.MODEL.length % 10 + Build.PRODUCT.length % 10 +

                    Build.TAGS.length % 10 + Build.TYPE.length % 10 +

                    Build.USER.length % 10

            try {
                serial = android.os.Build::class.java!!.getField("SERIAL").get(null).toString()
                return UUID(m_szDevIDShort.hashCode().toLong(), serial!!.hashCode().toLong()).toString()
            } catch (exception: Exception) {
                serial = "serial"
            }

            return UUID(m_szDevIDShort.hashCode().toLong(), serial!!.hashCode().toLong()).toString()
        }


    override fun doInBackground(vararg params: Context?): String? {
        var deviceId: String = ""
        //用于生成最终的唯一标识符
        val s = StringBuffer()
        try {
            deviceId = uniquePsuedoID?.replace("-", "")
            s.append(deviceId)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            //获取设备的MACAddress地址 去掉中间相隔的冒号
            deviceId = getLocalMac(params[0])!!.replace(":", "")
            s.append(deviceId)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //如果以上搜没有获取相应的则自己生成相应的UUID作为相应设备唯一标识符
        if (s.length == 0) {
            val uuid = UUID.randomUUID()
            deviceId = uuid.toString().replace("-", "")
            s.append(deviceId)
        }
        //为了统一格式对设备的唯一标识进行md5加密 最终生成32位字符串
        val md5RealDeviceId = getMD5(s.toString(), false)
        return md5RealDeviceId
    }

    override fun onPostExecute(md5RealDeviceId: String?) {
        super.onPostExecute(md5RealDeviceId)
        result.success(md5RealDeviceId)
    }

    private fun getLocalMac(context: Context?): String? {

        var macAddress: String? = null
        val buf = StringBuffer()
        var networkInterface: NetworkInterface? = null
        try {
            networkInterface = NetworkInterface.getByName("eth1")
            if (networkInterface == null) {
                networkInterface = NetworkInterface.getByName("wlan0")
            }
            if (networkInterface == null) {
                return ""
            }
            val addr = networkInterface!!.getHardwareAddress()


            for (b in addr) {
                buf.append(String.format("%02X:", b))
            }
            if (buf.length > 0) {
                buf.deleteCharAt(buf.length - 1)
            }
            macAddress = buf.toString()
        } catch (e: SocketException) {
            e.printStackTrace()
            return ""
        }

        return macAddress
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