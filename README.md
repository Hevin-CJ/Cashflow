# 💸 CashFlow — Modern Personal Finance & Expense Tracker

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-API%2024%2B-3DDC84.svg?style=flat&logo=android)](https://developer.android.com/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.6.0-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Backend](https://img.shields.io/badge/Backend-Ktor%202.x-000000.svg?style=flat&logo=ktor)](https://ktor.io/)
[![Database](https://img.shields.io/badge/Database-Room%20%7C%20MongoDB%20Atlas-47A248.svg?style=flat&logo=mongodb)](https://www.mongodb.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**CashFlow** is a modern, high-performance, offline-first personal finance and expense tracking Android application. Built with 100% **Jetpack Compose**, **Clean Architecture**, and **MVVM**, CashFlow enables users to manage transactions, budgets, credit cards, recurring subscriptions, scan paper receipts with AI/ML, and seamlessly sync data with a custom **Ktor + MongoDB** backend server.

<img width="154" height="346" alt="home" src="https://github.com/user-attachments/assets/433c7e32-1d35-40e6-8244-0649e5461edf" />
<img width="154" height="346" alt="statistics" src="https://github.com/user-attachments/assets/e88e30d0-db89-4c01-9c34-62b79b338385" />
<img width="154" height="346" alt="cards" src="https://github.com/user-attachments/assets/8b0d9481-6c63-45e8-89a8-6edb1b3153de" />
<img width="154" height="346" alt="profile" src="https://github.com/user-attachments/assets/27d85bf3-1e34-440d-8732-e153f61c5aad" />
<img width="154" height="346" alt="addtrans" src="https://github.com/user-attachments/assets/9c2be905-ad49-44dc-a23b-66a278c26be0" />
---

## ✨ Features

- 📊 **Interactive Financial Analytics**: View spending distributions with custom Compose donut charts, monthly net savings, category breakdowns, and transaction trends.
- ⚡ **Offline-First Synchronization**: Instant local persistence using Room SQLite DB. Background synchronization to MongoDB Atlas powered by `WorkManager` and a dual-ID mapping engine (`id: Int` local, `serverId: String?` MongoDB ObjectId).
- 📷 **AI Receipt & Barcode Scanner**: Powered by **Google ML Kit Text & Barcode Recognition**. Scan physical receipts or gallery photos to automatically extract item details and prices.
- 💳 **Credit Card & Budget Manager**: Manage credit card balances with auto-detected card brands (Visa, Mastercard, Amex, RuPay), dynamic gradient cards, and monthly budget progress bars.
- 🔄 **Subscription Tracker**: Manage recurring expenses (Daily, Weekly, Monthly, Yearly) with automatic background logging and renewal alerts.
- 💱 **Live Currency Converter**: Real-time and offline exchange rate conversion between major world currencies with custom numpad and dynamic rates caching.
- 📲 **UPI Contact Pay & App Chooser**: Direct integration with UPI payment apps (Google Pay, PhonePe, Paytm, BHIM) for instant payment redirects to phone contacts or VPAs.
- 📱 **Glance Home Screen Widget**: Android Glance AppWidget displaying real-time net balance directly on the device home screen.
- 🔒 **Enterprise-Grade Security**:
  - **Certificate Pinning**: SSL/TLS pinning against Google Trust Services (GTS) and GlobalSign root certificates.
  - **RSA Request Signing**: Outbound HTTP requests are cryptographically signed via `SignatureInterceptor` and verified by Ktor middleware to prevent request tampering.
  - **JWT Authentication & bcrypt**: Secure login/registration with encrypted local token storage.
- 🚀 **Performance Optimized**: 100% Declarative Compose UI, `@Immutable` state classes, `derivedStateOf` recomposition guards, and **Baseline Profiles** for zero scroll stutter and fast startup.

---

## 🏗 Architecture & Tech Stack

The application follows **Clean Architecture** principles separated into 3 core layers (**UI**, **Domain**, and **Data**):

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ANDROID APP (FRONTEND)                          │
│                                                                        │
│   Jetpack Compose UI  ◄──▶  ViewModels  ◄──▶  UseCases (Domain)        │
│                                                     │                  │
│                                                     ▼                  │
│                                          Repositories (Data)           │
│                                            ├── Room DB (Local SQLite)  │
│                                            └── Retrofit + OkHttp       │
└───────────────────────────────────────────────────┬────────────────────┘
                                                    │ HTTPS / REST (RSA Signed)
                                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     KTOR BACKEND (GOOGLE CLOUD RUN)                    │
│                                                                        │
<<<<<<< HEAD
│   Netty Engine  ──▶  Ktor Routes  ──▶  Koin DI  ──▶  KMongo Coroutines │
=======
│   Netty Engine  ──▶  Ktor Routes  ──▶  Koin DI  ──▶  KMongo Coroutines  │
>>>>>>> 14a172e (feat: in-app updater, release workflow, and performance fixes)
└───────────────────────────────────────────────────┬────────────────────┘
                                                    │ Mongo URI
                                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        DATABASE (MONGODB ATLAS)                        │
│   Collections: users, transactions, cards, recurring_expenses, budgets │
└────────────────────────────────────────────────────────────────────────┘
```

### Tech Stack Breakdown

#### **Android Client App**
* **UI**: 100% Jetpack Compose, Material 3, Custom Canvas Drawing, Navigation Compose
* **Architecture**: MVVM + Clean Architecture + Unidirectional Data Flow (`UiState` + `StateFlow`)
* **Dependency Injection**: Hilt
* **Local Storage**: Room DB, SQLite, SharedPreferences
* **Networking**: Retrofit 2, OkHttp 4, Gson, Certificate Pinner
* **Asynchronous**: Kotlin Coroutines, StateFlow, SharedFlow, Flow
* **Background Processing**: WorkManager (`SyncTransactionWorker`, `RecurringExpenseWorker`, `SyncBudgetWorker`)
* **Machine Learning**: Google ML Kit Text Recognition & Barcode Scanning
* **Widget**: Android Glance AppWidget
* **Performance**: Macrobenchmark, Baseline Profiles

#### **Backend Server**
* **Framework**: Ktor 2.x (Kotlin JVM 21), Netty Engine
* **Dependency Injection**: Koin
* **Database**: MongoDB Atlas, KMongo Coroutine Driver (`kmongo-coroutine-serialization`)
* **Security**: JWT Authentication, bcrypt password hashing, RSA Signature Verification Middleware
* **Hosting**: Docker, Google Cloud Run

---

## 🔄 Offline-First Sync Architecture

CashFlow uses a dual-ID strategy to guarantee zero UI latency when offline:

| State | Local `id` (`Int`) | Remote `serverId` (`String?`) | `isSynced` | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Created Offline** | `42` (Room Auto-gen) | `null` | `false` | Saved in 1ms locally. UI updates immediately. |
| **Synced to MongoDB** | `42` | `"667bc9a4f29e18001b92a4e2"` | `true` | WorkManager syncs to Ktor, receiving MongoDB ObjectId. |

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Jellyfish (2023.3.1) or newer
- **JDK**: 17 or 21
- **Android SDK**: API 34 (Minimum SDK: 24)

### Building the Android App

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/CashFlow.git
   cd CashFlow
   ```

2. Open the project in **Android Studio**.

3. Sync Gradle and build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```

4. Run unit tests:
   ```bash
   ./gradlew test
   ```

---

## 💻 Running the Ktor Backend

The backend server source code is located in the `cashflow-backend` folder.

1. Navigate to the backend directory:
   ```bash
   cd cashflow-backend
   ```

2. Set environment variables (or use defaults):
   ```bash
   export mongo_uri="mongodb+srv://<user>:<password>@<cluster>.mongodb.net/"
   export mongo_db="cashflow_ktor_db"
   ```

3. Run the server locally:
   ```bash
   ./gradlew run
   ```

The server will start at `http://0.0.0.0:8080`.

---

## 🧪 Testing & Quality Assurance

- **Unit Tests**: Comprehensive unit tests covering ViewModels, UseCases, Repositories, Mappers, and Parsers.
- **UI Tests**: Compose UI tests (`ComposeTestRule`) covering screen interactions, dialogs, and navigation flows.
- **Baseline Profiles**: Includes a `:baselineprofile` module using Macrobenchmark to generate startup and frame-render optimizations.

---

## 🛡 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

Developed by **Hevin CJ**.  
Feel free to reach out or contribute! 🚀
