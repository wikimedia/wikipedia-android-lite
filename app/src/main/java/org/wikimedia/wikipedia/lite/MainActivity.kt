package org.wikimedia.wikipedia.lite

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
val interactionHandler = "(action) => { pcsClient.onReceiveMessage(JSON.stringify(action)); }"
val loadCompletion = "() => { setTimeout(() => { pcsClient.onReceiveMessage('{\"action\": \"setup\"}'); }, 1) }"
val setupParams = "{theme: 'pagelib_theme_dark', dimImages: false, loadImages: true, margins: { top: '16px', right: '16px', bottom: '16px', left: '16px' }, areTablesInitiallyExpanded: false}"
val setupParamsJSON = "{\"theme\": \"pagelib_theme_dark\", \"dimImages\": false, \"loadImages\": true, \"margins\": { \"top\": \"16px\", \"right\": \"16px\", \"bottom\": \"16px\", \"left\": \"16px\" }, \"areTablesInitiallyExpanded\": false}"
val fullPageBaseURL = "https://en.wikipedia.org/api/rest_v1/page/mobile-html/"

class Client: WebViewClient() {
    var incomingMessageHandler: ValueCallback<String>? = null
    var onPageFinishedHandler: ValueCallback<String>? = null

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageFinishedHandler?.onReceiveValue(url)
    }

    fun progressivelyLoadIntoshell(url: String, webView: WebView) {
        var js = "pagelib.c1.Page.loadProgressively('${url}', 100, ${loadCompletion}, () => {  pagelib.c1.Page.setup(${setupParams}) }); "

        webView.evaluateJavascript(js,
            ValueCallback<String> {

            })
    }

    fun loadIntoShell(url: String, webView: WebView) {
        var js = "pagelib.c1.Page.load('${url}').then(() => { " +
                    "window.requestAnimationFrame(${loadCompletion});\n" +
                    "pagelib.c1.Page.setup(${setupParams});\n" +
                "}); " // load complete here because the page is visible

        webView.evaluateJavascript(js,
            ValueCallback<String> {

            })
    }

    fun navigateTo(url: String, webView: WebView) {
        webView.loadUrl("${url}")
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
enum class PageLoadType {
    STANDARD, SHELL
}
enum class PageLoadMode {
    PROGRESSIVE, FULL
}

enum class ShellPageLoadState {
    NONE, LOADING, LOADED, SETUP
}

class MainActivity : AppCompatActivity() {
    var client = Client()
    var startTime: Long = 0
    var endTime: Long = 0

    var currentPageTitle = "United_States"
    var currentPageURL = ""
    var type = PageLoadType.SHELL
    var mode = PageLoadMode.FULL
    var shellPageLoadState = ShellPageLoadState.NONE
    val loadedURLs:  MutableSet<String> = mutableSetOf()

    fun startTimer() {
        timeTextView.text = ""
        startTime = System.currentTimeMillis()
    }

    fun endTimer() {
        val text: String
        if (!loadedURLs.contains(currentPageURL)) {
            loadedURLs.add(currentPageURL)
            text = "uncached"
        } else {
            text = "cached"
        }
        endTime = System.currentTimeMillis()
        timeTextView.text = "${endTime - startTime} " + text
    }


    fun setUIEnabled(enabled: Boolean) {
        titleEditText.isEnabled = enabled
        firstButton.isEnabled = enabled
        loadButton.isEnabled = enabled
        fullLoadButton.isEnabled = enabled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView.settings.javaScriptEnabled = true
        titleEditText.setText(currentPageTitle)
        client.onPageFinishedHandler = ValueCallback {
            if (shellPageLoadState == ShellPageLoadState.LOADING) {
                shellPageLoadState = ShellPageLoadState.LOADED
                webView.evaluateJavascript("pagelib.c1.InteractionHandling.setInteractionHandler(${interactionHandler});") {
                    webView.evaluateJavascript("pagelib.c1.Page.setup(${setupParams}, ${loadCompletion});") {

                    }
                }

            }
        }
        client.incomingMessageHandler = ValueCallback {
            try {
                val messagePack = JSONObject(it)
                var action = messagePack.get("action")
                if (action == "setup") {
                    runOnUiThread {
                        if (shellPageLoadState == ShellPageLoadState.LOADED) {
                            shellPageLoadState = ShellPageLoadState.SETUP
                            startTimer()
                            if (mode == PageLoadMode.PROGRESSIVE) {
                                client.progressivelyLoadIntoshell(currentPageURL, webView)
                            } else {
                                client.progressivelyLoadIntoshell(currentPageURL, webView)
                            }
                        } else {
                            endTimer()
                            setUIEnabled(true)
                            webView.isVisible = true
                        }
                    }
                } else if (action == "link_clicked") {
                    val data = messagePack.get("data") as JSONObject
                    val href = data.get("href") as String
                    var path = href.replace("./", "")
                    runOnUiThread {
                        currentPageTitle = path
                        currentPageURL = "${fullPageBaseURL}${currentPageTitle}"
                        if (type == PageLoadType.SHELL) {
                            if (mode == PageLoadMode.PROGRESSIVE) {
                                startTimer()
                                client.progressivelyLoadIntoshell(currentPageURL, webView)
                            } else if (mode == PageLoadMode.FULL) {
                                startTimer()
                                client.loadIntoShell(currentPageURL, webView)
                            }
                        } else {
                            val url = webView.url
                            val newURL = url.replaceAfterLast("/", path)
                            startTimer()
                            webView.loadUrl(newURL)
                        }
                    }
                }
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }
        }
        webView.clearCache(true)
        webView.webViewClient = client
        webView.addJavascriptInterface(client, "pcsClient")
        loadButton.setOnClickListener {
            type = PageLoadType.SHELL
            mode = PageLoadMode.FULL
            setUIEnabled(false)
            currentPageTitle = titleEditText.text.toString()
            currentPageURL = "${fullPageBaseURL}${currentPageTitle}"
            if (shellPageLoadState != ShellPageLoadState.SETUP) {
                loadShell()
            } else {
                startTimer()
                client.loadIntoShell(currentPageURL, webView)
            }
        }
        fullLoadButton.setOnClickListener {
            type = PageLoadType.STANDARD
            mode = PageLoadMode.PROGRESSIVE
            currentPageTitle = titleEditText.text.toString()
            currentPageURL = "${fullPageBaseURL}${currentPageTitle}"
            shellPageLoadState = ShellPageLoadState.NONE
            setUIEnabled(false)
            webView.isVisible = false
            startTimer()
            client.navigateTo(currentPageURL, webView)
        }
        firstButton.setOnClickListener {
            type = PageLoadType.SHELL
            mode = PageLoadMode.PROGRESSIVE
            currentPageTitle = titleEditText.text.toString()
            currentPageURL = "${fullPageBaseURL}${currentPageTitle}"
            setUIEnabled(false)
            startTimer()
            if (shellPageLoadState != ShellPageLoadState.SETUP) {
                loadShell()
            } else {
                startTimer()
                client.progressivelyLoadIntoshell(currentPageURL, webView)
            }
        }
    }

    fun loadShell() {
        shellPageLoadState = ShellPageLoadState.LOADING
        webView.loadUrl("https://${shellPageHost}/en.wikipedia.org/v1/page/${shellPagePath}")
    }

}
