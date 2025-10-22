package com.example.momoautoreply

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Handler
import android.os.Looper
import android.content.Context
import android.os.Bundle
import java.util.*

class BotAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val pref by lazy { getSharedPreferences("momo_bot", Context.MODE_PRIVATE) }

    // keep a set of handled greeting IDs to avoid duplicate replies
    private val handled = HashSet<Int>()
    private var lastSendTime = 0L

    // default greeting keywords
    private val keywords = listOf("招呼", "打招呼", "hi", "hello", "你好", "hi!", "hello!")

    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        val targetPackage = pref.getString("target_package", "com.immomo.momo") ?: "com.immomo.momo"
        // only react to target package events or notifications
        if (!pkg.contains("momo", ignoreCase = true) && pkg != targetPackage) {
            if (event.eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) return
        }

        val root = rootInActiveWindow

        // Check notifications text for greeting keywords first
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val texts = event.text ?: return
            for (t in texts) {
                val s = t?.toString() ?: continue
                if (isGreetingText(s)) {
                    scheduleReply(null)
                    return
                }
            }
        }

        // For window content changes / state changes within the target app, scan nodes
        if (root != null) {
            val found = ArrayList<AccessibilityNodeInfo>()
            findGreetingNodes(root, found)
            for (node in found) {
                val id = computeNodeId(node)
                if (!handled.contains(id)) {
                    handled.add(id)
                    scheduleReply(node)
                }
            }
        }
    }

    override fun onInterrupt() {}

    private fun scheduleReply(triggerNode: AccessibilityNodeInfo?) {
        val intervalSec = pref.getInt("interval_seconds", 2)
        val now = System.currentTimeMillis()
        if (now - lastSendTime < intervalSec * 1000L) return

        val message = pref.getString("msg0", "你好！可以认识一下吗？") ?: "你好！可以认识一下吗？"

        handler.postDelayed({
            try {
                val root = rootInActiveWindow ?: return@postDelayed
                val edits = ArrayList<AccessibilityNodeInfo>()
                findEditTextNodes(root, edits)
                if (edits.isEmpty()) return@postDelayed
                val edit = edits[0]

                val args = Bundle()
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
                edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

                val send = findSendButton(root)
                if (send != null) {
                    send.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                } else {
                    edit.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                lastSendTime = System.currentTimeMillis()
            } catch (t: Throwable) {
            }
        }, intervalSec * 1000L)
    }

    private fun isGreetingText(s: String): Boolean {
        val low = s.toLowerCase()
        for (k in keywords) {
            if (low.contains(k)) return true
        }
        return false
    }

    private fun findGreetingNodes(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        try {
            val text = node.text?.toString() ?: ""
            if (text.isNotBlank() && text.length <= 120 && isGreetingText(text)) {
                out.add(node)
            }
            for (i in 0 until node.childCount) {
                val c = node.getChild(i) ?: continue
                findGreetingNodes(c, out)
            }
        } catch (t: Throwable) { }
    }

    private fun findEditTextNodes(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        try {
            val cls = node.className?.toString() ?: ""
            if ((cls.contains("EditText") || node.isEditable) && node.isEnabled) {
                out.add(node)
            }
            for (i in 0 until node.childCount) {
                val c = node.getChild(i) ?: continue
                findEditTextNodes(c, out)
            }
        } catch (t: Throwable) { }
    }

    private fun findSendButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        try {
            if (node.text != null) {
                val t = node.text.toString()
                if (t.contains("发送") || t.equals("Send", true) || t.contains("发消息")) return node
            }
            if (node.contentDescription != null) {
                val d = node.contentDescription.toString()
                if (d.contains("发送") || d.equals("Send", true)) return node
            }
            if (node.isClickable && node.className != null && node.className.toString().contains("Button")) {
                return node
            }
            for (i in 0 until node.childCount) {
                val c = node.getChild(i) ?: continue
                val found = findSendButton(c)
                if (found != null) return found
            }
        } catch (t: Throwable) { }
        return null
    }

    private fun computeNodeId(node: AccessibilityNodeInfo): Int {
        val sb = StringBuilder()
        try {
            sb.append(node.text?.toString() ?: "")
            sb.append("|")
            sb.append(node.viewIdResourceName ?: "")
            val b = android.graphics.Rect()
            node.getBoundsInScreen(b)
            sb.append("|").append(b.left).append(',').append(b.top)
        } catch (t: Throwable) { }
        return sb.toString().hashCode()
    }
}
