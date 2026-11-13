package men.rrbb.music

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread

class StreamWebView(context: Context) : WebView(context) {
    // Keep WebView audio and JS timers awake when the screen turns off or app is minimized
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(View.VISIBLE)
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var webView: StreamWebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#0f172a")
        window.navigationBarColor = Color.parseColor("#0f172a")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        val serviceIntent = Intent(this, AudioKeepAliveService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        webView = StreamWebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#0f172a"))
        }
        setContentView(webView)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            // Identifies the Android app to the backend so private personal playlists are unlocked
            userAgentString = "$userAgentString MoosicAndroidApp/1.0"
        }

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                CookieManager.getInstance().flush()

                // Guarantee a permanent Personal Room ID for this Android installation
                val prefs = getSharedPreferences("moosic_prefs", Context.MODE_PRIVATE)
                var roomId = prefs.getInt("personal_room_id", 0)
                if (roomId !in 900_000_000..999_999_999) {
                    roomId = (900_000_000..999_999_999).random()
                    prefs.edit().putInt("personal_room_id", roomId).apply()
                }
                view?.evaluateJavascript(
                    "if (!localStorage.getItem('moosic_personal_room_id')) { localStorage.setItem('moosic_personal_room_id', '$roomId'); }",
                    null
                )
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    moveTaskToBack(true)
                }
            }
        })

        // Detect whether HTTP is served on port 8000 or port 80 of music.rrbb.men
        thread {
            val targetUrl = resolveHttpEndpoint()
            runOnUiThread {
                webView.loadUrl(targetUrl)
            }
        }
    }

    private fun resolveHttpEndpoint(): String {
        val host = "music.rrbb.men"
        if (isPortOpen(host, 8000, 1200)) {
            return "http://$host:8000/"
        }
        if (isPortOpen(host, 80, 1200)) {
            return "http://$host/"
        }
        // Default to port 8000 (or port 80 if forwarded)
        return "http://$host:8000/"
    }

    private fun isPortOpen(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun onDestroy() {
        stopService(Intent(this, AudioKeepAliveService::class.java))
        webView.destroy()
        super.onDestroy()
    }
}
