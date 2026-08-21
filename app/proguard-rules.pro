# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep attributes for Crashlytics to map stack trace line numbers
-keepattributes SourceFile,LineNumberTable

# Preserve exception classes for accurate Crashlytics reporting
-keep public class * extends java.lang.Exception
-keepclassmembers class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Keep API request/response DTOs to prevent Gson serialization failures
-keep class com.hevincj.cashflow.data.remote.models.** { *; }

# Keep local database entities to prevent Room mapping issues
-keep class com.hevincj.cashflow.data.local.entity.** { *; }