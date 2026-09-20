# JPTT Morphe Patches

個人用的 [Morphe](https://github.com/MorpheApp) patch bundle，針對 JPTT（`com.joshua.jptt`，開發版本 3.8.4）。

## 這個 bundle 有什麼

| Patch | 做什麼 | 可調選項 |
| --- | --- | --- |
| **Preload article images** | 進入文章後就把整篇正文的圖片抓下來，不再等你捲到才開始下載。 | `preloadLimit`：一篇最多預載幾張（預設 60）、`concurrency`：同時抓幾張（預設 4） |
| **Increase image cache size** | 把 Fresco 圖片磁碟快取從預設的 40 MB 拉大，捲走再捲回來不用重抓。 | `cacheSizeMb`：預設 512 MB |
| **More recent searches** | 搜尋對話框的「最近看板搜尋 / 最近搜尋」保留更多關鍵字。 | `boardKeywordCount`（預設 15，原本 5）、`allKeywordCount`（預設 30，原本 15） |
| **Wrap recent searches** | 搜尋對話框的最近搜尋關鍵字改成一行放不下就往下排，不用再左右捲。單一清單超過螢幕四分之一高就在自己的區塊裡上下捲。 | 無 |
| **Fix article list loading** | 過濾掉 JPTT 終端機解析不了的 ANSI escape sequence。PTT 現在每次重畫都會送 `ESC[?2026h` / `ESC[?2026l`，沒有這個 patch 的話點任何看板都會卡在「載入中」然後變成「載入失敗」。 | 無 |
| **Fix photo upload in cloned installs** | 讓 FileProvider authority 改成跟著實際 package 名走，配 Clone app 用。 | 無 |
| **Disable Play license check** | 停掉 PairIP 的授權檢查。**任何重簽的 build 都需要**，否則一開啟就跳「Something went wrong」然後自己關掉。 | 無 |

推文裡的圖片也一起被涵蓋：JPTT 對正文和推文的圖片用的是同一條 `PicItem` 路徑，
`getAllPicUrl()` 兩者都會回傳。

### Preload 的行為

- 掛在 `ArticleFragment.notifyDataSetChanged()` 和 `showListPartial()` 上，
  每次文章的項目清單有變動就把目前已知的圖片網址丟給 extension。
- Extension 用 `Fresco.getImagePipeline().fetchEncodedImage(...)` 以 `Priority.LOW`
  預抓，所以不會跟你正在看的那張圖搶頻寬；抓到的編碼圖片正是 Fresco 存在磁碟上的東西。
- 已經送出過的網址會記住（最近 512 筆），不會重複請求；失敗的會忘掉，之後有機會重試。
- **會遵守 App 自己的設定**：讀 JPTT 的 `auto_load_pictures` 與
  `auto_load_pictures_only_on_wifi`，所以你原本設「只在 Wi-Fi 下載入圖片」時，
  行動網路下不會偷偷預載。

### 為什麼文章列表會壞掉

JPTT 是用 24x80 的終端機畫面去刮 PTT 的內容，欄位位置都寫死。
`JSocketSimple.startConnection()` 解 CSI 序列的方式是 switch 它拿到的字元：
`A B C D H J K m` 有處理並結束序列，**其他一律往一個 32 字元的 buffer 塞**，
包含它沒寫 case 的結束字元。序列永遠不結束，就會一路吃掉後面的畫面內容，
直到 buffer 滿了才放棄 —— 這時候畫面已經歪掉，escape 的碎片還被當成文字印出來。

PTT 現在用 synchronized output（`ESC[?2026h` / `ESC[?2026l`）把每次重畫包起來，
結束字元是 `h` 和 `l`。可以用 App 自己的隱藏功能看到後果：
**關於JPTT → 長按 JPTT 圖示 → 終端機內容**，看板列表那一頁會長這樣：

```
    ★    9 1/01 mkflyk23     □ [公告] 關於徵求分流文章之爭議／黑名單專區
[?2026h  32 3/10 mkflyk23     □ [公告] 免空帳號買賣置底文
 文章選讀  (y)回應(X)推文(^X)轉錄 (=[]<>)相關主題(/?a)找標題/作者 (b)進板畫面  [
```

`[?2026h` 正好蓋掉編號欄那 7 格。`getToBoard()` 按 `i` 之後在一個沒有 timeout 的
迴圈裡等第 24 行出現「請按任意鍵繼續」，等不到就一直空轉。

這個 patch 把 `JSocketSimple.in` 的每一個賦值都包一層 reader
（WebSocket、一般 socket、SSH 三種連線各一處），在 escape 進到解析器之前就把
它處理不了的序列濾掉。Synchronized output 只是告訴真的終端機什麼時候該呈現畫面，
沒有東西要模擬，丟掉不會少任何內容。

## 編譯

### 需要

- JDK 17+（已確認 Temurin 25 可用）
- Android SDK（`local.properties` 已指到 `C:\Users\jason\AppData\Local\Android\Sdk`）
- 一組能讀 GitHub Packages 的憑證 —— Morphe 的 Gradle plugin 放在
  `maven.pkg.github.com`，需要 `read:packages` scope。

### 設定 GitHub 憑證

到 <https://github.com/settings/tokens/new?scopes=read:packages&description=Morphe> 產生
classic token，寫進 `~/.gradle/gradle.properties`：

```properties
gpr.user = 你的GitHub帳號
gpr.key  = ghp_xxxxxxxxxxxxxxxxxxxx
```

或者用環境變數 `GITHUB_ACTOR` / `GITHUB_TOKEN`（CI 走的就是這條）。

> 光跑 `gh auth refresh -s read:packages` 是不夠的。`settings.gradle.kts` 的
> `pluginManagement` 有 `gh auth token` 的 fallback，那只夠解析到 plugin 本身；
> plugin 套用時又會自己加一次同一個 repo，而它只看 `gpr.user` / `gpr.key` 和
> `GITHUB_ACTOR` / `GITHUB_TOKEN`，找不到就丟一個沒有訊息的
> `IllegalArgumentException`。

### 建置

```bash
./gradlew buildAndroid
```

產出在 `patches/build/libs/patches-*.mpp`。

## 套用

### 手機（Morphe Manager）

Manager 只接受 GitHub URL / deep link 形式的 patch 來源，所以要先發一個 release：

1. GitHub 上這個 repo 的 **Actions** 分頁 → **Release** → **Run workflow**，填版本號（例如 `1.0.0`）。
2. Workflow 會編譯出 `patches-1.0.0.mpp`、更新 `patches-bundle.json`、建立 release。
   CI 用的是 Actions 自動發的 `GITHUB_TOKEN`，不需要你另外準備 PAT。
3. 手機上開這個連結把來源加進 Manager：

   <https://morphe.software/add-source?github=lchanc3/morphe-patches>

4. Manager 裡選 JPTT 的 APK，勾要用的 patch，需要的話在 Expert mode 調選項。

> **一定要勾 Disable Play license check。** JPTT 用 Google 的 PairIP 保護，
> `LicenseContentProvider.onCreate()` 會在 App 啟動時向 Play 驗證這份安裝是不是
> Google 發出來的。重簽過的 build 一定驗不過，`LicenseActivity` 會跳
> 「Something went wrong / Check that Google Play is enabled…」，只有 Close 可按，
> 按下去就 `System.exit(0)`。這跟有沒有用 Clone app 無關，所有 patch 過的版本都會遇到。

### 搭配 Clone app（跟原版並存）

用 Morphe 的 **Clone app** patch 把 package 改成 `com.joshua.jptt.morphe` 時，記得把它的
兩個選項都打開，否則裝不起來：

- **Update providers** → 不開會撞 `INSTALL_FAILED_CONFLICTING_PROVIDER`，
  因為 JPTT 的六個 provider authority（FileProvider、AdMob、Firebase、androidx-startup、
  Facebook、pairip）都還叫 `com.joshua.jptt.*`。
- **Update permissions** → JPTT 宣告了 `com.joshua.jptt.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`，
  protectionLevel 是 `signature`；複製版簽章不同，同名會撞
  `INSTALL_FAILED_DUPLICATE_PERMISSION`。改名是安全的 —— androidx 的 `ContextCompat`
  是用 `getPackageName() + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"` 在執行期組出來的。

開了 Update providers 之後會有一個副作用：`ImageUploadUtil.dispatchTakePictureIntent()`
把 authority 寫死成 `com.joshua.jptt.provider`，manifest 改名後這行就對不上，
「上傳圖片 → 拍照」會噴 `Couldn't find meta-data for provider`。
**Fix photo upload in cloned installs** 這個 patch 就是修這個，記得一起勾。

### 電腦（Morphe Desktop）

把 `patches/build/libs/patches-*.mpp` 當一般 patch bundle 指定給它，搭配 `JPTT_3.8.4.apk`。

修改過的 APK 會用新的簽章，**不能**直接覆蓋官方版本安裝；要先移除原本的 JPTT
（記得先備份 / 記下帳號設定），或改 package name 另裝一份。

## 這些 patch 是怎麼找出來的

JPTT 沒有混淆，類別與方法名稱都是原樣，所以 fingerprint 直接用
`definingClass` + `name` 就夠精準。相關位置：

- `com.joshua.jptt.ArticleFragment` — `getAllPicUrl()`、`notifyDataSetChanged()`、
  `showListPartial()`、`PicItem`
- `com.joshua.jptt.BoardFragment#showSearchDialog()` — 兩次
  `DBHelper.getBoardHistory(ctx, site, true, board, N)`，N 分別是 5 和 15
- `com.facebook.cache.disk.DiskCacheConfig$Builder#<init>` — `mMaxCacheSize = 41943040L`
- `com.joshua.jptt.JSocketSimple#in`、`startConnection()` 裡那個 Runnable 的 CSI 解析，
  以及 `JSocket#getToBoard()`

要自己重新分析的話，`.work/`（已 gitignore）裡有 jadx 反編譯結果與 apktool 的 smali。
