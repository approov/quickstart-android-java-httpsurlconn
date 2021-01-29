# Approov Quickstart: Android Java HttpsUrlConnection

This quickstart is written specifically for native Android apps that are written in Java and use [`HttpsUrlConnection`](https://developer.android.com/reference/javax/net/ssl/HttpsURLConnection) for making the API calls that you wish to protect with Approov. If this is not your situation then check if there is a more relevant Quickstart guide available.

## WHAT YOU WILL NEED
* Access to a trial or paid Approov account
* The `approov` command line tool [installed](https://approov.io/docs/latest/approov-installation/) with access to your account
* [Android Studio](https://developer.android.com/studio) installed (version 3.5.2 is used in this guide)
* The contents of the folder containing this README

## WHAT YOU WILL LEARN
* How to integrate Approov into a real app in a step by step fashion
* How to register your app to get valid tokens from Approov
* A solid understanding of how to integrate Approov into your own app that uses Java and [HttpsUrlConnection](https://developer.android.com/reference/javax/net/ssl/HttpsURLConnection) to make API calls
* Some pointers to other Approov features

## RUNNING THE SHAPES APP WITHOUT APPROOV

Open the project in the `shapes-app` folder using `File->Open` in Android Studio.

Ensure that the currently active build variant is `debug` and then build an APK as follows:

![Build APK](readme-images/build-apk.png)

The following dialog will popup when the build is complete:

![Build APK Result](readme-images/build-apk-result.png)

Click on `locate` to get the `app-debug.apk` location and a file explorer window will open. Now you need to open the terminal of your operating system on this location and execute:
```
$ adb install app-debug.apk
```
> **NOTE**:
>   * The [`adb`](https://developer.android.com/studio/command-line/adb) tool needs to be installed in in your `$PATH`.
>   * The mobile device needs to be in Developer mode, and USB debugging needs to be [enabled](https://developer.android.com/studio/command-line/adb#Enabling) and set to always trust in your device.

From the mobile device launch the app and you will see two buttons:

<p>
    <img src="readme-images/app-startup.png" width="256" title="Shapes App Startup">
</p>

Click on the `Say Hello` button and you should see this:

<p>
    <img src="readme-images/hello-okay.png" width="256" title="Hello Okay">
</p>

This checks the connectivity by connecting to the endpoint `https://shapes.approov.io/v1/hello`. Now press the `Get Shape` button and you will see this:

<p>
    <img src="readme-images/shapes-bad.png" width="256" title="Shapes Bad">
</p>

This contacts `https://shapes.approov.io/v2/shapes` to get the name of a random shape. It gets the status code 400 (`Bad Request`) because this endpoint is protected with an Approov token. Next, you will add Approov into the app so that it can generate valid Approov tokens and get shapes.

## ADD THE APPROOV SDK

Get the latest Approov SDK (if you are using Windows then substitute `approov` with `approov.exe` in all cases in this quickstart)
```
$ approov sdk -getLibrary approov-sdk.aar
```
Right-click on the `app` folder in the `Android` tab and select `New->Module`:

![Add new Module](readme-images/new-module.png)

Select the`Import .JAR/.AAR` option and click `Next`:

![Import AAR](readme-images/import-aar.png)

Select the path of the `approov-sdk.aar` file you saved earlier:

![Select AAR Path](readme-images/module-path.png)

You may cancel any request to add the module to the `git` repository. Right-click on the `app` module and select `Open Module Settings`:

![Module Settings](readme-images/module-settings.png)

Select `Dependencies`, then the `app` and then add a `Module Dependency` by clicking the + sign:

![Add Dependency Select](readme-images/add-dependency.png)

Select `approov-sdk` and click `OK`:

![Add Dependency SDK](readme-images/add-dependency-sdk.png)

The Approov SDK is now included as a dependency in your project. Since the Approov SDK itself uses [`OkHttp`](https://square.github.io/okhttp/) to make its requests, it needs to be added as a dependency in the project. Open the `build.gradle` for the `app`. You need to add dependencies for Java8 to support the versin of `OkHttp` being used:
```
android {
        compileOptions {
            sourceCompatibility JavaVersion.VERSION_1_8
            targetCompatibility JavaVersion.VERSION_1_8
        }
    }
```
and you also need to add a dependency `implementation 'com.squareup.okhttp3:okhttp:3.14.2'`. The file should then look like the following:

![Add Gradle Dependencies](readme-images/app-gradle.png)

## ADD THE APPROOV FRAMEWORK CODE

The folder `framework` has a `src/main/java/io/approov/framework/httpsurlconn/ApproovService.java` file. This provides a wrapper around the Approov SDK itself to make it easier to use with `HttpsUrlConnection`. Show the terminal in Android Studio by clicking on the `Terminal` tab, typically located at the bottom of the screen:

![Terminal](readme-images/android-studio-terminal.png)

This should have a current folder in the `shapes-app`. You need to copy the folder `framework`, retaining its path, into the `app` subdirectory of the project.

#### unix:
```
$ cp -r ../framework/src/main/java/io/approov/framework app/src/main/java/io/approov
```

#### windows:
```
$ powershell -Command "Copy-Item -Path '..\framework\src\main\java\io\approov\framework' -Destination 'app\src\main\java\io\approov' -Recurse"
```
> **NOTE:** When running directly on a PowerShell terminal you can drop the prefix `powershell -Command ""`.

Click on `java` in the `project` pane and the code should be included as follows:

![Approov Service Added](readme-images/approov-service-added.png)

## ENSURE THE SHAPES API IS ADDED

In order for Approov tokens to be generated for `https://shapes.approov.io/v2/shapes` it is necessary to inform Approov about it. If you are using a demo account this is unnecessary as it is already setup. For a trial account do:
```
$ approov api -add shapes.approov.io
```
Tokens for this domain will be automatically signed with the specific secret for this domain, rather than the normal one for your account.

## SETUP YOUR APPROOV CONFIGURATION

The Approov SDK needs a configuration string to identify the account associated with the app. Obtain it using:
```
$ approov sdk -getConfig initial-config.txt
```
Copy and paste the `initial-config.txt` file content into a new created `approov_config` string resource entry as shown (the actual entry will be much longer than shown).

![Initial Config String](readme-images/initial-config.png)

The app reads this string to initialize the Approov SDK.

## MODIFY THE APP TO USE APPROOV

Uncomment the three lines of Approov initialization code in `ShapesApp`:

![Approov Initialization](readme-images/approov-init-code.png)

This initializes Approov when the app is first created. It uses the configuration string we set earlier. A `public static` member allows other parts of the app to access the singleton Approov instance. All calls to `ApproovService` and the SDK itself are thread safe.

Next we need to use Approov when we make request for the shapes. Only a single line of code needs to be changed:

![Approov Fetch](readme-images/approov-fetch.png)

We pass the `HttpsUrlConnection` to the `ApproovService.addApproov` method and this automatically fetches an Approov token and adds it as a header to the request. It also pins the connection to the endpoint to ensure that no Man-in-the-Middle can eavesdrop on any communication being made.

Note that this method may throw an `IOException` if it is unable to fetch an Approov token, typically due to no or poor Internet connectivity. Therefore the call should be in the same `try-catch` block as [`connect`](https://developer.android.com/reference/java/net/URLConnection.html#connect()) or reads of the body for a `GET`. If `addApproov` throws an exception then the user should be able to initiate a retry. During development an exception may be thrown due to a misconfiguration, see [Token Fetch Errors](https://approov.io/docs/latest/approov-usage-documentation/#token-fetch-errors).

## REGISTER YOUR APP WITH APPROOV

In order for Approov to recognize the app as being valid it needs to be registered with the service. Ensure that the currently active build variant is `debug` and then build an APK as follows:

![Build APK](readme-images/build-apk.png)

The following dialog will popup when the build is complete:

![Build APK Result](readme-images/build-apk-result.png)

Click on `locate` to get the `app-debug.apk` location. Register the app with Approov:
```
$ approov registration -add app-debug.apk
```

## RUNNING THE SHAPES APP WITH APPROOV

Install the `app-debug.apk` that you just registered on the device. You will need to remove the old app from the device first.
```
$ adb install -r app-debug.apk
```
Launch the app and press the `Get Shape` button. You should now see this (or another shape):

<p>
    <img src="readme-images/shapes-good.png" width="256" title="Shapes Good">
</p>

This means that the app is getting a validly signed Approov token to present to the shapes endpoint.

## WHAT IF I DON'T GET SHAPES

If you still don't get a valid shape then there are some things you can try. Remember this may be because the device you are using has some characteristics that cause rejection for the currently set [Security Policy](https://approov.io/docs/latest/approov-usage-documentation/#security-policies) on your account:

* Ensure that the version of the app you are running is exactly the one you registered with Approov.
* If you running the app from a debugger then valid tokens are not issued.
* Look at the [`logcat`](https://developer.android.com/studio/command-line/logcat) output from the device. Information about any Approov token fetched or an error is output at the `INFO` level, e.g. `2020-02-10 13:55:55.774 10442-10705/io.approov.shapes I/ApproovFramework: Approov Token for shapes.approov.io: {"did":"+uPpGUPeq8bOaPuh+apuGg==","exp":1581342999,"ip":"1.2.3.4","sip":"R-H_vE"}`. You can easily [check](https://approov.io/docs/latest/approov-usage-documentation/#loggable-tokens) the validity.

If you have a trial (as opposed to demo) account you have some additional options:
* Consider using an [Annotation Policy](https://approov.io/docs/latest/approov-usage-documentation/#annotation-policies) during development to directly see why the device is not being issued with a valid token.
* Use `approov metrics` to see [Live Metrics](https://approov.io/docs/latest/approov-usage-documentation/#live-metrics) of the cause of failure.
* You can use a debugger and get valid Approov tokens on a specific device by [whitelisting](https://approov.io/docs/latest/approov-usage-documentation/#adding-a-device-security-policy).

## CHANGING YOUR OWN APP TO USE APPROOV

### Gradle Dependencies
Note that if proguard is being used to protect an application that includes an Approov SDK, the SafetyNet dependency has to be included in your gradle dependency section:
```
implementation 'com.google.android.gms:play-services-safetynet:16.0.0'
```

Your app will also need Java8 support for compatibility with `OkHttp`. Ensure the following is in the app's `build.gradle`:
```
android {
        compileOptions {
            sourceCompatibility JavaVersion.VERSION_1_8
            targetCompatibility JavaVersion.VERSION_1_8
        }
    }
```

### App Permissions
Your app will obviously need `INTERNET` permission, but be aware the Approov SDK also needs `ACCESS_NETWORK_STATE` permission, which can be added to the manifest as follows:
```
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### API Domains
Remember you need to [add](https://approov.io/docs/latest/approov-usage-documentation/#adding-api-domains) all of the API domains that you wish to send Approov tokens for. If you call `addApproov` on a connection to a domain that has not been added then no Approov token header is added.

### Preferences
An Approov app automatically downloads any new configurations of APIs and their pins that are available. These are stored in the [`SharedPreferences`](https://developer.android.com/reference/android/content/SharedPreferences) for the app in a preference file `approov-prefs` and key `approov-config`. You can store the preferences differently by modifying or overriding the methods `putApproovDynamicConfig` and `getApproovDynamicConfig` in `ApproovService.java`.

### Changing Your API Backend
The Shapes example app uses the API endpoint `https://shapes.approov.io/v2/shapes` hosted on Approov's servers. If you want to integrate Approov into your own app you will need to [integrate](https://approov.io/docs/latest/approov-usage-documentation/#backend-integration) an Approov token check. Since the Approov token is simply a standard [JWT](https://en.wikipedia.org/wiki/JSON_Web_Token) this is usually straightforward. [Backend integration](https://approov.io/docs/latest/approov-integration-examples/backend-api/) examples provide a detailed walk-through for particular languages. Note that the default header name of `Approov-Token` can be changed by editing `APPROOV_HEADER` in `ApproovService`. Moreover, a prefix to the header can be added in `APPROOV_TOKEN_PREFIX`. This is primarily for integrations where the JWT might need to be prefixed with `Bearer`, like the `Authorization` header.

### Token Prefetching
If you wish to reduce the latency associated with fetching the first Approov token, then a call to `ApproovService.prefetchApproovToken()` can be made immediately after initialization of the Approov SDK. This initiates the process of fetching an Approov token as a background task, so that a cached token is available immediately when subsequently needed, or at least the fetch time is reduced. Note that if this feature is being used with [Token Binding](https://approov.io/docs/latest/approov-usage-documentation/#token-binding) then the binding must be set prior to the prefetch, as changes to the binding invalidate any cached Approov token.

## NEXT STEPS

This quick start guide has shown you how to integrate Approov with your existing app. Now you might want to explore some other Approov features:

* Managing your app [registrations](https://approov.io/docs/latest/approov-usage-documentation/#managing-registrations)
* Manage the [pins](https://approov.io/docs/latest/approov-usage-documentation/#public-key-pinning-configuration) on the API domains to ensure that no Man-in-the-Middle attacks on your app's communication are possible.
* Update your [Security Policy](https://approov.io/docs/latest/approov-usage-documentation/#security-policies) that determines the conditions under which an app will be given a valid Approov token.
* Learn how to [Manage Devices](https://approov.io/docs/latest/approov-usage-documentation/#managing-devices) that allows you to change the policies on specific devices.
* Understand how to provide access for other [Users](https://approov.io/docs/latest/approov-usage-documentation/#user-management) of your Approov account.
* Use the [Metrics Graphs](https://approov.io/docs/latest/approov-usage-documentation/#metrics-graphs) to see live and accumulated metrics of devices using your account and any reasons for devices being rejected and not being provided with valid Approov tokens. You can also see your billing usage which is based on the total number of unique devices using your account each month.
* Use [Service Monitoring](https://approov.io/docs/latest/approov-usage-documentation/#service-monitoring) emails to receive monthly (or, optionally, daily) summaries of your Approov usage.
* Consider using [Token Binding](https://approov.io/docs/latest/approov-usage-documentation/#token-binding). The method `<AppClass>.approovService.setBindingHeader` takes the name of the header holding the value to be bound. This only needs to be called once but the header needs to be present on all API requests using Approov.
* Investigate other advanced features, such as [Offline Security Mode](https://approov.io/docs/latest/approov-usage-documentation/#offline-security-mode), [SafetyNet Integration](https://approov.io/docs/latest/approov-usage-documentation/#google-safetynet-integration) and [Android Automated Launch Detection](https://approov.io/docs/latest/approov-usage-documentation/#android-automated-launch-detection).



