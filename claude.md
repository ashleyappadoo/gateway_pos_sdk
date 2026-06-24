# CLAUDE.md — Gateway POS SDK · POC Smile&Pay / Nepting SoftPOS

> **Lis ce fichier en entier avant d'écrire la moindre ligne de code.**
> Il contient tout le contexte métier, technique, et les instructions précises de développement.

---

## 0. Objectif du projet

Proof-of-Concept Android d'une **Gateway Smile&Pay** installée sur un TPE Android, encapsulant le SDK Nepting SoftPOS. Le but : permettre à n'importe quelle app caisse Android de déclencher un paiement SoftPOS via une interface simple (Intent Android), sans intégrer directement le SDK Nepting.

**Contexte métier :**
- Aujourd'hui : app-to-app entre `App Caisse` → `App Smile&Pay` → `App Nepting` (3 apps)
- Cible POC : `App Caisse` → `Gateway Smile&Pay` (1 seule app Gateway, SDK Nepting intégré dedans)

---

## 1. Structure du repo — OBLIGATOIRE

```
gateway_pos_sdk/
├── CLAUDE.md
├── README.md
├── settings.gradle.kts          ← root, inclut les deux sous-projets
│
├── gateway/                     ← MODULE 1 : Application Gateway Smile&Pay
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/smileandpay/gateway/
│   │   │   │   ├── GatewayActivity.kt
│   │   │   │   ├── GatewayApplication.kt
│   │   │   │   ├── intent/
│   │   │   │   │   └── IntentPaymentHandler.kt
│   │   │   │   ├── auth/
│   │   │   │   │   └── CredentialsManager.kt
│   │   │   │   ├── nepting/
│   │   │   │   │   ├── NeptingOrchestrator.kt
│   │   │   │   │   ├── SoftPosClientWrapper.kt
│   │   │   │   │   └── UICallbackHandler.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── PaymentScreen.kt      ← Jetpack Compose
│   │   │   │   │   └── PinEntryView.kt
│   │   │   │   └── model/
│   │   │   │       ├── PaymentRequest.kt
│   │   │   │       └── PaymentResult.kt
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   ├── nepting-mock/            ← Mock SDK Nepting (remplace les vrais AARs)
│   │   └── src/main/java/
│   │       ├── com/nepting/softpos/client/
│   │       │   ├── SoftPosClient.kt
│   │       │   └── SoftPosHelper.kt
│   │       ├── com/nepting/common/client/
│   │       │   ├── model/
│   │       │   │   ├── LoginRequest.kt
│   │       │   │   ├── TransactionRequest.kt
│   │       │   │   └── TransactionResponse.kt
│   │       │   └── callback/
│   │       │       ├── UICallback.kt
│   │       │       └── UIRequest.kt
│   │       └── com/alcineo/softpos/
│   │           └── (stubs Alcineo)
│   └── settings.gradle.kts
│
└── caisse-test/                 ← MODULE 2 : App caisse simulée
    ├── app/
    │   ├── src/main/
    │   │   ├── java/com/smileandpay/caissetest/
    │   │   │   ├── MainActivity.kt
    │   │   │   ├── GatewayClient.kt      ← envoie l'Intent à la Gateway
    │   │   │   └── ui/
    │   │   │       └── MainScreen.kt     ← Jetpack Compose
    │   │   ├── res/
    │   │   └── AndroidManifest.xml
    │   └── build.gradle.kts
    └── settings.gradle.kts
```

---

## 2. Contexte technique complet

### 2.1 SDK Nepting SoftPOS (version 4.4.9)

Le SDK est normalement distribué sous forme de fichiers `.aar` locaux. Pour ce POC, tu vas **créer un module `nepting-mock`** qui implémente les mêmes interfaces/classes avec des stubs simulant le comportement.

