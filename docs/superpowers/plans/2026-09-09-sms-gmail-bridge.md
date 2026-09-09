# SMS–Gmail Two-Way Bridge Implementation Plan

> **For agentic workers:** Implement task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. The user already approved proceeding with code; execute inline in this session.

**Goal:** Replace the Gemini SMS gateway with a two-way SMS–Gmail bridge that runs as a foreground service on Android 5.1.1 (API 22).

**Architecture:** Clean layers: UI (Home/Setup/History + ViewModel/LiveData) → domain (parser, use cases) → data (encrypted config, SQLite logs, JavaMail, SmsManager) → runtime (foreground service, SMS receiver, boot receiver). Conscrypt provides TLS 1.2.

**Tech Stack:** Kotlin 1.9, AGP 8.1.4, AppCompat 1.3.1, Material 1.3.0, Lifecycle 2.5.1, JavaMail android-mail 1.6.7, Conscrypt 2.5.2, Coroutines 1.6.4, JUnit 4.

## Global Constraints

- minSdkVersion = 22, targetSdkVersion = 33, compileSdk = 34, applicationId = `io.turkmensms.aigateway`
- No Material3, WorkManager-as-primary, Gmail REST, Play Services Gmail, OkHttp, security-crypto, multidex
- JavaMail only for mail; Conscrypt installed first in Application
- Passwords never logged, toasted, or shown in history
- Credential wrap: RSA AndroidKeyStore + AES-256 in MODE_PRIVATE prefs (API 22 safe)
- Do not use `rm`; overwrite unused Kotlin files with compiling no-op stubs
- No comments in source unless required; no emoji

---

### Task 1: Gradle, ProGuard, strings, parser tests + domain

**Files:**
- Modify: `app/build.gradle`
- Modify: `app/proguard-rules.pro`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/java/io/turkmensms/aigateway/domain/PhoneNormalizer.kt`
- Create: `app/src/main/java/io/turkmensms/aigateway/domain/MessageParser.kt`
- Create: `app/src/test/java/io/turkmensms/aigateway/domain/MessageParserTest.kt`

**Produces:** `PhoneNormalizer.normalize`, `MessageParser.parse`, `InboundIntent` sealed class

- [ ] **Step 1:** Set minSdk 22, targetSdk 33, add lifecycle, recyclerview, JavaMail, conscrypt; drop OkHttp/Play Services/Gson
- [ ] **Step 2:** Keep JavaMail classes in ProGuard
- [ ] **Step 3:** Write failing parser unit tests, then implement PhoneNormalizer + MessageParser until `./gradlew testDebugUnitTest` passes

Parser rules: `QUESTION <phone>`; `Re:`/`Fwd:` stripped then `SMS from <phone>`; strip quoted reply at `^On .*wrote:` or line starting with `>`; else Skip.

---

### Task 2: Config, crypto store, log DB

**Files:**
- Create: `app/src/main/java/io/turkmensms/aigateway/data/BridgeConfig.kt`
- Create: `app/src/main/java/io/turkmensms/aigateway/data/CredentialCipher.kt`
- Create: `app/src/main/java/io/turkmensms/aigateway/data/BridgeConfigStore.kt`
- Create: `app/src/main/java/io/turkmensms/aigateway/data/BridgeLog.kt`
- Create: `app/src/main/java/io/turkmensms/aigateway/data/LogRepository.kt`

**Produces:** `BridgeConfigStore.load/save`, `LogRepository.insert/latest/trim(500)`

- [ ] **Step 1:** `BridgeConfig` data class with Gmail SMTP 587 STARTTLS / IMAP 993 SSL defaults
- [ ] **Step 2:** RSA-wrap AES key in AndroidKeyStore; encrypt password fields only
- [ ] **Step 3:** SQLite `bridge_log` with directions SMS_IN, EMAIL_IN, SMS_OUT, EMAIL_OUT, SKIP, ERROR

---

### Task 3: MailClient, SmsSender, use cases

**Files:**
- Create: `app/src/main/java/io/turkmensms/aigateway/data/MailClient.kt`
- Create: `app/src/main/java/io/turkmensms/aigateway/data/SmsSender.kt`
- Create: `app/src/main/java/io/turkmensms/aigateway/domain/SmsToEmailUseCase.kt`
- Create: `app/src/main/java/io/turkmensms/aigateway/domain/EmailToSmsUseCase.kt`

**Produces:** `MailClient.sendMail/fetchUnseen/markRead/testSmtp/testImap`, `SmsSender.send`, use cases

- [ ] **Step 1:** JavaMail SMTP STARTTLS and IMAPS with ssl.trust=*
- [ ] **Step 2:** Fetch UNSEEN, skip non-allowlist (mark read + SKIP log)
- [ ] **Step 3:** QUESTION → SMS + ANSWER reply; SMS-from reply → SMS only; persist last IMAP UID

---

### Task 4: Application, service, receivers, manifest

**Files:**
- Create: `app/src/main/java/io/turkmensms/aigateway/GatewayApp.kt`
- Create: `app/src/main/java/io/turkmensms/aigateway/service/BridgeForegroundService.kt`
- Modify: `app/src/main/java/io/turkmensms/aigateway/receiver/SmsReceiver.kt`
- Create: `app/src/main/java/io/turkmensms/aigateway/receiver/BootReceiver.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Produces:** Sticky foreground poll loop; SMS_RECEIVED → ACTION_FORWARD_SMS; boot restart if enabled

- [ ] **Step 1:** GatewayApp inserts Conscrypt at provider index 1
- [ ] **Step 2:** NotificationCompat foreground; WakeLock only during poll/send; backoff 5/15/45s
- [ ] **Step 3:** Manifest permissions RECEIVE_SMS, SEND_SMS, READ_SMS, INTERNET, ACCESS_NETWORK_STATE, FOREGROUND_SERVICE, WAKE_LOCK, RECEIVE_BOOT_COMPLETED

---

### Task 5: UI (Home, Setup, History) + ViewModels

**Files:**
- Create: layouts `activity_home.xml`, `activity_setup.xml`, `activity_history.xml`, `item_log.xml`
- Create: Home/Setup/History Activity + ViewModel + LogAdapter
- Modify: `app/src/main/java/io/turkmensms/aigateway/ui/MainActivity.kt` into a compile stub
- Modify: leftover Gemini/sampleapp files into compile stubs

**Produces:** Setup separate from logs; LiveData config; Start/Stop; Test SMTP/IMAP

---

### Task 6: Verify

- [ ] `./gradlew testDebugUnitTest assembleDebug`
- [ ] Confirm no OkHttp/Gemini in app module deps
- [ ] Confirm parser tests cover QUESTION, Re: SMS from, quoted body, skip, normalize
