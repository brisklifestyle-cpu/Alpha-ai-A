# Alpha AI — Devil Breath

**Android personal AI assistant** — project source and build instructions.

## Current Android app

The Android Studio project is in `android_studio/`.

- Bengali/English Gemini chat (user supplies their own API key in app Settings).
- Android speech recognition for voice input and Android Text-to-Speech for spoken replies.
- Encrypted local storage for the Gemini API key.
- Offline Helper for a small set of local responses.
- Home, AI Chat, Voice, Social and TV screens.
- TV screen currently discovers Android TV Remote services on the local network and tests connection ports. It does **not** yet pair with a TV or send remote-control commands.

## Build an APK

1. Open the repository's **Actions** tab.
2. Select **Build Alpha AI APK**.
3. Choose **Run workflow** (or push a commit to `main`).
4. When the workflow finishes, open its run and download the `AlphaAI-Debug-APK` artifact.

The workflow uses Java 17, Android SDK 35 and Gradle 8.9. The Android project is under `android_studio/`.

## API key safety

Do not commit Gemini API keys, signing keys, passwords, or personal tokens. Enter a personal test key in the app's Settings screen. Client-side APK secrets cannot be considered fully protected; a backend proxy is recommended before distributing the app to multiple users.

## Planned development

- **Voice assistant:** manual microphone mode first; investigate a reliable, permission-aware “Hey Devil” wake-word service with battery-conscious foreground operation.
- **TV control:** implement and test secure pairing and real remote commands for supported Android/Google TV devices.
- **MEGABOOM 3:** investigate supported Bluetooth/OS control capabilities and keep sound profiles managed inside Alpha AI; do not assume the speaker can be controlled when it is connected to a different host such as a TV.
- **Reliability:** build and test each feature on real Android devices before marking it complete.

## Project notes

Detailed Bengali Android setup notes: [android_studio/README_BN.md](android_studio/README_BN.md).
