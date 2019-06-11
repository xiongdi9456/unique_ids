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
