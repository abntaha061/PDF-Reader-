package com.example.pdf

import android.graphics.Bitmap
import android.net.Uri

data class PdfDocumentState(
    val id: String,
    val name: String,
    val uri: Uri? = null,
    val metadata: PdfMetadata = PdfMetadata(),
    val pages: List<PdfPage> = emptyList(),
    val security: PdfSecurity = PdfSecurity(),
    val bookmarks: List<PdfBookmark> = emptyList(),
    val certificateSignatures: List<PdfCertificateSignature> = emptyList()
)

data class PdfMetadata(
    val title: String = "Untitled Document",
    val author: String = "System Author",
    val subject: String = "PDF Manual",
    val keywords: String = "PDF, Reader, Edit",
    val creationDate: String = "2026-05-30",
    val modificationDate: String = "2026-05-30",
    val producer: String = "PDF Reader Professional"
)

data class PdfSecurity(
    val isEncrypted: Boolean = false,
    val userPassword: String = "",
    val ownerPassword: String = "",
    val permissions: PdfPermissions = PdfPermissions()
)

data class PdfPermissions(
    val allowPrinting: Boolean = true,
    val allowModification: Boolean = true,
    val allowCopying: Boolean = true,
    val allowAnnotations: Boolean = true
)

data class PdfBookmark(
    val title: String,
    val pageIndex: Int,
    val isUserAdded: Boolean = false
)

data class PdfPage(
    val index: Int,
    val width: Int = 1000,
    val height: Int = 1400,
    val textBlocks: List<PdfTextBlock> = emptyList(),
    val imageBlocks: List<PdfImageBlock> = emptyList(),
    val hyperlinks: List<PdfHyperlink> = emptyList(),
    val annotations: List<PdfAnnotation> = emptyList(),
    val formFields: List<PdfFormField> = emptyList()
)

data class PdfTextBlock(
    val id: String,
    val text: String,
    val x: Float,
    val y: Float,
    val fontSize: Float = 16f,
    val fontColor: String = "#000000",
    val fontName: String = "Roboto",
    val width: Float = 300f,
    val height: Float = 40f
)

data class PdfImageBlock(
    val id: String,
    val imageResId: Int? = null,
    val imageBitmap: Bitmap? = null,
    val x: Float,
    val y: Float,
    val width: Float = 200f,
    val height: Float = 200f,
    val rotation: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 0f,
    val cropBottom: Float = 0f
)

data class PdfHyperlink(
    val id: String,
    val label: String,
    val url: String? = null,
    val destPage: Int? = null,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

enum class FormFieldType {
    TEXT, CHECKBOX, RADIO, DROPDOWN
}

data class PdfFormField(
    val id: String,
    val name: String,
    val type: FormFieldType,
    val x: Float,
    val y: Float,
    val width: Float = 180f,
    val height: Float = 40f,
    val value: String = "",
    val options: List<String> = emptyList(),
    val isRequired: Boolean = false
)

data class PdfCertificateSignature(
    val id: String,
    val signerName: String,
    val timestamp: String,
    val algorithm: String = "SHA-256 with RSA/PKI",
    val integrityVerified: Boolean = true,
    val certificateSerial: String
)

sealed class PdfAnnotation {
    abstract val id: String
    abstract val x: Float
    abstract val y: Float
    abstract val pageIndex: Int

    data class Highlight(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val pageIndex: Int,
        val width: Float,
        val height: Float,
        val color: String = "#FFFF00"
    ) : PdfAnnotation()

    data class Underline(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val pageIndex: Int,
        val width: Float,
        val height: Float,
        val color: String = "#C62828"
    ) : PdfAnnotation()

    data class Strikethrough(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val pageIndex: Int,
        val width: Float,
        val height: Float,
        val color: String = "#000000"
    ) : PdfAnnotation()

    data class Squiggly(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val pageIndex: Int,
        val width: Float,
        val height: Float,
        val color: String = "#E53935"
    ) : PdfAnnotation()

    data class TextNote(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val pageIndex: Int,
        val text: String,
        val color: String = "#FFEB3B"
    ) : PdfAnnotation()

    data class Stamp(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val pageIndex: Int,
        val label: String, // e.g., "APPROVED", "CONFIDENTIAL", "DRAFT", "URGENT"
        val color: String = "#E53935"
    ) : PdfAnnotation()

    data class Signature(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val pageIndex: Int,
        val strokePoints: List<Pair<Float, Float>>,
        val color: String = "#0000FF",
        val strokeWidth: Float = 5f
    ) : PdfAnnotation()

    enum class GeometryType {
        ARROW, ROUND_RECT, OVAL, LINE
    }

    data class Geometry(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val pageIndex: Int,
        val type: GeometryType,
        val width: Float,
        val height: Float,
        val color: String = "#E53935",
        val strokeWidth: Float = 4f
    ) : PdfAnnotation()

    data class Measuring(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val pageIndex: Int,
        val toX: Float,
        val toY: Float,
        val scaleRatio: Float = 1.0f, // 1px = 1mm, etc.
        val unit: String = "mm",
        val color: String = "#1E88E5"
    ) : PdfAnnotation()

    data class AudioAnnotation(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val pageIndex: Int,
        val audioFilename: String,
        val label: String,
        val textToSpeak: String,
        val language: String // "de" or "ar"
    ) : PdfAnnotation()
}
