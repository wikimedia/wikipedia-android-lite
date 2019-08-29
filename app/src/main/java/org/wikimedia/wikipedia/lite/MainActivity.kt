package org.wikimedia.wikipedia.lite

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.activity_main.*

class MyClient: WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
    }
}
class MainActivity : AppCompatActivity() {
    var client = MyClient()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView.loadUrl("http://192.168.1.26:6927/en.wikipedia.org/v1/page/mobile-html-shell/")
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = client
        loadButton.setOnClickListener {
            var js = "pagelib.c1.Page.load('http://192.168.1.26:6927/en.wikipedia.org/v1/page/mobile-html/${titleEditText.text}');"
            webView.evaluateJavascript(js,
            ValueCallback<String> {

            })
        }
    }

}
