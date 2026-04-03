package com.udchemistry.mobile.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.udchemistry.mobile.model.AttendanceRecord
import com.udchemistry.mobile.model.DashboardData
import com.udchemistry.mobile.model.Institute
import com.udchemistry.mobile.model.PaymentRecord
import com.udchemistry.mobile.model.QrCodeRecord
import com.udchemistry.mobile.model.Student
import java.io.File
import java.io.FileOutputStream

class ExportHelper(private val context: Context) {
    private val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")

    private val exportDir: File by lazy {
        File(context.cacheDir, "exports").apply { mkdirs() }
    }

    fun shareStudentsCsv(students: List<Student>, institutes: List<Institute>) {
        val instituteMap = institutes.associateBy { it.id }
        val rows = buildList {
            add("Student Code,Student Name,A/L Year,Institute,WhatsApp,Joined Date,Status")
            students.forEach { student ->
                add(
                    listOf(
                        student.studentCode.orEmpty(),
                        student.fullName,
                        student.alYear.toString(),
                        instituteMap[student.instituteId]?.name.orEmpty(),
                        student.whatsappNumber,
                        student.joinedDate,
                        student.status,
                    ).joinToString(",") { escapeCsv(it) },
                )
            }
        }
        shareTextFile("students-report.csv", rows.joinToString("\n"), "text/csv")
    }

    fun sharePaymentsCsv(payments: List<PaymentRecord>, students: List<Student>) {
        val studentMap = students.associateBy { it.id }
        val rows = buildList {
            add("Student,Month,Year,Amount,Paid,Paid Date")
            payments.forEach { payment ->
                add(
                    listOf(
                        studentMap[payment.studentId]?.fullName.orEmpty(),
                        payment.paymentMonth.toString(),
                        payment.paymentYear.toString(),
                        payment.amount.toString(),
                        payment.paid.toString(),
                        payment.paidDate.orEmpty(),
                    ).joinToString(",") { escapeCsv(it) },
                )
            }
        }
        shareTextFile("payments-report.csv", rows.joinToString("\n"), "text/csv")
    }

    fun shareSummaryPdf(
        dashboard: DashboardData,
        attendance: List<AttendanceRecord>,
        payments: List<PaymentRecord>,
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val titlePaint = Paint().apply {
            textSize = 24f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            textSize = 14f
        }

        var y = 48f
        canvas.drawText("UD chemistry summary", 40f, y, titlePaint)
        y += 40f
        dashboard.metrics.forEach { metric ->
            canvas.drawText("${metric.label}: ${metric.value}", 40f, y, bodyPaint)
            y += 24f
        }
        y += 18f
        canvas.drawText("Attendance rows: ${attendance.size}", 40f, y, bodyPaint)
        y += 24f
        canvas.drawText("Payment rows: ${payments.size}", 40f, y, bodyPaint)

        document.finishPage(page)

        val file = File(exportDir, "ud-chemistry-summary.pdf")
        FileOutputStream(file).use(document::writeTo)
        document.close()
        shareFile(file, "application/pdf")
    }

    fun shareStudentQrImage(qrCode: QrCodeRecord, studentName: String, whatsappNumber: String) {
        val imageBytes = QrCodeUtils.decodeDataUrlBytes(qrCode.qrImageUrl)
            ?: QrCodeUtils.decodeDataUrlBytes(QrCodeUtils.createPngDataUrl(qrCode.qrData))
            ?: run {
                showToast("QR image is not available yet.")
                return
            }
        val normalizedNumber = normalizeWhatsAppNumber(whatsappNumber) ?: run {
            showToast("Student WhatsApp number is invalid.")
            return
        }
        val whatsappPackage = resolveWhatsAppPackage() ?: run {
            showToast("WhatsApp is not installed on this device.")
            return
        }

        val file = File(exportDir, "student-qr-${qrCode.shareToken}.png")
        file.writeBytes(imageBytes)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            `package` = whatsappPackage
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Hello $studentName, here is your Chemistry class QR code.")
            putExtra("jid", "$normalizedNumber@s.whatsapp.net")
            putExtra("phone", normalizedNumber)
            clipData = ClipData.newRawUri("student_qr", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (shareIntent.resolveActivity(context.packageManager) == null) {
            showToast("Unable to open WhatsApp for this student.")
            return
        }

        context.grantUriPermission(whatsappPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(shareIntent)
        } catch (_: ActivityNotFoundException) {
            showToast("Unable to open WhatsApp for this student.")
        }
    }

    private fun shareTextFile(fileName: String, content: String, mimeType: String) {
        val file = File(exportDir, fileName)
        file.writeText(content)
        shareFile(file, mimeType)
    }

    private fun shareFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share export").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun resolveWhatsAppPackage(): String? {
        return whatsappPackages.firstOrNull { packageName ->
            runCatching {
                context.packageManager.getPackageInfo(packageName, 0)
            }.isSuccess
        }
    }

    private fun normalizeWhatsAppNumber(rawNumber: String): String? {
        val digits = rawNumber.filter(Char::isDigit)
        if (digits.isBlank()) {
            return null
        }

        val normalized = when {
            digits.startsWith("00") && digits.length > 2 -> digits.drop(2)
            digits.startsWith("94") && digits.length >= 11 -> digits
            digits.startsWith("0") && digits.length == 10 -> "94${digits.drop(1)}"
            digits.length == 9 -> "94$digits"
            else -> digits
        }

        return normalized.takeIf { it.length >= 11 }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
