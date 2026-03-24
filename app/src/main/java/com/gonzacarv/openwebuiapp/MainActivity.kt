package com.gonzacarv.openwebuiapp

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.webkit.*
import android.net.http.SslError
import android.webkit.WebChromeClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var originalSystemUiVisibility = 0

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webView = findViewById<WebView>(R.id.webview)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                originalSystemUiVisibility = window.decorView.systemUiVisibility

                val decor = window.decorView as android.widget.FrameLayout
                decor.addView(view, android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                ))

                // Solo al ver video en fullscreen se oculta todo
                hideSystemUI()
            }

            override fun onHideCustomView() {
                val decor = window.decorView as android.widget.FrameLayout
                customView?.let { decor.removeView(it) }
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
                window.decorView.systemUiVisibility = originalSystemUiVisibility
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                handler.proceed()
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return false
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimetype)
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Descargando archivo...")
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype))

            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)

            val cookies = CookieManager.getInstance().getCookie(url)
            request.addRequestHeader("Cookie", cookies)
        }

        webView.loadUrl("http://10.11.12.50:3002/")
    }

    // Solo para fullscreen de video
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && customView != null) hideSystemUI()
    }

    override fun onBackPressed() {
        val webView = findViewById<WebView>(R.id.webview)
        if (customView != null) {
            webView.webChromeClient?.onHideCustomView()
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
