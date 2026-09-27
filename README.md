# IntentX

Explore, craft, and launch Android intents.

IntentX helps you explore Android apps and create intents:

- Browse and search installed apps and their activities.
- See whether activities are exported or enabled and which permissions they require.
- Customize activities and broadcasts, including actions, categories, flags, and extras.
- Launch with normal, Root, or Shizuku access.
- Save intents and create home screen shortcuts.

Selecting a component opens a prefilled editor that you can adjust before launching or saving it.

## Build

The project requires JDK 25 and Android SDK 37. The minimum supported Android version is API 31.

```bash
./gradlew :app:assembleDebug
```

## License

IntentX is released under the [GNU General Public License v3.0](LICENSE).

## Acknowledgements

IntentX builds on ideas and code from these projects:

- [Anywhere](https://github.com/zhaobozhen/Anywhere-)
- [AndroidAppProcess](https://github.com/iamr0s/AndroidAppProcess)
- [KernelSU](https://github.com/tiann/KernelSU)
- [Shizuku](https://github.com/RikkaApps/Shizuku)
