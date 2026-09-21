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

- **文章列表卡在 載入中、畫面欄位歪掉，是 JPTT 的 escape 解析器只做半套。**
  `JSocketSimple.startConnection()` 只對 `A B C D H J K m` 這幾個結束字元有 case，
  其他一律往 32 字的 buffer 塞，序列永遠不結束，就一路吃掉後面的畫面內容。PTT 從
  2026/09/20 起陸續加了 DEC2026 同步輸出（`ESC[?2026h` / `ESC[?2026l`）、SGR Mouse、
  SGR 66 一字雙色、Cursor Position Report（`ESC[6n`），並在 PttCurrent 板請各家 client
  乾脆把 ECMA-48 的 CSI 讀完整：`\x1B\[[0-?]*[ -/]*[@-~]`，讀完丟掉也行。
  **JPTT 3.8.5 只補了「`?` 開頭再收到 `h` / `l`」那一種**，所以 DEC2026 和 SGR Mouse
  沒事，但 `ESC[6n`（PTT1 9/27 上線，登入時用來偵測終端機編碼）的結束字元是 `n`，一樣
  會中。**Fix article list loading 在 3.8.5 上要繼續勾**，它做的就是 PTT 建議的那件事：
  整段 CSI 讀完，emulator 沒實作的就丟掉。想自己看的話：關於JPTT → 長按 JPTT 圖示 →
  終端機內容。

- **PTT 的介面改版會打到 JPTT，而且沒有 patch 擋得住。** PTT1 10/04 換標題列與主選單
  狀態列，10/18 換各列表的左下標籤：`　選擇看板　` → `　看板列表　`／`　我的最愛　`，
  `　文章選讀　` → `　文章列表　`／`　系列文章　`／`　文摘列表　`，`　鴻雁往返　` →
  `　信件列表　`。JPTT 的狀態機是 `lineNContains(23, 1, 9, "文章選讀")` 這樣寫死比對的，
  3.8.5 的 dex 裡還是只有舊字串。到時候進看板、收信、我的最愛都會卡住，要等官方更新，
  或是等這裡生一個改字串比對的 patch。

- 修改過的 APK 會用新的簽章，**不能**直接蓋掉官方版本安裝。要嘛先移除原本的 JPTT
  （先記下帳號設定），要嘛用 Clone app 改 package name 另裝一份。
