package com.xaedenturner.deletescroll

import android.view.accessibility.AccessibilityNodeInfo

class ReelsDetector {

    companion object {
        const val CONFIDENCE_THRESHOLD = 3
    }

    data class DetectionResult(
        val score: Int,
        val reelsTabSelected: Boolean,
        val reelReferenceCount: Int
    ) {
        val triggered get() = score >= CONFIDENCE_THRESHOLD

        // Human-readable summary of exactly which signals fired
        fun describe(): String = buildString {
            append("Score $score/${CONFIDENCE_THRESHOLD} [")
            append("reels_tab=${if (reelsTabSelected) "SELECTED" else "unselected"}, ")
            append("reel_refs=$reelReferenceCount")
            append("]")
        }
    }

    fun detect(rootNode: AccessibilityNodeInfo?): DetectionResult {
        rootNode ?: return DetectionResult(0, false, 0)

        val reelsTabSelected = isReelsTabSelected(rootNode)
        val reelReferenceCount = countNodesContaining(rootNode, "reel")

        var score = 0
        if (reelsTabSelected) score += 3
        when {
            reelReferenceCount >= 2 -> score += 2
            reelReferenceCount == 1 -> score += 1
        }

        return DetectionResult(score, reelsTabSelected, reelReferenceCount)
    }

    private fun isReelsTabSelected(rootNode: AccessibilityNodeInfo): Boolean {
        val candidates = rootNode.findAccessibilityNodeInfosByText("Reels")
        var found = false

        for (node in candidates) {
            val nodeSelected = node.isSelected || node.isChecked
            val parent = node.parent
            val parentSelected = parent?.isSelected == true || parent?.isChecked == true
            parent?.recycle()
            if (nodeSelected || parentSelected) {
                found = true
                break
            }
        }

        candidates.forEach { it.recycle() }
        return found
    }

    private fun countNodesContaining(rootNode: AccessibilityNodeInfo, query: String): Int {
        val nodes = rootNode.findAccessibilityNodeInfosByText(query)
        val count = nodes.size
        nodes.forEach { it.recycle() }
        return count
    }
}