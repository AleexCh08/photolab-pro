package com.example.photolab

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.TermCriteria
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import kotlin.math.pow
import kotlin.math.roundToInt

object QuantizationService {
    // MÉTODO 1: Reducción de Bits (Uniforme)
    // Reduce la profundidad de color eliminando los bits menos significativos.
    fun reduceBitDepth(source: Image, bitsPerChannel: Int): Image {
        val w = source.width.toInt()
        val h = source.height.toInt()
        val output = WritableImage(w, h)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        val levels = 2.0.pow(bitsPerChannel).toInt()
        val divisor = 255.0 / (levels - 1)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = reader.getColor(x, y)

                // Cuantización uniforme por canal
                val r = (((c.red * 255) / divisor).roundToInt() * divisor) / 255.0
                val g = (((c.green * 255) / divisor).roundToInt() * divisor) / 255.0
                val b = (((c.blue * 255) / divisor).roundToInt() * divisor) / 255.0

                writer.setColor(x, y, Color(r.coerceIn(0.0, 1.0), g.coerceIn(0.0, 1.0), b.coerceIn(0.0, 1.0), c.opacity))
            }
        }
        return output
    }

    // MÉTODO 2: Algoritmo de Popularidad (Paleta)
    // 1. Histograma de frecuencias. 2. Seleccionar Top K colores. 3. Mapear al más cercano.
    fun applyPopularityQuantization(source: Image, paletteSize: Int): Image {
        val w = source.width.toInt()
        val h = source.height.toInt()
        val reader = source.pixelReader

        val colorCounts = HashMap<Int, Int>()
        for (y in 0 until h) {
            for (x in 0 until w) {
                val argb = reader.getArgb(x, y)
                colorCounts[argb] = colorCounts.getOrDefault(argb, 0) + 1
            }
        }

        val palette = colorCounts.entries
            .sortedByDescending { it.value }
            .take(paletteSize)
            .map { it.key }
            .toIntArray()

        val cache = HashMap<Int, Int>()
        val output = WritableImage(w, h)
        val writer = output.pixelWriter

        for (y in 0 until h) {
            for (x in 0 until w) {
                val original = reader.getArgb(x, y)
                if (cache.containsKey(original)) {
                    writer.setArgb(x, y, cache[original]!!)
                } else {
                    val nearest = findNearestColor(original, palette)
                    cache[original] = nearest
                    writer.setArgb(x, y, nearest)
                }
            }
        }
        return output
    }

    private fun findNearestColor(target: Int, palette: IntArray): Int {
        var minDist = Double.MAX_VALUE
        var bestColor = target
        val tR = (target shr 16) and 0xFF
        val tG = (target shr 8) and 0xFF
        val tB = target and 0xFF

        for (color in palette) {
            val pR = (color shr 16) and 0xFF
            val pG = (color shr 8) and 0xFF
            val pB = color and 0xFF
            val dist = ((tR - pR) * (tR - pR) + (tG - pG) * (tG - pG) + (tB - pB) * (tB - pB)).toDouble()
            if (dist < minDist) {
                minDist = dist
                bestColor = color
            }
            if (dist == 0.0) break
        }
        return (target and -0x1000000) or (bestColor and 0x00FFFFFF)
    }

    // MÉTODO 3: K-Means Clustering (OpenCV)
    // Agrupa los colores en K clusters basados en similitud estadística.
    fun applyKMeansQuantization(source: Image, k: Int): Image {
        val matOriginal = imageToMat(source)

        val width = matOriginal.cols()
        val height = matOriginal.rows()
        val channels = matOriginal.channels() // Debería ser 3

        val numPixels = width * height
        val data = ByteArray(numPixels * channels)
        matOriginal.get(0, 0, data)

        val samples = Mat(numPixels, 3, CvType.CV_32F)
        val floatData = FloatArray(numPixels * channels)
        for (i in data.indices) {
            floatData[i] = (data[i].toInt() and 0xFF).toFloat()
        }

        samples.put(0, 0, floatData)

        val labels = Mat()
        val centers = Mat()
        val criteria = TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 10, 1.0)

        // Ejecutar K-Means
        Core.kmeans(samples, k, labels, criteria, 1, Core.KMEANS_PP_CENTERS, centers)

        val centersData = FloatArray(k * 3)
        centers.get(0, 0, centersData)

        val labelsData = IntArray(numPixels)
        labels.get(0, 0, labelsData)

        val resultData = ByteArray(numPixels * channels)

        var byteIdx = 0
        for (i in 0 until numPixels) {
            val clusterIdx = labelsData[i]
            // Centers data está en float B,G,R
            val b = centersData[clusterIdx * 3].toInt().toByte()
            val g = centersData[clusterIdx * 3 + 1].toInt().toByte()
            val r = centersData[clusterIdx * 3 + 2].toInt().toByte()

            resultData[byteIdx++] = b
            resultData[byteIdx++] = g
            resultData[byteIdx++] = r
        }

        val resultMat = Mat(height, width, CvType.CV_8UC3)
        resultMat.put(0, 0, resultData)

        return matToImage(resultMat)
    }

    private fun imageToMat(image: Image): Mat {
        val bImage: BufferedImage = SwingFXUtils.fromFXImage(image, null)
        val bImageConverted = BufferedImage(bImage.width, bImage.height, BufferedImage.TYPE_3BYTE_BGR)
        val g = bImageConverted.createGraphics()
        g.drawImage(bImage, 0, 0, null)
        g.dispose()

        val data = (bImageConverted.raster.dataBuffer as DataBufferByte).data
        val mat = Mat(bImageConverted.height, bImageConverted.width, CvType.CV_8UC3)
        mat.put(0, 0, data)
        return mat
    }

    private fun matToImage(mat: Mat): Image {
        val mat8u = Mat()
        mat.convertTo(mat8u, CvType.CV_8U)
        val data = ByteArray(mat8u.rows() * mat8u.cols() * mat8u.channels())
        mat8u.get(0, 0, data)
        val image = BufferedImage(mat8u.cols(), mat8u.rows(), BufferedImage.TYPE_3BYTE_BGR)
        image.raster.setDataElements(0, 0, mat8u.cols(), mat8u.rows(), data)
        return SwingFXUtils.toFXImage(image, null)
    }
}