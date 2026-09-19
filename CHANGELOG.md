# Changelog

## 1.2.1

- **Preload article images**: fetch one image at a time on a background thread
  instead of submitting the whole article at once, which crashed the app on
  image heavy articles. Failures, including OutOfMemoryError, now drop that one
  image instead of taking the app down.

## 1.2.0

- Add **Disable Play license check**: PairIP's license check fails on any
  re-signed build and closes the app on launch.

## 1.1.0

- Add **Fix photo upload in cloned installs**: derives JPTT's FileProvider
  authority from the running package, so `上傳圖片 → 拍照` keeps working when the
  "Clone app" patch renames the package.
- Fix `created_at` in `patches-bundle.json` so Morphe Manager can parse it.

## 1.0.3

- Publish `patches-list.json` so other tooling can read the patch list.

## 1.0.2

- Shrink the extension from 2.2 MB to 6 KB by keeping only `app.jptt.**`,
  instead of merging a second copy of the Kotlin stdlib into the APK.

## 1.0.0

- **Preload article images** — download an article's images up front instead of
  waiting for each one to be scrolled into view.
- **Increase image cache size** — raise Fresco's 40 MB disk cache so images are
  not evicted and re-downloaded when scrolling back.
- **More recent searches** — show more recent search keywords in the article
  search dialog.
