# Alpha AI — Devil Breath (Phase 1)

এই সংস্করণে বাস্তবায়িত:
- বাংলা/ইংরেজি Gemini AI চ্যাট (নিজের Gemini API key প্রয়োজন; ফ্রি tier quota সাপেক্ষ)
- Android Speech Recognizer দিয়ে voice input এবং Text-to-Speech দিয়ে উত্তর শোনা
- API key Android Keystore-ভিত্তিক EncryptedSharedPreferences-এ সংরক্ষণ
- API key না থাকলে Offline Helper mode: সীমিত স্থানীয় কমান্ড/সহায়তা
- Home, AI Chat, Voice, Settings স্ক্রিন

## Build
GitHub Actions-এর মাধ্যমে Debug APK তৈরি হয়। Android Studio বাধ্যতামূলক নয়।

## Gemini সেটআপ
Google AI Studio থেকে API key তৈরি করে অ্যাপের Settings-এ নিজে প্রবেশ করান। Key কখনো GitHub-এ commit করবেন না। Gemini free-tier-এর মডেল/কোটা/দেশভিত্তিক প্রাপ্যতা Google পরিবর্তন করতে পারে। Key অ্যাপের ভিতরে রাখলেও ব্যক্তিগত APK থেকে দক্ষ আক্রমণকারী তা বের করতে পারে; বহু-ব্যবহারকারীর production release-এর জন্য backend proxy প্রয়োজন।

## Voice Assistant — বর্তমান অবস্থা

- Voice স্ক্রিনে মাইক্রোফোন চাপ দিয়ে বাংলা/ইংরেজি নির্দেশ দেওয়া যায়।
- ম্যানুয়াল ভয়েস ইনপুটে “Hey Devil” / “হেই ডেভিল” prefix শনাক্ত করে কমান্ড থেকে আলাদা করা হয়।
- শুধু wake phrase বললে অ্যাপ পরের নির্দেশের জন্য অপেক্ষার বার্তা দেখায়; এরপর আবার মাইক্রোফোন চাপতে হবে।
- এটি এখনো always-listening wake-word service নয়। Android-এর background microphone, foreground-service notification, battery policy এবং নির্ভরযোগ্য on-device wake-word engine যোগ ও বাস্তব ডিভাইসে পরীক্ষা না করে সবসময় শোনার দাবি করা হবে না।

## সীমাবদ্ধতা
এই Phase 1-এ TV remote control, Truecaller, cloud backup, social media account automation, always-on wake word এবং পূর্ণ offline LLM নেই। Offline Helper কেবল সাধারণ স্থানীয় নির্দেশনা দেয়। পরবর্তী Phase-এ প্রতিটি integration আলাদা করে যোগ ও বাস্তব ডিভাইসে পরীক্ষা করতে হবে।
