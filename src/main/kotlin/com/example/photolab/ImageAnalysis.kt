package com.example.photolab

import javafx.scene.image.Image
import java.util.HashSet

object ImageAnalysis {

    enum class ImageType { BINARY, GRAYSCALE, COLOR }

    // Data Class para el Histograma. Estructura de datos para almacenar la distribución de frecuencias de color.
    data class Histogram(val red: IntArray, val green: IntArray, val blue: IntArray, val gray: IntArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Histogram

            if (!red.contentEquals(other.red)) return false
            if (!green.contentEquals(other.green)) return false
            if (!blue.contentEquals(other.blue)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = red.contentHashCode()
            result = 31 * result + green.contentHashCode()
            result = 31 * result + blue.contentHashCode()
            return result
        }
    }

    // Información de Imagen
    // Cuenta cuántos colores únicos existen en la imagen.
    fun countUniqueColors(image: Image): Int {
        val reader = image.pixelReader
        val width = image.width.toInt()
        val height = image.height.toInt()

        val uniqueColors = HashSet<Int>()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = reader.getArgb(x, y)
                uniqueColors.add(color)
            }
        }
        return uniqueColors.size
    }

    // Cálculo de Histograma. Calcula el Histograma de la imagen.
    fun calculateHistogram(image: Image): Histogram {
        val reader = image.pixelReader
        val width = image.width.toInt()
        val height = image.height.toInt()

        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)
        val gray = IntArray(256)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val c = reader.getColor(x, y)

                // Canal RGB
                val r = (c.red * 255).toInt().coerceIn(0, 255)
                val g = (c.green * 255).toInt().coerceIn(0, 255)
                val b = (c.blue * 255).toInt().coerceIn(0, 255)

                // Canal Gris (Luma)
                val lumaVal = (0.21 * c.red + 0.72 * c.green + 0.07 * c.blue) * 255
                val grayVal = lumaVal.toInt().coerceIn(0, 255)

                red[r]++
                green[g]++
                blue[b]++
                gray[grayVal]++
            }
        }
        return Histogram(red, green, blue, gray)
    }

    // Analizar tipo de imagen para optimizar el formato guardado
    // Analiza los píxeles para determinar el tipo de imagen más simple posible (Binaria < Escala de Grises < Color).
    fun analyzeImageType(image: Image): ImageType {
        val reader = image.pixelReader
        val w = image.width.toInt()
        val h = image.height.toInt()

        var isBinary = true
        var isGrayscale = true

        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = reader.getColor(x, y)

                if (isBinary) {
                    val isWhite = c.red > 0.99 && c.green > 0.99 && c.blue > 0.99
                    val isBlack = c.red < 0.01 && c.green < 0.01 && c.blue < 0.01
                    if (!isWhite && !isBlack) isBinary = false
                }
                if (isGrayscale) {
                    if (c.red != c.green || c.green != c.blue) {
                        isGrayscale = false
                    }
                }
                if (!isBinary && !isGrayscale) return ImageType.COLOR
            }
        }
        return if (isBinary) ImageType.BINARY else if (isGrayscale) ImageType.GRAYSCALE else ImageType.COLOR
    }
}