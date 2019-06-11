import Flutter
import UIKit
import AdSupport

public class SwiftUniqueIdsPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "unique_ids", binaryMessenger: registrar.messenger())
    let instance = SwiftUniqueIdsPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
          case "adId":
              result(ASIdentifierManager.shared().advertisingIdentifier.uuidString)
          case "uuid":
              result(NSUUID().uuidString.lowercased())
          default:
              result(nil)
    }
  }
}
