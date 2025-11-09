# Application for Geolocation based notifications

To test the notifications do for example:
```
adb emu geo fix 19.9450 50.0647
adb emu geo fix 19.9380 50.0612
adb emu geo fix 19.9560 50.0680
```
for the initial sample tasks, keep in mind that longitude is first, then latitude
Also it's best to execute the commands in the Maps app so that the position updates immediately

If the app doesnt want to launch because of permissions make sure that access to location in the background is granted in the settings