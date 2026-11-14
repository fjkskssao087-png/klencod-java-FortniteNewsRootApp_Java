package com.example.fortnitenews;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView rootStatusBanner;
    private boolean isRootGranted = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        rootStatusBanner = findViewById(R.id.rootStatusBanner);

        // إعدادات الـ WebView ليعمل بسلاسة لمس كاملة
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // ربط جافاسكريبت مع جافا أندرويد (JS Bridge)
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                swipeRefreshLayout.setRefreshing(false);
                // تمرير حالة الروت إلى الواجهة بعد التحميل
                view.evaluateJavascript("if(window.updateRootBadge){ updateRootBadge(" + isRootGranted + "); }", null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        // السحب للتحديث
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.reload();
            }
        });

        // تحميل واجهة التطبيق المحلية
        webView.loadUrl("file:///android_asset/index.html");

        // طلب صلاحيات الروت مباشرة عند الدخول للتطبيق
        triggerMagiskRootRequest();
    }

    private void triggerMagiskRootRequest() {
        rootStatusBanner.setVisibility(View.VISIBLE);
        rootStatusBanner.setText("⏳ جاري طلب صلاحيات الروت من Magisk...");

        RootHelper.requestRootAndExecute("id", new RootHelper.RootCallback() {
            @Override
            public void onRootResult(final boolean isRooted, final String output) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isRootGranted = isRooted;
                        if (isRooted) {
                            rootStatusBanner.setText("✅ صلاحيات الروت مفعلة (Magisk Superuser)");
                            rootStatusBanner.setBackgroundColor(0xFF1B5E20);
                            Toast.makeText(MainActivity.this, "تم منح صلاحيات الروت بنجاح!", Toast.LENGTH_SHORT).show();
                        } else {
                            rootStatusBanner.setText("⚠️ وضع المستخدم العادي (لم يتم منح الروت)");
                            rootStatusBanner.setBackgroundColor(0xFFB71C1C);
                        }
                        webView.evaluateJavascript("if(window.updateRootBadge){ updateRootBadge(" + isRootGranted + "); }", null);
                    }
                });
            }
        });
    }

    // واجهة للتواصل بين كود HTML/JS وكود Java الأصلي
    public class WebAppInterface {
        @JavascriptInterface
        public void requestRootAgain() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    triggerMagiskRootRequest();
                }
            });
        }

        @JavascriptInterface
        public void showToast(String message) {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}