# SourCherry

SourCherry is an Android app to open CherryTree’s (note taking program [1](https://github.com/giuspen/cherrytree),[2](https://www.giuspen.com/)) databases. Right now app can only read databases and do not have any writing capabilities.

![gif with SourCherry UI](https://github.com/FFDA/ffda.github.storage/raw/main/images/SourCherry.gif)

App is targeted for sdk 31 (Android 12) with min version of 24 (Android 7).

This project is for Android/Java/Git learning purposes and is not official CherryTree android app or are associated with CherryTree project or it's creator.

## Installation / Download

[![](https://raw.githubusercontent.com/FFDA/ffda.github.storage/main/images/google-play-badge.png)](https://play.google.com/store/apps/details?id=lt.ffda.sourcherry)

Anyone can compile this app by cloning this repository or download an apk compiled by me from [releases](https://github.com/FFDA/SourCherry/releases/).

## Limitations

* Right now it can only open databases in read only mode.
* There is a possibility that not all text will be displayed.
* Some text most definitely will be missing some formatting and it will look differently compared to how it looks in CherryTree.
* Not all latex boxes might be displayed. From example latex code in CherryTree only code between ***\begin{align\*}*** and ***\end{align\*}*** tags is used (and only it should be modified). Rest of the code should not be edited in any way.
* In some instances image will not be displayed from SQL databases (if an image is too big to fit in SQL cursor).

## Password protected databases

It is also possible to open password protected databases, however there will be one time performance hit, because of following reasons:

* Android / Java do not have good support for password protected 7-zip file extraction. There are few options, but they are created by third parties for Java and not specifically for Android and are lacking some features, namely to open InputStream.
* Starting Android 10 Google implemented stricter file access system ([SAF](https://developer.android.com/guide/topics/providers/document-provider)), that made impossible to read/open files as objects and not InputStreams that are not in app-specific storage.

So to open password protected database app needs to copy file to app-specific storage, extract it, and only then open it. After extraction database will be kept inside app-specific storage and won’t be reachable by other apps. From that point forward that database will be opened without a password.

## SQL databases

For the same reason (SAF) that prevents file extraction without saving files inside app-specific storage SQL based databases have to be copied to it before opening. There will be a performance hit. Should be noticeable when opening a SQL database for the first time.

## Software used

* XZ for Java - https://tukaani.org/xz/java.html
* Apache Commons Compress - https://commons.apache.org/proper/commons-compress/
* JLatexMath Android - https://github.com/noties/jlatexmath-android

## Privacy policy

Generated privacy policy using https://privacypolicytemplate.net, because it is required for apps published on Google Play Store.

**Personal note:** I DO NOT collect any data from the users of this app (even crash reports). It comes free of charge and as is.

You can read full privacy policy [here](https://htmlpreview.github.io/?https://github.com/FFDA/ffda.github.storage/blob/main/misc/SourCherry-PrivacyPolicy.html).