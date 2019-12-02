import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:unique_ids/unique_ids.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _adId = 'Unknown';
  String _uuid = 'Unknown';
  String _realDeviceId = 'Unknown';

  @override
  void initState() {
    super.initState();
    _init();
  }

  Future<void> _init() async {
    String adId;
    String uuid;
    String realDeviceId;

    try {
      uuid = await UniqueIds.uuid;
    } on PlatformException {
      uuid = 'Failed to create uuid.v1';
    }

    try {
      adId = await UniqueIds.adId;
    } on PlatformException {
      adId = 'Failed to get adId version.';
    }

    try {
      realDeviceId = await UniqueIds.realDeviceId;
    } on PlatformException {
      realDeviceId = 'Failed to get realDeviceId.';
    }

    if (!mounted) return;

    setState(() {
      _adId = adId;
      _uuid = uuid;
      _realDeviceId = realDeviceId;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
            child: Column(
              children: [
                Text('Running on adId: $_adId\n'),
                Text('created uuid: $_uuid'),
                Text('created realDeviceId: $_realDeviceId'),
              ],
            )),
      ),
    );
  }
}
