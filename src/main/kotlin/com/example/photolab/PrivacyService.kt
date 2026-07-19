package com.example.photolab

import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color

object PrivacyService {

    // Aplica un efecto de pixelado en un área cuadrada específica de la imagen
    fun pixelateArea(source: Image, centerX: Int, centerY: Int, areaSize: Int, blockSize: Int): Image {
        val width = source.width.toInt()
        val height = source.height.toInt()
        val output = WritableImage(width, height)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        // 1. Copiar la imagen original intacta
        for (y in 0 until height) {
            for (x in 0 until width) {
                writer.setColor(x, y, reader.getColor(x, y))
            }
        }

        // 2. Definir los límites seguros del área de censura
        val startX = (centerX - areaSize / 2).coerceIn(0, width)
        val endX = (centerX + areaSize / 2).coerceIn(0, width)
        val startY = (centerY - areaSize / 2).coerceIn(0, height)
        val endY = (centerY + areaSize / 2).coerceIn(0, height)

        // 3. Recorrer el área saltando por el tamaño del bloque para promediar
        for (y in startY until endY step blockSize) {
            for (x in startX until endX step blockSize) {
                val bEndX = (x + blockSize).coerceAtMost(endX)
                val bEndY = (y + blockSize).coerceAtMost(endY)

                var r = 0.0; var g = 0.0; var b = 0.0; var a = 0.0
                var count = 0

                // Sumar los colores del bloque actual
                for (by in y until bEndY) {
                    for (bx in x until bEndX) {
                        val color = reader.getColor(bx, by)
                        r += color.red
                        g += color.green
                        b += color.blue
                        a += color.opacity
                        count++
                    }
                }

                if (count > 0) {
                    val avgColor = Color(r / count, g / count, b / count, a / count)
                    // Pintar todo el bloque con el color promedio
                    for (by in y until bEndY) {
                        for (bx in x until bEndX) {
                            writer.setColor(bx, by, avgColor)
                        }
                    }
                }
            }
        }
        return output
    }
}