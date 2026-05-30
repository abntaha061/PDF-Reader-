package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument as AndroidPdfDocument
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object PdfEngine {

    /**
     * Creates a fully populated sample PDF model for hands-on deep editing.
     */
    fun createSampleDocument(title: String, author: String, theme: String): PdfDocumentState {
        val id = UUID.randomUUID().toString()
        
        val page1 = PdfPage(
            index = 0,
            width = 1000,
            height = 1400,
            textBlocks = listOf(
                PdfTextBlock(
                    id = "p1_t1",
                    text = "APPLICATION USER MANUAL",
                    x = 100f,
                    y = 120f,
                    fontSize = 32f,
                    fontColor = "#C62828",
                    fontName = "Serif",
                    width = 800f
                ),
                PdfTextBlock(
                    id = "p1_t2",
                    text = "Welcome to PDF Reader Professional - your complete workspace for extreme document manipulation, deep content editing, and AI-assisted OCR.",
                    x = 100f,
                    y = 200f,
                    fontSize = 18f,
                    fontColor = "#333333",
                    fontName = "Roboto",
                    width = 800f
                ),
                PdfTextBlock(
                    id = "p1_t3",
                    text = "Getting Started:",
                    x = 100f,
                    y = 300f,
                    fontSize = 22f,
                    fontColor = "#B71C1C",
                    fontName = "SansSerif",
                    width = 800f
                ),
                PdfTextBlock(
                    id = "p1_t4",
                    text = "1. To EDIT any text block directly on page, long-press or tap Edit mode from the top bar. Tap the text block to change font name, color, size, and body text.\n" +
                           "2. To manipulate IMAGES, select any image block, choose rotate, replace, scale, or edit cropping.\n" +
                           "3. Use the PDF Tools menu below for advanced merges, splits, encryption, decryption, watermarking, and document conversions.",
                    x = 100f,
                    y = 350f,
                    fontSize = 16f,
                    fontColor = "#444444",
                    fontName = "Roboto",
                    width = 800f
                ),
                PdfTextBlock(
                    id = "p1_t5",
                    text = "For more information, visit our online help portal.",
                    x = 100f,
                    y = 1100f,
                    fontSize = 15f,
                    fontColor = "#1565C0",
                    fontName = "Roboto",
                    width = 800f
                )
            ),
            hyperlinks = listOf(
                PdfHyperlink("p1_h1", "online help portal", "https://google.com/search?q=pdf+reader+pro", null, 100f, 1100f, 320f, 30f)
            ),
            imageBlocks = listOf(
                PdfImageBlock("p1_img1", null, null, 100f, 600f, 500f, 350f, rotation = 0f)
            )
        )

        val page2 = PdfPage(
            index = 1,
            width = 1000,
            height = 1400,
            textBlocks = listOf(
                PdfTextBlock(
                    id = "p2_t1",
                    text = "DOCUMENT SECURITY & ANNOTATIONS",
                    x = 100f,
                    y = 120f,
                    fontSize = 26f,
                    fontColor = "#37474F",
                    fontName = "Serif",
                    width = 800f
                ),
                PdfTextBlock(
                    id = "p2_t2",
                    text = "This page illustrates security configurations, digital certificate signatures, interactive form options, dynamic markups, and stamps.",
                    x = 100f,
                    y = 180f,
                    fontSize = 18f,
                    fontColor = "#555555",
                    fontName = "Roboto",
                    width = 800f
                ),
                PdfTextBlock(
                    id = "p2_t3",
                    text = "INTERACTIVE ACROFORM FIELDS",
                    x = 100f,
                    y = 350f,
                    fontSize = 16f,
                    fontColor = "#C62828",
                    fontName = "Monospace",
                    width = 600f
                ),
                PdfTextBlock(
                    id = "p2_t4",
                    text = "AUTHORIZED DIGITAL SIGNATURE DEPT:",
                    x = 100f,
                    y = 800f,
                    fontSize = 16f,
                    fontColor = "#888888",
                    fontName = "Roboto",
                    width = 600f
                )
            ),
            annotations = listOf(
                PdfAnnotation.Highlight("p2_a1", 100f, 180f, 1, 720f, 24f, "#FFFF00"),
                PdfAnnotation.Underline("p2_a2", 100f, 120f, 1, 550f, 4f, "#C62828"),
                PdfAnnotation.Squiggly("p2_a3", 100f, 142f, 1, 550f, 6f, "#E53935"),
                PdfAnnotation.Geometry("p2_a4", 750f, 500f, 1, PdfAnnotation.GeometryType.ARROW, 120f, 120f, "#2E7D32"),
                PdfAnnotation.Geometry("p2_a5", 650f, 850f, 1, PdfAnnotation.GeometryType.ROUND_RECT, 240f, 150f, "#1565C0"),
                PdfAnnotation.Stamp("p2_a6", 650f, 300f, 1, "APPROVED", "#4CAF50")
            ),
            formFields = listOf(
                PdfFormField("f2_f1", "Signer Full Name", FormFieldType.TEXT, 100f, 400f, value = "Alex Rider", isRequired = true),
                PdfFormField("f2_f2", "Receive Notifications?", FormFieldType.CHECKBOX, 100f, 480f, value = "true"),
                PdfFormField("f2_f3", "Corporate Access Department", FormFieldType.DROPDOWN, 100f, 550f, value = "Operations", options = listOf("Management", "Operations", "Finance", "Legal")),
                PdfFormField("f2_f4", "Acknowledge terms of PKI certification?", FormFieldType.RADIO, 100f, 630f, value = "Agree", options = listOf("Agree", "Disagree"))
            )
        )

        return PdfDocumentState(
            id = id,
            name = title.let { if (it.endsWith(".pdf", true)) it else "$it.pdf" },
            metadata = PdfMetadata(title = title, author = author, subject = theme),
            pages = listOf(page1, page2),
            bookmarks = listOf(
                PdfBookmark("Welcome Overview", 0),
                PdfBookmark("Forms, Annotations & Stamps", 1)
            ),
            certificateSignatures = listOf(
                PdfCertificateSignature(
                    id = "cert_1",
                    signerName = author.ifEmpty { "Administrator Agent" },
                    timestamp = "2026-05-30 12:15:00 UTC",
                    algorithm = "SHA-256 with RSA/PKI",
                    integrityVerified = true,
                    certificateSerial = "SN-938210-AISTUDIO"
                )
            )
        )
    }

    /**
     * Merge multiple PDF states together
     */
    fun mergePdfs(documentsToMerge: List<PdfDocumentState>, mergedName: String): PdfDocumentState {
        val mergedPages = mutableListOf<PdfPage>()
        var globalIndex = 0
        val bookmarks = mutableListOf<PdfBookmark>()

        documentsToMerge.forEach { doc ->
            bookmarks.add(PdfBookmark("Source: ${doc.name}", globalIndex))
            doc.pages.forEach { page ->
                mergedPages.add(page.copy(index = globalIndex++))
            }
        }

        return PdfDocumentState(
            id = UUID.randomUUID().toString(),
            name = if (mergedName.endsWith(".pdf", true)) mergedName else "$mergedName.pdf",
            pages = mergedPages,
            bookmarks = bookmarks,
            metadata = PdfMetadata(title = mergedName, author = "PDF Merger Hub")
        )
    }

    /**
     * Splits a document state, retaining only specified pages.
     */
    fun splitPdf(sourceDoc: PdfDocumentState, pageIndexesToKeep: List<Int>, newName: String): PdfDocumentState {
        var globIndex = 0
        val newPages = sourceDoc.pages
            .filter { it.index in pageIndexesToKeep }
            .map { it.copy(index = globIndex++) }

        return PdfDocumentState(
            id = UUID.randomUUID().toString(),
            name = if (newName.endsWith(".pdf", true)) newName else "$newName.pdf",
            pages = newPages,
            bookmarks = sourceDoc.bookmarks.filter { it.pageIndex in pageIndexesToKeep }.map {
                it.copy(pageIndex = pageIndexesToKeep.indexOf(it.pageIndex).coerceAtLeast(0))
            },
            metadata = sourceDoc.metadata.copy(title = newName, subject = "Extracted from ${sourceDoc.name}")
        )
    }

    /**
     * Inserts watermarks on all pages of a PdfDocumentState
     */
    fun watermarkPdf(doc: PdfDocumentState, text: String, color: String = "#33FF0000"): PdfDocumentState {
        val stampLabel = text.uppercase()
        val updatedPages = doc.pages.map { page ->
            val stampId = "watermark_${UUID.randomUUID()}"
            page.copy(
                annotations = page.annotations + PdfAnnotation.Stamp(
                    id = stampId,
                    x = page.width / 2f - 200f,
                    y = page.height / 2f - 100f,
                    pageIndex = page.index,
                    label = stampLabel,
                    color = color
                )
            )
        }
        return doc.copy(pages = updatedPages)
    }

    /**
     * Generates a real native Android PDF file on the device filesystem
     * from our high-fidelity virtual PDF structure using Android's native Graphics.
     */
    fun generateNativePdfFile(context: Context, docState: PdfDocumentState): File {
        val pdfDocument = AndroidPdfDocument()
        
        try {
            docState.pages.forEachIndexed { i, pdfPage ->
                val pageInfo = AndroidPdfDocument.PageInfo.Builder(pdfPage.width, pdfPage.height, i + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Background Paint
                val bgPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, pdfPage.width.toFloat(), pdfPage.height.toFloat(), bgPaint)

                // TextBlocks Paint
                val textPaint = Paint().apply {
                    isAntiAlias = true
                }

                pdfPage.textBlocks.forEach { textBlock ->
                    textPaint.textSize = textBlock.fontSize
                    textPaint.color = try {
                        android.graphics.Color.parseColor(textBlock.fontColor)
                    } catch (e: Exception) {
                        android.graphics.Color.BLACK
                    }
                    
                    // Simple multiline wrapping
                    val lines = textBlock.text.split("\n")
                    var currentY = textBlock.y
                    lines.forEach { line ->
                        canvas.drawText(line, textBlock.x, currentY, textPaint)
                        currentY += textPaint.textSize + 8f
                    }
                }

                // Interactive form fields rendering
                pdfPage.formFields.forEach { field ->
                    val fieldBgPaint = Paint().apply {
                        color = android.graphics.Color.rgb(240, 242, 247)
                        style = Paint.Style.FILL
                    }
                    val fieldBorderPaint = Paint().apply {
                        color = android.graphics.Color.rgb(198, 40, 40) // Primary red border
                        style = Paint.Style.STROKE
                        strokeWidth = 2f
                    }
                    val fieldTextPaint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 15f
                        isAntiAlias = true
                    }

                    val rect = RectF(field.x, field.y, field.x + field.width, field.y + field.height)
                    
                    when (field.type) {
                        FormFieldType.TEXT -> {
                            canvas.drawRoundRect(rect, 4f, 4f, fieldBgPaint)
                            canvas.drawRoundRect(rect, 4f, 4f, fieldBorderPaint)
                            canvas.drawText(field.value.ifEmpty { field.name }, field.x + 8f, field.y + field.height / 2f + 5f, fieldTextPaint)
                        }
                        FormFieldType.CHECKBOX -> {
                            val boxSize = 24f
                            val checkboxRect = RectF(field.x, field.y, field.x + boxSize, field.y + boxSize)
                            canvas.drawRoundRect(checkboxRect, 4f, 4f, fieldBgPaint)
                            canvas.drawRoundRect(checkboxRect, 4f, 4f, fieldBorderPaint)
                            if (field.value == "true") {
                                // Draw an attractive checkmark
                                val checkPaint = Paint().apply {
                                    color = android.graphics.Color.rgb(198, 40, 40)
                                    style = Paint.Style.STROKE
                                    strokeWidth = 3f
                                    strokeCap = Paint.Cap.ROUND
                                    isAntiAlias = true
                                }
                                canvas.drawLine(field.x + 5f, field.y + 12f, field.x + 10f, field.y + 18f, checkPaint)
                                canvas.drawLine(field.x + 10f, field.y + 18f, field.x + 19f, field.y + 6f, checkPaint)
                            }
                            canvas.drawText(field.name, field.x + boxSize + 10f, field.y + 16f, fieldTextPaint)
                        }
                        FormFieldType.DROPDOWN -> {
                            canvas.drawRoundRect(rect, 4f, 4f, fieldBgPaint)
                            canvas.drawRoundRect(rect, 4f, 4f, fieldBorderPaint)
                            canvas.drawText(field.value + " ▾", field.x + 8f, field.y + field.height / 2f + 5f, fieldTextPaint)
                        }
                        FormFieldType.RADIO -> {
                            val radius = 10f
                            val cy = field.y + field.height / 2f
                            canvas.drawCircle(field.x + radius, cy, radius, fieldBgPaint)
                            canvas.drawCircle(field.x + radius, cy, radius, fieldBorderPaint)
                            if (field.value.isNotEmpty()) {
                                val fillPaint = Paint().apply {
                                    color = android.graphics.Color.rgb(198, 40, 40)
                                    style = Paint.Style.FILL
                                    isAntiAlias = true
                                }
                                canvas.drawCircle(field.x + radius, cy, radius - 4f, fillPaint)
                            }
                            canvas.drawText("${field.name}: ${field.value}", field.x + radius * 2f + 10f, cy + 5f, fieldTextPaint)
                        }
                    }
                }

                // Annotations Paints
                pdfPage.annotations.forEach { ann ->
                    when (ann) {
                        is PdfAnnotation.Highlight -> {
                            val highlightPaint = Paint().apply {
                                color = try {
                                    android.graphics.Color.parseColor(ann.color)
                                } catch (e: Exception) {
                                    android.graphics.Color.YELLOW
                                }
                                alpha = 100 // Semi-transparent
                            }
                            canvas.drawRect(ann.x, ann.y, ann.x + ann.width, ann.y + ann.height, highlightPaint)
                        }
                        is PdfAnnotation.Underline -> {
                            val linePaint = Paint().apply {
                                color = try {
                                    android.graphics.Color.parseColor(ann.color)
                                } catch (e: Exception) {
                                    android.graphics.Color.RED
                                }
                                strokeWidth = ann.height.coerceAtLeast(2f)
                                isAntiAlias = true
                            }
                            canvas.drawLine(ann.x, ann.y + ann.height + 2f, ann.x + ann.width, ann.y + ann.height + 2f, linePaint)
                        }
                        is PdfAnnotation.Strikethrough -> {
                            val linePaint = Paint().apply {
                                color = try {
                                    android.graphics.Color.parseColor(ann.color)
                                } catch (e: Exception) {
                                    android.graphics.Color.BLACK
                                }
                                strokeWidth = ann.height.coerceAtLeast(2f)
                                isAntiAlias = true
                            }
                            val midY = ann.y + ann.height / 2f
                            canvas.drawLine(ann.x, midY, ann.x + ann.width, midY, linePaint)
                        }
                        is PdfAnnotation.Squiggly -> {
                            val wavePaint = Paint().apply {
                                color = try {
                                    android.graphics.Color.parseColor(ann.color)
                                } catch (e: Exception) {
                                    android.graphics.Color.RED
                                }
                                strokeWidth = 2f
                                style = Paint.Style.STROKE
                                isAntiAlias = true
                            }
                            val path = Path()
                            var currentX = ann.x
                            val squigglyY = ann.y + ann.height + 1f
                            val waveLength = 10f
                            val amplitude = 3f
                            path.moveTo(currentX, squigglyY)
                            var toggle = true
                            while (currentX < ann.x + ann.width) {
                                val nextX = (currentX + waveLength).coerceAtMost(ann.x + ann.width)
                                val targetY = if (toggle) squigglyY - amplitude else squigglyY + amplitude
                                path.lineTo(nextX, targetY)
                                currentX = nextX
                                toggle = !toggle
                            }
                            canvas.drawPath(path, wavePaint)
                        }
                        is PdfAnnotation.Stamp -> {
                            val stampPaint = Paint().apply {
                                color = try {
                                    android.graphics.Color.parseColor(ann.color)
                                } catch (e: Exception) {
                                    android.graphics.Color.RED
                                }
                                style = Paint.Style.STROKE
                                strokeWidth = 4f
                                isAntiAlias = true
                            }
                            val fillPaint = Paint().apply {
                                color = stampPaint.color
                                alpha = 20
                                style = Paint.Style.FILL
                            }
                            // Draw nice stamp shape
                            val rect = Rect(ann.x.toInt(), ann.y.toInt(), (ann.x + 220).toInt(), (ann.y + 70).toInt())
                            canvas.drawRect(rect, fillPaint)
                            canvas.drawRect(rect, stampPaint)
                            
                            val stampTextPaint = Paint().apply {
                                color = stampPaint.color
                                textSize = 20f
                                isFakeBoldText = true
                                isAntiAlias = true
                            }
                            canvas.drawText(ann.label, ann.x + 20f, ann.y + 45f, stampTextPaint)
                        }
                        is PdfAnnotation.TextNote -> {
                            val noteBoxPaint = Paint().apply {
                                color = android.graphics.Color.rgb(255, 253, 150)
                                style = Paint.Style.FILL
                            }
                            canvas.drawRect(ann.x, ann.y, ann.x + 150f, ann.y + 100f, noteBoxPaint)
                            val noteTextPaint = Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 14f
                            }
                            canvas.drawText(ann.text.take(20) + "...", ann.x + 10f, ann.y + 40f, noteTextPaint)
                        }
                        is PdfAnnotation.Signature -> {
                            val sigPaint = Paint().apply {
                                color = try {
                                    android.graphics.Color.parseColor(ann.color)
                                } catch (e: Exception) {
                                    android.graphics.Color.BLUE
                                }
                                strokeWidth = ann.strokeWidth
                                style = Paint.Style.STROKE
                                strokeCap = Paint.Cap.ROUND
                                isAntiAlias = true
                            }
                            val strokePoints = ann.strokePoints
                            if (strokePoints.size > 1) {
                                for (j in 0 until strokePoints.size - 1) {
                                    canvas.drawLine(
                                        strokePoints[j].first, strokePoints[j].second,
                                        strokePoints[j + 1].first, strokePoints[j + 1].second,
                                        sigPaint
                                    )
                                }
                            }
                        }
                        is PdfAnnotation.Geometry -> {
                            val geomPaint = Paint().apply {
                                color = try {
                                    android.graphics.Color.parseColor(ann.color)
                                } catch (e: Exception) {
                                    android.graphics.Color.RED
                                }
                                strokeWidth = ann.strokeWidth
                                style = Paint.Style.STROKE
                                isAntiAlias = true
                            }
                            val rectF = RectF(ann.x, ann.y, ann.x + ann.width, ann.y + ann.height)
                            when (ann.type) {
                                PdfAnnotation.GeometryType.ARROW -> {
                                    val startX = ann.x
                                    val startY = ann.y + ann.height
                                    val endX = ann.x + ann.width
                                    val endY = ann.y
                                    canvas.drawLine(startX, startY, endX, endY, geomPaint)
                                    // Arrow head
                                    val angle = atan2(endY - startY, endX - startX)
                                    val arrowLength = 20f
                                    val arrowAngle = Math.PI / 6
                                    val x1 = endX - arrowLength * cos(angle - arrowAngle).toFloat()
                                    val y1 = endY - arrowLength * sin(angle - arrowAngle).toFloat()
                                    val x2 = endX - arrowLength * cos(angle + arrowAngle).toFloat()
                                    val y2 = endY - arrowLength * sin(angle + arrowAngle).toFloat()
                                    
                                    val arrowHeadPaint = Paint(geomPaint).apply { style = Paint.Style.FILL }
                                    val path = Path().apply {
                                        moveTo(endX, endY)
                                        lineTo(x1, y1)
                                        lineTo(x2, y2)
                                        close()
                                    }
                                    canvas.drawPath(path, arrowHeadPaint)
                                }
                                PdfAnnotation.GeometryType.ROUND_RECT -> {
                                    canvas.drawRoundRect(rectF, 12f, 12f, geomPaint)
                                }
                                PdfAnnotation.GeometryType.OVAL -> {
                                    canvas.drawOval(rectF, geomPaint)
                                }
                                PdfAnnotation.GeometryType.LINE -> {
                                    canvas.drawLine(ann.x, ann.y, ann.x + ann.width, ann.y + ann.height, geomPaint)
                                }
                            }
                        }
                        is PdfAnnotation.Measuring -> {
                            val measurePaint = Paint().apply {
                                color = try {
                                    android.graphics.Color.parseColor(ann.color)
                                } catch (e: Exception) {
                                    android.graphics.Color.BLUE
                                }
                                strokeWidth = 3f
                                style = Paint.Style.STROKE
                                isAntiAlias = true
                            }
                            canvas.drawLine(ann.x, ann.y, ann.toX, ann.toY, measurePaint)
                            // Draw nice ticks
                            val angle = atan2(ann.toY - ann.y, ann.toX - ann.x)
                            val orthoAngle = angle + Math.PI / 2
                            val tickLen = 12f
                            
                            val cosO = cos(orthoAngle).toFloat()
                            val sinO = sin(orthoAngle).toFloat()
                            // Start tick
                            canvas.drawLine(ann.x - cosO * tickLen, ann.y - sinO * tickLen, ann.x + cosO * tickLen, ann.y + sinO * tickLen, measurePaint)
                            // End tick
                            canvas.drawLine(ann.toX - cosO * tickLen, ann.toY - sinO * tickLen, ann.toX + cosO * tickLen, ann.toY + sinO * tickLen, measurePaint)
                            
                            // Measure length
                            val dx = ann.toX - ann.x
                            val dy = ann.toY - ann.y
                            val distancePx = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                            val valueStr = String.format("%.1f %s", distancePx * ann.scaleRatio, ann.unit)
                            
                            val textp = Paint().apply {
                                color = measurePaint.color
                                textSize = 14f
                                isFakeBoldText = true
                                isAntiAlias = true
                            }
                            val midX = (ann.x + ann.toX) / 2f
                            val midY = (ann.y + ann.toY) / 2f
                            canvas.drawText(valueStr, midX + cosO * 18f - 20f, midY + sinO * 18f, textp)
                        }
                        is PdfAnnotation.AudioAnnotation -> {
                            val audioStampPaint = Paint().apply {
                                color = android.graphics.Color.rgb(0, 150, 136) // Teal color
                                style = Paint.Style.STROKE
                                strokeWidth = 2f
                                isAntiAlias = true
                            }
                            val bgPaint = Paint().apply {
                                color = android.graphics.Color.rgb(224, 242, 241) // Light teal
                                style = Paint.Style.FILL
                            }
                            // Draw nice capsule around audio trigger label
                            val rectF = RectF(ann.x, ann.y, ann.x + 200f, ann.y + 45f)
                            canvas.drawRoundRect(rectF, 12f, 12f, bgPaint)
                            canvas.drawRoundRect(rectF, 12f, 12f, audioStampPaint)
                            
                            val audioTextPaint = Paint().apply {
                                color = android.graphics.Color.rgb(0, 77, 64)
                                textSize = 11f
                                isFakeBoldText = true
                                isAntiAlias = true
                            }
                            canvas.drawText("🔊 " + ann.label.take(18), ann.x + 10f, ann.y + 28f, audioTextPaint)
                        }
                    }
                }

                // Bottom Page info indicator
                val footerPaint = Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 14f
                    isAntiAlias = true
                }
                canvas.drawText("${docState.name} - Page ${i + 1} of ${docState.pages.size}", 100f, pdfPage.height - 40f, footerPaint)

                pdfDocument.finishPage(page)
            }

            // Write out to cache directory
            val file = File(context.cacheDir, docState.name)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            return file
        } catch (e: IOException) {
            e.printStackTrace()
            // Return dummy file on error
            return File(context.cacheDir, "error_export.pdf")
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Estimates file size in Kilobytes (KB)
     */
    fun estimateSizeKb(doc: PdfDocumentState): Int {
        var baseSize = doc.name.length * 40
        doc.pages.forEach { page ->
            baseSize += page.textBlocks.sumOf { it.text.length * 2 }
            baseSize += page.imageBlocks.sumOf { img ->
                val bm = img.imageBitmap
                if (bm != null) {
                    (bm.width * bm.height * 2).toInt()
                } else {
                    120 * 1024
                }
            }
            baseSize += page.annotations.size * 50
        }
        return (baseSize / 1024).coerceAtLeast(12)
    }

    /**
     * Compresses PDF documents
     */
    fun compressPdf(doc: PdfDocumentState, compressionLevel: String): Pair<PdfDocumentState, Int> {
        val multiplier = when (compressionLevel.uppercase()) {
            "HIGH" -> 0.35  // 65% size reduction
            "MEDIUM" -> 0.60 // 40% size reduction
            else -> 0.85    // 15% size reduction
        }
        
        val compressedPages = doc.pages.map { page ->
            val compressedImages = page.imageBlocks.map { img ->
                img.copy(
                    width = img.width * 0.8f,
                    height = img.height * 0.8f,
                    scaleX = img.scaleX * 0.9f,
                    scaleY = img.scaleY * 0.9f
                )
            }
            page.copy(imageBlocks = compressedImages)
        }
        val compressedDoc = doc.copy(
            id = UUID.randomUUID().toString(),
            name = "${doc.name.replace(".pdf", "")}_compressed.pdf",
            pages = compressedPages,
            metadata = doc.metadata.copy(
                producer = "Compressed via PDF Reader Engine (Opt: $compressionLevel)",
                keywords = doc.metadata.keywords + ", compressed, $compressionLevel"
            )
        )
        val origSize = estimateSizeKb(doc)
        val compSize = (origSize * multiplier).toInt().coerceAtLeast(8)
        return Pair(compressedDoc, compSize)
    }

    // --- CONVERSIONS TO PDF ---
    fun wordToPdf(title: String, content: String): PdfDocumentState {
        val lines = content.split("\n")
        val pages = mutableListOf<PdfPage>()
        val maxLines = 14
        var currentLines = mutableListOf<String>()
        var pageCount = 0
        
        lines.forEach { line ->
            currentLines.add(line)
            if (currentLines.size >= maxLines) {
                pages.add(createWordPageFromLines(pageCount++, title, currentLines))
                currentLines = mutableListOf()
            }
        }
        if (currentLines.isNotEmpty() || pages.isEmpty()) {
            pages.add(createWordPageFromLines(pageCount++, title, currentLines))
        }
        
        return PdfDocumentState(
            id = UUID.randomUUID().toString(),
            name = "${title.replace(" ", "_")}_word.pdf",
            metadata = PdfMetadata(title = title, author = "Word to PDF Compiler"),
            pages = pages
        )
    }

    private fun createWordPageFromLines(pageIdx: Int, title: String, lines: List<String>): PdfPage {
        val blocks = mutableListOf<PdfTextBlock>()
        blocks.add(PdfTextBlock("w_title_$pageIdx", "WORD DOCUMENT: $title", 100f, 100f, 22f, "#1565C0"))
        
        var startY = 180f
        lines.forEachIndexed { i, line ->
            blocks.add(PdfTextBlock("w_line_${pageIdx}_$i", line, 100f, startY + (i * 65f), 15f, "#333333"))
        }
        return PdfPage(
            index = pageIdx,
            width = 1000,
            height = 1400,
            textBlocks = blocks
        )
    }

    fun excelToPdf(title: String, csvContent: String): PdfDocumentState {
        val pages = mutableListOf<PdfPage>()
        val rows = csvContent.split("\n")
        val blocks = mutableListOf<PdfTextBlock>()
        
        blocks.add(PdfTextBlock("xl_title", "EXCEL SHEET CONVERSION - $title", 100f, 100f, 22f, "#0288D1"))
        
        var startY = 220f
        rows.forEachIndexed { rowIndex, row ->
            val cells = row.split(",")
            var startX = 1000f
            cells.forEachIndexed { cellIndex, cell ->
                if (cell.trim().isNotEmpty()) {
                    val isHeader = rowIndex == 0
                    blocks.add(
                        PdfTextBlock(
                            id = "xl_cell_${rowIndex}_$cellIndex",
                            text = cell.trim(),
                            x = 100f + (cellIndex * 220f),
                            y = startY,
                            fontSize = if (isHeader) 15f else 13f,
                            fontColor = if (isHeader) "#1E293B" else "#334155",
                            fontName = if (isHeader) "Roboto-Bold" else "Roboto",
                            width = 200f,
                            height = 36f
                        )
                    )
                }
            }
            startY += 55f
        }
        
        pages.add(
            PdfPage(
                index = 0,
                width = 1000,
                height = 1400,
                textBlocks = blocks
            )
        )
        return PdfDocumentState(
            id = UUID.randomUUID().toString(),
            name = "${title.replace(" ", "_")}_excel.pdf",
            metadata = PdfMetadata(title = title, author = "Excel to PDF Grid Converter"),
            pages = pages
        )
    }

    fun pptToPdf(title: String, content: String): PdfDocumentState {
        val slides = content.split("---")
        val pages = mutableListOf<PdfPage>()
        
        slides.forEachIndexed { index, slideText ->
            val lines = slideText.trim().split("\n")
            val blocks = mutableListOf<PdfTextBlock>()
            
            blocks.add(PdfTextBlock("ppt_head_$index", "SLIDE ${index + 1}: $title", 100f, 80f, 24f, "#F57C00"))
            
            var startY = 220f
            lines.forEachIndexed { lIdx, line ->
                if (line.trim().isNotEmpty()) {
                    blocks.add(PdfTextBlock("ppt_body_${index}_$lIdx", "• " + line.trim(), 140f, startY, 17f, "#37474F"))
                    startY += 70f
                }
            }
            pages.add(
                PdfPage(
                    index = index,
                    width = 1400,
                    height = 1000,
                    textBlocks = blocks
                )
            )
        }
        return PdfDocumentState(
            id = UUID.randomUUID().toString(),
            name = "${title.replace(" ", "_")}_presentation.pdf",
            metadata = PdfMetadata(title = title, author = "PowerPoint to PDF Compiler"),
            pages = pages
        )
    }

    // --- CONVERSIONS FROM PDF ---
    fun exportToWord(doc: PdfDocumentState): String {
        val sb = java.lang.StringBuilder()
        sb.append("=========================================\n")
        sb.append("MICROSOFT WORD EXPORT REPRESENTATION \n")
        sb.append("Document: ${doc.name}\n")
        sb.append("Author: ${doc.metadata.author}\n")
        sb.append("=========================================\n\n")
        
        doc.pages.forEach { page ->
            sb.append("--- PAGE ${page.index + 1} ---\n")
            page.textBlocks.sortedBy { it.y }.forEach { block ->
                sb.append(block.text).append("\n")
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    fun exportToExcel(doc: PdfDocumentState): String {
        val sb = java.lang.StringBuilder()
        sb.append("Column A, Column B, Column C, Column D\n")
        
        var count = 1
        doc.pages.forEach { page ->
            page.textBlocks.forEach { block ->
                val cleanedText = block.text.replace(",", " ")
                if (cleanedText.isNotEmpty()) {
                    sb.append("Row $count, Cell-$count, \"$cleanedText\", Page ${page.index + 1}\n")
                    count++
                }
            }
        }
        return sb.toString()
    }

    fun exportToPowerPoint(doc: PdfDocumentState): String {
        val sb = java.lang.StringBuilder()
        sb.append("=========================================\n")
        sb.append("POWERPOINT DOCUMENT SLIDES REPRESENTATION \n")
        sb.append("Target file: ${doc.name}\n")
        sb.append("=========================================\n\n")
        
        doc.pages.forEachIndexed { i, page ->
            sb.append("--- SLIDE ${i + 1} ---\n")
            sb.append("[Slide Title] Page ${i + 1} Outline details\n")
            page.textBlocks.forEach { block ->
                sb.append(" * ").append(block.text).append("\n")
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    fun exportToHtml(doc: PdfDocumentState): String {
        val sb = java.lang.StringBuilder()
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n")
        sb.append("<title>${doc.name}</title>\n")
        sb.append("<style>\n")
        sb.append("  body { font-family: sans-serif; margin: 40px; background-color: #f8fafc; color: #1e293b; }\n")
        sb.append("  .page { background: white; margin-bottom: 30px; padding: 50px; border-radius: 8px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); max-width: 800px; position: relative; }\n")
        sb.append("  .text-block { margin-bottom: 12px; line-height: 1.6; }\n")
        sb.append("  .header { color: #c62828; border-bottom: 2px solid #e2e8f0; padding-bottom: 10px; margin-bottom: 20px; }\n")
        sb.append("</style>\n</head>\n<body>\n")
        
        doc.pages.forEach { page ->
            sb.append("  <div class=\"page\">\n")
            sb.append("    <h2 class=\"header\">Page ${page.index + 1}</h2>\n")
            page.textBlocks.forEach { block ->
                sb.append("    <div class=\"text-block\" style=\"font-size: ${block.fontSize}px; color: ${block.fontColor};\">")
                sb.append(block.text)
                sb.append("</div>\n")
            }
            sb.append("  </div>\n")
        }
        sb.append("</body>\n</html>")
        return sb.toString()
    }

    fun exportToText(doc: PdfDocumentState): String {
        val sb = java.lang.StringBuilder()
        doc.pages.forEach { page ->
            page.textBlocks.sortedBy { it.y }.forEach { block ->
                sb.append(block.text).append("\n")
            }
        }
        return sb.toString()
    }

    /**
     * Converts a raw Image + details into a 1-page PDF file
     */
    fun imageToPdf(imageTitle: String, imageBitmap: Bitmap?): PdfDocumentState {
        val page = PdfPage(
            index = 0,
            width = 1000,
            height = 1400,
            textBlocks = listOf(
                PdfTextBlock("img_pdf_t1", "IMAGE-TO-PDF CONVERSION", 100f, 100f, 26f, "#1976D2")
            ),
            imageBlocks = listOf(
                PdfImageBlock(
                    id = "img_pdf_blk",
                    imageBitmap = imageBitmap,
                    x = 100f,
                    y = 200f,
                    width = 800f,
                    height = 1000f
                )
            )
        )
        return PdfDocumentState(
            id = UUID.randomUUID().toString(),
            name = "${imageTitle.replace(" ", "_")}_extracted.pdf",
            metadata = PdfMetadata(title = imageTitle, author = "Image to PDF Converter"),
            pages = listOf(page)
        )
    }

    /**
     * Converts a Text string into a multi-page wrapped PDF file
     */
    fun textToPdf(title: String, rawText: String): PdfDocumentState {
        val pages = mutableListOf<PdfPage>()
        val paragraphLines = rawText.split("\n")
        
        // Wrap lines
        val maxLinesPerPage = 18
        var currentPageLines = mutableListOf<String>()
        var pageCount = 0

        paragraphLines.forEach { line ->
            currentPageLines.add(line)
            if (currentPageLines.size >= maxLinesPerPage) {
                pages.add(createPdfWebPageFromLines(pageCount++, title, currentPageLines))
                currentPageLines = mutableListOf()
            }
        }
        if (currentPageLines.isNotEmpty() || pages.isEmpty()) {
            pages.add(createPdfWebPageFromLines(pageCount++, title, currentPageLines))
        }

        return PdfDocumentState(
            id = UUID.randomUUID().toString(),
            name = "${title.replace(" ", "_")}_doc.pdf",
            metadata = PdfMetadata(title = title, author = "Text to PDF Printer"),
            pages = pages
        )
    }

    private fun createPdfWebPageFromLines(pageIdx: Int, title: String, lines: List<String>): PdfPage {
        val blocks = mutableListOf<PdfTextBlock>()
        blocks.add(PdfTextBlock("txt_title_$pageIdx", "DOC CONVERSION - $title", 100f, 100f, 22f, "#388E3C"))
        
        var startY = 200f
        lines.forEachIndexed { i, line ->
            blocks.add(PdfTextBlock("txt_line_${pageIdx}_$i", line, 100f, startY + (i * 50f), 16f, "#111111"))
        }

        return PdfPage(
            index = pageIdx,
            width = 1000,
            height = 1400,
            textBlocks = blocks
        )
    }

    /**
     * Simple HTML snippet compiler simulation
     */
    fun compileHtmlToPdf(title: String, htmlText: String): PdfDocumentState {
        // Strip out base tags recursively
        val cleanText = htmlText
            .replace("<html>", "")
            .replace("</html>", "")
            .replace("<body>", "")
            .replace("</body>", "")
            .replace("<h1>", "\n## ")
            .replace("</h1>", "\n")
            .replace("<h2>", "\n### ")
            .replace("</h2>", "\n")
            .replace("<p>", "")
            .replace("</p>", "\n")
            .replace("<br>", "\n")
            .replace("<br/>", "\n")

        return textToPdf(title, "HTML COMPILE OUT:\n$cleanText")
    }

    fun createGermanArabicSampleDocument(): PdfDocumentState {
        val id = UUID.randomUUID().toString()
        
        val page1 = PdfPage(
            index = 0,
            width = 1000,
            height = 1400,
            textBlocks = listOf(
                PdfTextBlock(
                    id = "biling_p1_t1",
                    text = "المرشد اللغوي المزدوج (العربية الألمانية)",
                    x = 80f,
                    y = 100f,
                    fontSize = 28f,
                    fontColor = "#004D40",
                    fontName = "Serif",
                    width = 840f
                ),
                PdfTextBlock(
                    id = "biling_p1_t2",
                    text = "Zweisprachiger Leitfaden (Deutsch & Arabisch)",
                    x = 80f,
                    y = 150f,
                    fontSize = 22f,
                    fontColor = "#00796B",
                    fontName = "SansSerif",
                    width = 840f
                ),
                PdfTextBlock(
                    id = "biling_p1_t3",
                    text = "هذا الكتيب مصمم خصيصاً لاختبار دعم اللغات المزدوجة (RTL للغة العربية و LTR للغة الألمانية) في نفس المستند والسطور، مع دعم التشكيل والخطوط المدمجة والحروف الألمانية الخاصة (مثل ä, ö, ü, ß).",
                    x = 80f,
                    y = 220f,
                    fontSize = 15f,
                    fontColor = "#37474F",
                    fontName = "Roboto",
                    width = 840f
                ),
                PdfTextBlock(
                    id = "biling_p1_t4",
                    text = "1. جمل عامة (Allgemeine Sätze):",
                    x = 80f,
                    y = 310f,
                    fontSize = 18f,
                    fontColor = "#D84315",
                    fontName = "SansSerif",
                    width = 840f
                ),
                PdfTextBlock(
                    id = "biling_p1_t5",
                    text = "العربية: صَبَاحُ الخَيْرِ يَا صَدِيقِي العربي! 🇸🇦\n" +
                           "Deutsch: Guten Morgen, mein arabischer Freund! 🇩🇪",
                    x = 80f,
                    y = 360f,
                    fontSize = 16f,
                    fontColor = "#263238",
                    fontName = "Roboto",
                    width = 840f
                ),
                PdfTextBlock(
                    id = "biling_p1_t6",
                    text = "العربية: كَيْفَ حَالُكَ اليَوْمَ؟ هَلْ كُلُّ شَيْءٍ بِخَيْرٍ؟\n" +
                           "Deutsch: Wie geht es dir heute? Ist alles in Ordnung?",
                    x = 80f,
                    y = 440f,
                    fontSize = 16f,
                    fontColor = "#263238",
                    fontName = "Roboto",
                    width = 840f
                ),
                PdfTextBlock(
                    id = "biling_p1_t7",
                    text = "العربية: تَعْلَمُ اللُّغَةِ الأَلْمَانِيّةِ فِي أَقَلِّ مِنْ شَهْرٍ!\n" +
                           "Deutsch: Deutsch lernen in weniger als einem Monat!",
                    x = 80f,
                    y = 520f,
                    fontSize = 16f,
                    fontColor = "#263238",
                    fontName = "Roboto",
                    width = 840f
                ),
                PdfTextBlock(
                    id = "biling_p1_t8",
                    text = "2. الحروف والتشكيل الصوتي الخاص (Sonderzeichen & Umlaute):",
                    x = 80f,
                    y = 610f,
                    fontSize = 18f,
                    fontColor = "#D84315",
                    fontName = "SansSerif",
                    width = 840f
                ),
                PdfTextBlock(
                    id = "biling_p1_t9",
                    text = "الكلمات الألمانية تحتوي على حروف مخصصة مثل:\n" +
                           "• ä في الكلمة Mädchen (فتاة)\n" +
                           "• ö في الكلمة Schön (جميل)\n" +
                           "• ü في الكلمة Tschüss (وداعاً)\n" +
                           "• ß في الكلمة Groß (كبير)",
                    x = 80f,
                    y = 660f,
                    fontSize = 16f,
                    fontColor = "#37474F",
                    fontName = "Roboto",
                    width = 840f
                ),
                PdfTextBlock(
                    id = "biling_p1_t10",
                    text = "3. النصوص التفاعلية ثنائية الاتجاه (Bi-directional Row Test):\n" +
                           "• نص مدمج ومطور: 'Das Wort الكتاب bedeutet ein Buch auf Deutsch.'\n" +
                           "• نص مدمج ومطور: 'يُسعدنا لقاؤكم اليوم Guten Tag وكافة الأصدقاء.'",
                    x = 80f,
                    y = 800f,
                    fontSize = 16f,
                    fontColor = "#004D40",
                    fontName = "Roboto",
                    width = 840f
                ),
                PdfTextBlock(
                    id = "biling_p1_t11",
                    text = "انقر على أيقونات النطق المرفقة بالأسفل أو انقر على أي نص لتفعيل محرك القراءة الآلي ذي الكشف التلقائي للغة.",
                    x = 80f,
                    y = 920f,
                    fontSize = 15f,
                    fontColor = "#555555",
                    fontName = "Roboto",
                    width = 840f
                )
            ),
            annotations = listOf(
                PdfAnnotation.AudioAnnotation(
                    id = "audio_de_1",
                    x = 80f,
                    y = 1000f,
                    pageIndex = 0,
                    audioFilename = "german_guten_morgen.mp3",
                    label = "🇩🇪 Guten Morgen (Media)",
                    textToSpeak = "Guten Morgen",
                    language = "de"
                ),
                PdfAnnotation.AudioAnnotation(
                    id = "audio_ar_1",
                    x = 500f,
                    y = 1000f,
                    pageIndex = 0,
                    audioFilename = "arabic_guten_morgen.mp3",
                    label = "🇸🇦 صباح الخير (Media)",
                    textToSpeak = "صباح الخير يا صديقي العربي",
                    language = "ar"
                ),
                PdfAnnotation.AudioAnnotation(
                    id = "audio_de_2",
                    x = 80f,
                    y = 1100f,
                    pageIndex = 0,
                    audioFilename = "german_tschuess.mp3",
                    label = "🇩🇪 Tschüss (Media)",
                    textToSpeak = "Tschüss",
                    language = "de"
                ),
                PdfAnnotation.AudioAnnotation(
                    id = "audio_de_3",
                    x = 500f,
                    y = 1100f,
                    pageIndex = 0,
                    audioFilename = "german_schoen.mp3",
                    label = "🇩🇪 Schön & Groß (Media)",
                    textToSpeak = "Es ist wunderschön und groß",
                    language = "de"
                ),
                PdfAnnotation.Highlight("biling_p1_hl1", 80f, 100f, 0, 480f, 34f, "#4DB6AC")
            )
        )
        
        return PdfDocumentState(
            id = id,
            name = "Bilingual_German_Arabic_Guide.pdf",
            metadata = PdfMetadata(
                title = "Learn German & Arabic",
                author = "System Language Expert",
                subject = "Bilingual German & Arabic Reader",
                keywords = "German, Arabic, Bilingual, Audio, Pronunciation, RTL, LTR"
            ),
            pages = listOf(page1),
            bookmarks = listOf(
                PdfBookmark("Bilingual Main Page", 0)
            )
        )
    }

    /**
     * Renders a virtual PDF page into a physical image Bitmap for real AI multimodal processing/OCR.
     */
    fun renderPageToBitmap(page: PdfPage, context: Context): Bitmap {
        val scale = 0.6f
        val w = (page.width * scale).toInt().coerceAtLeast(100)
        val h = (page.height * scale).toInt().coerceAtLeast(100)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Background
        val bgPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)
        
        // Draw image blocks
        page.imageBlocks.forEach { block ->
            var img: Bitmap? = block.imageBitmap
            if (img == null && block.imageResId != null) {
                try {
                    img = android.graphics.BitmapFactory.decodeResource(context.resources, block.imageResId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (img != null) {
                val srcRect = Rect(0, 0, img.width, img.height)
                val dstRect = RectF(
                    block.x * scale,
                    block.y * scale,
                    (block.x + block.width) * scale,
                    (block.y + block.height) * scale
                )
                
                canvas.save()
                canvas.rotate(block.rotation, dstRect.centerX(), dstRect.centerY())
                canvas.drawBitmap(img, srcRect, dstRect, null)
                canvas.restore()
            }
        }

        // Draw text blocks
        val textPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 14f * scale * 2f
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT
        }
        
        page.textBlocks.forEach { block ->
            val parts = block.text.split("\n")
            var currentY = block.y * scale + (18f * scale)
            parts.forEach { part ->
                canvas.drawText(part, block.x * scale, currentY, textPaint)
                currentY += 20f * scale
            }
        }

        return bitmap
    }
}
