# BatteryMonitor

> This project must be used in conjunction with [the repository](https://github.com/DEPhantom/BatteryMonitorServer).

An Android app that lets you monitor your phone's battery from a PC command line.

## Dependencies 
python 3
android 7.0以上

## Download
Download the latest pre-release from the [releases page](https://github.com/DEPhantom/BatteryMonitor).
Or
Build the project by yourself.

```sh
git clone https://github.com/DEPhantom/BatteryMonitor.git
```

## Get started

To use Battery Monitor, follow these steps:

1. Connect the device to your PC via ADB
2. Disable Doze Mode using the following command:
```sh
adb shell dumpsys deviceidle disable
```
3. You should see the following output:
```sh
Deep idle mode disabled
Light idle mode disabled
```
:::info
P.S. This needs to be done every time the device restarts.
:::

4. After installation, make sure to:

* Enable notifications

* Turn on notification sounds

* Allow notifications on the lock screen
~~5. 電源白名單~~