**Modules AAR réels (à simuler) :**
| Module Gradle | Fichier AAR réel | Rôle |
|---|---|---|
| `:nepting-softpos` | AAR local | Client SoftPOS principal |
| `:nepting-pinpad-security` | `softpos-0.1.6-16K-SecurityPinRelease.aar` | Sécurité PIN |
| `:nepting-softpos-pin-security` | AAR local | Sécurité PIN SoftPOS |
| `:nepting-payment-security` | AAR local | Sécurité paiement |
| `:nepting-visa-sensory` | `visa-sensory-branding-2.2.aar` | Branding Visa Sensory |

**Packages à simuler :**
- `com.nepting.softpos.client.SoftPosClient` — classe principale
- `com.nepting.softpos.client.SoftPosHelper`
- `com.nepting.common.client.model.LoginRequest`
- `com.nepting.common.client.model.TransactionRequest`
- `com.nepting.common.client.model.TransactionResponse`
- `com.nepting.common.client.callback.UICallback`
- `com.nepting.common.client.callback.UIRequest`
- `com.nepting.common.client.callback.UIRequest.ActionType` (enum: NFC_LEDS, MESSAGE, VIDEO, PIN_ENTRY, QUESTION, MENU, KEYS_ENTRY)
- `com.nepting.common.client.LoadBalancingAlgorithm` (enum: FIRST_ALIVE, ROUND_ROBIN)
- `com.alcineo.softpos.payment.api.*` (stubs vides suffisent)
- `com.alcineo.softpos.pinpad.api.*` (stubs vides suffisent)

### 2.2 Classe SoftPosClient — API exacte à implémenter dans le mock

```kotlin
// com.nepting.softpos.client.SoftPosClient
class SoftPosClient(
    private val uiCallback: UICallback,
    private val logger: Any?,
    private val debugEnabled: Boolean,
    private val context: Context
) {
    fun setContainer(layoutId: Int, view: View?)
    fun setActivity(activity: Activity)
    fun start()
    fun softposLogin(request: LoginRequest, callback: UIRequestCallback)
    fun softposStartTransaction(request: TransactionRequest, callback: UIRequestCallback)
    fun softposStopTransaction()
    fun logoff()
    fun interrupt()
    fun abort()
}
```

**Comportement du mock `SoftPosClient` :**
- `softposLogin()` : attendre 1 seconde, appeler `callback.onSuccess()` avec un token fictif
- `softposStartTransaction()` : simuler la séquence NFC :
  1. Appeler `uiCallback.postUIRequest(UIRequest(ActionType.NFC_LEDS, ...))` — "Lecteur NFC activé"
  2. Après 1s : `uiCallback.postUIRequest(UIRequest(ActionType.MESSAGE, "PRÉSENTEZ VOTRE CARTE"))`
  3. Après 2s : simuler présentation carte, appeler `uiCallback.postUIRequest(UIRequest(ActionType.PIN_ENTRY, ...))`
  4. Après 1s supplémentaire : appeler `callback.onSuccess(TransactionResponse(...))` avec statut SUCCESS
- `abort()` / `softposStopTransaction()` : appeler `callback.onCancelled()`

### 2.3 LoginRequest — structure exacte

```kotlin
// com.nepting.common.client.model.LoginRequest
data class LoginRequest(
    val login: String,
    val password: String,
    val extra: String = "",
    val urls: Array<String>,
    val loadBalancingAlgorithm: LoadBalancingAlgorithm,
    val merchantCode: String = "",
    val sslConfig: Any? = null
) {
    var posEditor: String = ""
    var posSolution: String = ""
    var posVersion: String = ""
}
```

### 2.4 TransactionResponse — structure exacte

```kotlin
data class TransactionResponse(
    val amount: Long,                    // en centimes
    val type: String,                    // DEBIT, CREDIT, VOID...
    val status: TransactionStatus,       // SUCCESS, REFUSED, CANCELLED
    val ticketClient: String = "",
    val ticketMerchant: String = "",
    val tip: Long = 0L,
    val cardToken: String = "",
    val authCode: String = ""
)

enum class TransactionStatus { SUCCESS, REFUSED, CANCELLED, ERROR }
```

