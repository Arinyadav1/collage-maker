package com.collageMaker.data.ml

import android.graphics.Rect
import com.collageMaker.data.model.AppearanceSegment
import com.collageMaker.data.model.DetectedFaceFrame
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class AppearanceClusteringEngine {

    private class ActiveSegmentTracker(
        val id: String,
        var startTimeMs: Long,
        var lastSeenTimeMs: Long,
        val frames: MutableList<DetectedFaceFrame> = mutableListOf()
    ) {
        fun computeAverageEmbedding(): FloatArray {
            if (frames.isEmpty()) return FloatArray(128)
            val sum = FloatArray(frames.first().embedding.size)
            for (f in frames) {
                for (i in f.embedding.indices) {
                    sum[i] += f.embedding[i]
                }
            }
            var sumSq = 0f
            for (i in sum.indices) {
                sum[i] /= frames.size
                sumSq += sum[i] * sum[i]
            }
            val norm = sqrt(sumSq.toDouble()).toFloat()
            if (norm > 1e-6f) {
                for (i in sum.indices) {
                    sum[i] /= norm
                }
            }
            return sum
        }
    }

    fun clusterVideoFaceFrames(
        faceFrames: List<DetectedFaceFrame>,
        similarityThreshold: Float = 0.50f
    ): List<Pair<Int, List<AppearanceSegment>>> {
        if (faceFrames.isEmpty()) return emptyList()

        val sortedFrames = faceFrames.sortedBy { it.frameTimestampMs }

        val activeTrackers = mutableListOf<ActiveSegmentTracker>()
        val finalizedSegments = mutableListOf<AppearanceSegment>()

        var segmentCounter = 1

        val frameGroupsByTime = sortedFrames.groupBy { it.frameTimestampMs }
        val sortedTimestamps = frameGroupsByTime.keys.sorted()

        for (ts in sortedTimestamps) {
            val currentFaces = frameGroupsByTime[ts] ?: continue

            val staleTrackers = activeTrackers.filter { ts - it.lastSeenTimeMs > 850 }
            for (stale in staleTrackers) {
                finalizedSegments.add(
                    AppearanceSegment(
                        segmentId = stale.id,
                        startTimeMs = stale.startTimeMs,
                        endTimeMs = stale.lastSeenTimeMs,
                        faceFrames = stale.frames.toList(),
                        representativeEmbedding = stale.computeAverageEmbedding()
                    )
                )
            }
            activeTrackers.removeAll(staleTrackers)

            val matchedTrackerIndices = mutableSetOf<Int>()

            for (face in currentFaces) {
                var bestTrackerIdx = -1
                var bestSimilarity = -1.0f

                for ((idx, tracker) in activeTrackers.withIndex()) {
                    if (idx in matchedTrackerIndices) continue
                    val lastFrame = tracker.frames.last()

                    val iou = calculateIoU(face.faceBoundingBox, lastFrame.faceBoundingBox)
                    val sim = FaceEmbeddingEngine.calculateCosineSimilarity(face.embedding, lastFrame.embedding)

                    val matchScore = if (iou > 0.25f) max(sim, iou) else sim

                    if (matchScore > 0.40f && matchScore > bestSimilarity) {
                        bestSimilarity = matchScore
                        bestTrackerIdx = idx
                    }
                }

                if (bestTrackerIdx != -1) {
                    val tracker = activeTrackers[bestTrackerIdx]
                    tracker.frames.add(face)
                    tracker.lastSeenTimeMs = ts
                    matchedTrackerIndices.add(bestTrackerIdx)
                } else {
                    val newTracker = ActiveSegmentTracker(
                        id = "seg_${segmentCounter++}",
                        startTimeMs = ts,
                        lastSeenTimeMs = ts,
                        frames = mutableListOf(face)
                    )
                    activeTrackers.add(newTracker)
                    matchedTrackerIndices.add(activeTrackers.lastIndex)
                }
            }
        }

        for (tracker in activeTrackers) {
            finalizedSegments.add(
                AppearanceSegment(
                    segmentId = tracker.id,
                    startTimeMs = tracker.startTimeMs,
                    endTimeMs = tracker.lastSeenTimeMs,
                    faceFrames = tracker.frames.toList(),
                    representativeEmbedding = tracker.computeAverageEmbedding()
                )
            )
        }

        val segmentsToCluster = finalizedSegments.filter { seg ->
            val avgSharpness = seg.faceFrames.map { it.sharpnessScore }.average()
            seg.faceFrames.isNotEmpty() && avgSharpness >= 0.01
        }.ifEmpty { finalizedSegments }

        if (segmentsToCluster.isEmpty()) return emptyList()

        val personClusters = groupSegmentsIntoPersons(segmentsToCluster, similarityThreshold)
        return personClusters
    }

    private fun groupSegmentsIntoPersons(
        segments: List<AppearanceSegment>,
        threshold: Float
    ): List<Pair<Int, List<AppearanceSegment>>> {
        val clusters = mutableListOf<MutableList<AppearanceSegment>>()

        for (seg in segments) {
            var bestClusterIdx = -1
            var maxSim = -1.0f

            for ((idx, cluster) in clusters.withIndex()) {
                val hasTimeOverlap = cluster.any { existing ->
                    max(seg.startTimeMs, existing.startTimeMs) <= min(seg.endTimeMs, existing.endTimeMs)
                }
                if (hasTimeOverlap) continue

                val avgSim = cluster.map { existing ->
                    FaceEmbeddingEngine.calculateCosineSimilarity(seg.representativeEmbedding, existing.representativeEmbedding)
                }.average().toFloat()

                if (avgSim >= threshold && avgSim > maxSim) {
                    maxSim = avgSim
                    bestClusterIdx = idx
                }
            }

            if (bestClusterIdx != -1) {
                clusters[bestClusterIdx].add(seg)
            } else {
                clusters.add(mutableListOf(seg))
            }
        }

        return clusters.mapIndexed { index, list ->
            Pair(index + 1, list)
        }
    }

    private fun calculateIoU(rectA: Rect, rectB: Rect): Float {
        val interLeft = max(rectA.left, rectB.left)
        val interTop = max(rectA.top, rectB.top)
        val interRight = min(rectA.right, rectB.right)
        val interBottom = min(rectA.bottom, rectB.bottom)

        if (interLeft >= interRight || interTop >= interBottom) return 0.0f

        val interArea = (interRight - interLeft) * (interBottom - interTop)
        val areaA = rectA.width() * rectA.height()
        val areaB = rectB.width() * rectB.height()

        val unionArea = areaA + areaB - interArea
        if (unionArea <= 0) return 0.0f
        return interArea.toFloat() / unionArea.toFloat()
    }
}
