package dev.fluttercommunity.plus.packageinfo

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

/** PackageInfoPlugin  */
class PackageInfoPlugin : MethodCallHandler, FlutterPlugin {
    private var applicationContext: Context? = null
    private var methodChannel: MethodChannel? = null

    /** Plugin registration.  */
    override fun onAttachedToEngine(binding: FlutterPluginBinding) {
        applicationContext = binding.applicationContext
        methodChannel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
        methodChannel!!.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        applicationContext = null
        methodChannel!!.setMethodCallHandler(null)
        methodChannel = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            if (call.method == "getAll") {
                val packageManager = applicationContext!!.packageManager
                val info = packageManager.getPackageInfo(applicationContext!!.packageName, 0)

                val installerPackage = getInstallerPackageName()

                val infoMap = HashMap<String, String>()
                infoMap.apply {
                    put("appName", info.applicationInfo.loadLabel(packageManager).toString())
                    put("packageName", applicationContext!!.packageName)
                    put("version", info.versionName)
                    put("buildNumber", getLongVersionCode(info).toString())
                    if (installerPackage != null) put("installerStore", installerPackage)
                }.also { resultingMap ->
                    result.success(resultingMap)
                }
            } else {
                result.notImplemented()
            }
        } catch (ex: PackageManager.NameNotFoundException) {
            result.error("Name not found", ex.message, null)
        }
    }

    /**
     * Using initiatingPackageName on Android 11 and newer because it can't be changed
     * https://developer.android.com/reference/android/content/pm/InstallSourceInfo#getInitiatingPackageName()
     */
    private fun getInstallerPackageName(): String? {
        val packageManager = applicationContext!!.packageManager
        val packageName = applicationContext!!.packageName
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(packageName).initiatingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(packageName)
        }
    }

    @Suppress("deprecation")
    private fun getLongVersionCode(info: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            info.versionCode.toLong()
        }
    }

    companion object {
        private const val CHANNEL_NAME = "dev.fluttercommunity.plus/package_info"
    }
}