---

## 3. Credentials et configuration — QUALIF

```kotlin
// À utiliser dans CredentialsManager.kt
object QualifConfig {
    const val NEPTING_URL = "qualif.nepting.com:443/nepweb/ws?wsdl"
    const val SMILE_MERCHANT_CODE = "72086618504806197"
    const val SSL_PINNING_ENABLE = false
    const val SSL_ENABLE = false
    const val POS_EDITOR = "SMILEANDPAY"
    const val POS_SOLUTION = "SMILEANDPAY"
}
// Les credentials utilisateur (login/password/n° îlot) seront saisis
// dans l'écran de configuration de la Gateway, stockés via EncryptedSharedPreferences
```

---

## 4. Interface caisse → Gateway : Intent Android

### 4.1 Intent envoyé par la caisse

```kotlin
// Dans GatewayClient.kt (caisse-test)
const val GATEWAY_PACKAGE = "com.smileandpay.gateway"
const val GATEWAY_ACTION  = "com.smileandpay.gateway.ACTION_PAYMENT"

// Extras envoyés :
const val EXTRA_AMOUNT      = "amount"        // Long, en centimes (ex: 1050 = 10,50€)
const val EXTRA_CURRENCY    = "currency"      // String (ex: "EUR")
const val EXTRA_TXN_TYPE    = "txn_type"      // String ("DEBIT", "CREDIT", "VOID")
const val EXTRA_ORDER_ID    = "order_id"      // String (référence caisse)
const val EXTRA_DESCRIPTION = "description"   // String optionnel

// Appel :
val intent = Intent(GATEWAY_ACTION).apply {
    setPackage(GATEWAY_PACKAGE)
    putExtra(EXTRA_AMOUNT, 1050L)
    putExtra(EXTRA_CURRENCY, "EUR")
    putExtra(EXTRA_TXN_TYPE, "DEBIT")
    putExtra(EXTRA_ORDER_ID, "CMD-001")
}
startActivityForResult(intent, REQUEST_CODE_PAYMENT)
```

### 4.2 Intent retourné par la Gateway

```kotlin
// Dans GatewayActivity.kt, setResult() avant finish()
const val RESULT_EXTRA_STATUS       = "status"        // "SUCCESS", "REFUSED", "CANCELLED", "ERROR"
const val RESULT_EXTRA_AMOUNT       = "amount"        // Long
const val RESULT_EXTRA_TICKET_CLIENT   = "ticket_client"
const val RESULT_EXTRA_TICKET_MERCHANT = "ticket_merchant"
const val RESULT_EXTRA_CARD_TOKEN   = "card_token"
const val RESULT_EXTRA_AUTH_CODE    = "auth_code"
const val RESULT_EXTRA_ERROR_MSG    = "error_message" // si ERROR
```

---

## 5. Architecture Gateway — détail des composants

### 5.1 GatewayActivity.kt
- Reçoit l'Intent entrant (montant, type, order_id)
- Vérifie que les credentials sont configurés (sinon → écran de config)
- Lance `NeptingOrchestrator`
- Affiche `PaymentScreen` (Compose) pendant la transaction
- Reçoit le résultat de l'orchestrateur et appelle `setResult()` + `finish()`

### 5.2 CredentialsManager.kt
- Utilise `EncryptedSharedPreferences` (androidx.security.crypto)
- Stocke : login Smile&Pay, password, n° îlot (client_id)
- Expose : `hasCredentials(): Boolean`, `getLoginRequest(): LoginRequest`

### 5.3 NeptingOrchestrator.kt
- Gère le cycle complet via coroutines Kotlin
- Pattern :
  ```
  initClient() → softposLogin() [si pas déjà logué] → softposStartTransaction() → résultat
  ```
- Mémorise `isSoftPosLoginCalledOnce` pour éviter re-login à chaque transaction
- Expose un `StateFlow<OrchestratorState>` observé par `GatewayActivity`

