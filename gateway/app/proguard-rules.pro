-dontoptimize
-dontshrink
-ignorewarnings
-overloadaggressively
-repackageclasses ''
-allowaccessmodification

-keep class com.nepting.** { *; }
-keep class com.nepting.common.client.* { *; }
-keep class com.nepting.common.client.model.* { *; }
-keep class com.nepting.common.client.callback.* { *; }
-keep class com.nepting.softpos.client.* { *; }

-keep class com.alcineo.softpos.payment.api.** { *; }
-keep class com.alcineo.softpos.payment.model.** { *; }
-keep class com.alcineo.softpos.security.api.** { *; }
-keep class com.alcineo.softpos.pinpad.api.** { *; }

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** i(...);
    public static *** v(...);
}
