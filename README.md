# 🧩 lchanc3's patches

個人用的 [Morphe](https://github.com/MorpheApp) patch bundle。

## ❓ About

用 Morphe 的 patcher 寫的自用 patch，跟原版
[morphe-patches](https://github.com/MorpheApp/morphe-patches) 互不相干，
各自是獨立的 bundle，可以同時加進 Morphe Manager。

手機上開這個連結把這裡加進 Manager 的來源：

<https://morphe.software/add-source?github=lchanc3/morphe-patches>

想自己編或加新的 patch，看 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 🩹 Patches

<!-- PATCHES_START -->
> **[v1.2.4](https://github.com/lchanc3/morphe-patches/releases/tag/v1.2.4)**&nbsp;&nbsp;•&nbsp;&nbsp;7 patches&nbsp;&nbsp;•&nbsp;&nbsp;1 app

<details open>
<summary>📦 JPTT&nbsp;&nbsp;•&nbsp;&nbsp;7 patches</summary>

**Supported versions:**

| 3.8.4 | 🧪&nbsp;any |
| :---: | :---: |

| Patch | Description | Options |
|---|---|---|
| **Disable Play license check** | Stops PairIP's license check from running. It verifies with the Play Store that the install is the one Google shipped, which a re-signed build never is, so without this the app shows "Something went wrong" on launch and closes itself. |  |
| **Fix article list loading** | Filters out the ANSI escape sequences JPTT's terminal emulator cannot parse, which PTT started sending around every screen repaint. Without this, opening any board corrupts the parsed screen and the article list never gets past 載入中 before giving up with 載入失敗. |  |
| **Fix photo upload in cloned installs** | Derives JPTT's FileProvider authority from the running package instead of the hardcoded com.joshua.jptt.provider, so 上傳圖片 → 拍照 keeps working when the "Clone app" patch renames the package. Changes nothing on a normal install. |  |
| **Increase image cache size** | Raises Fresco's image disk cache from its 40 MB default, so images you already looked at are not evicted and re-downloaded when you scroll back. | `cacheSizeMb` |
| **More recent searches** | Shows more of your recently used search keywords in the article search dialog, instead of the five and fifteen the app hardcodes. | `boardKeywordCount`<br>`allKeywordCount` |
| **Preload article images** | Downloads every image of the article you are reading up front, instead of starting each download only once you scroll it into view. Obeys the app's own 自動載入圖片 / 只在 Wi-Fi 下載入 settings. | `preloadLimit`<br>`concurrency` |
| **Wrap recent searches** | Lays the 最近看板搜尋 / 最近搜尋 keywords out over several lines in the article search dialog, instead of one line you have to scroll sideways through. A list longer than a quarter of the screen scrolls within its own strip. |  |

</details>

<!-- PATCHES_END -->

## 📝 Notes

各個 patch 在做什麼，上面的表格就是全部了。這裡只放從表格看不出來、又會害你卡住的事。

### JPTT

- **Disable Play license check 一定要勾。** JPTT 用 Google 的 PairIP 保護，
  `LicenseContentProvider.onCreate()` 會在啟動時向 Play 驗證這份安裝是不是 Google 發的。
  重簽過的 build 一定驗不過，`LicenseActivity` 會跳「Something went wrong」，只有 Close
  可按，按下去就 `System.exit(0)`。這跟有沒有用 Clone app 無關，所有 patch 過的版本都會遇到。

- **搭配 Morphe 的 Clone app patch 時，它自己那兩個選項都要開**，否則裝不起來：
  *Update providers* 不開會撞 `INSTALL_FAILED_CONFLICTING_PROVIDER`（JPTT 有六個 provider
  authority 還叫 `com.joshua.jptt.*`）；*Update permissions* 不開會撞
  `INSTALL_FAILED_DUPLICATE_PERMISSION`（`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` 的
  protectionLevel 是 signature）。開了 Update providers 之後「上傳圖片 → 拍照」會壞掉，
  那正是 **Fix photo upload in cloned installs** 要修的。

- **文章列表打不開是 PTT 那邊變的。** PTT 現在用 synchronized output
  （`ESC[?2026h` / `ESC[?2026l`）把每次螢幕重畫包起來，JPTT 的終端機模擬器沒有 `h` / `l`
  這兩個結束字元的 case，序列永遠不結束，就一路吃掉後面的畫面內容，欄位全歪掉。
  **Fix article list loading** 就是在 escape 進到解析器之前把它濾掉。想自己看的話：
  關於JPTT → 長按 JPTT 圖示 → 終端機內容。

- 修改過的 APK 會用新的簽章，**不能**直接蓋掉官方版本安裝。要嘛先移除原本的 JPTT
  （先記下帳號設定），要嘛用 Clone app 改 package name 另裝一份。
