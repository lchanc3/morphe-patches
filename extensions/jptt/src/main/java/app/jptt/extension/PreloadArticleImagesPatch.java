package app.jptt.extension;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.concurrent.Executor;

/**
 * Downloads every image of the article being read into Fresco's cache up front,
 * instead of waiting for each image to be scrolled into view.
 *
 * <p>JPTT builds a {@code PicItem} for every image link in the article body and
 * only starts the download when the list row is bound. This class is called
 * whenever the article's item list changes, asks the fragment for the full list
 * of image URLs, and warms the cache with the ones it has not seen yet.
 */
@SuppressWarnings("unused")
public final class PreloadArticleImagesPatch {

    /**
     * Cap on how many images of a single article are preloaded.
     * Overwritten by the patch with the value of its "preloadLimit" option.
     */
    private static int maxImagesPerArticle = 60;

    /** How many URLs to remember so the same image is not requested twice. */
    private static final int REQUEST_HISTORY_SIZE = 512;

    private static final LinkedHashSet<String> requestedUrls = new LinkedHashSet<>();

    /** Runs the completion callback inline; it does nothing but release the result. */
    private static final Executor INLINE_EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    };

    /** Called from the patched {@code JpttApplication.onCreate()}. */
    public static void setMaxImagesPerArticle(int max) {
        maxImagesPerArticle = max;
    }

    /**
     * Called from the patched {@code ArticleFragment} with the result of
     * {@code getAllPicUrl()}.
     */
    public static void preload(ArrayList<String> urls) {
        try {
            if (urls == null || urls.isEmpty() || maxImagesPerArticle <= 0) {
                return;
            }

            Context context = JpttContext.get();
            if (context == null || !isImageDownloadAllowed(context)) {
                return;
            }

            ImagePipeline pipeline = Fresco.getImagePipeline();
            int limit = Math.min(urls.size(), maxImagesPerArticle);

            for (int i = 0; i < limit; i++) {
                String url = urls.get(i);
                if (url == null || url.isEmpty()) {
                    continue;
                }
                if (!rememberUrl(url)) {
                    continue;
                }
                fetch(pipeline, url);
            }
        } catch (Throwable ex) {
            Log.e(JpttContext.LOG_TAG, "Could not preload article images", ex);
        }
    }

    /** @return true if this URL has not been requested before. */
    private static boolean rememberUrl(String url) {
        synchronized (requestedUrls) {
            if (!requestedUrls.add(url)) {
                return false;
            }
            if (requestedUrls.size() > REQUEST_HISTORY_SIZE) {
                Iterator<String> oldest = requestedUrls.iterator();
                oldest.next();
                oldest.remove();
            }
            return true;
        }
    }

    private static void forgetUrl(String url) {
        synchronized (requestedUrls) {
            requestedUrls.remove(url);
        }
    }

    private static void fetch(ImagePipeline pipeline, final String url) {
        // Low priority, so a picture the user is actually looking at is still
        // fetched first. The encoded image is what Fresco keeps on disk, so this
        // is also what makes scrolling back to an image instant.
        DataSource dataSource = pipeline.fetchEncodedImage(
                ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
                        .setRequestPriority(Priority.LOW)
                        .build(),
                null);

        dataSource.subscribe(new BaseDataSubscriber() {
            @Override
            protected void onNewResultImpl(DataSource dataSource) {
                // Nothing to do. Fresco has cached the image and closes the
                // data source for us once it is finished.
            }

            @Override
            protected void onFailureImpl(DataSource dataSource) {
                // Allow a later attempt; the image may just have timed out.
                forgetUrl(url);
            }
        }, INLINE_EXECUTOR);
    }

    /**
     * Mirrors {@code SettingsActivity.getAutoLoadPictures()} so preloading obeys
     * the app's own "自動載入圖片" and "只在 Wi-Fi 下載入" settings.
     */
    private static boolean isImageDownloadAllowed(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);

        if (!preferences.getBoolean("auto_load_pictures", true)) {
            return false;
        }
        if (!preferences.getBoolean("auto_load_pictures_only_on_wifi", true)) {
            return true;
        }
        return isOnWifi(context);
    }

    private static boolean isOnWifi(Context context) {
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = manager.getActiveNetwork();
            if (network == null) {
                return false;
            }
            NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
            return capabilities != null
                    && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        }

        NetworkInfo info = manager.getActiveNetworkInfo();
        return info != null && info.isConnected()
                && info.getType() == ConnectivityManager.TYPE_WIFI;
    }

    private PreloadArticleImagesPatch() {
    }
}
