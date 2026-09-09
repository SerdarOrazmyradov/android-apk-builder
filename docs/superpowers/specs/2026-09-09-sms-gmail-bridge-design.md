# SMS–Gmail Two-Way Bridge Design

Date: 2026-09-09
Status: Approved for implementation planning
Target: Android 5.1.1 (API 22), compile/target SDK 33

## 1. Goal

Replace the incomplete Gemini AI SMS gateway in this repository with a fully functional two-way background bridge:

1. Incoming SMS is forwarded to Gmail.
2. Incoming email from allowed senders is processed, marked read, and can trigger an SMS.

The app must keep running on Android 5.1.1 without Google Play Gmail APIs, WorkManager as the primary scheduler, or Material3.

## 2. Decisions (locked)

| Topic | Choice |
|---|---|
| Gmail access | SMTP send + IMAP poll via JavaMail, app password |
| Inbound command | Subject `QUESTION <phone>`, body = SMS text |
| Command ack | Reply email with subject `ANSWER` (sent/failed) |
| SMS-to-email format | Subject `SMS from <number>`, body = SMS text, Reply-To = configured Gmail account |
| Email reply-to-SMS | Subject `Re: SMS from <phone>` (or `SMS from <phone>`) sends SMS, no ANSWER reply |
| Background | Foreground service + SMS BroadcastReceiver |
| minSdk | 22 |
| targetSdk | 33 |
| Package | Keep `io.turkmensms.aigateway` |

## 3. Architecture

Clean layers under `io.turkmensms.aigateway`:

```
ui/          HomeActivity, SetupActivity, HistoryActivity, ViewModels, adapters
domain/      SmsToEmailUseCase, EmailToSmsUseCase, MessageParser, PhoneNormalizer
data/        BridgeConfigStore, LogRepository, MailClient (SMTP/IMAP), SmsSender
service/     BridgeForegroundService, SmsReceiver, BootReceiver
app/         GatewayApp (Conscrypt TLS 1.2)
```

Rules:

- UI never talks to JavaMail or SmsManager directly.
- Receivers only parse and hand off to the service / use cases.
- Passwords never appear in logs, toasts, or history rows.
- Existing `com.sampleapp/*` stubs and unused Gemini client are removed so one package owns the app.

### Component contracts

**BridgeConfigStore**
- Load/save SMTP, IMAP, forward-to address, allowed senders, poll interval seconds, IMAP folder, bridge enabled flag.
- Do not use `androidx.security:security-crypto` (minSdk 23) or AES AndroidKeyStore (API 23+).
- Credential storage that works on API 22: generate an AES-256 key, wrap it with an RSA key in AndroidKeyStore (RSA is available from API 18), persist the wrapped AES key plus ciphertext in `MODE_PRIVATE` SharedPreferences. Exclude the prefs file from backup. Never log passwords.

**LogRepository**
- SQLite table `bridge_log`: id, timestamp, direction (`SMS_IN`, `EMAIL_IN`, `SMS_OUT`, `EMAIL_OUT`, `SKIP`, `ERROR`), peer, subject, snippet, status (`OK`, `FAILED`, `SKIPPED`), detail.
- Newest first. Cap at 500 rows (delete oldest).

**MailClient**
- `sendMail(to, subject, body, inReplyToMessageId?)`
- `fetchUnseen(folder, allowedSenders): List<InboundMail>`
- `markRead(uid)`
- Gmail defaults: SMTP `smtp.gmail.com:587` STARTTLS, IMAP `imap.gmail.com:993` SSL, folder `INBOX`.
- All fields remain user-editable so other providers work.

**SmsSender**
- `send(phone, text): Result` using `SmsManager`, multipart when needed.

**MessageParser**
- `QUESTION <phone>` → command send + ANSWER reply.
- `Re: SMS from <phone>` / `SMS from <phone>` → reply send, no ANSWER.
- Strip `Re:`, `Fwd:`, `RE:`, `FW:` prefixes before matching.
- Phone tokens: optional `+`, then 8–15 digits. Keep `+` and digits only.
- For reply emails, send only the new body: cut at the first line matching `^On .*wrote:` or starting with `>`.

