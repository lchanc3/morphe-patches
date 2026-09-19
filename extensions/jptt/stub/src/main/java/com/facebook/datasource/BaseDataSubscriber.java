package com.facebook.datasource;

/**
 * Stub of the class already present in the JPTT APK.
 *
 * <p>The bodies here are never compiled into the extension; only the signatures
 * matter. At runtime the real Fresco implementation is used, which closes the
 * data source after {@code onNewResultImpl} / {@code onFailureImpl} returns.
 */
@SuppressWarnings("ALL")
public abstract class BaseDataSubscriber implements DataSubscriber {

    @Override
    public void onCancellation(DataSource dataSource) {
    }

    @Override
    public void onFailure(DataSource dataSource) {
    }

    @Override
    public void onNewResult(DataSource dataSource) {
    }

    @Override
    public void onProgressUpdate(DataSource dataSource) {
    }

    protected abstract void onFailureImpl(DataSource dataSource);

    protected abstract void onNewResultImpl(DataSource dataSource);
}
