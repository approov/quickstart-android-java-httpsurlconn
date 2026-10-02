# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in <Android SDK>/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keep class com.criticalblue.approovsdk.** {*;}

# The Approov SDK refers to optional Google Play services and Play Integrity classes that
# are not present unless the app includes those libraries. Android Gradle plugin 8 treats
# missing classes as an error in R8.
-dontwarn com.google.android.gms.tasks.**
-dontwarn com.google.android.play.core.integrity.**
