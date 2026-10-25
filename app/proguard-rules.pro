# Proguard rules for OpenTVBox
-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.opentvbox.app.core.** { *; }
