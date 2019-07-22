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
import android.graphics.Typeface
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

  init {
    adapter = Adapter(context)
  }

  @Volatile
  var isClosed = false

  fun close() {
    isClosed = true
  }

  private fun onCatchNewMessages() {
    (adapter as BaseAdapter).notifyDataSetChanged()
    setSelection(adapter.count - 1)
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

  override fun print(message: String) {
    postIfNotClosed {
      messages.add(message)
      onCatchNewMessages()
    }
  }

  override fun print(messages: MessageQueue) {
    if (!isClosed) {
      post {
        if (!isClosed) {
          this.messages.addAll(messages)
          onCatchNewMessages()
        }
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
