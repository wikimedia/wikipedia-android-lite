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
val loadCompletion = "() => { setTimeout(() => { pcsClient.onReceiveMessage('{\"action\": \"setup\"}'); }, 1) }"
val setupParams = "{theme: 'pagelib_theme_dark', dimImages: false, loadImages: false, margins: { top: '16px', right: '16px', bottom: '16px', left: '16px' }, areTablesInitiallyExpanded: false}"
val setupParamsJSON = "{\"theme\": \"pagelib_theme_dark\", \"dimImages\": false, \"loadImages\": false, \"margins\": { \"top\": \"16px\", \"right\": \"16px\", \"bottom\": \"16px\", \"left\": \"16px\" }, \"areTablesInitiallyExpanded\": false}"
val localBaseURL = "http://192.168.1.26:6927"

class Client: WebViewClient() {
    var incomingMessageHandler: ValueCallback<String>? = null
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (url != null && url.contains(shellPagePath)) {
            view?.evaluateJavascript("pagelib.c1.Page.setup(${setupParams}, ${loadCompletion});",
                ValueCallback<String> {

                })
        }

    }
    fun loadFirstSectionOfTitle(title: String, webView: WebView) {
        var js = "pagelib.c1.Page.loadProgressively('https://en.wikipedia.org/api/rest_v1/page/mobile-html/${title}', 100, ${loadCompletion}, () => {  pagelib.c1.Page.setup(${setupParams}) }); "

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
        //webView.loadUrl("https://en.wikipedia.org/api/rest_v1/page/mobile-html/${title}")
        webView.loadUrl("${localBaseURL}/en.wikipedia.org/v1/page/mobile-html/${title}")
    }

    @JavascriptInterface
    fun onReceiveMessage(message: String) {
        incomingMessageHandler?.onReceiveValue(message)
    }

    @JavascriptInterface
    fun getSetupSettings(): String {
        return setupParamsJSON
    }
}

class MainActivity : AppCompatActivity() {
    var client = Client()
    var startTime: Long = 0
    var endTime: Long = 0
    var titleToLoadIntoShell: String? = null
    var loadFirstSection: Boolean = false

    fun startTimer() {
        timeTextView.text = ""
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
            try {
                val messagePack = JSONObject(it)
                var action = messagePack.get("action")
                if (action == "setup") {
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
                }
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }


        }
        webView.webViewClient = client
        webView.addJavascriptInterface(client, "pcsClient")
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
