package com.example.seekhoassignment.presentation.component

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.seekhoassignment.util.buildYouTubeWatchUrl

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeWebPlayer(
    embedOrWatchOrId: String,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val videoId = extractYoutubeId(embedOrWatchOrId) ?: embedOrWatchOrId

    val origin = "https://example.com"
    val embedUrl = remember(videoId, origin) {
        "https://www.youtube-nocookie.com/embed/$videoId?rel=0&modestbranding=1&origin=${Uri.encode(origin)}"
    }

    var showFallback by remember { mutableStateOf(false) }

    // Fullscreen view and callback states
    var customView: View? by remember { mutableStateOf(null) }
    var customViewCallback: WebChromeClient.CustomViewCallback? by remember { mutableStateOf(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (!showFallback) {
            AndroidView(factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    settings.allowContentAccess = false
                    settings.loadsImagesAutomatically = true

                    WebView.setWebContentsDebuggingEnabled(true)

                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {

                            view?.parent?.let { parent ->
                                try {
                                    (parent as? ViewGroup)?.removeView(view)
                                } catch (e: Exception) {
                                    Log.w("YouTubeWebPlayer", "onShowCustomView: failed to remove parent: ${e.message}")
                                }
                            }
                            view?.layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            customView = view
                            customViewCallback = callback
                        }

                        override fun onHideCustomView() {
                            customView = null
                            customViewCallback?.onCustomViewHidden()
                            customViewCallback = null
                        }

                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            Log.d("YouTubeWebPlayer-Console", "${consoleMessage?.message()} -- ${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()}")
                            return super.onConsoleMessage(consoleMessage)
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                            super.onPageFinished(view, finishedUrl)
                            Log.d("YouTubeWebPlayer", "onPageFinished: $finishedUrl")
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            // If the main frame fails, mark to show fallback UI.
                            if (request?.isForMainFrame == true) {
                                Log.w("YouTubeWebPlayer", "onReceivedError: ${error?.errorCode} ${error?.description}")
                                showFallback = true
                            }
                            super.onReceivedError(view, request, error)
                        }

                        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                            if (request?.isForMainFrame == true) {
                                Log.w("YouTubeWebPlayer", "onReceivedHttpError: ${errorResponse?.statusCode} ${errorResponse?.reasonPhrase}")
                                showFallback = true
                            }
                            super.onReceivedHttpError(view, request, errorResponse)
                        }

                        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                            Log.w("YouTubeWebPlayer", "onReceivedSslError: $error")
                            // treat SSL issues as fallback rather than proceeding insecurely
                            handler?.cancel()
                            showFallback = true
                        }
                    }

                    // Build minimal HTML wrapper that loads the iframe using your origin
                    val html = """
                        <!doctype html>
                        <html>
                          <head>
                            <meta name="viewport" content="width=device-width,initial-scale=1" />
                            <style>html,body{height:100%;margin:0;background:black;}iframe{display:block;border:0;height:100%;width:100%;}</style>
                          </head>
                          <body>
                            <iframe
                              src="$embedUrl"
                              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen"
                              allowfullscreen
                              sandbox="allow-same-origin allow-scripts allow-presentation allow-forms allow-popups"
                            ></iframe>
                          </body>
                        </html>
                    """.trimIndent()

                    // Use loadDataWithBaseURL to provide an origin base. This can improve some embedding checks.
                    try {
                        loadDataWithBaseURL(origin, html, "text/html", "utf-8", null)
                    } catch (e: Exception) {
                        Log.e("YouTubeWebPlayer", "loadDataWithBaseURL failed: ${e.message}")
                        showFallback = true
                    }

                    Log.d("YouTubeWebPlayer", "Loaded embed url: $embedUrl")
                }
            }, update = { webView ->
                // If videoId changes you may want to reload new HTML here. For now noop.
            })
        }

        // If fullscreen custom view was requested by the player, render it on top of the Compose UI
        customView?.let { view ->
            AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())
        }

        // Simple fallback UI when WebView cannot load (open externally)
        if (showFallback) {
            val watchUrl = buildYouTubeWatchUrl(videoId)
            Surface(modifier = Modifier.fillMaxSize()) {
                Column {
                    Text("Trailer cannot be played inline.")
                    Button(onClick = {
                        // Try opening in YouTube app first, then browser
                        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(watchUrl))
                        try {
                            if (appIntent.resolveActivity(ctx.packageManager) != null) {
                                ctx.startActivity(appIntent)
                            } else {
                                ctx.startActivity(webIntent)
                            }
                        } catch (e: ActivityNotFoundException) {
                            try {
                                ctx.startActivity(webIntent)
                            } catch (ee: Exception) {
                                Log.e("YouTubeWebPlayer", "Failed to open external player: ${ee.message}")
                            }
                        } catch (e: Exception) {
                            Log.e("YouTubeWebPlayer", "Fallback open error: ${e.message}")
                        }
                    }) {
                        Text("Open in YouTube")
                    }
                }
            }
        }
    }
}
