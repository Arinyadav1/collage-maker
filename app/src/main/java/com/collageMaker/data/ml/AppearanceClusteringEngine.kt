package com.collageMaker.data.ml

import android.graphics.Rect
import android.util.Log
import com.collageMaker.data.model.AppearanceSegment
import com.collageMaker.data.model.DetectedFaceFrame
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Converts a flat list of per-frame face detections into a list of
 * (personId → appearances) pairs.
 *
 * Pipeline:
 *  1. TRACKING  – IoU + embedding similarity links consecutive detections of the
 *                 same face into continuous [AppearanceSegment] objects. Two trackers
 *                 that are alive at the same instant are provably different people;
 *                 their segment-ID pair is recorded in [concurrentSegmentPairs].
 *  2. CLUSTERING – Segments are merged into person-identity clusters.
 *                  Hard constraint A: time overlap → different people.
 *                  Hard constraint B: concurrent tracker pair → different people.
 *                  Soft hint: embedding cosine similarity ≥ threshold → same person.
 */
class AppearanceClusteringEngine {

    // ── inner tracker ────────────────────────────────────────────────────────

    private class ActiveSegmentTracker(
        val id: String,
        var startTimeMs: Long,
        var lastSeenTimeMs: Long,
        val frames: MutableList<DetectedFaceFrame> = mutableListOf()
    ) {
        /** L2-normalised average of all frame embeddings in this segment. */
        fun computeAverageEmbedding(): FloatArray {
            if (frames.isEmpty()) return FloatArray(128)
            val sum = FloatArray(frames.first().embedding.size)
            for (f in frames) {
                for (i in f.embedding.indices) sum[i] += f.embedding[i]
            }
            var sumSq = 0f
            for (i in sum.indices) {
                sum[i] /= frames.size
                sumSq  += sum[i] * sum[i]
            }
            val norm = sqrt(sumSq.toDouble()).toFloat()
            if (norm > 1e-6f) {
                for (i in sum.indices) sum[i] /= norm
            }
            return sum
        }

        /**
         * Returns the sharpest frame from the most-recent [windowSize] frames.
         * Avoids using a motion-blurred transition frame as the identity anchor.
         */
        fun bestRecentFrame(windowSize: Int = 5): DetectedFaceFrame {
            val window = if (frames.size <= windowSize) frames else frames.takeLast(windowSize)
            return window.maxByOrNull { it.sharpnessScore } ?: frames.last()
        }
    }

    // ── public entry point ───────────────────────────────────────────────────

