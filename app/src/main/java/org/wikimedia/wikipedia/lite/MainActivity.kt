package org.wikimedia.wikipedia.lite

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import org.json.JSONObject

var shellPageHost = "talk-pages.wmflabs.org"
var shellPagePath = "mobile-html-shell"
var loadCompletion = "() => { marshaller.onReceiveMessage('{\"action\": \"load_complete\"}'); }"
var setupParams = "{platform: pagelib.c1.Platforms.ANDROID, theme: pagelib.c1.Themes.DARK, dimImages: false, margins: { top: '16px', right: '16px', bottom: '16px', left: '16px' }, areTablesInitiallyExpanded: false}"

class Client: WebViewClient() {
    var incomingMessageHandler: ValueCallback<String>? = null
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view?.evaluateJavascript("pagelib.c1.Page.setup(${setupParams}, ${loadCompletion});",
            ValueCallback<String> {

            })
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
    }

    fun loadTitle(title: String, webView: WebView) {
        var js = "pagelib.c1.Page.load('https://en.wikipedia.org/api/rest_v1/page/mobile-html/${title}').then(() => { " +
                    "window.requestAnimationFrame(${loadCompletion});\n" +
                    "pagelib.c1.Page.setup(${setupParams});\n" +
                "}); " // load complete here because the page is visible

        webView.evaluateJavascript(js,
            ValueCallback<String> {

            })
    }

    fun fullyLoadTitle(title: String, webView: WebView) {
        webView.loadUrl("https://en.wikipedia.org/api/rest_v1/page/mobile-html/${title}")
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

    fun startTimer() {
        startTime = System.currentTimeMillis()
    }

    fun endTimer() {
        endTime = System.currentTimeMillis()
        timeTextView.text = "${endTime - startTime}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView.settings.javaScriptEnabled = true
        client.incomingMessageHandler = ValueCallback<String> {
            runOnUiThread {
                endTimer()
                webView.isVisible = true
            }
//            try {
//                val messagePack = JSONObject(it)
//                var action = messagePack.get("action")
//            } catch (e: JSONException) {
//                throw RuntimeException(e)
//            }
        }
        webView.webViewClient = client
        webView.addJavascriptInterface(client, "marshaller")
        loadButton.setOnClickListener {
            if (!webView.url.contains(shellPagePath)) {
                timeTextView.text = "load shell first"
            } else {
                startTimer()
                client.loadTitle(titleEditText.text.toString(), webView)
            }

        }
        fullLoadButton.setOnClickListener {
            webView.isVisible = false
            startTimer()
            client.fullyLoadTitle(titleEditText.text.toString(), webView)
        }
        shellButton.setOnClickListener {
            loadShell()
        }
        loadShell()
    }
    fun loadShell() {
        startTimer()
        webView.loadUrl("https://${shellPageHost}/en.wikipedia.org/v1/page/${shellPagePath}")
    }

}
