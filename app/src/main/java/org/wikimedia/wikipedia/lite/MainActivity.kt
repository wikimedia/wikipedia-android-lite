package org.wikimedia.wikipedia.lite

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import org.json.JSONObject

class Client: WebViewClient() {
    var incomingMessageHandler: ValueCallback<String>? = null
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view?.evaluateJavascript("pagelib.c1.Page.setup({" +
                "platform: pagelib.c1.Platforms.ANDROID," +
                "clientVersion: '0.0.0'," +
                "theme: pagelib.c1.Themes.DARK," +
                "dimImages: false," +
                "margins: { top: '16px', right: '16px', bottom: '16px', left: '16px' }," +
                "areTablesInitiallyExpanded: false," +
                "textSizeAdjustmentPercentage: '100%%'," +
                "loadImages: true" +
                "}," +
                "() => { marshaller.onReceiveMessage('{\"action\": \"load_complete\"}') }" +
                ")",
            ValueCallback<String> {

            })
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
    }

    fun loadTitle(title: String, webView: WebView) {
        var js = "pagelib.c1.Page.load('https://en.wikipedia.org/api/rest_v1/page/mobile-html/${title}').then(() => { " +
                "pagelib.c1.Page.setup({" +
                "platform: pagelib.c1.Platforms.ANDROID," +
                "clientVersion: '0.0.0'," +
                "theme: pagelib.c1.Themes.DARK," +
                "dimImages: false," +
                "margins: { top: '16px', right: '16px', bottom: '16px', left: '16px' }," +
                "areTablesInitiallyExpanded: false," +
                "textSizeAdjustmentPercentage: '100%%'," +
                "loadImages: true" +
                "}," +
                "() => { marshaller.onReceiveMessage('{\"action\": \"load_complete\"}') }" +
                ")" +
                "} )"
        webView.evaluateJavascript(js,
            ValueCallback<String> {

            })
    }

    fun fullyLoadTitle(title: String, webView: WebView) {
        webView.loadUrl("http://192.168.1.26:6927/en.wikipedia.org/v1/page/mobile-html/${title}")
    }

    @JavascriptInterface
    fun onReceiveMessage(message: String) {
        incomingMessageHandler?.onReceiveValue(message)
    }
}

class MainActivity : AppCompatActivity() {
    var client = Client()
    var startTime: Long = 0
    var endTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startTime = System.currentTimeMillis()
        webView.loadUrl("http://192.168.1.26:6927/en.wikipedia.org/v1/page/mobile-html-shell/")
        webView.settings.javaScriptEnabled = true
        client.incomingMessageHandler = ValueCallback<String> {
            try {
                val messagePack = JSONObject(it)
                var action = messagePack.get("action")
                endTime = System.currentTimeMillis()
                timeTextView.text = "${endTime - startTime}"
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }
        }
        webView.webViewClient = client
        webView.addJavascriptInterface(client, "marshaller")
        loadButton.setOnClickListener {
            startTime = System.currentTimeMillis()
            client.loadTitle(titleEditText.text.toString(), webView)
        }
        fullLoadButton.setOnClickListener {
            startTime = System.currentTimeMillis()
            client.fullyLoadTitle(titleEditText.text.toString(), webView)
        }
    }

}
