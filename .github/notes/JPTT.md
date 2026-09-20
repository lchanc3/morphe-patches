**要注意的：**

- **Disable Play license check 一定要勾。** JPTT 用 Google 的 PairIP 保護，
  `LicenseContentProvider.onCreate()` 會在啟動時向 Play 驗證這份安裝是不是 Google 發的。
  重簽過的 build 一定驗不過，`LicenseActivity` 會跳「Something went wrong」，只有 Close
  可按，按下去就 `System.exit(0)`。這跟有沒有用 Clone app 無關，所有 patch 過的版本都會遇到。

- **搭配 Morphe 的 Clone app patch 時，它自己那兩個選項都要開**，否則裝不起來：
  *Update providers* 不開會撞 `INSTALL_FAILED_CONFLICTING_PROVIDER`（JPTT 有六個 provider
  authority 還叫 `com.joshua.jptt.*`）；*Update permissions* 不開會撞
  `INSTALL_FAILED_DUPLICATE_PERMISSION`（`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` 的
  protectionLevel 是 signature）。開了 Update providers 之後「上傳圖片 → 拍照」會壞掉，
  那正是 **Fix photo upload in clones** 要修的。

- **文章列表打不開是 PTT 那邊變的**，不是 patch 壞了。PTT 現在用 synchronized output
  （`ESC[?2026h` / `ESC[?2026l`）把每次螢幕重畫包起來，JPTT 的終端機模擬器沒有 `h` / `l`
  這兩個結束字元的 case，序列永遠不結束，就一路吃掉後面的畫面內容，欄位全歪掉。
  **Fix article list loading** 就是在 escape 進到解析器之前把它濾掉。想自己看的話：
  關於JPTT → 長按 JPTT 圖示 → 終端機內容。

- 修改過的 APK 會用新的簽章，**不能**直接蓋掉官方版本安裝。要嘛先移除原本的 JPTT
  （先記下帳號設定），要嘛用 Clone app 改 package name 另裝一份。
