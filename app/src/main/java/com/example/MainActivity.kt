package com.example

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.ui.theme.MyApplicationTheme
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdSize
import kotlinx.coroutines.delay
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.activity.compose.rememberLauncherForActivityResult
import android.Manifest

class MainActivity : ComponentActivity() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val dataString = result.data?.dataString
            if (dataString != null) {
                filePathCallback?.onReceiveValue(arrayOf(Uri.parse(dataString)))
            } else if (result.data?.clipData != null) {
                val clipData = result.data!!.clipData!!
                val uris = Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                filePathCallback?.onReceiveValue(uris)
            } else {
                filePathCallback?.onReceiveValue(null)
            }
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}
        AdManager.loadInterstitial(this)
        AdManager.loadRewarded(this)

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("MainActivity", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("MainActivity", "FCM Token: $token")
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppScreen(this)
                }
            }
        }
    }

    fun launchFileChooser(callback: ValueCallback<Array<Uri>>?, intent: Intent) {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = callback
        try {
            fileChooserLauncher.launch(intent)
        } catch (e: Exception) {
            filePathCallback = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(activity: MainActivity) {
    val context = LocalContext.current
    var isOffline by remember { mutableStateOf(!isNetworkAvailable(context)) }
    var hasWebError by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var adClickCount by remember { mutableStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    
    // Premium Web Features States
    var isDesktopMode by remember { mutableStateOf(true) }
    var autoDarkMode by remember { mutableStateOf(true) }
    var webProgress by remember { mutableStateOf(0) }
    var isWebLoading by remember { mutableStateOf(false) }

    val systemDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val isDarkTheme = systemDarkTheme && autoDarkMode

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Notifications granted
            }
        }
    )

    // Recheck network status periodically
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        while (true) {
            val currentOffline = !isNetworkAvailable(context)
            if (currentOffline != isOffline) {
                isOffline = currentOffline
                if (!currentOffline) {
                    hasWebError = false
                    webViewRef?.reload()
                }
            }
            delay(3000)
        }
    }

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit App") },
            text = { Text("Are you sure you want to exit?") },
            confirmButton = {
                TextButton(onClick = { activity.finish() }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("NB TV") },
            actions = {
                IconButton(onClick = {
                    webViewRef?.reload()
                }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                }
                IconButton(onClick = {
                    val currentUrl = webViewRef?.url ?: "https://toolswebsite205.blogspot.com"
                    val playStoreLink = "https://play.google.com/store/apps/details?id=com.edu.mu"
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Download NB TV: YouSEO & Prompt Karo from Play Store:\n$playStoreLink\n\nOr check out this link: $currentUrl")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share App"))
                }) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                }
                IconButton(onClick = { showMenu = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Desktop Mode") },
                        leadingIcon = {
                            Checkbox(
                                checked = isDesktopMode,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            isDesktopMode = !isDesktopMode
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Auto Dark Mode") },
                        leadingIcon = {
                            Checkbox(
                                checked = autoDarkMode,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            autoDarkMode = !autoDarkMode
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("About") },
                        onClick = {
                            showMenu = false
                            showAboutDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Privacy Policy") },
                        onClick = {
                            showMenu = false
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://toolswebsite205.blogspot.com/p/privacy-policy.html"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Contact Support") },
                        onClick = {
                            showMenu = false
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:nazimmustafa785@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "NB TV Support Request")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear App Cache") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Cache"
                            )
                        },
                        onClick = {
                            showMenu = false
                            try {
                                // Clear WebView Cache (both disk and RAM)
                                webViewRef?.clearCache(true)
                                
                                // Clear WebStorage database, local storage, session storage
                                android.webkit.WebStorage.getInstance().deleteAllData()
                                
                                // Clean up app's private cache directory
                                context.cacheDir.deleteRecursively()
                                
                                Toast.makeText(context, "Cache Cleared Successfully!", Toast.LENGTH_SHORT).show()
                                
                                // Reload to reflect fresh state
                                webViewRef?.reload()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error clearing cache: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        
        if (showAboutDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text("About NB TV") },
                text = {
                    Text("Version: 5.0\n\nNB TV provides the best entertainment right to your screen. " +
                         "Enjoy seamless streaming, regular updates, and a vast library of content. " +
                         "Developed with love for our users.\n\n" +
                         "Contact: nazimmustafa785@gmail.com")
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { showAboutDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
        
        val showErrorScreen = isOffline || hasWebError

        Box(modifier = Modifier.weight(1f)) {
            if (showErrorScreen) {
                OfflineScreen {
                    val networkOk = isNetworkAvailable(context)
                    isOffline = !networkOk
                    if (networkOk) {
                        hasWebError = false
                        webViewRef?.reload()
                    } else {
                        Toast.makeText(context, "Please check your internet connection.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                WebViewContainer(
                    activity = activity,
                    isDesktopMode = isDesktopMode,
                    isDarkTheme = isDarkTheme,
                    onWebViewCreated = { webViewRef = it },
                    onPageLoaded = {
                        adClickCount++
                        // Show Interstitial on natural break points (e.g. 4 page loads)
                        if (adClickCount > 0 && adClickCount % 4 == 0) {
                            AdManager.showInterstitial(activity)
                        }
                    },
                    onProgressChanged = { progress ->
                        webProgress = progress
                        isWebLoading = progress < 100
                    },
                    onReceivedError = {
                        hasWebError = true
                    }
                )

                if (isWebLoading) {
                    LinearProgressIndicator(
                        progress = webProgress / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.TopCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                }
            }
        }
        
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = AdManager.BANNER_ID
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    activity: MainActivity,
    isDesktopMode: Boolean,
    isDarkTheme: Boolean,
    onWebViewCreated: (WebView) -> Unit,
    onPageLoaded: () -> Unit,
    onProgressChanged: (Int) -> Unit,
    onReceivedError: () -> Unit
) {
    var swipeRefreshLayout: SwipeRefreshLayout? by remember { mutableStateOf(null) }

    AndroidView(
        factory = { context ->
            SwipeRefreshLayout(context).apply {
                isEnabled = false
                val webView = WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        setSupportMultipleWindows(true)
                        javaScriptCanOpenWindowsAutomatically = true
                    }

                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            swipeRefreshLayout?.isRefreshing = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            swipeRefreshLayout?.isRefreshing = false
                            onPageLoaded()
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                onReceivedError()
                            }
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                return false
                            }
                            return try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                                true
                            } catch (e: Exception) {
                                false
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            onProgressChanged(newProgress)
                        }

                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            val intent = fileChooserParams?.createIntent()
                            if (intent != null) {
                                activity.launchFileChooser(filePathCallback, intent)
                                return true
                            }
                            return false
                        }
                    }

                    setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                        try {
                            val request = DownloadManager.Request(Uri.parse(url))
                            request.setMimeType(mimetype)
                            request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(url))
                            request.addRequestHeader("User-Agent", userAgent)
                            request.setDescription("Downloading file...")
                            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype))
                            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            dm.enqueue(request)
                            Toast.makeText(context, "Downloading File", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                        }
                    }

                    loadUrl("https://toolswebsite205.blogspot.com")
                }
                
                setOnRefreshListener {
                    webView.reload()
                }
                
                swipeRefreshLayout = this
                addView(webView)
                onWebViewCreated(webView)
            }
        },
        update = { swipeLayout ->
            val webView = swipeLayout.getChildAt(0) as? WebView ?: return@AndroidView
            
            // 1. Desktop Mode Configuration
            val targetUserAgent = if (isDesktopMode) {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            } else {
                null
            }
            if (webView.settings.userAgentString != targetUserAgent) {
                webView.settings.userAgentString = targetUserAgent
                webView.settings.useWideViewPort = isDesktopMode
                webView.settings.loadWithOverviewMode = isDesktopMode
                webView.reload()
            }

            // 2. Auto Dark Mode Configuration (API 29+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (webView.settings.isAlgorithmicDarkeningAllowed != isDarkTheme) {
                    webView.settings.isAlgorithmicDarkeningAllowed = isDarkTheme
                }
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                val targetForceDark = if (isDarkTheme) {
                    WebSettings.FORCE_DARK_ON
                } else {
                    WebSettings.FORCE_DARK_OFF
                }
                @Suppress("DEPRECATION")
                if (webView.settings.forceDark != targetForceDark) {
                    webView.settings.forceDark = targetForceDark
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun OfflineScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Offline",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Internet Connection",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Please check your network settings and try again.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        NativeAdViewComposable()
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Support us by watching an ad while you wait!", fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        val activity = LocalContext.current as Activity
        Button(
            onClick = { AdManager.showRewarded(activity) {} },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Watch Rewarded Ad")
        }
    }
}

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return when {
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
}
