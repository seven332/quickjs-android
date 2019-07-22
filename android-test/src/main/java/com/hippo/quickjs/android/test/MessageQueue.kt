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

import java.util.*

class MessageQueue(
  private val bufferSize: Int = 8192
) {

  constructor(other: MessageQueue): this(other.bufferSize) {
    this.messages.addAll(other.messages)
  }

  private val messages = LinkedList<String>()

  val size: Int
    get() = messages.size

  operator fun get(index: Int): String = messages[index]

  fun add(message: String) {
    // Remove the last one if it ends with \u001B[K
    if (messages.isNotEmpty()) {
      if (messages.last.endsWith("\u001B[K")) {
        messages.removeLast()
      }
    }

    while (messages.size >= bufferSize) {
      messages.removeFirst()
    }
    messages.addLast(message)
  }

  fun addAll(messageQueue: MessageQueue) {
    messageQueue.messages.forEach {
      add(it)
    }
  }

  fun copy(): MessageQueue = MessageQueue(this)
}
