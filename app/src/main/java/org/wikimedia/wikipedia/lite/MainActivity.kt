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

val shellPageHost = "talk-pages.wmflabs.org"
val shellPagePath = "mobile-html-shell"
val loadCompletion = "() => { marshaller.onReceiveMessage('{\"action\": \"load_complete\"}'); }"
val setupParams = "{platform: pagelib.c1.Platforms.ANDROID, theme: pagelib.c1.Themes.DARK, dimImages: false, margins: { top: '16px', right: '16px', bottom: '16px', left: '16px' }, areTablesInitiallyExpanded: false}"

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
    fun loadFirstSectionOfTitle(title: String, webView: WebView) {
        var js = "pagelib.c1.Page.loadFirstSection('https://en.wikipedia.org/api/rest_v1/page/mobile-html/${title}').then(() => { " +
                "window.requestAnimationFrame(${loadCompletion});\n" +
                "pagelib.c1.Page.setup(${setupParams});\n" +
                "}); " // load complete here because the page is visible

        webView.evaluateJavascript(js,
            ValueCallback<String> {

            })
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
    var titleToLoadIntoShell: String? = null
    var loadFirstSection: Boolean = false

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
                val title = titleToLoadIntoShell
                titleToLoadIntoShell = null
                if (title != null) {
                    startTimer()
                    if (loadFirstSection) {
                        client.loadFirstSectionOfTitle(title, webView)
                    } else {
                        client.loadTitle(title, webView)
                    }
                } else {
                    endTimer()
                    webView.isVisible = true
                }
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
                loadFirstSection = true
                titleToLoadIntoShell = titleEditText.text.toString()
                loadShell()
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
        firstButton.setOnClickListener {
            startTimer()
            if (!webView.url.contains(shellPagePath)) {
                titleToLoadIntoShell = titleEditText.text.toString()
                loadShell()
            } else {
                startTimer()
                client.loadFirstSectionOfTitle(titleEditText.text.toString(), webView)
            }
        }
        loadShell()
    }
    fun loadShell() {
        startTimer()
        webView.loadUrl("https://${shellPageHost}/en.wikipedia.org/v1/page/${shellPagePath}")
    }

}
