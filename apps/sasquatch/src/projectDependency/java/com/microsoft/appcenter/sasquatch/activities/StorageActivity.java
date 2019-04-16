/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.storage.Constants;
import com.microsoft.appcenter.storage.Storage;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.Page;
import com.microsoft.appcenter.storage.models.PaginatedDocuments;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.microsoft.appcenter.sasquatch.SasquatchConstants.ACCOUNT_ID;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_ID;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.DOCUMENT_PARTITION;

class TestDocument {

    @SuppressWarnings("unused")
    String key;
}

public class StorageActivity extends AppCompatActivity {

    private RecyclerView mListView;

    private Boolean isLoading = false;

    private MenuItem addNewDocument;

    private CustomItemAdapter mAdapterUser;

    private AppDocumentListAdapter mAppDocumentListAdapter;

    private ProgressBar mProgressBar;

    private Spinner mStorageTypeSpinner;

    private StorageType mStorageType = StorageType.READONLY;

    private PaginatedDocuments<TestDocument> currentAppDocuments;

    private PaginatedDocuments<Map> currentUserDocuments;

    private TextView mMessageText;

    private boolean mUserDocumentsLoading;

    private boolean mAppDocumentsLoading;

    private AppCenterConsumer<PaginatedDocuments<TestDocument>> uploadApp = new AppCenterConsumer<PaginatedDocuments<TestDocument>>() {

        @Override
        public void accept(PaginatedDocuments<TestDocument> documents) {
            if (!mUserDocumentsLoading) {
               hideProgress();
            }
            mAppDocumentsLoading = false;
            currentAppDocuments = documents;
            updateAppDocument(documents.getCurrentPage().getItems());
        }
    };

    private AppCenterConsumer<PaginatedDocuments<Map>> uploadUser = new AppCenterConsumer<PaginatedDocuments<Map>>() {

        @Override
        public void accept(PaginatedDocuments<Map> documents) {
            if (!mAppDocumentsLoading) {
                hideProgress();
            }
            mUserDocumentsLoading = false;
            currentUserDocuments = documents;
            updateUserDocuments(documents.getCurrentPage().getItems());
        }
    };

    private void hideProgress() {
        mProgressBar.setVisibility(View.GONE);
        mStorageTypeSpinner.setEnabled(true);
    }

    private void showProgress() {
        mProgressBar.setVisibility(View.VISIBLE);
        mStorageTypeSpinner.setEnabled(false);
    }