**BridgeForegroundService**
- Sticky, persistent notification “SMS–Gmail bridge running”.
- Loop: wait poll interval (default 30s, min 10s, max 300s) → IMAP fetch → process → sleep.
- Accepts `ACTION_FORWARD_SMS` from `SmsReceiver`.
- `START_STICKY`. Restart on boot if bridge was enabled.
- Partial WakeLock only while sending mail or polling.

## 4. Data flow

### 4.1 SMS → Gmail

1. `SmsReceiver` handles `android.provider.Telephony.SMS_RECEIVED`.
2. Concatenate PDU parts; take originating address and body.
3. Start/wake service with sender + body.
4. Service SMTP-sends:
   - To: configured forward-to address (default = IMAP username if empty)
   - Subject: `SMS from <normalized-number>`
   - Body: raw SMS text
   - Reply-To: SMTP username (the Gmail account)
5. Log `SMS_IN` + `EMAIL_OUT`.

### 4.2 Gmail → SMS (`QUESTION`)

1. Service IMAP-searches UNSEEN in configured folder.
2. Drop mail whose From is not in the allowlist (case-insensitive email match). Mark read and log `SKIP`.
3. If subject matches `QUESTION <phone>`:
   - Send body as SMS to normalized phone.
   - Mark original read.
   - SMTP reply to original From, subject `ANSWER`, body `SENT <phone>` or `FAILED <phone>: <reason>`, set In-Reply-To/References.
   - Log `EMAIL_IN` + `SMS_OUT` + `EMAIL_OUT`.

### 4.3 Gmail reply → SMS

1. Subject matches `SMS from <phone>` after stripping reply prefixes.
2. Extract new body (quoted text removed).
3. Send SMS. Mark read. Do not send ANSWER.
4. Log `EMAIL_IN` + `SMS_OUT`.

### 4.4 Other mail

Mark read, log `SKIP`, ignore.

### 4.5 Dedup

Persist last processed IMAP UID per folder. Ignore UID <= last. After success, store UID.

## 5. UI

Three activities, AppCompat + Material 1.3, no credentials on the log screen.

**HomeActivity (launcher)**
- Bridge status (running/stopped)
- Last successful poll time
- Last error (non-secret)
- Start / Stop
- Buttons: Setup, History
- Runtime permission prompt: RECEIVE_SMS, SEND_SMS, READ_SMS
- Service will not start if SMTP/IMAP user+password or SMS permissions are missing

**SetupActivity**
- SMTP host, port, username, password, STARTTLS toggle
- IMAP host, port, username, password, SSL toggle, folder
- Forward-to email
- Allowed senders (one email per line)
- Poll interval seconds
- Save
- Test SMTP / Test IMAP (result on screen, never echo password)

**HistoryActivity**
- RecyclerView: time, direction, peer, subject/snippet, status
- Newest first

ViewModels expose LiveData. Config writes go through `BridgeConfigStore`. History reads go through `LogRepository`.

## 6. Manifest and background survival

Permissions:

- RECEIVE_SMS, SEND_SMS, READ_SMS
- INTERNET, ACCESS_NETWORK_STATE
- FOREGROUND_SERVICE
- WAKE_LOCK
- RECEIVE_BOOT_COMPLETED

Components:

- `GatewayApp` as Application (Conscrypt first)
- Home / Setup / History activities
- `SmsReceiver` exported with `RECEIVE_SMS` permission and `SMS_RECEIVED` filter
- `BridgeForegroundService` (not exported)
- `BootReceiver` with `BOOT_COMPLETED` (`android:exported="true"` so the system can deliver the broadcast on targetSdk 33); starts service only if bridge enabled

Notification channel created only when SDK >= 26; on API 22 use `Notification.Builder` / `NotificationCompat` without a channel.

`telephony` feature required=false so the APK still installs on Wi-Fi devices (SMS no-ops there).

