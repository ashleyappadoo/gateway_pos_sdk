# Gateway POS SDK — POC Smile&Pay / Nepting SoftPOS

POC Android d'une **Gateway Smile&Pay** qui encapsule le SDK Nepting SoftPOS. Une app caisse déclenche un paiement via Intent Android ; la Gateway gère toute la communication NFC/Nepting.

## Architecture

```
App Caisse  ──Intent──►  Gateway Smile&Pay  ──SDK──►  Nepting SoftPOS
(caisse-test)              (gateway/)                  (nepting-mock/)
```

- **`gateway/`** : Application Android Gateway (`com.smileandpay.gateway`)
  - **`nepting-mock/`** : Module library simulant le SDK Nepting (remplaçable par les vrais AARs)
  - **`app/`** : Activity + ViewModel + UI Compose + orchestrateur Nepting
- **`caisse-test/`** : Application Android de test caisse (`com.smileandpay.caissetest`)

Les deux apps communiquent **uniquement via Intent Android** — aucune dépendance de code.

## Lancer les apps

### Prérequis
- Android Studio Ladybug (2024.2) ou plus récent
- SDK Android 35
- Java 17

### Gateway

```bash
cd gateway/
./gradlew assembleDebug
# Installer sur le device :
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Caisse-test

```bash
cd caisse-test/
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Connecter la caisse à la Gateway

1. Installer la **Gateway** sur le device Android
2. Ouvrir la Gateway une première fois via son intent `ACTION_CONFIG` pour configurer les credentials :
   ```kotlin
   startActivity(Intent("com.smileandpay.gateway.ACTION_CONFIG"))
   ```
3. Saisir : login Smile&Pay, mot de passe, n° îlot
4. Installer la **Caisse-test** sur le même device
5. Lancer la Caisse-test, saisir un montant, taper "Payer via Gateway"

## Interface Intent

### Envoi (caisse → Gateway)

| Extra | Type | Description |
|---|---|---|
| `amount` | Long | Montant en centimes (ex: 1050 = 10,50€) |
| `currency` | String | Devise (ex: "EUR") |
| `txn_type` | String | "DEBIT", "CREDIT" ou "VOID" |
| `order_id` | String | Référence commande |
| `description` | String | Optionnel |

### Retour (Gateway → caisse)

| Extra | Type | Description |
|---|---|---|
| `status` | String | "SUCCESS", "REFUSED", "CANCELLED", "ERROR" |
| `amount` | Long | Montant traité |
| `ticket_client` | String | Ticket porteur |
| `ticket_merchant` | String | Ticket commerçant |
| `auth_code` | String | Code d'autorisation |
| `card_token` | String | Token carte |
| `error_message` | String | Message d'erreur (si ERROR) |

## Remplacer le mock par les vrais AARs Nepting

Le module `nepting-mock` expose exactement la même API que le SDK Nepting réel. Pour passer en production :

1. Copier les AARs dans `gateway/libs/` :
   - `softpos-X.X.X.aar`
   - `softpos-0.1.6-16K-SecurityPinRelease.aar`
   - `visa-sensory-branding-2.2.aar`
   - etc.

2. Dans `gateway/app/build.gradle.kts`, remplacer :
   ```kotlin
   implementation(project(":nepting-mock"))
   ```
   par :
   ```kotlin
   implementation(files("../libs/softpos-X.X.X.aar"))
   implementation(files("../libs/softpos-0.1.6-16K-SecurityPinRelease.aar"))
   // ... autres AARs
   ```

3. Supprimer le module `nepting-mock` de `settings.gradle.kts`

## Configuration qualif

Les credentials Nepting de qualification sont stockés dans `CredentialsManager` :
- URL : `qualif.nepting.com:443/nepweb/ws?wsdl`
- Merchant code : `72086618504806197`
- SSL : désactivé pour la qualif