    private RecyclerView.OnScrollListener scrollAppListener = new RecyclerView.OnScrollListener() {

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (currentAppDocuments != null && currentAppDocuments.hasNextPage() && !isLoading) {
                isLoading = true;
                currentAppDocuments.getNextPage().thenAccept(new AppCenterConsumer<Page<TestDocument>>() {
                    @Override
                    public void accept(Page<TestDocument> testDocumentPage) {
                        isLoading = false;
                        updateAppDocument(testDocumentPage.getItems());
                    }
                });
            }
        }
    };

    private RecyclerView.OnScrollListener scrollUserListener = new RecyclerView.OnScrollListener() {

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (currentUserDocuments != null && currentUserDocuments.hasNextPage() && !isLoading) {
                isLoading = true;
                currentUserDocuments.getNextPage().thenAccept(new AppCenterConsumer<Page<Map>>() {

                    @Override
                    public void accept(Page<Map> mapPage) {
                        updateUserDocuments(mapPage.getItems());
                    }
                });
            }
        }
    };

    private void updateUserDocuments(List<Document<Map>> documents) {
        if (documents == null)
            return;
        mAdapterUser.setList(new ArrayList<>(documents));
        mAdapterUser.notifyDataSetChanged();
    }

    private void updateAppDocument(List<Document<TestDocument>> list) {
        mAppDocumentListAdapter.upload(list);
        mAppDocumentListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);
        mListView = findViewById(R.id.list);
        mListView.setLayoutManager(new LinearLayoutManager(this));
        mProgressBar = findViewById(R.id.load_progress);
        mStorageTypeSpinner = findViewById(R.id.storage_type);
        mMessageText = findViewById(R.id.storage_message);

        /* List the app read-only documents. */
        mAppDocumentListAdapter = new AppDocumentListAdapter(this, new ArrayList<Document<TestDocument>>());
        mAppDocumentListAdapter.setOnItemClickListener(new AppDocumentListAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(int position) {
                Intent intent = new Intent(StorageActivity.this, DocumentDetailActivity.class);
                intent.putExtra(DOCUMENT_PARTITION, Constants.READONLY);
                intent.putExtra(DOCUMENT_ID, mAppDocumentListAdapter.getDocumentByPosition(position));
                startActivity(intent);
            }
        });
        showProgress();
        mAppDocumentsLoading = true;
        Storage.list(Constants.READONLY, TestDocument.class).thenAccept(uploadApp);

        /* List the user documents. */
        mAdapterUser = new CustomItemAdapter(new ArrayList<Document<Map>>(), this);
        mAdapterUser.setOnItemClickListener(new CustomItemAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(int position) {
                Intent intent = new Intent(StorageActivity.this, DocumentDetailActivity.class);
                intent.putExtra(DOCUMENT_PARTITION, Constants.USER);
                intent.putExtra(DOCUMENT_ID, mAdapterUser.getDocumentByPosition(position));
                startActivity(intent);
            }

            @Override
            public void onRemoveClick(final int position) {
                Storage.delete(Constants.USER, mAdapterUser.getItem(position)).thenAccept(new AppCenterConsumer<Document<Void>>() {

                    @Override
                    public void accept(Document<Void> voidDocument) {
                        if (voidDocument.failed()) {
                            Toast.makeText(StorageActivity.this, getResources().getString(R.string.storage_file_remove_error), Toast.LENGTH_SHORT).show();
                        } else {
                            mAdapterUser.removeItem(position);
                            mAdapterUser.notifyDataSetChanged();
                            Toast.makeText(StorageActivity.this, getResources().getString(R.string.storage_file_remove_success), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        loadUserDocuments();

        /* Selector for App VS User documents. */
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.storage_type_names));
        mStorageTypeSpinner.setAdapter(typeAdapter);
        mStorageTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateStorageType(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadUserDocuments() {
        showProgress();
        mUserDocumentsLoading = true;

        /* List the user documents. */
        String accountId = MainActivity.sSharedPreferences.getString(ACCOUNT_ID, null);
        if (accountId != null) {
            Storage.list(Constants.USER, Map.class).thenAccept(uploadUser);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        isLoading = false;
        switch (item.getItemId()) {
            case R.id.action_add:
                switch (mStorageType) {
                    case USER:
                        String accountId = MainActivity.sSharedPreferences.getString(ACCOUNT_ID, null);
                        if (accountId != null) {
                            Intent intent = new Intent(StorageActivity.this, NewUserDocumentActivity.class);
                            startActivity(intent);
                        }
                        break;

                    case READONLY:
                        final AlertDialog.Builder builder = new AlertDialog.Builder(StorageActivity.this);
                        builder.setIcon(R.drawable.ic_appcenter_logo);
                        builder.setTitle(getApplicationContext().getResources().getString(R.string.storage_type_reminder));
                        builder.setPositiveButton(getApplicationContext().getResources().getString(R.string.alert_ok), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                builder.setCancelable(true);
                            }
                        });
                        builder.show();
                        break;
                }
                break;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add, menu);
        addNewDocument = menu.findItem(R.id.action_add);
        return true;
    }

    private void updateStorageType(int position) {
        mMessageText.setText("");
        mStorageType = StorageType.values()[position];
        switch (mStorageType) {
            case READONLY:
                addNewDocument.setVisible(false);
                mListView.setAdapter(mAppDocumentListAdapter);
                mListView.addOnScrollListener(scrollAppListener);
                break;
            case USER:
                addNewDocument.setVisible(true);
                mListView.removeOnScrollListener(scrollUserListener);
                String accountId = MainActivity.sSharedPreferences.getString(ACCOUNT_ID, null);
                if (accountId != null) {
                    mListView.setAdapter(mAdapterUser);
                } else {
                    mMessageText.setText(getApplicationContext().getResources().getString(R.string.sign_in_reminder));
                    mListView.setAdapter(null);
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserDocuments();
    }

    private enum StorageType {
        READONLY,
        USER
    }
}
