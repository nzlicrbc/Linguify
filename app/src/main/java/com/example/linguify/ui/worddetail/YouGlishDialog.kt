package com.example.linguify.ui.worddetail

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.Window
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.appcompat.widget.Toolbar
import com.example.linguify.R

class YouGlishDialog(
    context: Context,
    private val word: String,
    private val language: String = "english"
) {

    private val dialog: Dialog = Dialog(context)
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: Toolbar

    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_youglish)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        initViews()
        setupWebView()
    }

    private fun initViews() {
        webView = dialog.findViewById(R.id.webViewYouglish)
        progressBar = dialog.findViewById(R.id.progressBarYouglish)
        toolbar = dialog.findViewById(R.id.toolbarYouglish)

        toolbar.title = "Videos for \"$word\""
        toolbar.setNavigationOnClickListener {
            dismiss()
        }
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (newProgress == 100) {
                    progressBar.visibility = android.view.View.GONE
                } else {
                    progressBar.visibility = android.view.View.VISIBLE
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView, url: String) {
                progressBar.visibility = android.view.View.GONE
            }
        }

        val youglishUrl = "https://youglish.com/pronounce/$word/$language"
        webView.loadUrl(youglishUrl)
    }

    fun show() {
        dialog.show()
    }

    fun dismiss() {
        webView.stopLoading()
        dialog.dismiss()
    }
}
