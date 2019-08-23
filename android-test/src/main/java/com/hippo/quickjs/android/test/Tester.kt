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
import android.content.Intent
import android.net.Uri
import com.getkeepsafe.relinker.ReLinker
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import net.lingala.zip4j.core.ZipFile
import java.io.*
import java.lang.ref.WeakReference

class Tester(
  private val context: Context
) {

  private val logFile: File
  private val logFileUri: Uri
  private val printer: MessageHolder

  private val assetsNameFile = File(context.filesDir, "testassets.name")
  private val assetsDir = File(context.filesDir, "testassets")
  private val tempFile = File(context.cacheDir, "testassets.zip")

  @Volatile
  private var testNumber = 0

  var isFinished: Boolean = false
    private set

  init {
    val logDir = File(context.filesDir, "logs")
    logDir.mkdirs()

    logFile = File(logDir, "log.txt")
    logFileUri = Uri.Builder()
      .scheme("content")
      .authority("com.hippo.quickjs.android.test.fileprovider")
      .appendPath("logs")
      .appendPath("log.txt")
      .build()
    logFile.delete()

    printer = MessageHolder(logFile)
  }

  fun registerMessageQueuePrinter(messageQueuePrinter: MessageQueuePrinter) {
    printer.registerMessageQueuePrinter(messageQueuePrinter)
  }

  private fun ensureAssetFiles() {
    val exceptAssetsName = try {
      assetsNameFile.readText()
    } catch (e: IOException) {
      null
    }

    var actualAssetsName: String? = null
    for (asset in context.assets.list("") ?: emptyArray()) {
      if (asset.startsWith("testassets-") && asset.endsWith(".crc32")) {
        actualAssetsName = asset
      }
    }
    if (actualAssetsName == null) {
      error("Can't find test assets")
    }

    if (exceptAssetsName != actualAssetsName) {
      printer.print("Need exact assets")
      printer.print("except = $exceptAssetsName")
      printer.print("actual = $actualAssetsName")

      assetsDir.deleteRecursively()
      if (!assetsDir.mkdirs()) {
        error("Can't create test assets dir")
      }

      context.assets.open("testassets.zip").use { `in` ->
        tempFile.outputStream().use { out ->
          `in`.copyTo(out)
        }
      }

      val zipFile = ZipFile(tempFile)
      zipFile.extractAll(assetsDir.path)

      assetsNameFile.writeText(actualAssetsName)

      printer.print("All test assets are copied")
    } else {
      printer.print("All test assets are UP-TO-DATE")
    }
  }

  private fun ensureExecutable() {
    ReLinker.loadLibrary(context, "qjs")
    ReLinker.loadLibrary(context, "qjsbn")
  }

  private fun runTest(name: String, executable: String, parameter: String) {
    printer.print("********************************")
    printer.print("** ${++testNumber}. $name")
    printer.print("********************************")
    val code = run(executable, parameter)
    printer.print("EXIT CODE: $code")
  }

  private fun runTest(executable: String, parameter: String) {
    val name = "$executable $parameter"
    runTest(name, executable, parameter)
  }

  private fun js2c() {
    runTest("qjsc", "-c -o repl.c -m repl.js")
    runTest("qjsbnc", "-c -o repl-bn.c -m repl.js")
    runTest("qjsbnc", "-c -o qjscalc.c qjscalc.js")
  }

  private fun test() {
    runTest("qjs", "tests/test_closure.js")
    runTest("qjs", "tests/test_op.js")
    runTest("qjs", "tests/test_builtin.js")
    runTest("qjs", "tests/test_loop.js")
    // tmpfile returns null
    runTest("qjs", "tests/test_std.js")
    runTest("qjsbn", "tests/test_closure.js")
    runTest("qjsbn", "tests/test_op.js")
    runTest("qjsbn", "tests/test_builtin.js")
    runTest("qjsbn", "tests/test_loop.js")
    // tmpfile returns null
    runTest("qjsbn", "tests/test_std.js")
    runTest("qjsbn", "--qjscalc tests/test_bignum.js")
  }

  private fun stats() {
    runTest("qjs", "-qd")
  }

  private fun microbench() {
    runTest("qjs", "--std tests/microbench.js")
  }

  private fun runTest262() {
    runTest("run-test262", "-m -c test262o.conf")
    runTest("run-test262", "-u -c test262o.conf")
    runTest("run-test262", "-m -c test262.conf")
    runTest("run-test262", "-m -c test262.conf -a")
    runTest("run-test262", "-u -c test262.conf -a")
    runTest("run-test262", "-m -c test262.conf -E -a")
  }

  private fun runTest262bn() {
    runTest("run-test262-bn", "-m -c test262bn.conf")
    runTest("run-test262-bn", "-m -c test262bn.conf -a")
  }

  private fun benchV8() {
    runTest("qjs", "-d tests/bench-v8/combined.js")
  }

  private fun printThrowable(e: Throwable) {
    val baos = ByteArrayOutputStream()
    PrintWriter(baos).apply {
      e.printStackTrace(this)
      flush()
    }
    ByteArrayInputStream(baos.toByteArray()).reader().buffered().forEachLine { printer.print(it) }
  }

  fun start() {
    GlobalScope.launch {
      try {
        ensureAssetFiles()
        ensureExecutable()

        js2c()

        test()
        stats()
        microbench()
        runTest262()
        runTest262bn()
        benchV8()

        printer.print("********************************")
        printer.print("********************************")
        printer.print("********************************")
        printer.print("TEST COMPLETE")
      } catch (e: Throwable) {
        e.printStackTrace()
        printer.print("********************************")
        printer.print("********************************")
        printer.print("********************************")
        printer.print("TEST INTERRUPT")
        printThrowable(e)
      }
      printer.finish()

      (this + Dispatchers.Main).launch {
        isFinished = true
        shareLogFile()
      }
    }
  }

  fun shareLogFile() {
    val title = "Send Log File"
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_STREAM, logFileUri)
    intent.putExtra(Intent.EXTRA_SUBJECT, title)
    intent.putExtra(Intent.EXTRA_TEXT, title)
    val chooser = Intent.createChooser(intent, title)
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
  }

  private fun run(executable: String, parameter: String): Int = runBlocking {
    val nativeDir = context.applicationInfo.nativeLibraryDir
    val executableFile = File(nativeDir, "lib$executable.so")
    val command = "${executableFile.path} $parameter"

    val processChannel = Channel<Process>()

    val job1 = (GlobalScope + Dispatchers.IO).launch {
      val process = processChannel.receive()
      process.inputStream.reader().buffered().forEachLine { printer.print(it) }
    }

    val job2 = (GlobalScope + Dispatchers.IO).launch {
      val process = processChannel.receive()
      process.errorStream.reader().buffered().forEachLine { printer.print(it) }
    }

    val process = Runtime.getRuntime().exec(command, null, assetsDir)
    processChannel.send(process)
    processChannel.send(process)
    val code = process.waitFor()

    job1.join()
    job2.join()

    code
  }

  /**
   * MessageList cache messages and dispatch message to the last registered MultiPrinter.
   */
  private class MessageHolder(
    logFile: File
  ) : MessagePrinter {

    private val messages = MessageQueue()
    private val writer = logFile.writer()

    @Volatile
    private var weakMessageQueuePrinter: WeakReference<MessageQueuePrinter>? = null

    @Synchronized
    fun registerMessageQueuePrinter(messageQueuePrinter: MessageQueuePrinter) {
      messageQueuePrinter.print(messages.copy())
      weakMessageQueuePrinter = WeakReference(messageQueuePrinter)
    }

    @Synchronized
    override fun print(message: String) {
      messages.add(message)
      weakMessageQueuePrinter?.get()?.print(message)
      if (!message.endsWith("\u001B[K")) {
        writer.write(message)
        writer.write("\n")
      }
    }

    fun finish() {
      writer.flush()
      writer.close()
    }
  }
}