```kotlin
sealed class OrchestratorState {
    object Idle : OrchestratorState()
    object LoggingIn : OrchestratorState()
    object WaitingForCard : OrchestratorState()
    object ProcessingPin : OrchestratorState()
    object Processing : OrchestratorState()
    data class Success(val response: TransactionResponse) : OrchestratorState()
    data class Failed(val reason: String) : OrchestratorState()
    object Cancelled : OrchestratorState()
}
```

### 5.4 UICallbackHandler.kt
- Implémente `UICallback` du SDK
- `postUIRequest(request: UIRequest)` : traduit les ActionType en événements UI :
  - `NFC_LEDS` → mise à jour de l'icône NFC dans PaymentScreen
  - `MESSAGE` → affichage du message dans PaymentScreen
  - `PIN_ENTRY` → affichage de `PinEntryView`
  - `QUESTION` → dialogue de confirmation
- Émet via un `SharedFlow<UIEvent>` observé par le ViewModel

### 5.5 PaymentScreen.kt (Jetpack Compose)
- Affiche : montant, statut (en attente carte / PIN / traitement), animation NFC
- Bouton "Annuler" → appelle `orchestrator.cancel()`
- Responsive aux états `OrchestratorState`

---

## 6. App caisse-test — détail

### 6.1 MainScreen.kt (Compose)
- Champ montant (en euros, converti en centimes)
- Sélecteur type transaction (Débit / Crédit / Annulation)
- Champ order_id
- Bouton "Payer via Gateway"
- Zone résultat : affiche le retour de la Gateway (statut, tickets)

### 6.2 GatewayClient.kt
- Vérifie que la Gateway est installée (`packageManager.getLaunchIntentForPackage`)
- Lance l'Intent et gère `onActivityResult`
- Parse les extras de retour en `PaymentResult`

---

## 7. Versions et dépendances

```kotlin
// Versions cibles
kotlin          = "2.0.21"
agp             = "8.3.2"          // Android Gradle Plugin
compileSdk      = 35
targetSdk       = 35
minSdk          = 24               // Android 7.0+ (SoftPOS requiert Android 10 mais mock = 24)

// Dépendances principales Gateway
"androidx.core:core-ktx:1.13.1"
"androidx.appcompat:appcompat:1.7.0"
"androidx.activity:activity-compose:1.9.0"
"androidx.compose.bom:2024.06.00"
"androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3"
"androidx.security:security-crypto:1.1.0-alpha06"
"org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1"
"com.google.guava:guava:33.4.8-jre"         // requis par Alcineo mock
"com.neovisionaries:nv-i18n:1.29"           // requis par Alcineo mock
"com.squareup.retrofit2:retrofit:3.0.0"    // requis par Alcineo mock

// Dépendances caisse-test
// Même BOM Compose, pas de dépendances Nepting
```

---

## 8. AndroidManifest.xml — Gateway

```xml
<!-- Permissions requises par le SDK Nepting SoftPOS -->
<uses-permission android:name="android.permission.NFC" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-feature android:name="android.hardware.bluetooth" android:required="false" />
<uses-feature android:name="android.hardware.nfc" android:required="false" />

<!-- GatewayActivity doit être exportée pour recevoir l'Intent de la caisse -->
<activity
    android:name=".GatewayActivity"
    android:exported="true"
    android:launchMode="singleTask">
    <intent-filter>
        <action android:name="com.smileandpay.gateway.ACTION_PAYMENT" />
        <action android:name="com.smileandpay.gateway.ACTION_CONFIG" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

---

## 9. ProGuard / R8 — règles critiques à inclure dans gateway/app/proguard-rules.pro

```proguard
-dontoptimize
-dontshrink
-ignorewarnings
-overloadaggressively
-repackageclasses ''
-allowaccessmodification

