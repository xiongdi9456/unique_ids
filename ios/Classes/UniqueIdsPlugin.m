#import "UniqueIdsPlugin.h"
#import <unique_ids/unique_ids-Swift.h>

@implementation UniqueIdsPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftUniqueIdsPlugin registerWithRegistrar:registrar];
}
@end