## 7. Gradle / libraries (API 22 safe)

Root: Android Gradle Plugin 8.1.4, Kotlin 1.9.0.

App:

- minSdk 22, targetSdk 33, compileSdk 34
- Java 8 / Kotlin JVM 1.8
- androidx.appcompat:appcompat:1.3.1
- androidx.core:core:1.3.2
- com.google.android.material:material:1.3.0
- androidx.constraintlayout:constraintlayout:2.0.4
- androidx.recyclerview:recyclerview:1.2.1
- androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1
- androidx.lifecycle:lifecycle-livedata-ktx:2.5.1
- androidx.lifecycle:lifecycle-runtime-ktx:2.5.1
- org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4
- org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4
- com.sun.mail:android-mail:1.6.7
- com.sun.mail:android-activation:1.6.7
- org.conscrypt:conscrypt-android:2.5.2
- junit 4.13.2 for unit tests of parser/normalizer

Do not add: Material3, WorkManager as primary, Gmail REST, Play Services Gmail, OkHttp, security-crypto. JavaMail is the mail stack. Keep multidex off; this app stays under the 64k limit.

JavaMail on Android requires these manifest/proguard keeps for mail providers and the activation framework (document in `proguard-rules.pro`).

## 8. Error handling

| Failure | Behavior |
|---|---|
| SMTP/IMAP network or auth | Retry 5s, 15s, 45s; then wait until next poll. Surface last error on Home. |
| SMS send failed | Log FAILED. For QUESTION, ANSWER body is `FAILED <phone>: <reason>`. |
| Missing SMS permission | Home requests. Service refuses to start. |
| Empty allowlist | Process no inbound email. SMS→Gmail still works. |
| Malformed subject / no phone | Mark read, log SKIP. |
| Duplicate UID | Ignore. |
| Process death | START_STICKY + BootReceiver if enabled. |
| TLS on API 22 | Conscrypt installed in `GatewayApp` before any sockets. |

Do not crash the receiver. Catch, log ERROR, return.

## 9. Testing

Unit tests (no emulator required for parser):

- `QUESTION +99361111111` extracts phone
- `Re: SMS from +99361111111` extracts phone
- Quoted reply body is stripped
- Unknown subject is skip
- Number normalization keeps `+` and digits

Manual / instrumented (documented, not blocking unit tests):

- Save config, Test SMTP/IMAP
- Start service, notification visible
- Inject SMS via `adb emu sms` or device, email appears
- Send `QUESTION <phone>` from allowlisted Gmail, device sends SMS, ANSWER arrives
- Reply to `SMS from <phone>`, device sends SMS

## 10. Out of scope

- Gemini / AI replies
- OAuth2 Gmail API
- Multiple Gmail accounts
- MMS / attachments
- End-to-end encryption of SMS content in Gmail
- Material3 / Compose

## 11. File map (implementation)

```
app/src/main/java/io/turkmensms/aigateway/
  GatewayApp.kt
  ui/HomeActivity.kt
  ui/SetupActivity.kt
  ui/HistoryActivity.kt
  ui/HomeViewModel.kt
  ui/SetupViewModel.kt
  ui/HistoryViewModel.kt
  ui/LogAdapter.kt
  domain/MessageParser.kt
  domain/PhoneNormalizer.kt
  domain/SmsToEmailUseCase.kt
  domain/EmailToSmsUseCase.kt
  data/BridgeConfig.kt
  data/BridgeConfigStore.kt
  data/LogRepository.kt
  data/MailClient.kt
  data/SmsSender.kt
  service/BridgeForegroundService.kt
  receiver/SmsReceiver.kt
  receiver/BootReceiver.kt
```

Remove:

- `com/sampleapp/*`
- `io/turkmensms/aigateway/data/network/GeminiApiClient.kt`
- Gemini-only UI/strings that conflict with the bridge

Keep GitHub Actions APK build workflow as-is unless the package/applicationId change requires it (applicationId stays `io.turkmensms.aigateway`).