# Classes Nepting
-keep class com.nepting.** { *; }
-keep class com.nepting.common.client.* { *; }
-keep class com.nepting.common.client.model.* { *; }
-keep class com.nepting.common.client.callback.* { *; }
-keep class com.nepting.softpos.client.* { *; }

# Classes Alcineo (mock)
-keep class com.alcineo.softpos.payment.api.** { *; }
-keep class com.alcineo.softpos.payment.model.** { *; }
-keep class com.alcineo.softpos.security.api.** { *; }
-keep class com.alcineo.softpos.pinpad.api.** { *; }
-keep class com.alcineo.softpos.pinpad.view.PinpadView { *; }
-keep class com.alcineo.softpos.pinpad.view.PinpadView$* { *; }
-keep class com.alcineo.softpos.security.service.DeviceInfoService** { *; }
-keep class com.alcineo.softpos.payment.jni.MPANativeInterface { *; }
-keep class com.alcineo.softpos.pinpad.jni.PinpadNativeInterface { *; }
-keep class com.alcineo.transactionParameters.* { public *; }
-keep class com.alcineo.administrative.** { *; }

# Supprimer logs en release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** i(...);
    public static *** v(...);
}
```

---

## 10. Bug connu à éviter — Token refresh / 401

Dans la Gateway, si tu implémentes un mécanisme de refresh de token Smile&Pay :
- **Ne jamais** déclencher le refresh dans `onResume()` ou `onAttach()` immédiatement après login
- Vérifier le claim `exp` du JWT avant de déclencher le refresh
- Seulement déclencher si le token expire dans moins de 300 secondes

```kotlin
private fun isTokenExpiringSoon(token: String, marginSeconds: Long = 300): Boolean {
    return try {
        val parts = token.split(".")
        if (parts.size < 2) return true
        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING))
        val expMatch = Regex(""""exp"\s*:\s*(\d+)""").find(payload)
        val exp = expMatch?.groupValues?.get(1)?.toLongOrNull() ?: return true
        val now = Instant.now().epochSecond
        (exp - now) < marginSeconds
    } catch (e: Exception) { true }
}
```

---

## 11. Instructions de développement pour Claude Code

1. **Commence par créer la structure de dossiers** complète telle que définie en section 1
2. **Crée d'abord le module `nepting-mock`** — toutes les autres classes en dépendent
3. **Ensuite la Gateway** dans l'ordre : modèles → CredentialsManager → UICallbackHandler → NeptingOrchestrator → GatewayActivity → UI Compose
4. **Enfin la caisse-test** : GatewayClient → MainScreen → MainActivity
5. **Le projet doit compiler** avec `./gradlew assembleDebug` depuis chaque sous-dossier
6. **Ajoute un README.md** à la racine expliquant : architecture, comment lancer les deux apps, comment connecter la caisse à la Gateway, comment remplacer le mock par les vrais AARs

### Contraintes de qualité
- Kotlin idiomatique, coroutines partout (pas de callbacks imbriqués)
- Jetpack Compose pour tout l'UI
- ViewModel + StateFlow (pas de LiveData)
- Aucune logique métier dans les Activities/Composables
- Le module `nepting-mock` doit avoir une interface identique au vrai SDK — le remplacement doit se faire en changeant uniquement la dépendance dans `build.gradle.kts`

### Point de vigilance architectural
- La **Gateway** est une app Android à part entière (applicationId: `com.smileandpay.gateway`)
- La **caisse-test** est une app Android séparée (applicationId: `com.smileandpay.caissetest`)
- Elles communiquent **uniquement via Intent Android** — aucune dépendance de code entre elles
- Ne jamais mettre de logique Nepting dans `caisse-test`

---

## 12. Commandes pour pousser sur GitHub

```bash
git init
git remote add origin https://github.com/ashleyappadoo/gateway_pos_sdk.git
git add .
git commit -m "feat: initial POC structure — Gateway + caisse-test + nepting-mock"
git branch -M main
git push -u origin main
```

---

*Ce document contient tout le contexte nécessaire. Commence à développer.*
