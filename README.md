# JPTT Morphe Patches

個人用的 [Morphe](https://github.com/MorpheApp) patch bundle，針對 JPTT（`com.joshua.jptt`，開發版本 3.8.4）。

## 這個 bundle 有什麼

| Patch | 做什麼 | 可調選項 |
| --- | --- | --- |
| **Preload article images** | 進入文章後就把整篇正文的圖片一次抓下來，不再等你捲到才開始下載。 | `preloadLimit`：一篇文章最多預載幾張（預設 60） |
| **Increase image cache size** | 把 Fresco 圖片磁碟快取從預設的 40 MB 拉大，捲走再捲回來不用重抓。 | `cacheSizeMb`：預設 512 MB |
| **More recent searches** | 搜尋對話框的「最近看板搜尋 / 最近搜尋」保留更多關鍵字。 | `boardKeywordCount`（預設 15，原本 5）、`allKeywordCount`（預設 30，原本 15） |
| **Fix photo upload in cloned installs** | 讓 FileProvider authority 改成跟著實際 package 名走，配 Clone app 用。 | 無 |

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

## 編譯

### 需要

- JDK 17+（已確認 Temurin 25 可用）
- Android SDK（`local.properties` 已指到 `C:\Users\jason\AppData\Local\Android\Sdk`）
- 一組能讀 GitHub Packages 的憑證 —— Morphe 的 Gradle plugin 放在
  `maven.pkg.github.com`，需要 `read:packages` scope。

### 設定 GitHub 憑證（擇一）

`settings.gradle.kts` 會依序找：`~/.gradle/gradle.properties` → 環境變數 → `gh auth token`。

**用 GitHub CLI（最省事）**

```bash
gh auth refresh -s read:packages
```

之後直接編譯即可，token 不會落地到專案裡。

**或自己開一個 PAT**

到 <https://github.com/settings/tokens/new?scopes=read:packages&description=Morphe> 產生
classic token，寫進 `~/.gradle/gradle.properties`：

```properties
gpr.user = 你的GitHub帳號
gpr.key  = ghp_xxxxxxxxxxxxxxxxxxxx
```

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

4. Manager 裡選 JPTT 的 APK，勾這三個 patch，需要的話在 Expert mode 調選項。

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

要自己重新分析的話，`.work/`（已 gitignore）裡有 jadx 反編譯結果與 apktool 的 smali。
