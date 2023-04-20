package com.ryotashona.itlink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private TextView mSharedUrlTextView;
    private TextView mSharedTitleTextView;
    private EditText mCommentEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedUrlTextView = findViewById(R.id.shared_url);
        mSharedTitleTextView = findViewById(R.id.shared_title);
        handleIntent(getIntent());

        mCommentEditText = findViewById(R.id.comment_edittext);
        Button mClearButton = findViewById(R.id.clear_button);
        mClearButton.setOnClickListener(l -> {
            mCommentEditText.setText("");
        });

        Button mSendButton = findViewById(R.id.send_button);
        mSendButton.setOnClickListener(v -> sendDataToServer());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
            if ("text/plain".equals(intent.getType())) {
                String sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT);
                mSharedUrlTextView.setText(sharedUrl);
                fetchTitleFromUrl(sharedUrl).thenAccept(title -> runOnUiThread(() -> mSharedTitleTextView.setText(title)));
            }
        }
    }

    private CompletableFuture<String> fetchTitleFromUrl(String url) {
        CompletableFuture<String> future = new CompletableFuture<>();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    try {
                        assert response.body() != null;
                        String html = response.body().string();
                        Document doc = Jsoup.parse(html);
                        String title = Objects.requireNonNull(doc.head().select("title").first()).text();
                        future.complete(title);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                } else {
                    future.completeExceptionally(new IOException("Unexpected code " + response));
                }
            }
        });

        return future;
    }

    private void sendDataToServer() {
        String sharedUrl = mSharedUrlTextView.getText().toString();
        String sharedTitle = mSharedTitleTextView.getText().toString();
        String comment = mCommentEditText.getText().toString();

        // 仮実装:Android標準の共有機能で代替
        String msg = "";
        msg += "comment: " + comment+"\n";
        msg += "title: " + sharedTitle + "\n";
        msg += "URL: " + sharedUrl + "\n";

        ShareCompat.IntentBuilder builder
                = new ShareCompat.IntentBuilder(MainActivity.this);
        builder.setText(msg)
                .setType("text/plain");
        builder.createChooserIntent();
        builder.startChooser();
    }
}
