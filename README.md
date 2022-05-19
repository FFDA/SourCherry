# SourCherry

SourCherry is an Android app to open CherryTree’s (note taking program [1](https://github.com/giuspen/cherrytree),[2](https://www.giuspen.com/)) databases. Right now app can only read and do not have any writing capabilities.

![gif with SourCherry UI](https://github.com/FFDA/ffda.github.storage/raw/main/images/SourCherry.gif)

App is targeted for sdk 31 (Android 12) with min version of 23 (Android 6).

This project is for for Android/Java/Git learning purposes.

## Instalation / Download

I plan to add SourCherry to Play Store when it has more features and can open and read all types of CherryTree databases.

Right now anyone can download this repository and build the apk file using Android Studio or download and install apk built by me: [SourCherry-alpha-2022-05-19](https://github.com/FFDA/ffda.github.storage/raw/main/SourCherry-releases/SourCherry-alpha.apk)

## Limitations

* Right now it can only open databases based on XML files in read only mode.
* There is a possibility that not all text will be displayed. 
* Some text most definitely will be missing some formatting and it will look differently compared to how it looks in CherryTree.
* Password protected databases can be opened only on phones with Android 8 or later.

## Password protected databases

It is also possible to open password protected databases, however there will be one time performance hit, because of following reasons:

* Android / Java do not have good support for password protected 7-zip file extraction. There are few options, but they are created by third parties for Java and not specifically for Android and are lacking some features, namely to open InputStream.
* Starting Android 10 Google implemented stricter file access system ([SAF](https://developer.android.com/guide/topics/providers/document-provider)), that made impossible to read/open files as objects and not InputStreams that are not in app-specific storage.

So to open password protected database app needs to copy file to app-specific storage, extract it, and only then open it. After extraction database will be kept inside app-specific storage and won’t be reachable by other apps. From that point forward that database will be opened without a password.