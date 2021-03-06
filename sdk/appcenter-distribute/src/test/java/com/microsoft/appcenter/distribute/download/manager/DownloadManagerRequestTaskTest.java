/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import android.app.DownloadManager;
import android.net.Uri;

import com.microsoft.appcenter.distribute.ReleaseDetails;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DownloadManagerRequestTaskTest {

    private static final long DOWNLOAD_ID = 42;

    @Mock
    private ReleaseDetails mReleaseDetails;

    @Mock
    private DownloadManager mDownloadManager;

    @Mock
    private DownloadManager.Request mDownloadManagerRequest;

    @Mock
    private DownloadManagerReleaseDownloader mDownloader;

    private DownloadManagerRequestTask mRequestTask;

    @Before
    public void setUp() {

        /* Mock DownloadManager. */
        when(mDownloadManager.enqueue(eq(mDownloadManagerRequest))).thenReturn(DOWNLOAD_ID);

        /* Mock Downloader. */
        when(mDownloader.getReleaseDetails()).thenReturn(mReleaseDetails);
        when(mDownloader.getDownloadManager()).thenReturn(mDownloadManager);

        /* Create RequestTask. */
        mRequestTask = spy(new DownloadManagerRequestTask(mDownloader));
        when(mRequestTask.createRequest(any(Uri.class))).thenReturn(mDownloadManagerRequest);
    }

    @Test
    public void downloadStarted() {
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(false);

        /* Perform background task. */
        mRequestTask.doInBackground();

        /* Verify. */
        verifyZeroInteractions(mDownloadManagerRequest);
        verify(mDownloader).onDownloadStarted(eq(DOWNLOAD_ID), anyLong());
    }

    @Test
    public void hideNotificationOnMandatoryUpdate() {
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(true);

        /* Perform background task. */
        mRequestTask.doInBackground();

        /* Verify. */
        verify(mDownloadManagerRequest).setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        verify(mDownloadManagerRequest).setVisibleInDownloadsUi(false);
        verify(mDownloader).onDownloadStarted(eq(DOWNLOAD_ID), anyLong());
    }

    @Test
    public void cancelledDuringEnqueue() {
        mRequestTask = spy(mRequestTask);
        when(mRequestTask.isCancelled()).thenReturn(true);

        /* Perform background task. */
        mRequestTask.doInBackground();

        /* Verify. */
        verify(mDownloader, never()).onDownloadStarted(anyLong(), anyLong());
    }
}
