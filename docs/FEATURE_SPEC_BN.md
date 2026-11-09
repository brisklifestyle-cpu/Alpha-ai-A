# Alpha AI — Devil Breath: সম্পূর্ণ ফিচার ও কমান্ড স্পেসিফিকেশন

এই নথি প্রকল্পের বাধ্যতামূলক product requirements। UI placeholder, network scan বা mock response-কে বাস্তব feature completion বলা যাবে না। প্রতিটি capability-এর implementation, Android permission, device compatibility, test result এবং known limitation আলাদাভাবে নথিভুক্ত করতে হবে।

## Product identity and UI
- App/assistant: Alpha AI — Devil Breath.
- Wake phrases: “Hey Devil” এবং “হেই ডেভিল”.
- Bengali and English commands; Deep Cinematic voice preference.
- Both manual microphone activation and always-ready wake-word mode (permission-aware foreground service, battery and privacy constraints).
- Black/charcoal background, white text, red accent; clean Home Assistant-style separate functional cards and navigation.
- One integrated app: Home, TV Control, Phone Manager, Sound Studio, Voice Assistant, AI Settings; add other modules without clutter.
- Online AI plus useful offline commands/helper.

## Alpha International Sound Engine
All sound-mode selection and profile management belongs inside Alpha AI, not by sending the user to the UE | BOOM app.
Modes/presets:
1. International Hi-Fi
2. Bass
3. Vocal
4. Boom
5. Cinematic
6. Broadcast
7. Sports
8. Night
9. Custom EQ

Each preset must have distinct, documented tuning for Bass, Vocal, Clarity and Dynamic Range. Provide profile selection, save/edit custom profile, and show whether a setting was actually applied or is only a local preset.
Investigate actual control paths for MEGABOOM 3 and Android audio. Do not claim phone-side EQ controls the speaker when audio is being played from another host.

## Known device setup and sound-control requirement
- TV: Hisense 43A6Q, VIDAA OS; device name “Alif Bedroom TV”.
- Separate ONN Google TV Box connected to the TV through HDMI.
- MEGABOOM 3 is normally Bluetooth-connected to the Hisense TV while the user watches TV; phone pairing is occasional.
- User observed that changing volume up/down with the TV remote changes the Bluetooth speaker volume.
- Therefore distinguish TV-hosted playback from phone-hosted playback. When speaker is connected to TV, investigate supported TV/network/remote control paths and report actual capabilities. When paired to phone, expose only controls supported by the phone/speaker protocol. Never assume simultaneous phone control while the speaker is connected to TV.

## TV Control
Separate device profiles and controls for:
- Hisense 43A6Q / VIDAA TV (“Alif Bedroom TV”).
- ONN Google TV Box (external HDMI device).

Implement discovery, secure pairing/authentication where supported, reconnect, and actual remote commands. Provide a device-specific control surface with supported navigation/d-pad, OK/select, Back, Home, volume up/down, mute, power, input/HDMI selection, and app launch only where the device protocol supports each command. TV and ONN box must be represented separately; distinguish controlling the TV from controlling the HDMI box. Voice commands should route to the selected/identified device and ask when ambiguous. Do not call discovery, ping, or port testing “remote control”. Confirm protocol compatibility and test on the actual devices before marking controls complete.

## Devil Breath voice command system
- Bengali and English speech recognition and spoken responses.
- Manual microphone and always-ready wake-word mode.
- Detect wake phrase, remove it from the utterance, parse the remaining command; if wake phrase is detected without a command, prompt the user to speak the command.
- Deep Cinematic voice preference, with voice selection/settings if supported.
- Route commands to AI, TV, phone or sound modules; confirm before destructive or privacy-sensitive actions.
- Offline command set for local functions that do not require network access.

## Phone Manager
- Installed app list/launch and Android-supported app management.
- Contacts/call workflows with required roles and permissions.
- Notification access and management only with explicit user-granted access.
- File manager, storage access and permission center.
- Explain restricted/privileged Android operations honestly; no fake controls.

## WhatsApp and Social Manager
- WhatsApp, Facebook and other social-app launch/deep-link and supported integration.
- Notification-based search/organization/export only where supported and authorized.
- No private database extraction, encryption bypass or unsupported chat access.

## Backup, Phone Clone and Security
- Backup/restore workflow, local and supported cloud destinations including Google Drive.
- Daily/automatic scheduling where Android background limits allow.
- Phone migration/clone planning; clearly distinguish transferable user files/settings from app-private data that Android/OEM restrictions prevent copying.
- Encryption, secure credentials, progress and restore verification.
- Never report success without verifying the resulting backup/restore.

## Caller ID and spam protection
- Supported caller-ID provider integration (including Truecaller where available), with required default-role/permission and consent.
- Spam warning/blocking only where the Android version and provider APIs permit.

## Connect Center
- TV and ONN pairing/status, Bluetooth device status, Google Drive and supported cloud/service connections.
- Permission, connection and error states for every integration.

## Build and QA acceptance criteria
- Feature checklist with statuses: Not started / In progress / Implemented / Tested on device / Blocked by platform or device.
- Every control must perform a real action or clearly state why it cannot.
- Test installation and launch on the target Android phone; test TV/box/speaker on actual hardware.
- Generate APK only from a successful reproducible build; publish build log and artifact.
- User explicitly does not want Termux or an extra helper app installed; prefer an approach that works through Alpha AI and built-in Android capabilities where feasible.
