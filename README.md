# SourCherry

SourCherry is an Android app to open CherryTree’s (note taking program [1](https://github.com/giuspen/cherrytree),[2](https://www.giuspen.com/)) databases. Right now app can read databases and do basic editing tasks.

![gif with SourCherry UI](https://github.com/FFDA/ffda.github.storage/raw/main/images/SourCherry.gif)

App is targeted for sdk 35 (Android 15) with min version of 24 (Android 7).

This project is for Android/Java/Git learning purposes and is not official CherryTree android app or are associated with CherryTree project or it's creator.

## Installation / Download

[![](https://raw.githubusercontent.com/FFDA/ffda.github.storage/main/images/google-play-badge.png)](https://play.google.com/store/apps/details?id=lt.ffda.sourcherry)
[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Get it on IzzyOnDroid" height="75">](https://apt.izzysoft.de/fdroid/index/apk/lt.ffda.sourcherry)

Google introduced [new requirements](https://support.google.com/googleplay/android-developer/answer/10788890?hl=en) for publishing applications on the Play Store. New requirements include displaying name, address and phone number on the store. I do not plan to provide these, so most likley at some point in the future SourCherry will be removed from the Play store by Google.

Anyone can compile this app by cloning this repository or download an apk compiled by me from [releases](https://github.com/FFDA/SourCherry/releases/).

## Limitations

* Use of SQL type database is ***strongly*** recommended. Users using large XML type database might experience OOM crashes that can't be mitigated.
* Right now it can read databases and do basic editing of it. To not lose any formatting when saving Rich-Text nodes two codeboxes should have at least a space between them, otherwise they will be saved, and display onwards, as one codebox.
* There is a possibility that not all text will be displayed (Make a bug report)
* Some text most definitely will be missing some formatting and it will look differently compared to how it looks in CherryTree. Notably the is no way of to display “Fill” text paragraph formatting in SourCherry.
* Not all latex boxes might be displayed. From example latex code in CherryTree only code between ***\begin{align\*}*** and ***\end{align\*}*** tags is used (and only it should be modified). Rest of the code should not be edited in any way.
* In some instances image will not be displayed and opening/saving attached might cause a crash. It depends on Android version and phone. Default size for Android SQL window size is 2mb. For Android9+ (>=API 28) I increased it to 15mb and it can be adjusted in the settings up to 500mb. XML databases do not have any way to adjust files that can be opened/saved, but it has bigger limit. In my experience it is 11mb.
* Unsaved data in Node Editor will be lost during configuration changes (screen orientation change, theme change, etc).

## Password protected databases

It is also possible to open password protected databases, however there will be one time performance hit, because of following reasons:

* Android / Java do not have good support for password protected 7-zip file extraction. There are few options, but they are created by third parties for Java and not specifically for Android and are lacking some features, namely to open InputStream.
* Starting Android 10 Google implemented stricter file access system ([SAF](https://developer.android.com/guide/topics/providers/document-provider)), that made impossible to read/open files as objects and not InputStreams that are not in app-specific storage.

So to open password protected database app needs to copy file to app-specific storage, extract it, and only then open it. After extraction database will be kept inside app-specific storage and won’t be reachable by other apps. From that point forward that database will be opened without a password.

## SQL databases

For the same reason (SAF) that prevents file extraction without saving files inside app-specific storage SQL based databases have to be copied to it before opening. There will be a performance hit. Should be noticeable when opening a SQL database for the first time.

## Software used

* 7-Zip-JBinding-4Android - https://github.com/omicronapps/7-Zip-JBinding-4Android
* JLatexMath Android - https://github.com/noties/jlatexmath-android
* ColorPickerDialog - https://github.com/fennifith/ColorPickerDialog

## Fonts used
* Caladea (Apache 2.0)
* Comfortaa (OFL)
* Merriweather (OFL)
* DejaVu Sans Mono (DejaVu Fonts License v1.00)

## Privacy policy

Generated privacy policy using https://privacypolicytemplate.net, because it is required for apps published on Google Play Store.

**Personal note:** I DO NOT collect any data from the users of this app (even crash reports). It comes free of charge and as is.

You can read full privacy policy [here](https://htmlpreview.github.io/?https://github.com/FFDA/ffda.github.storage/blob/main/misc/SourCherry-PrivacyPolicy.html).