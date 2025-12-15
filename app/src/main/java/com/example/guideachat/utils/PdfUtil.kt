package com.example.guideachat.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.guideachat.data.model.VoitureEntity
import java.io.File
import java.io.FileOutputStream

object PdfUtil {

    fun generateAndSharePdf(context: Context, voiture: VoitureEntity) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Marges
        val marginLeft = 40f
        val marginRight = 40f
        val contentWidth = (595 - marginLeft - marginRight).toInt()

        var currentY = 50f

        // --- Styles ---
        val titlePaint = TextPaint().apply {
            textSize = 26f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }
        val normalPaint = TextPaint().apply {
            textSize = 14f
            color = android.graphics.Color.DKGRAY
        }
        val greenPaint = TextPaint().apply {
            textSize = 16f
            isFakeBoldText = true
            color = 0xFF2E7D32.toInt()
        }
        val redPaint = TextPaint().apply {
            textSize = 16f
            isFakeBoldText = true
            color = 0xFFC62828.toInt()
        }

        // --- Dessin ---

        // 1. Titre
        drawMultiLineText(canvas, "${voiture.marque} ${voiture.nom_modele}", marginLeft, currentY, contentWidth, titlePaint)
        currentY += 40f

        // 2. Dates & Prix
        val debut = voiture.annees_production.firstOrNull() ?: "?"
        val fin = voiture.annees_production.getOrNull(1)?.toString() ?: "Aujourd'hui"
        val dates = "Production : $debut - $fin"

        drawMultiLineText(canvas, dates, marginLeft, currentY, contentWidth, normalPaint)
        currentY += 20f

        drawMultiLineText(canvas, "Prix estimé : ${voiture.prix_min}€ - ${voiture.prix_max}€", marginLeft, currentY, contentWidth, greenPaint)
        currentY += 40f

        // 3. Fiabilité (Texte long avec retour ligne auto)
        drawMultiLineText(canvas, "BILAN FIABILITÉ :", marginLeft, currentY, contentWidth, titlePaint)
        currentY += 30f

        // C'est ici que la magie du retour à la ligne opère
        currentY += drawMultiLineText(canvas, voiture.bilan.fiabilite_texte, marginLeft, currentY, contentWidth, normalPaint)
        currentY += 30f

        // 4. Moteurs
        drawMultiLineText(canvas, "Moteurs Conseillés :", marginLeft, currentY, contentWidth, greenPaint)
        currentY += 25f
        voiture.bilan.moteurs_conseilles.forEach { moteur ->
            // On affiche : "Nom Moteur        5.8 L/100km"
            val ligne = "• ${moteur.nom}  --  ${moteur.conso_mixte}"
            currentY += drawMultiLineText(canvas, ligne, marginLeft + 10, currentY, contentWidth, normalPaint) + 5f
        }

        currentY += 20f
        drawMultiLineText(canvas, "Moteurs à Éviter :", marginLeft, currentY, contentWidth, redPaint)
        currentY += 25f
        voiture.bilan.moteurs_deconseilles.forEach { moteur ->
            val ligne = "• ${moteur.nom}  --  ${moteur.conso_mixte}"
            currentY += drawMultiLineText(canvas, ligne, marginLeft + 10, currentY, contentWidth, normalPaint) + 5f
        }

//        drawMultiLineText(canvas, "Moteurs Conseillés :", marginLeft, currentY, contentWidth, greenPaint)
//        currentY += 25f
//        voiture.bilan.moteurs_conseilles.forEach {
//            currentY += drawMultiLineText(canvas, "• $it", marginLeft + 10, currentY, contentWidth, normalPaint) + 5f
//        }
//
//        currentY += 20f
//        drawMultiLineText(canvas, "Moteurs à Éviter :", marginLeft, currentY, contentWidth, redPaint)
//        currentY += 25f
//        voiture.bilan.moteurs_deconseilles.forEach {
//            currentY += drawMultiLineText(canvas, "• $it", marginLeft + 10, currentY, contentWidth, normalPaint) + 5f
//        }

        pdfDocument.finishPage(page)

        // Sauvegarde Fichier
        val fileName = "Fiche_${voiture.marque}_${voiture.nom_modele}.pdf".replace(" ", "_")
        val file = File(context.cacheDir, fileName) // On utilise cacheDir pour le partage temporaire

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            sharePdfFile(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Helper pour dessiner du texte multi-lignes
    private fun drawMultiLineText(canvas: Canvas, text: String, x: Float, y: Float, width: Int, paint: TextPaint): Float {
        val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1.0f, 1.0f)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(x, y)
        staticLayout.draw(canvas)
        canvas.restore()

        return staticLayout.height.toFloat() // Retourne la hauteur utilisée
    }

    private fun sharePdfFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            // Ces flags sont cruciaux pour permettre aux applis externes (Messages, Drive) de lire le fichier
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Le chooser permet de voir "Enregistrer dans Fichiers", "Messages", etc.
        val chooser = Intent.createChooser(intent, "Partager la fiche")
        // Hack pour donner la permission au chooser lui-même sur certaines versions d'Android
        val resInfoList = context.packageManager.queryIntentActivities(chooser, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(chooser)
    }
}