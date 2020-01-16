/*
 *  Copyright (c) 2019 Razeware LLC
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 *  distribute, sublicense, create a derivative work, and/or sell copies of the
 *  Software in any work that is designed, intended, or marketed for pedagogical or
 *  instructional purposes related to programming, coding, application development,
 *  or information technology.  Permission for such use, copying, modification,
 *  merger, publication, distribution, sublicensing, creation of derivative works,
 *  or sale is expressly withheld.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 */

package com.raywenderlich.android.awesomebrowser

import android.net.Uri
import android.os.Bundle
import android.support.v4.text.HtmlCompat
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.Spanned
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class MainActivity : AppCompatActivity() {
  private lateinit var geckoView: GeckoView
  private val geckoSession = GeckoSession()
  private lateinit var urlEditText: EditText
  private lateinit var progressView: ProgressBar
  private lateinit var trackersCount: TextView
  private var trackersBlockedList: List<ContentBlocking.BlockEvent> = mutableListOf()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    setupToolbar()

    setupUrlEditText()

    setupGeckoView()

    progressView = findViewById(R.id.page_progress)
  }

  private fun setupGeckoView() {
    geckoView = findViewById(R.id.geckoview)
    val runtime = GeckoRuntime.create(this)
    geckoSession.open(runtime)
    geckoView.setSession(geckoSession)
    geckoSession.loadUri(INITIAL_URL)
    urlEditText.setText(INITIAL_URL)

    geckoSession.progressDelegate = createProgressDelegate()
    geckoSession.settings.useTrackingProtection = true
    geckoSession.contentBlockingDelegate = createBlockingDelegate()
    setupTrackersCounter()
  }

  private fun setupToolbar() {
    setSupportActionBar(findViewById(R.id.toolbar))
    supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
  }

  private fun setupUrlEditText() {
    urlEditText = findViewById(R.id.location_view)
    urlEditText.setOnEditorActionListener(object : View.OnFocusChangeListener, TextView.OnEditorActionListener {

      override fun onFocusChange(view: View?, hasFocus: Boolean) = Unit

      override fun onEditorAction(textView: TextView, actionId: Int, event: KeyEvent?): Boolean {
        onCommit(textView.text.toString())
        textView.hideKeyboard()
        return true
      }

    })
  }

  fun onCommit(text: String) {
    clearTrackersCount()

    if ((text.contains(".") || text.contains(":")) && !text.contains(" ")) {
      geckoSession.loadUri(text)
    } else {
      geckoSession.loadUri(SEARCH_URI_BASE + text)
    }
    geckoView.requestFocus()
  }

  private fun createProgressDelegate(): GeckoSession.ProgressDelegate {
    return object : GeckoSession.ProgressDelegate {
      override fun onPageStop(session: GeckoSession, success: Boolean) = Unit
      override fun onSecurityChange(session: GeckoSession, securityInfo: GeckoSession.ProgressDelegate.SecurityInformation) = Unit
      override fun onPageStart(session: GeckoSession, url: String) = Unit

      override fun onProgressChange(session: GeckoSession, progress: Int) {
        progressView.progress = progress

        if (progress in 1..99) {
          progressView.visibility = View.VISIBLE
        } else {
          progressView.visibility = View.GONE
        }
      }
    }
  }

  private fun createBlockingDelegate(): ContentBlocking.Delegate {
    return object : ContentBlocking.Delegate {
      override fun onContentBlocked(session: GeckoSession, event: ContentBlocking.BlockEvent) {
        trackersBlockedList = trackersBlockedList + event
        trackersCount.text = "${trackersBlockedList.size}"
      }
    }
  }

  private fun setupTrackersCounter() {
    trackersCount = findViewById(R.id.trackers_count)
    trackersCount.text = "0"

    trackersCount.setOnClickListener {

      if (trackersBlockedList.isNotEmpty()) {
        val friendlyURLs = getFriendlyTrackersUrls()
        showDialog(friendlyURLs)
      }
    }
  }

  private fun getFriendlyTrackersUrls(): List<Spanned> {
    return trackersBlockedList.map { blockEvent ->

      val host = Uri.parse(blockEvent.uri).host
      val category = blockEvent.categoryToString()

      Html.fromHtml("<b><font color='#D55C7C'>[$category]</font></b> <br/> $host", HtmlCompat.FROM_HTML_MODE_COMPACT)

    }
  }

  private fun ContentBlocking.BlockEvent.categoryToString(): String {
    val stringResource = when (categories) {
      ContentBlocking.NONE -> R.string.none
      ContentBlocking.AT_ANALYTIC -> R.string.analytic
      ContentBlocking.AT_AD -> R.string.ad
      ContentBlocking.AT_TEST -> R.string.test
      ContentBlocking.SB_MALWARE -> R.string.malware
      ContentBlocking.SB_UNWANTED -> R.string.unwanted
      ContentBlocking.AT_SOCIAL -> R.string.social
      ContentBlocking.AT_CONTENT -> R.string.content
      ContentBlocking.SB_HARMFUL -> R.string.harmful
      ContentBlocking.SB_PHISHING -> R.string.phishing
      else -> R.string.none

    }
    return getString(stringResource)
  }

  private fun clearTrackersCount() {
    trackersBlockedList = emptyList()
    trackersCount.text = "0"
  }

}
