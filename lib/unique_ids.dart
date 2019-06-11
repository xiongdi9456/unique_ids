import 'dart:async';

import 'package:flutter/services.dart';

class UniqueIds {
  static const MethodChannel _channel = const MethodChannel('unique_ids');

  static Future<String> get adId async {
    final String _adId = await _channel.invokeMethod('adId');
    return _adId;
  }

  static Future<String> get uuid async {
    final String _uuid = await _channel.invokeMethod('uuid');
    return _uuid;
  }
}
