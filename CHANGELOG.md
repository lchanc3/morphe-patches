# Changelog

## 1.2.5

- The bundle is no longer JPTT only. It shows up in Morphe Manager as `lchanc3`
  rather than `JPTT Patches`, and patches now live under
  `app.lchanc3.patches.<app>` with their extensions under
  `app.lchanc3.extension.<app>`, so a second app is a folder beside JPTT's
  instead of a rename. No patch behaves differently.

## 1.2.4

- Add **Wrap recent searches**: the 最近看板搜尋 / 最近搜尋 keywords in the article
  search dialog are laid out over as many lines as they need instead of one line
  you scroll sideways through. `dialog_search.xml` puts each list in a
  `HorizontalScrollView`, which is bearable for the five and fifteen keywords
  JPTT shows and not for the counts **More recent searches** allows. A list
  longer than a quarter of the screen scrolls within its own strip, so a long
  history cannot push the dialog's buttons off the bottom.

## 1.2.3

- Add **Fix article list loading**: PTT now brackets its screen repaints with the
  synchronized output sequences `ESC[?2026h` / `ESC[?2026l`. JPTT's terminal
  emulator has no case for the `h` and `l` final bytes, so it keeps appending to
  its 32 character escape buffer, swallowing the screen content that follows and
  printing fragments of the sequence as visible text. Entering a board never got
  past the wait for 請按任意鍵繼續 and the article list sat at 載入中 until it
  gave up with 載入失敗. This filters the sequences the emulator cannot parse out
  of the stream before they reach it.
- **Preload article images**: implement `onProgressUpdate` on the Fresco
  subscriber. `BaseDataSubscriber` in the APK has no implementation to inherit
  — R8 dropped the empty override because the one subclass it kept declares its
  own — so the first progress report from a download threw
  `AbstractMethodError` on Fresco's network thread, where none of this
  extension's `catch` blocks can see it, and the app died on opening any
  article with images. Also handle `onCancellation`, so a cancelled fetch
  releases its worker immediately instead of after the 90 s timeout.

## 1.2.2

- **Preload article images**: download several images in parallel again, with a
  bounded pool instead of the whole article at once. 1.2.1 over-corrected to one
  at a time, which made preloading pointlessly slow. New `concurrency` option,
  default 4.

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