    fun clusterVideoFaceFrames(
        faceFrames: List<DetectedFaceFrame>,
        /**
         * Minimum cosine similarity between two segment embeddings for them to be
         * considered the same person. The value is deliberately lower than the
         * intra-frame tracking threshold: segment embeddings average several clean
         * frames, while the simultaneous-visibility constraint prevents two people
         * from being merged simply because their appearance is similar.
         */
        similarityThreshold: Float = 0.60f
    ): List<Pair<Int, List<AppearanceSegment>>> {

        if (faceFrames.isEmpty()) {
            Log.d(TAG, "No face frames received — returning empty result.")
            return emptyList()
        }

        Log.d(TAG, "Starting tracking on ${faceFrames.size} face-frame detections.")

        val sortedFrames       = faceFrames.sortedBy { it.frameTimestampMs }
        val activeTrackers     = mutableListOf<ActiveSegmentTracker>()
        val finalizedSegments  = mutableListOf<AppearanceSegment>()

        /**
         * Hard constraint: segment pairs that were tracked simultaneously.
         * These are PROVEN to be different people regardless of embedding similarity.
         * Key = (minSegId, maxSegId) so the pair is order-independent.
         */
        val concurrentSegmentPairs = mutableSetOf<Pair<String, String>>()

        var segmentCounter = 1
        val frameGroupsByTime  = sortedFrames.groupBy { it.frameTimestampMs }
        val sortedTimestamps   = frameGroupsByTime.keys.sorted()

        for (ts in sortedTimestamps) {
            val currentFaces = frameGroupsByTime[ts] ?: continue

            // ── Finalize stale trackers ────────────────────────────────────
            // A continuous appearance may tolerate a few missed sampled frames, but not
            // a long absence. Keeping dead trackers alive for 1.5 s previously made a
            // person in the next shot look "concurrent" with the old person.
            val staleTrackers = activeTrackers.filter { ts - it.lastSeenTimeMs > MAX_TRACK_GAP_MS }
            for (stale in staleTrackers) {
                finalizedSegments.add(stale.toSegment())
                Log.d(TAG, "  Finalized stale segment ${stale.id} [${stale.startTimeMs}–${stale.lastSeenTimeMs}ms, ${stale.frames.size} frames]")
            }
            activeTrackers.removeAll(staleTrackers)

            // ── Match each detected face to an existing tracker ────────────
            val matchedTrackerIndices = mutableSetOf<Int>()

            for (face in currentFaces) {
                var bestTrackerIdx = -1
                var bestScore      = -1.0f

                for ((idx, tracker) in activeTrackers.withIndex()) {
                    if (idx in matchedTrackerIndices) continue

                    val refFrame   = tracker.bestRecentFrame()
                    val iou        = calculateIoU(face.faceBoundingBox, refFrame.faceBoundingBox)
                    val sim        = FaceEmbeddingEngine.calculateCosineSimilarity(
                        face.embedding, refFrame.embedding
                    )
                    val gapMs = ts - tracker.lastSeenTimeMs
                    // Spatial overlap is only reliable across adjacent sampled frames.
                    // Across a cut, a new person can occupy the same screen location, so
                    // require a strong embedding match when there is no reliable overlap.
                    val matchScore = when {
                        gapMs <= 250L && iou > 0.25f -> max(sim, iou)
                        sim >= 0.78f -> sim
                        else -> -1f
                    }

                    if (matchScore > 0.0f && matchScore > bestScore) {
                        bestScore      = matchScore
                        bestTrackerIdx = idx
                    }
                }

                if (bestTrackerIdx != -1) {
                    val tracker = activeTrackers[bestTrackerIdx]
                    tracker.frames.add(face)
                    tracker.lastSeenTimeMs = ts
                    matchedTrackerIndices.add(bestTrackerIdx)
                } else {
                    // New face — start a fresh tracker / appearance segment.
                    val newTracker = ActiveSegmentTracker(
                        id            = "seg_${segmentCounter++}",
                        startTimeMs   = ts,
                        lastSeenTimeMs = ts,
                        frames        = mutableListOf(face)
                    )
                    activeTrackers.add(newTracker)
                    matchedTrackerIndices.add(activeTrackers.lastIndex)
                    Log.d(TAG, "  New tracker ${newTracker.id} started at ${ts}ms  (active trackers now: ${activeTrackers.size})")
                }
            }

            // Only faces detected in this exact sampled frame are concurrent. A tracker
            // awaiting the gap timeout is not visible and must not prevent the next
            // appearance of the same person from joining its identity cluster.
            val visibleTrackers = activeTrackers.filter { it.lastSeenTimeMs == ts }
            for (i in 0 until visibleTrackers.size) {
                for (j in i + 1 until visibleTrackers.size) {
                    val a = visibleTrackers[i].id
                    val b = visibleTrackers[j].id
                    concurrentSegmentPairs.add(Pair(minOf(a, b), maxOf(a, b)))
                }
            }
        }

        // Finalize remaining live trackers (end of video)
        for (tracker in activeTrackers) {
            finalizedSegments.add(tracker.toSegment())
            Log.d(TAG, "  Finalized end-of-video segment ${tracker.id} [${tracker.startTimeMs}–${tracker.lastSeenTimeMs}ms, ${tracker.frames.size} frames]")
        }

        Log.d(TAG, "Tracking complete: ${finalizedSegments.size} raw segments, ${concurrentSegmentPairs.size} concurrent pairs.")

        // ── Quality gate ──────────────────────────────────────────────────────
        // Require ≥ 2 frames (100 ms at 10 fps sample rate) to discard single-frame
        // noise detections and blurry whip-pan glimpses.
        val segmentsToCluster = finalizedSegments.filter { seg ->
            val avgSharpness = seg.faceFrames.map { it.sharpnessScore }.average()
            seg.faceFrames.size >= 2 && avgSharpness >= 0.10f
        }

        Log.d(TAG, "After quality filter: ${segmentsToCluster.size} segments remain.")

        if (segmentsToCluster.isEmpty()) return emptyList()

        val personClusters = groupSegmentsIntoPersons(
            segmentsToCluster,
            similarityThreshold,
            concurrentSegmentPairs
        )

        Log.d(TAG, "Person clusters found: ${personClusters.size}. Details:")
        for ((id, segs) in personClusters) {
            Log.d(TAG, "  Person $id → ${segs.size} appearance(s) " +
                    segs.joinToString { "[${it.startTimeMs}–${it.endTimeMs}ms]" })
        }

        return personClusters
    }

