import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:unique_ids/unique_ids.dart';

void main() {
  const MethodChannel channel = MethodChannel('unique_ids');

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('adId', () async {
    expect(await UniqueIds.adId, '42');
  });
}
