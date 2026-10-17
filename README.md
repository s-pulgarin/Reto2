<div align="center">

# 💱 App Fintech

**Aplicación FinTech nativa para Android con roles de usuario y administrador, operaciones en tiempo real, portafolio de activos, auditoría de transacciones e integración con el hardware del dispositivo (cámara y lector de huellas).**

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-100%25%20declarativo-4285F4?logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%20Design-3-757575?logo=materialdesign&logoColor=white)
![Architecture](https://img.shields.io/badge/Arquitectura-MVVM-2E7D32)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore-FFCA28?logo=firebase&logoColor=black)
![Cloudinary](https://img.shields.io/badge/Cloudinary-KYC-3448C5?logo=cloudinary&logoColor=white)
![OkHttp](https://img.shields.io/badge/OkHttp-WebSockets-0A7EA4)
![Coroutines](https://img.shields.io/badge/Coroutines-Flow-7F52FF?logo=kotlin&logoColor=white)
![Biometric](https://img.shields.io/badge/Biometric-Huella%20dactilar-00897B?logo=android&logoColor=white)
![Camera](https://img.shields.io/badge/C%C3%A1mara-FileProvider-546E7A?logo=android&logoColor=white)
![Android](https://img.shields.io/badge/Android-Nativo-3DDC84?logo=android&logoColor=white)

</div>

---

## 📖 Descripción

**App Fintech** (repositorio `App_Reto2`) simula una plataforma de trading de divisas y criptomonedas. Los usuarios consultan cotizaciones en vivo, ejecutan órdenes de compra y venta, y siguen su saldo, su portafolio y su historial en tiempo real. Un administrador audita cada orden: la aprueba o la rechaza según el margen de precio respecto al mercado, y el saldo y el portafolio del usuario se actualizan de forma atómica.

La app aprovecha el **hardware del dispositivo**: cada operación se autoriza con el **lector de huellas** y el documento de identidad (KYC) puede capturarse directamente con la **cámara**.

El proyecto está construido con **Kotlin y Jetpack Compose (UI sin XML)**, siguiendo **MVVM** y flujos reactivos con **Corrutinas y Flow**.

## 📑 Tabla de contenidos

- [Tech Stack](#-tech-stack)
- [Características principales](#-características-principales)
- [Integración de hardware](#-integración-de-hardware)
- [Arquitectura](#-arquitectura)
- [Instalación](#-instalación)
- [Capturas de pantalla](#-capturas-de-pantalla)
- [Notas y limitaciones](#-notas-y-limitaciones)
- [Autor](#-autor)

---

## 🛠 Tech Stack

| Categoría | Tecnología | Uso en el proyecto |
|---|---|---|
| Lenguaje | **Kotlin 2.2.10** | 100 % del código (sin Java) |
| UI | **Jetpack Compose + Material 3** | Interfaz totalmente declarativa, sin XML |
| Arquitectura | **MVVM** | `ViewModel` + `StateFlow` + State Hoisting |
| Navegación | **Navigation Compose** | Rutas tipadas con `sealed class` y protección del backstack |
| Autenticación | **Firebase Authentication** | Registro, login y sesión persistente (Email/Password) |
| Base de datos | **Cloud Firestore** | Perfiles, saldos y transacciones en tiempo real |
| Tiempo real | **OkHttp WebSocket** | Cotizaciones de mercado desde Finnhub |
| Multimedia | **Cloudinary** | Subida de documentos KYC con *unsigned upload preset* |
| Biometría | **AndroidX Biometric 1.1.0** | `BiometricPrompt` para autorizar cada operación con huella |
| Cámara | **Activity Result API + FileProvider** | `TakePicture` y `RequestPermission`; la foto se guarda en caché y se comparte con una `Uri` segura |
| Asincronía | **Corrutinas + Flow** | `callbackFlow`, `StateFlow`, `suspendCancellableCoroutine` |
| Serialización | **Kotlinx Serialization** | Parseo de mensajes JSON del WebSocket |
| Build | **Gradle Kotlin DSL + Version Catalog** | AGP 9.3.3, Firebase BoM 34.17.0 |

---

## ✨ Características principales

### 👥 Sistema de roles (User / Admin)
- Cada usuario tiene un perfil en Firestore (`users/{uid}`) con su `role` (`user` o `admin`).
- Tras iniciar sesión se redirige automáticamente al panel correspondiente.
- La sesión persiste entre ejecuciones y se restaura leyendo el rol desde Firestore.
- El cierre de sesión vacía por completo el *backstack*, de modo que el botón "Atrás" no permite volver a pantallas protegidas.
- Las cuentas nuevas empiezan con un saldo de **$300,000** y un portafolio vacío.
- **Errores de autenticación específicos**: las excepciones de Firebase se traducen a mensajes claros en español, por ejemplo "La contraseña debe tener al menos 6 caracteres", "El formato del correo es inválido" o "El correo ya está registrado".

### 📈 Cotizaciones en tiempo real (Finnhub + WebSockets)
- Conexión WebSocket a Finnhub expuesta como `Flow<Double>` mediante `callbackFlow`.
- Un `Interceptor` de OkHttp inyecta el token en cada petición, así que la clave nunca aparece en el código fuente.
- El símbolo se configura desde un único punto (`MarketConfig`), por ejemplo `OANDA:EUR_USD` en producción o `BINANCE:BTCUSDT` para pruebas en fin de semana.

### 🧑‍💼 Panel de Usuario
- **Saldo en vivo**: tarjeta destacada alimentada por un `addSnapshotListener` sobre su documento.
- **Mercado en vivo**: precio actualizado en tiempo real, con opción de reconectar.
- **Portafolio**: muestra las unidades que posees del activo operado (ej. "Tienes: 1.5 BTC") y las que están comprometidas en ventas pendientes.
- **Compra / Venta con validación estricta**, antes de crear la orden:
    - Compra: `cantidad × precio` no puede superar el saldo libre.
    - Venta: la cantidad no puede superar las unidades del portafolio.
    - El saldo y las unidades comprometidos en órdenes `PENDING` se descuentan, para que no se puedan usar dos veces mientras el administrador no audita.
- **Autorización biométrica**: COMPRAR y VENDER piden la huella antes de enviar la orden (ver [Integración de hardware](#-integración-de-hardware)).
- **Historial de transacciones**: lista reactiva con indicadores por estado: 🟢 `APPROVED`, 🔴 `REJECTED`, 🟠 `PENDING`.

### 🛡 Panel de Administrador
- Lista en tiempo real de todas las transacciones, separadas en pendientes e historial.
- Precio de mercado en vivo visible en la barra superior.
- **Auditoría automática con margen del 2.5 %**: la orden se aprueba solo si
  `|precioEjecución − precioEnVivo| / precioEnVivo ≤ 0.025`; en caso contrario se rechaza.

### ⚛️ Transacciones atómicas
- La auditoría usa `firestore.runTransaction`: el cambio de estado de la transacción y la actualización del saldo y del portafolio del usuario se confirman juntos o no se confirma ninguno.
- Compra: resta `cantidad × precio de ejecución` del saldo y suma `cantidad` al portafolio del par.
- Venta: suma `cantidad × precio de ejecución` al saldo y resta `cantidad` del portafolio del par.
- Si una compra dejaría el saldo en negativo, o una venta supera las unidades disponibles, la orden se rechaza automáticamente sin tocar el perfil.
- Una transacción ya auditada no puede auditarse de nuevo.

### 🪪 KYC (Know Your Customer)
- Dos formas de aportar el documento: **Tomar Foto** con la cámara o **Subir desde Galería** con el *Photo Picker* nativo (`PickVisualMedia`).
- La imagen se sube en cuanto se elige; si falla, aparece un botón para reintentar.
- Subida a **Cloudinary** con *unsigned upload preset*, sin exponer el API Secret.
- El callback del SDK se convierte en una función `suspend` con `suspendCancellableCoroutine`.
- La `secure_url` resultante se guarda en `users/{uid}.kycDocumentUrl`.

### 🔐 Seguridad
- API Keys y configuración sensible se leen desde `local.properties` (fuera del control de versiones) y se inyectan con `buildConfigField` en `BuildConfig`.
- **Reglas de seguridad de Firestore** por rol:
    - El usuario no puede modificar su `role`, su `balance` ni su `portfolio`, y se registra con el portafolio vacío.
    - El administrador solo puede cambiar `status` en transacciones `PENDING`, y `balance` y `portfolio` en usuarios.
    - Las transacciones solo se crean con `status = PENDING`, a nombre del propio usuario y con marca de tiempo del servidor.
- **Autorización biométrica** obligatoria para enviar cualquier orden de compra o venta.

---

## 📱 Integración de hardware

### 🔏 Lector de huellas (BiometricPrompt)
- Antes de ejecutar una compra o una venta se muestra el diálogo del sistema **"Autorizar Transacción"**; la orden solo se envía si la huella es válida.
- La lógica está encapsulada en `util/BiometricHelper.kt`, que recibe la `FragmentActivity` y los *callbacks* `onSuccess` y `onError`.
- Usa autenticadores `BIOMETRIC_STRONG`. Si el dispositivo no tiene huellas registradas o no tiene sensor, se informa al usuario y la orden no se envía.
- Si la huella no coincide, el diálogo sigue abierto para reintentar; si se cancela, se muestra "Autenticación cancelada."
- `MainActivity` hereda de `FragmentActivity`, como exige `BiometricPrompt`.

### 📷 Cámara (KYC)
- **Tomar Foto** usa `ActivityResultContracts.TakePicture` y pide el permiso `CAMERA` en tiempo de ejecución con `RequestPermission` solo si aún no está concedido.
- La foto se guarda en un archivo temporal en la caché de la app (`cache/images/`) y se expone mediante **`FileProvider`** (`util/ContextExt.kt` → `createTempImageUri()`), sin permisos de almacenamiento.
- En el manifest, la cámara se declara con `android:required="false"`, así la app también puede instalarse en dispositivos sin cámara, donde queda disponible la galería.

---

## 🏗 Arquitectura

El proyecto sigue **MVVM** con flujo de datos unidireccional y separación por capas:

```
┌──────────────────────────── UI (Jetpack Compose) ────────────────────────────┐
│  Screens (stateless)  ◄── StateFlow<UiState> ──  ViewModel  ── eventos ──►   │
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │ llama a
┌──────────────────────── Repository (contratos / dominio) ────────────────────┐
│  AuthRepository · UserRepository · TransactionRepository · FinnhubRepository │
│  KycRepository   →  exponen suspend funs y Flow<Resource<T>>                 │
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │ implementados sobre
┌──────────────────────────────── Data (fuentes) ──────────────────────────────┐
│  Firebase Auth · Firestore · OkHttp WebSocket (Finnhub) · Cloudinary SDK     │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Estructura de paquetes

```
app/src/main/java/com/example/app_reto2/
├── App.kt                   # Inicializa Cloudinary MediaManager
├── MainActivity.kt          # FragmentActivity (requerida por BiometricPrompt)
├── data/
│   ├── model/               # User (con portfolio), Transaction, DTOs de Finnhub
│   ├── network/             # Cliente OkHttp, interceptor y WebSocket de Finnhub
│   └── repository/          # Interfaces + implementaciones (Auth, User, Transaction, Finnhub, Kyc)
├── navigation/              # Screen (rutas) y AppNavigation (NavHost)
├── ui/
│   ├── auth/                # Login, Registro y AuthViewModel
│   ├── user/                # Dashboard del usuario (portafolio, trading con biometría)
│   ├── admin/               # Dashboard de auditoría
│   ├── kyc/                 # Verificación de identidad (cámara + galería)
│   └── theme/
└── util/                    # Resource, MarketConfig, BiometricHelper, ContextExt (Uri de FileProvider)
```

### Decisiones de diseño

- **MVVM**: cada pantalla tiene su `ViewModel`, que expone un único `StateFlow` inmutable y encapsula la lógica de negocio. La vista solo renderiza el estado y emite eventos.
- **State Hoisting**: los *composables* de pantalla reciben estado y *callbacks* (`onLogout`, `onNavigateToKyc`, `onTrade`…); la navegación vive en `AppNavigation` y nunca dentro de las vistas.
- **Capa de repositorios**: las interfaces actúan como contrato de dominio y las implementaciones encapsulan Firebase, OkHttp y Cloudinary, por lo que cada fuente puede sustituirse sin tocar la UI.
- **Manejo de errores**: el *wrapper* `Resource<T>` (`Success` / `Error`) evita propagar excepciones a la UI.
- **Reactividad**: los listeners de Firestore y el WebSocket se convierten en `Flow` con `callbackFlow`, y se cancelan automáticamente al destruirse el `ViewModel`.
- **Inyección de dependencias manual**: `ViewModelProvider.Factory` por pantalla, sin frameworks adicionales.

### Modelo de datos (Firestore)

| Colección | Campos |
|---|---|
| `users/{uid}` | `uid`, `email`, `role`, `balance`, `kycDocumentUrl`, `portfolio` (mapa `par → unidades`, ej. `{ "BTCUSDT": 1.5 }`) |
| `transactions/{id}` | `transactionId`, `userId`, `type` (`BUY`/`SELL`), `currencyPair`, `amount`, `executionPrice`, `timestamp` (servidor), `status` (`PENDING`/`APPROVED`/`REJECTED`) |

---

## 🚀 Instalación

### Requisitos previos
- Android Studio reciente con soporte para **AGP 9.x** y **JDK 17+**
- Cuenta de [Firebase](https://console.firebase.google.com/)
- Cuenta de [Cloudinary](https://cloudinary.com/)
- Cuenta de [Finnhub](https://finnhub.io/) (API Key gratuita)
- Dispositivo o emulador con **lector de huellas** y al menos una huella registrada (necesario para operar) y, de preferencia, **cámara**

### 1. Clonar el repositorio

```bash
git clone https://github.com/s-pulgarin/Reto2.git
cd Reto2
```

### 2. Configurar `local.properties`

En la raíz del proyecto, agrega:

```properties
FINNHUB_API_KEY=tu_api_key_de_finnhub
CLOUDINARY_CLOUD_NAME=tu_cloud_name
CLOUDINARY_UPLOAD_PRESET=tu_upload_preset_unsigned
```

> Sin comillas ni espacios. Si falta alguna clave, la app muestra un mensaje de error en lugar de fallar.

### 3. Configurar Cloudinary

1. En la consola ve a **Settings → Upload → Upload presets → Add upload preset**.
2. Establece **Signing mode: Unsigned** y guarda.
3. Usa el nombre del preset en `CLOUDINARY_UPLOAD_PRESET`.

### 4. Configurar Firebase

1. Crea un proyecto y registra una app Android con el package name **`com.example.app_reto2`**.
2. Descarga **`google-services.json`** y colócalo en `app/google-services.json`.
3. En **Authentication → Sign-in method**, habilita **Email/Password**.
4. En **Firestore Database**, crea la base de datos.
5. En la pestaña **Rules**, publica las reglas de seguridad:

<details>
<summary>📜 Reglas de seguridad de Firestore</summary>

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isAdmin() {
      return request.auth != null
        && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }

    match /users/{uid} {
      allow read: if request.auth != null && (request.auth.uid == uid || isAdmin());
      // Al registrarse: rol user, saldo inicial y portafolio vacío (o ausente)
      allow create: if request.auth != null && request.auth.uid == uid
                    && request.resource.data.role == 'user'
                    && request.resource.data.balance == 300000
                    && (!('portfolio' in request.resource.data)
                        || (request.resource.data.portfolio is map
                            && request.resource.data.portfolio.size() == 0));
      // El usuario solo puede actualizar su documento KYC (URL de Cloudinary)
      allow update: if request.auth != null && request.auth.uid == uid
                    && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['kycDocumentUrl'])
                    && request.resource.data.kycDocumentUrl is string
                    && request.resource.data.kycDocumentUrl.matches('https://res.cloudinary.com/.*');
      // El admin solo puede tocar el balance y el portafolio (al aprobar transacciones)
      allow update: if isAdmin()
                    && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['balance', 'portfolio'])
                    && request.resource.data.balance is number
                    && request.resource.data.balance >= 0
                    && (!('portfolio' in request.resource.data) || request.resource.data.portfolio is map);
      allow delete: if false;
    }

    match /transactions/{transactionId} {
      allow create: if request.auth != null
                    && request.resource.data.userId == request.auth.uid
                    && request.resource.data.status == 'PENDING'
                    && request.resource.data.type in ['BUY', 'SELL']
                    && request.resource.data.amount > 0
                    && request.resource.data.executionPrice > 0
                    && request.resource.data.timestamp == request.time;
      allow read: if request.auth != null
                  && (resource.data.userId == request.auth.uid || isAdmin());
      allow update: if isAdmin()
                    && resource.data.status == 'PENDING'
                    && request.resource.data.status in ['APPROVED', 'REJECTED']
                    && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['status']);
      allow delete: if false;
    }
  }
}
```

</details>

### 5. Ejecutar

Abre el proyecto en Android Studio, espera la sincronización de Gradle y ejecuta la app en un emulador o dispositivo.

### 6. Crear un administrador

1. Registra un usuario desde la app.
2. En Firestore, abre `users/{uid}` y cambia el campo `role` de `user` a `admin`.
3. Cierra sesión y vuelve a iniciarla: se abrirá el panel de administración.

> ⚠️ El saldo inicial de la regla `create` (`300000`) debe coincidir con `INITIAL_BALANCE` en `data/model/User.kt`; si no coinciden, el registro falla con "Sin permisos".

### 7. Probar el hardware en el emulador

- **Huella**: en el emulador ve a **Ajustes → Seguridad** y registra una huella. Cuando aparezca "Autorizar Transacción", simula el toque desde **Extended controls (⋯) → Fingerprint → Touch sensor**.
- **Cámara**: en **Device Manager → Edit → Advanced Settings**, asigna a la cámara trasera `VirtualScene` o `Webcam0`.
- En un dispositivo físico basta con tener una huella registrada y conceder el permiso de cámara la primera vez.

> 💡 **Mercado cerrado:** el mercado forex no emite cotizaciones de viernes por la tarde a domingo. Para probar en fin de semana, cambia el símbolo en `util/MarketConfig.kt` a `BINANCE:BTCUSDT` / `BTCUSDT` (cripto opera 24/7).

---

## 📸 Capturas de pantalla

> Capturas tomadas en un **dispositivo Android físico**.

<table>
  <tr>
    <td align="center"><b>Inicio de sesión</b></td>
    <td align="center"><b>Panel de Usuario</b></td>
  </tr>
  <tr>
    <td><img src="screenshots/Inicio%20de%20sesion.jpeg" alt="Inicio de sesión" width="300"></td>
    <td><img src="screenshots/Dashboard%20User.jpeg" alt="Panel de Usuario con saldo, mercado en vivo y portafolio" width="300"></td>
  </tr>
  <tr>
    <td align="center"><b>Historial de movimientos</b></td>
    <td align="center"><b>Panel de Administrador</b></td>
  </tr>
  <tr>
    <td><img src="screenshots/Historial%20de%20movimientos%20user.jpeg" alt="Historial de transacciones del usuario" width="300"></td>
    <td><img src="screenshots/Dashboard%20Admin.jpeg" alt="Panel de auditoría del administrador" width="300"></td>
  </tr>
  <tr>
    <td align="center" colspan="2"><b>Verificación KYC: cámara o galería</b></td>
  </tr>
  <tr>
    <td align="center" colspan="2"><img src="screenshots/Subir%20documento.jpeg" alt="Verificación KYC con botones Tomar Foto y Subir desde Galería" width="300"></td>
  </tr>
</table>

---

## ⚠️ Notas y limitaciones

- **Validación del 2.5 %**: se ejecuta en el cliente del administrador. Las reglas de Firestore controlan qué campos puede modificar cada rol, pero no recalculan el margen de precio ni el saldo. En producción, esta lógica debería moverse a una **Cloud Function**.
- **API Keys en `BuildConfig`**: evitan subir secretos al repositorio, pero quedan dentro del APK. Por eso se usa un *unsigned preset* en Cloudinary (nunca el API Secret).
- **Documentos KYC**: con un preset *unsigned* la imagen es accesible por su URL. Para producción se recomienda un preset con *delivery type* **authenticated**.
- **Horario de mercado**: las cotizaciones forex solo llegan con el mercado abierto.
- **Biometría solo en el cliente**: la huella protege la interfaz, pero no usa un `CryptoObject` ni se verifica en el servidor; las reglas de Firestore no saben si una orden se autorizó con huella. En producción, la confirmación debería firmarse con una clave del Android Keystore y validarse en el backend.
- **Validación del portafolio en reglas**: Firestore no permite recorrer los valores de un mapa en las reglas, así que la cantidad de cada activo la controla la auditoría, que nunca deja unidades negativas.
- **Cuentas anteriores al portafolio**: no tienen el campo `portfolio`, así que empiezan con 0 unidades; las compras aprobadas antes de este cambio no se reflejan en él.

---

## 👨‍💻 Autor

[**Santiago Pulgarín Sánchez**](https://github.com/s-pulgarin)  
Estudiante de desarrollo de software

---

<div align="center">
Hecho con Kotlin y Jetpack Compose 💚
</div>