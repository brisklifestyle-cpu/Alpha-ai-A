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

## অফলাইন লোকাল অডিও কমান্ড

Voice স্ক্রিনে মাইক্রোফোন দিয়ে বা লিখে এই কমান্ডগুলো দেওয়া যায়:
- “ভলিউম বাড়াও” / “volume up”
- “ভলিউম কমাও” / “volume down”
- “মিউট” / “mute”
- “আনমিউট” / “unmute”

এগুলো Android-এর ফোনের Media stream volume নিয়ন্ত্রণ করে; ফোনে Bluetooth অডিও ডিভাইস যুক্ত থাকলে তার ভলিউমও প্রভাবিত হতে পারে। UE BOOM 3 যদি TV-র সঙ্গে যুক্ত থাকে, ফোনের অ্যাপ দিয়ে TV বা স্পিকারের ভলিউম সরাসরি বদলানোর নিশ্চয়তা নেই। এই কমান্ডগুলো always-on wake-word নয়—মাইক্রোফোন চালু করতে ব্যবহারকারীকে Voice স্ক্রিনে চাপতে হবে।

## Voice Assistant — বর্তমান অবস্থা

- Voice স্ক্রিনে মাইক্রোফোন চাপ দিয়ে বাংলা/ইংরেজি নির্দেশ দেওয়া যায়।
- ম্যানুয়াল ভয়েস ইনপুটে “Hey Devil” / “হেই ডেভিল” prefix শনাক্ত করে কমান্ড থেকে আলাদা করা হয়।
- শুধু wake phrase বললে অ্যাপ পরের নির্দেশের জন্য অপেক্ষার বার্তা দেখায়; এরপর আবার মাইক্রোফোন চাপতে হবে।
- এটি এখনো always-listening wake-word service নয়। Android-এর background microphone, foreground-service notification, battery policy এবং নির্ভরযোগ্য on-device wake-word engine যোগ ও বাস্তব ডিভাইসে পরীক্ষা না করে সবসময় শোনার দাবি করা হবে না।

## সীমাবদ্ধতা
এই Phase 1-এ TV remote control, Truecaller, cloud backup, social media account automation, always-on wake word এবং পূর্ণ offline LLM নেই। Offline Helper কেবল সাধারণ স্থানীয় নির্দেশনা দেয়। পরবর্তী Phase-এ প্রতিটি integration আলাদা করে যোগ ও বাস্তব ডিভাইসে পরীক্ষা করতে হবে।


## ইন-অ্যাপ Sound Control

- নতুন **Sound** ট্যাব থেকে ফোনের Media volume স্লাইডার দিয়ে বাড়ানো-কমানো যায়।
- একই স্ক্রিনে Mute, Unmute এবং এক ধাপ করে volume control আছে।
- এটি Android-এর `STREAM_MUSIC` নিয়ন্ত্রণ করে; TV-তে যুক্ত UE BOOM 3 বা অন্য স্পিকারের ভলিউম সরাসরি নিয়ন্ত্রণ করে না।
- Bluetooth স্পিকার ফোনে যুক্ত থাকলে Android-এর অডিও রাউটিং অনুযায়ী ভলিউম প্রভাবিত হতে পারে।


## অতিরিক্ত অফলাইন ফোন কমান্ড

Voice স্ক্রিনে ম্যানুয়াল মাইক্রোফোন ইনপুট বা লিখে নিচের কমান্ডগুলো ব্যবহার করা যায়:
- “সেটিংস খোলো” / “open settings”
- “Wi-Fi settings” / “ওয়াইফাই সেটিংস”
- “Bluetooth settings” / “ব্লুটুথ সেটিংস”
- “Sound settings” / “সাউন্ড সেটিংস”
- “YouTube খোলো” / “open YouTube”
- “ক্যামেরা খোলো” / “open camera”
- “ফোন খোলো” / “open phone”

এসব কমান্ড ফোনের Android settings screen বা ইনস্টল করা অ্যাপ খুলবে; এগুলো ইন্টারনেট ছাড়াই স্থানীয়ভাবে চালানো হয়। অ্যাপটি ইনস্টল না থাকলে বা Android screen উপলভ্য না হলে উপযুক্ত বার্তা দেখাবে। নিরাপত্তার কারণে কল শুরু করা হয় না—শুধু Phone dialer খোলা হয়।


## Social Media Manager

- Social ট্যাবে Facebook, Instagram, YouTube, TikTok, X ও LinkedIn খোলা যায়; ইনস্টল না থাকলে ওয়েবসাইট খোলে।
- প্রতিটি ইনস্টল করা অ্যাপের **App info** থেকে Android-এর Permission, Storage ও Notification settings-এ যাওয়া যায়।
- Post Composer-এ caption লিখে কপি অথবা Android Share Sheet দিয়ে অন্য অ্যাপে পাঠানো যায়।
- এই ধাপে কোনো social account-এর password/OAuth নেওয়া হয় না এবং স্বয়ংক্রিয়ভাবে পোস্ট/মেসেজ প্রকাশ বা inbox পড়া হয় না। এগুলোর জন্য platform-approved API ও OAuth access প্রয়োজন।

## Caller / Truecaller Integration

- নতুন Caller ট্যাব থেকে Truecaller ইনস্টল আছে কি না দেখা, অ্যাপ খোলা এবং App info-তে যাওয়া যায়।
- Caller ID ও Spam Protection চালু করতে Truecaller অ্যাপের নিজস্ব সেটআপ অনুসরণ করতে হবে।
- Alpha AI নিজে caller database, live caller identification বা spam blocking করছে না; এই ধাপে Call Log, Contacts বা Phone permission চাওয়া হয় না।


## Contacts & Dialer Integration

- নতুন **Contacts** ট্যাব থেকে ব্যবহারকারী নিজে Contacts permission দিলে নাম বা নম্বর দিয়ে ফোনের Contacts খোঁজা যায়।
- কোনো নম্বরের পাশে **Dial** চাপলে Android Phone Dialer খোলে; কলটি ব্যবহারকারী নিজে নিশ্চিত করেন।
- Alpha AI কল লগ পড়ে না, কল রেকর্ড করে না এবং নিজে থেকে কল করে না।
- Contacts permission না দিলে contact search ব্যবহার করা যাবে না; অন্য ফিচার স্বাভাবিকভাবে থাকবে।

## Social Draft Save

- Social Media Manager-এর Post Composer-এ **Save Draft** চাপলে সর্বশেষ ক্যাপশন ফোনে স্থানীয়ভাবে সংরক্ষিত হয়।
- অ্যাপ পুনরায় খুললে সর্বশেষ সংরক্ষিত খসড়া ফিরে আসে। এটি cloud sync নয়; একই ডিভাইসের app data-তে থাকে।


## একাধিক Social Draft পরিচালনা

- Post Composer থেকে একই ডিভাইসে একাধিক caption আলাদা খসড়া হিসেবে সেভ করা যায়।
- **Saved Drafts** তালিকায় সংরক্ষিত খসড়াগুলো দেখা যায়; **খুলুন** চাপলে caption Composer-এ ফিরে আসে।
- প্রতিটি খসড়া আলাদাভাবে Delete করা যায়।
- খসড়াগুলো Android app-এর স্থানীয় SharedPreferences-এ JSON আকারে থাকে; cloud backup বা social platform-এ auto-publish হয় না।
