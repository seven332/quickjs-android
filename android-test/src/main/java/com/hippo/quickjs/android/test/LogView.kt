/*
 * Copyright 2019 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.quickjs.android.test

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView

/**
 * LogView display lots of messages.
 */
class LogView(context: Context) : ListView(context), MessageQueuePrinter {

  private val messages = MessageQueue()

  @Volatile
  private var isClosed = false

  private var pendingLastVisiblePosition = 0
  private var lastVisibleBottomShows = true

  val Int.dp: Int
    get() {
      val f = context.resources.displayMetrics.density * this
      return (if (f >= 0) (f + 0.5f) else (f - 0.5f)).toInt()
    }

  val Int.sp: Float
    get() = context.resources.displayMetrics.scaledDensity * this

  init {
    adapter = Adapter(context)
    divider = null
    selector = ColorDrawable(Color.TRANSPARENT)
    clipToPadding = false
    setPadding(4.dp, 4.dp, 4.dp, 4.dp)
  }

  fun close() {
    isClosed = true
  }

  private fun postIfNotClosed(block: () -> Unit) {
    if (!isClosed) {
      post {
        if (!isClosed) {
          block()
        }
      }
    }
  }

  private inline fun catchNewMessages(block: () -> Unit) {
    val scrollToBottom = pendingLastVisiblePosition == adapter.count - 1 && lastVisibleBottomShows
    block()
    (adapter as BaseAdapter).notifyDataSetChanged()
    if (scrollToBottom) {
      setSelection(adapter.count - 1)
      pendingLastVisiblePosition = adapter.count - 1
      lastVisibleBottomShows = true
    }
  }

  override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
    super.onScrollChanged(l, t, oldl, oldt)
    pendingLastVisiblePosition = lastVisiblePosition
    lastVisibleBottomShows = if (childCount > 0) getChildAt(childCount - 1).bottom <= height - paddingBottom else true
  }

  override fun print(message: String) {
    postIfNotClosed {
      catchNewMessages {
        messages.add(message)
      }
    }
  }

  override fun print(messages: MessageQueue) {
    postIfNotClosed {
      catchNewMessages {
        this.messages.addAll(messages)
      }
    }
  }

  inner class Adapter(private val context: Context): BaseAdapter() {

    private fun fixMessage(message: String) =
      if (message.endsWith("\u001B[K")) message.substring(0, message.length - 3) else message

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
      var view = convertView as? TextView
      if (view == null) {
        view = TextView(context).apply {
          typeface = Typeface.MONOSPACE
          textSize = 14.0f
          setLineSpacing(3.sp, 1.0f)
          layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
      }
      view.text = fixMessage(messages[position])
      return view
    }

    override fun getItem(position: Int): Any = messages[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int = messages.size
  }
}
