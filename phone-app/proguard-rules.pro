# gRPC (okhttp transport, not netty) — none of the resolved io.grpc
# runtime jars bundle consumer proguard rules, so these ServiceLoader-
# discovered providers need protecting explicitly; the FQCN is read from
# a META-INF/services text file, so both the name and the class itself
# must survive.
-keep class io.grpc.internal.DnsNameResolverProvider { *; }
-keep class io.grpc.okhttp.OkHttpChannelProvider { *; }
-keep class io.grpc.util.SecretRoundRobinLoadBalancerProvider$Provider { *; }
-dontwarn io.grpc.**

# protobuf-java (full runtime, not lite — :protos has no `option "lite"`,
# so generated messages extend GeneratedMessage). Its own jars bundle no
# consumer rules either; GeneratedMessage's field-accessor-table
# machinery is the actual reflection point, not the whole runtime
# package.
-keep class com.google.protobuf.GeneratedMessage { *; }
-keep class com.google.protobuf.GeneratedMessage$* { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessage {
    <fields>;
}
-dontwarn com.google.protobuf.**

# App-owned generated wire models — exact field layout must match the
# Python gRPC server's wire format.
-keep class com.ubopod.ubokotlin.** { *; }
