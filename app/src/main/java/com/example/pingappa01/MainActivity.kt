package com.example.pingappa01

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import com.example.pingappa01.databinding.ActivityMainBinding
import com.example.pingappa01.databinding.ResultRowBinding
import java.lang.ref.WeakReference
import java.util.*

class MainActivity : AppCompatActivity() {
  private lateinit var activityMainBinding: ActivityMainBinding

  private var myHandler = MyHandler(WeakReference(this), Looper.getMainLooper())

  private var mThread: Thread? = null
  private var isThreadRunning = false
  private var errorMessage = ""

  val queue: Queue<String> = ArrayDeque()

  companion object {
    private const val PING = 102
    private const val STOP = 101
    private const val START = 100
  }

  private fun updateText() {
    if (queue.size == 0) return

    val str = queue.remove()
    Log.i("queue remove", str)

    val resultRowBinding = ResultRowBinding.inflate(layoutInflater)
    resultRowBinding.rowText.text = str

    val resultRowView = resultRowBinding.root
    activityMainBinding.tableLayout.addView(resultRowView)
  }

  private class MyHandler(private val outerClass: WeakReference<MainActivity>, looper: Looper):
    Handler(looper) {
    override fun handleMessage(msg: Message) {
      super.handleMessage(msg)
      when (msg.what) {
        PING -> outerClass.get()?.updateText()
        STOP -> outerClass.get()?.togglePing(false)
        START -> outerClass.get()?.togglePing(true)
      }
    }
  }

  internal inner class PingProcess: Runnable {
    override fun run() {
      val cmd = mutableListOf("ping", "-c", "5", "192.168.43.231")

      val builder = ProcessBuilder()
      builder.command(cmd)

      val process = builder.start()

      val stdInput = process.inputStream.bufferedReader()

      val messageStart = Message()
      messageStart.what = START
      myHandler.sendMessage(messageStart)

      while (isThreadRunning) {
        val currentStr
          = stdInput.readLine() // wrap into try catch block later
          ?: break

        queue.add(currentStr)

        val messagePing = Message()
        messagePing.what = PING
        myHandler.sendMessage(messagePing)
      }
      if (isThreadRunning) {
         errorMessage =
//           try {
           process.errorStream.bufferedReader().readLine() ?: ""//"no error"
//         }
//         catch (e: Throwable) {
//           e.message.toString()
////           e.toString()
////           "somthung wrong"
//         }
//           ?: "___________"
      }

      val messageStop = Message()
      messageStop.what = STOP
      myHandler.sendMessage(messageStop)

      process.destroy()
    }
  }

  private fun togglePing(on: Boolean) {
    if (errorMessage != "") {
      Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
      Log.i("errorMessage", errorMessage)
    }

    if (on) {
      activityMainBinding.pingButton.text = resources.getString(R.string.btn_stop)
    } else {
      mThread = null
      activityMainBinding.pingButton.text = resources.getString(R.string.btn_start)
    }

    errorMessage = ""
    activityMainBinding.pingButton.isClickable = true
  }

  private fun triggerTogglePing() {
    activityMainBinding.pingButton.isClickable = false

    val doEnable = mThread == null
    isThreadRunning = doEnable

    if (doEnable) {
      mThread = Thread(PingProcess())
      mThread?.start()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(activityMainBinding.root)

    activityMainBinding.pingButton.setOnClickListener {
//      Log.i("PING BUTTON", "PING CLICKED!")
      triggerTogglePing()
    }
  }
}