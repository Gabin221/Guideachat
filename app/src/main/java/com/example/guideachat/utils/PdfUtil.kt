package com.example.guideachat.utils

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.guideachat.data.model.VoitureEntity
import java.io.File
import java.io.FileOutputStream

object PdfUtil {

    fun generateAndSharePdf(context: Context, voiture: VoitureEntity) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Format A4
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Titre
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("${voiture.marque} ${voiture.nom_modele}", 40f, 60f, paint)

        // Prix
        paint.textSize = 18f
        paint.color = 0xFF2E7D32.toInt() // Vert
        canvas.drawText("Prix estimé : ${voiture.prix_min}€ - ${voiture.prix_max}€", 40f, 100f, paint)

        // Reset Noir
        paint.color = 0xFF000000.toInt()
        paint.textSize = 14f
        paint.isFakeBoldText = false

        var y = 140f

        // Fiabilité
        paint.isFakeBoldText = true
        canvas.drawText("BILAN FIABILITÉ :", 40f, y, paint)
        paint.isFakeBoldText = false
        y += 25f

        // Gestion sommaire du retour à la ligne pour le texte long
        val words = voiture.bilan.fiabilite_texte.split(" ")
        var line = ""
        for (word in words) {
            if (paint.measureText(line + word) < 500) {
                line += "$word "
            } else {
                canvas.drawText(line, 40f, y, paint)
                y += 20f
                line = "$word "
            }
        }
        canvas.drawText(line, 40f, y, paint) // Reste de la ligne

        y += 40f

        // Moteurs
        paint.isFakeBoldText = true
        canvas.drawText("Moteurs Conseillés :", 40f, y, paint)
        y += 25f
        paint.isFakeBoldText = false
        voiture.bilan.moteurs_conseilles.forEach {
            canvas.drawText("- $it", 60f, y, paint)
            y += 20f
        }

        y += 20f
        paint.isFakeBoldText = true
        paint.color = 0xFFC62828.toInt() // Rouge
        canvas.drawText("Moteurs à Éviter :", 40f, y, paint)
        y += 25f
        paint.isFakeBoldText = false
        voiture.bilan.moteurs_deconseilles.forEach {
            canvas.drawText("- $it", 60f, y, paint)
            y += 20f
        }

        document.finishPage(page)

        // Sauvegarde temporaire
        val file = File(context.cacheDir, "guide_auto_${voiture.id_modele}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()

        sharePdf(context, file)
    }

    private fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Partager la fiche PDF"))
    }
}