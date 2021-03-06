package com.helagone.airelibre.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class TermsActivity extends Activity {
    private WebView mWebView;
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        mWebView = new WebView(this);
        mWebView.getSettings().setJavaScriptEnabled(true);
        final Activity activity = this;
        mWebView.setWebViewClient(new WebViewClient(){
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl ){
                Toast.makeText(activity, description, Toast.LENGTH_SHORT).show();
            }
        });

        mWebView.loadUrl("http://airelibre.fm/");
        setContentView(mWebView);
    }
}
