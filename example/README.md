# unique_ids_example

    Future<void> example() async {
        String adId;
        String uuid;
        
        // generate uuid
        try {
          uuid = await UniqueIds.uuid;
        } on PlatformException {
          uuid = 'Failed to create uuid.v1';
        }
    
        // get adid(idfa)
        try {
          adId = await UniqueIds.adId;
        } on PlatformException {
          adId = 'Failed to get adId version.';
        }
      }

## Getting Started

This project is a starting point for a Flutter application.

A few resources to get you started if this is your first Flutter project:

- [Lab: Write your first Flutter app](https://flutter.dev/docs/get-started/codelab)
- [Cookbook: Useful Flutter samples](https://flutter.dev/docs/cookbook)

For help getting started with Flutter, view our
[online documentation](https://flutter.dev/docs), which offers tutorials,
samples, guidance on mobile development, and a full API reference.
