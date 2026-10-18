package com.example.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class SamplePdfGenerator(private val context: Context) {

    suspend fun getOrCreateSampleProposal(): File = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "sample_proposal.pdf")
        if (file.exists() && file.length() > 0) return@withContext file

        val doc = PdfDocument()
        val width = 595 // A4 standard pt
        val height = 842
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        canvas.drawColor(Color.WHITE)

        // Top Banner
        val bannerPaint = Paint().apply {
            color = Color.parseColor("#0F172A") // deep slate
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 130f, bannerPaint)

        // Accent strip
        val accentPaint = Paint().apply {
            color = Color.parseColor("#E11D48") // crimson
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 130f, width.toFloat(), 136f, accentPaint)

        // Header Title
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("PROJECT PROPOSAL & STRATEGY", 40f, 65f, titlePaint)

        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText("Aether & Craft Studio • Q4 Global Roadmap • Client: Nexus Corp", 40f, 95f, subtitlePaint)

        // Embedded image (from sample photo)
        try {
            val sampleBmp = BitmapFactory.decodeResource(context.resources, R.drawable.img_sample_photo_1792325021983)
            if (sampleBmp != null) {
                val imgDest = RectF(40f, 160f, 260f, 320f)
                canvas.drawBitmap(sampleBmp, null, imgDest, Paint(Paint.FILTER_BITMAP_FLAG))

                // Subtle photo border
                val borderPaint = Paint().apply {
                    color = Color.parseColor("#E2E8F0")
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                canvas.drawRect(imgDest, borderPaint)
            }
        } catch (_: Exception) {}

        // Right side info beside image
        val headingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 16f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("Executive Vision", 280f, 185f, headingPaint)

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val introText = "This project delivers a next-generation cloud infrastructure with high-performance mobile toolchains, adaptive real-time data sync, and interactive visual editing capabilities for global enterprises."
        val introLayout = StaticLayout.Builder.obtain(introText, 0, introText.length, bodyPaint, 275).build()
        canvas.save()
        canvas.translate(280f, 200f)
        introLayout.draw(canvas)
        canvas.restore()

        // Key Deliverables Section
        canvas.drawText("Key Project Deliverables", 40f, 360f, headingPaint)

        val tablePaint = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(40f, 380f, width - 40f, 520f), 8f, 8f, tablePaint)

        val rowPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1f
        }
        canvas.drawLine(40f, 415f, width - 40f, 415f, rowPaint)
        canvas.drawLine(40f, 450f, width - 40f, 450f, rowPaint)
        canvas.drawLine(40f, 485f, width - 40f, 485f, rowPaint)

        val colHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("PHASE", 55f, 402f, colHeaderPaint)
        canvas.drawText("DESCRIPTION", 160f, 402f, colHeaderPaint)
        canvas.drawText("TIMELINE", 380f, 402f, colHeaderPaint)
        canvas.drawText("BUDGET", 480f, 402f, colHeaderPaint)

        val rowTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText("Phase 01", 55f, 437f, rowTextPaint)
        canvas.drawText("System Architecture & PDF Core Engine", 160f, 437f, rowTextPaint)
        canvas.drawText("Weeks 1 - 4", 380f, 437f, rowTextPaint)
        canvas.drawText("$18,500", 480f, 437f, rowTextPaint)

        canvas.drawText("Phase 02", 55f, 472f, rowTextPaint)
        canvas.drawText("Interactive Canvas, Text & Image Editing", 160f, 472f, rowTextPaint)
        canvas.drawText("Weeks 5 - 8", 380f, 472f, rowTextPaint)
        canvas.drawText("$24,000", 480f, 472f, rowTextPaint)

        canvas.drawText("Phase 03", 55f, 507f, rowTextPaint)
        canvas.drawText("Digital Signing, Export & High-DPI Output", 160f, 507f, rowTextPaint)
        canvas.drawText("Weeks 9 - 12", 380f, 507f, rowTextPaint)
        canvas.drawText("$14,500", 480f, 507f, rowTextPaint)

        // Terms & Signatures
        canvas.drawText("Authorization & Agreement", 40f, 560f, headingPaint)
        val termsText = "By countersigning below, both parties endorse the deliverables, schedule, and budgetary terms set forth in this document. Any scope modifications shall require written consent."
        val termsLayout = StaticLayout.Builder.obtain(termsText, 0, termsText.length, bodyPaint, width - 80).build()
        canvas.save()
        canvas.translate(40f, 575f)
        termsLayout.draw(canvas)
        canvas.restore()

        // Signature Lines
        val sigLinePaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
        }
        canvas.drawLine(60f, 720f, 240f, 720f, sigLinePaint)
        canvas.drawLine(350f, 720f, 530f, 720f, sigLinePaint)

        canvas.drawText("Authorized Representative", 60f, 740f, colHeaderPaint)
        canvas.drawText("Client Sign-off & Seal", 350f, 740f, colHeaderPaint)

        // Footer
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 9f
        }
        canvas.drawText("Page 1 of 1 • Generated with PDF Craft Studio", 40f, 810f, footerPaint)

        doc.finishPage(page)
        FileOutputStream(file).use { out -> doc.writeTo(out) }
        doc.close()
        file
    }

    suspend fun getOrCreateSampleInvoice(): File = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "sample_invoice.pdf")
        if (file.exists() && file.length() > 0) return@withContext file

        val doc = PdfDocument()
        val width = 595
        val height = 842
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        // Invoice Header
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E11D48")
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("INVOICE", 40f, 70f, brandPaint)

        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 10f
        }
        canvas.drawText("Invoice Number: #INV-2026-0841", 40f, 95f, metaPaint)
        canvas.drawText("Issue Date: October 18, 2026", 40f, 110f, metaPaint)
        canvas.drawText("Due Date: November 01, 2026", 40f, 125f, metaPaint)

        val rightHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("CRAFT STUDIOS INC.", width - 40f, 65f, rightHeaderPaint)

        val rightSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 9f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("742 Evergreen Terrace, Suite 400", width - 40f, 85f, rightSubPaint)
        canvas.drawText("San Francisco, CA 94107", width - 40f, 100f, rightSubPaint)
        canvas.drawText("billing@craftstudios.design", width - 40f, 115f, rightSubPaint)

        // Bill to
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1.5f
        }
        canvas.drawLine(40f, 150f, width - 40f, 150f, dividerPaint)

        val sectionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("Billed To:", 40f, 180f, sectionPaint)

        val clientPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 10f
        }
        canvas.drawText("Acme Corporation", 40f, 200f, clientPaint)
        canvas.drawText("Attn: Financial Accounting Dept.", 40f, 215f, clientPaint)
        canvas.drawText("500 Market Street, New York, NY", 40f, 230f, clientPaint)

        // Items table
        val headerBg = Paint().apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(40f, 260f, width - 40f, 290f), 6f, 6f, headerBg)

        val colPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("ITEM DESCRIPTION", 55f, 280f, colPaint)
        canvas.drawText("QTY", 320f, 280f, colPaint)
        canvas.drawText("RATE", 400f, 280f, colPaint)
        canvas.drawText("AMOUNT", 490f, 280f, colPaint)

        // Rows
        val rowItemPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 10f
        }
        canvas.drawText("Brand Identity & UI Design System", 55f, 320f, rowItemPaint)
        canvas.drawText("1", 325f, 320f, rowItemPaint)
        canvas.drawText("$4,500.00", 400f, 320f, rowItemPaint)
        canvas.drawText("$4,500.00", 490f, 320f, rowItemPaint)

        canvas.drawText("Native Android App Development", 55f, 355f, rowItemPaint)
        canvas.drawText("1", 325f, 355f, rowItemPaint)
        canvas.drawText("$8,200.00", 400f, 355f, rowItemPaint)
        canvas.drawText("$8,200.00", 490f, 355f, rowItemPaint)

        canvas.drawText("PDF Engine & Export Integration", 55f, 390f, rowItemPaint)
        canvas.drawText("1", 325f, 390f, rowItemPaint)
        canvas.drawText("$2,800.00", 400f, 390f, rowItemPaint)
        canvas.drawText("$2,800.00", 490f, 390f, rowItemPaint)

        canvas.drawLine(40f, 420f, width - 40f, 420f, dividerPaint)

        // Total
        val totalLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 11f
            textAlign = Paint.Align.RIGHT
        }
        val totalValPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Subtotal:", 460f, 450f, totalLabelPaint)
        canvas.drawText("$15,500.00", width - 40f, 450f, totalValPaint)

        canvas.drawText("Tax (8.5%):", 460f, 475f, totalLabelPaint)
        canvas.drawText("$1,317.50", width - 40f, 475f, totalValPaint)

        val grandTotalPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E11D48")
            textSize = 16f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Total Due:", 460f, 515f, totalValPaint)
        canvas.drawText("$16,817.50", width - 40f, 515f, grandTotalPaint)

        // Payment Info & Stamp
        canvas.drawText("Payment Instructions", 40f, 570f, sectionPaint)
        canvas.drawText("Bank: Chase Manhattan Bank", 40f, 595f, metaPaint)
        canvas.drawText("Account No: 9876-5432-1098", 40f, 610f, metaPaint)
        canvas.drawText("Routing No: 121000358", 40f, 625f, metaPaint)

        // Signature placeholder
        val sigLinePaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        }
        canvas.drawLine(350f, 680f, width - 40f, 680f, sigLinePaint)
        val sigLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Authorized Financial Signature", 450f, 700f, sigLabelPaint)

        doc.finishPage(page)
        FileOutputStream(file).use { out -> doc.writeTo(out) }
        doc.close()
        file
    }

    suspend fun createBlankDocument(): File = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "new_document_${System.currentTimeMillis()}.pdf")
        val doc = PdfDocument()
        val width = 595
        val height = 842
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Tap 'Text' or 'Image' tools below to design your PDF", width / 2f, height / 2f, headerPaint)

        doc.finishPage(page)
        FileOutputStream(file).use { out -> doc.writeTo(out) }
        doc.close()
        file
    }

    suspend fun getOrCreateSampleAgreement(): File = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "sample_agreement.pdf")
        if (file.exists() && file.length() > 0) return@withContext file

        val doc = PdfDocument()
        val width = 595
        val height = 842
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#047857") // emerald
            textSize = 22f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("NON-DISCLOSURE & SERVICE AGREEMENT", 40f, 65f, headerPaint)

        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 10f
        }
        canvas.drawText("Standard Enterprise Form • Effective Date: October 18, 2026", 40f, 85f, metaPaint)

        val divPaint = Paint().apply {
            color = Color.parseColor("#10B981")
            strokeWidth = 2f
        }
        canvas.drawLine(40f, 100f, width - 40f, 100f, divPaint)

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 10f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        val p1 = "This Non-Disclosure and Consulting Services Agreement is entered into by and between the Disclosing Party and the Receiving Party. Both parties mutually covenant and agree to uphold strict confidentiality regarding all proprietary technology, algorithmic implementations, and architectural designs disclosed under this agreement."
        val p1Layout = StaticLayout.Builder.obtain(p1, 0, p1.length, bodyPaint, width - 80).build()
        canvas.save()
        canvas.translate(40f, 125f)
        p1Layout.draw(canvas)
        canvas.restore()

        val secPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 12f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("Section 1: Scope of Confidential Information", 40f, 220f, secPaint)
        val p2 = "Confidential Information encompasses all technical data, trade secrets, software code, research, product plans, source code, and customer records. The receiving party agrees to hold all such items in fiduciary trust."
        val p2Layout = StaticLayout.Builder.obtain(p2, 0, p2.length, bodyPaint, width - 80).build()
        canvas.save()
        canvas.translate(40f, 235f)
        p2Layout.draw(canvas)
        canvas.restore()

        canvas.drawText("Section 2: Term and Termination", 40f, 320f, secPaint)
        val p3 = "The obligations hereunder shall remain in full force for a term of three (3) calendar years from the execution date. Termination shall not relieve either party of ongoing obligations with respect to trade secrets."
        val p3Layout = StaticLayout.Builder.obtain(p3, 0, p3.length, bodyPaint, width - 80).build()
        canvas.save()
        canvas.translate(40f, 335f)
        p3Layout.draw(canvas)
        canvas.restore()

        // Signatures area
        canvas.drawText("Execution & Signatures", 40f, 480f, secPaint)

        val sigLinePaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        }
        canvas.drawLine(60f, 600f, 240f, 600f, sigLinePaint)
        canvas.drawLine(350f, 600f, 530f, 600f, sigLinePaint)

        val colHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 9f
        }
        canvas.drawText("Disclosing Party Signature", 60f, 620f, colHeaderPaint)
        canvas.drawText("Receiving Party Sign-off", 350f, 620f, colHeaderPaint)

        doc.finishPage(page)
        FileOutputStream(file).use { out -> doc.writeTo(out) }
        doc.close()
        file
    }

    suspend fun getOrCreateSampleResume(): File = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "sample_resume.pdf")
        if (file.exists() && file.length() > 0) return@withContext file

        val doc = PdfDocument()
        val width = 595
        val height = 842
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        // Left sidebar
        val sidebarPaint = Paint().apply {
            color = Color.parseColor("#1E1B4B") // deep purple
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 190f, height.toFloat(), sidebarPaint)

        // Photo circle placeholder in sidebar
        val avatarPaint = Paint().apply {
            color = Color.parseColor("#4338CA")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(95f, 90f, 45f, avatarPaint)

        val avatarTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Photo", 95f, 95f, avatarTextPaint)

        // Sidebar content
        val whiteSidePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("CONTACT", 25f, 180f, whiteSidePaint)

        val sideItemPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C7D2FE")
            textSize = 9f
        }
        canvas.drawText("alex.morgan@email.com", 25f, 200f, sideItemPaint)
        canvas.drawText("+1 (555) 234-5678", 25f, 218f, sideItemPaint)
        canvas.drawText("San Francisco, CA", 25f, 236f, sideItemPaint)

        canvas.drawText("SKILLS", 25f, 280f, whiteSidePaint)
        canvas.drawText("• Jetpack Compose", 25f, 300f, sideItemPaint)
        canvas.drawText("• Kotlin Architecture", 25f, 318f, sideItemPaint)
        canvas.drawText("• PDF Rendering Engines", 25f, 336f, sideItemPaint)
        canvas.drawText("• UI/UX Systems", 25f, 354f, sideItemPaint)

        // Main content
        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("ALEX MORGAN", 215f, 70f, namePaint)

        val rolePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7C3AED")
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("Staff Mobile Architect & Product Designer", 215f, 92f, rolePaint)

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 10f
        }
        val summary = "Passionate Android engineer with 10+ years specializing in high-performance graphics engines, document processing toolchains, and modern Material 3 design systems."
        val summaryLayout = StaticLayout.Builder.obtain(summary, 0, summary.length, bodyPaint, 340).build()
        canvas.save()
        canvas.translate(215f, 110f)
        summaryLayout.draw(canvas)
        canvas.restore()

        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("WORK EXPERIENCE", 215f, 200f, headerPaint)

        val expTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("Lead Software Engineer • Cloud Craft", 215f, 225f, expTitlePaint)
        val exp1 = "Led architecture of native mobile PDF and image editing pipelines serving 2M+ active users. Achieved 60fps canvas performance and sub-second export times."
        val exp1Layout = StaticLayout.Builder.obtain(exp1, 0, exp1.length, bodyPaint, 340).build()
        canvas.save()
        canvas.translate(215f, 235f)
        exp1Layout.draw(canvas)
        canvas.restore()

        canvas.drawText("Senior Android Developer • Nexus Corp", 215f, 320f, expTitlePaint)
        val exp2 = "Spearheaded Jetpack Compose migration across core application modules, increasing team velocity by 40% and cutting codebase size significantly."
        val exp2Layout = StaticLayout.Builder.obtain(exp2, 0, exp2.length, bodyPaint, 340).build()
        canvas.save()
        canvas.translate(215f, 330f)
        exp2Layout.draw(canvas)
        canvas.restore()

        doc.finishPage(page)
        FileOutputStream(file).use { out -> doc.writeTo(out) }
        doc.close()
        file
    }
}

