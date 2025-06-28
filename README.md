
# 💰 PocketGuard – Personal Finance Tracker

PocketGuard is a lightweight and offline-first Android application built with Kotlin. It helps users manage their daily finances by tracking income, expenses, and category-wise budgets. All data is stored locally using SharedPreferences — no internet connection required.

---

## 📱 Features

- 🧾 Add and track income and expense transactions
- 📊 Category-wise summary of financial records
- 🚨 Budget limits with notifications when exceeded
- 🔄 Backup and restore data 
- 📆 Daily and monthly expense views

---

## 📸 Screenshots

<table>
  <tr>
    <td><img src="dashboard.png" width="250"/><br><center><sub>Dashboard</sub></center></td>
    <td><img src="addtrns.png" width="250"/><br><center><sub>Add Transaction</sub></center></td>
    <td><img src="addexpensspiner.png" width="250"/><br><center><sub>Expense Categories</sub></center></td>
  </tr>
  <tr>
    <td><img src="addincomspiner.png" width="250"/><br><center><sub>Income Categories</sub></center></td>
    <td><img src="catabudnotifi.png" width="250"/><br><center><sub>Budget Exceeded Notification</sub></center></td>
    <td><img src="addcatabudget.png" width="250"/><br><center><sub>Add Budget Category</sub></center></td>
  </tr>
  <tr>
    <td><img src="currency.png" width="250"/><br><center><sub>Currency Settings</sub></center></td>
    <td><img src="backup.png" width="250"/><br><center><sub>Backup Created</sub></center></td>
    <td><img src="changepcode.png" width="250"/><br><center><sub>Change Passcode</sub></center></td>
  </tr>
  <tr>
    <td colspan="3" align="center">
      <img src="code.png" width="250"/><br><center><sub>Enter Passcode</sub></center>
    </td>
  </tr>
</table>

---

## ⚙️ Tech Stack

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![XML](https://img.shields.io/badge/XML-E44D26?style=for-the-badge&logo=xml&logoColor=white)
![SharedPreferences](https://img.shields.io/badge/SharedPreferences-Important?style=for-the-badge&logo=google&logoColor=white)

---

## 🚀 Getting Started

### Requirements

- Android Studio Arctic Fox or newer
- Kotlin 1.8+
- Android SDK 29+

### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/Ravidujee19/PocketGuard.git
   ```
2. Open the project in Android Studio
3. Click **Sync Gradle** and build the project
4. Run the app on your emulator or Android device

---

## 🛠 Backup & Restore

PocketGuard allows you to backup your financial data to a local `.json` file and restore it when needed — ideal for offline usage and manual transfers.

---

## 📃 License

This project is open-source and available under the [MIT License](LICENSE).

---

## 🤝 Contributing

PocketGuard is an open-source project — contributions from the community are welcome and appreciated!

If you'd like to improve the app, fix a bug, or suggest new features:

1. Fork the repository
2. Create your feature branch:
   ```bash
   git checkout -b feature/YourFeature
   ```
3. Commit your changes:
   ```bash
   git commit -m "Add YourFeature"
   ```
4. Push to the branch:
   ```bash
   git push origin feature/YourFeature
   ```
5. Open a Pull Request

Make sure your changes are well tested and documented. Let’s build a better finance tool together!