    // ── Person identity clustering ────────────────────────────────────────────

    private fun groupSegmentsIntoPersons(
        segments: List<AppearanceSegment>,
        threshold: Float,
        concurrentPairs: Set<Pair<String, String>>
    ): List<Pair<Int, List<AppearanceSegment>>> {

        val clusters = mutableListOf<MutableList<AppearanceSegment>>()

        for (seg in segments) {
            var bestClusterIdx = -1
            var maxSim         = -1.0f

            for ((idx, cluster) in clusters.withIndex()) {

                // ── Hard constraint A: temporal overlap ───────────────────
                // One person cannot be in two non-overlapping simultaneous shots.
                val hasTimeOverlap = cluster.any { existing ->
                    max(seg.startTimeMs, existing.startTimeMs) <=
                            min(seg.endTimeMs, existing.endTimeMs)
                }
                if (hasTimeOverlap) continue

                // ── Hard constraint B: concurrent tracker pair ────────────
                // If these two segments were EVER tracked simultaneously their
                // trackers were alive at the same instant → provably different people.
                val isConcurrent = cluster.any { existing ->
                    val a = minOf(seg.segmentId, existing.segmentId)
                    val b = maxOf(seg.segmentId, existing.segmentId)
                    concurrentPairs.contains(Pair(a, b))
                }
                if (isConcurrent) continue

                // ── Soft hint: embedding similarity ───────────────────────
                val avgSim = cluster.map { existing ->
                    FaceEmbeddingEngine.calculateCosineSimilarity(
                        seg.representativeEmbedding,
                        existing.representativeEmbedding
                    )
                }.average().toFloat()

                if (avgSim >= threshold && avgSim > maxSim) {
                    maxSim         = avgSim
                    bestClusterIdx = idx
                }
            }

            if (bestClusterIdx != -1) {
                clusters[bestClusterIdx].add(seg)
            } else {
                clusters.add(mutableListOf(seg))
            }
        }

        return clusters.mapIndexed { index, list -> Pair(index + 1, list) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun ActiveSegmentTracker.toSegment() = AppearanceSegment(
        segmentId               = id,
        startTimeMs             = startTimeMs,
        endTimeMs               = lastSeenTimeMs,
        faceFrames              = frames.toList(),
        representativeEmbedding = computeAverageEmbedding()
    )

    private fun calculateIoU(rectA: Rect, rectB: Rect): Float {
        val interLeft   = max(rectA.left,   rectB.left)
        val interTop    = max(rectA.top,    rectB.top)
        val interRight  = min(rectA.right,  rectB.right)
        val interBottom = min(rectA.bottom, rectB.bottom)

        if (interLeft >= interRight || interTop >= interBottom) return 0.0f

        val interArea = (interRight - interLeft) * (interBottom - interTop)
        val areaA     = rectA.width() * rectA.height()
        val areaB     = rectB.width() * rectB.height()
        val unionArea = areaA + areaB - interArea
        if (unionArea <= 0) return 0.0f
        return interArea.toFloat() / unionArea.toFloat()
    }

    companion object {
        private const val TAG = "AppearanceClustering"
        private const val MAX_TRACK_GAP_MS = 700L
    }
}
