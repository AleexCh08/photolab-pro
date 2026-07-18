package com.example.photolab

import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import kotlin.math.round

object TransformService {

    enum class InterpolationMethod(val label: String) {
        NEAREST_NEIGHBOR("Vecino más próximo"), // Selecciona el píxel más cercano. Produce "dientes de sierra" (aliasing).
        BILINEAR("Interpolación Bilineal"); // Calcula el promedio ponderado de los 4 píxeles vecinos. Suaviza los bordes.
        override fun toString(): String = label
    }

    // Sección: ESCALADO Y ZOOM
    // Escala la imagen usando el factor y modo especificados.
    fun scaleImage(source: Image, scaleFactor: Double, method: InterpolationMethod): Image {
        val newWidth = (source.width * scaleFactor).toInt().coerceAtLeast(1)
        val newHeight = (source.height * scaleFactor).toInt().coerceAtLeast(1)
        return resizeImage(source, newWidth, newHeight, method)
    }

    fun resizeImage(source: Image, newWidth: Int, newHeight: Int, method: InterpolationMethod): Image {
        val sourceWidth = source.width.toInt()
        val sourceHeight = source.height.toInt()

        val safeWidth = newWidth.coerceAtLeast(1)
        val safeHeight = newHeight.coerceAtLeast(1)

        val scaleX = safeWidth / sourceWidth.toDouble()
        val scaleY = safeHeight / sourceHeight.toDouble()

        val output = WritableImage(safeWidth, safeHeight)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        for (y in 0 until safeHeight) {
            for (x in 0 until safeWidth) {
                val gx = x / scaleX
                val gy = y / scaleY

                val color = when (method) {
                    InterpolationMethod.NEAREST_NEIGHBOR -> {
                        // Vecino más próximo. Se redondea al entero más cercano.
                        val srcX = round(gx).toInt().coerceIn(0, sourceWidth - 1)
                        val srcY = round(gy).toInt().coerceIn(0, sourceHeight - 1)
                        reader.getColor(srcX, srcY)
                    }
                    InterpolationMethod.BILINEAR -> {
                        // Interpolación Bilineal. Requiere el cálculo de los vecinos y sus pesos.
                        val gxi = gx.toInt()
                        val gyi = gy.toInt()

                        val a = gx - gxi
                        val b = gy - gyi

                        val x0 = gxi.coerceIn(0, sourceWidth - 1)
                        val x1 = (gxi + 1).coerceIn(0, sourceWidth - 1)
                        val y0 = gyi.coerceIn(0, sourceHeight - 1)
                        val y1 = (gyi + 1).coerceIn(0, sourceHeight - 1)

                        val c00 = reader.getColor(x0, y0)
                        val c10 = reader.getColor(x1, y0)
                        val c01 = reader.getColor(x0, y1)
                        val c11 = reader.getColor(x1, y1)

                        val red = bilinearInterpolate(c00.red, c10.red, c01.red, c11.red, a, b)
                        val green = bilinearInterpolate(c00.green, c10.green, c01.green, c11.green, a, b)
                        val blue = bilinearInterpolate(c00.blue, c10.blue, c01.blue, c11.blue, a, b)
                        val opacity = bilinearInterpolate(c00.opacity, c10.opacity, c01.opacity, c11.opacity, a, b)

                        Color(red, green, blue, opacity)
                    }
                }
                writer.setColor(x, y, color)
            }
        }
        return output
    }

    // Auxiliar privado para la fórmula bilineal
    private fun bilinearInterpolate(v00: Double, v10: Double, v01: Double, v11: Double, a: Double, b: Double): Double {
        val result = (1 - a) * (1 - b) * v00 +
                a * (1 - b) * v10 +
                (1 - a) * b * v01 +
                a * b * v11
        return result.coerceIn(0.0, 1.0)
    }

    // Sección: TRANSFORMACIONES GEOMÉTRICAS
    // Espejo horizontal. x' = width - 1 - x
    fun flipHorizontal(source: Image): Image {
        val w = source.width.toInt()
        val h = source.height.toInt()
        val output = WritableImage(w, h)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        for (y in 0 until h) {
            for (x in 0 until w) {
                val color = reader.getColor(w - 1 - x, y)
                writer.setColor(x, y, color)
            }
        }
        return output
    }

    // Espejo Vertical. Y' = height - 1 - y
    fun flipVertical(source: Image): Image {
        val w = source.width.toInt()
        val h = source.height.toInt()
        val output = WritableImage(w, h)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        for (y in 0 until h) {
            for (x in 0 until w) {
                val color = reader.getColor(x, h - 1 - y)
                writer.setColor(x, y, color)
            }
        }
        return output
    }

    // Rotación de 180 grados. Equivale a un espejo horizontal + espejo vertical.
    fun rotate180(source: Image): Image {
        val w = source.width.toInt()
        val h = source.height.toInt()
        val output = WritableImage(w, h)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        for (y in 0 until h) {
            for (x in 0 until w) {
                val color = reader.getColor(w - 1 - x, h - 1 - y)
                writer.setColor(x, y, color)
            }
        }
        return output
    }

    // Rotación 90 grados a la derecha
    // La fila x de destino se llena con la columna invertida de origen.
    fun rotate90Right(source: Image): Image {
        val srcW = source.width.toInt()
        val srcH = source.height.toInt()
        val output = WritableImage(srcH, srcW)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        for (y in 0 until srcW) {
            for (x in 0 until srcH) {
                val color = reader.getColor(y, srcH - 1 - x)
                writer.setColor(x, y, color)
            }
        }
        return output
    }

    // Rotación 90 grados a la izquierda.
    fun rotate90Left(source: Image): Image {
        val srcW = source.width.toInt()
        val srcH = source.height.toInt()
        val output = WritableImage(srcH, srcW)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        for (y in 0 until srcW) {
            for (x in 0 until srcH) {
                val color = reader.getColor(srcW - 1 - y, x)
                writer.setColor(x, y, color)
            }
        }
        return output
    }

    // Recorta una subregión de la imagen.
    fun cropImage(source: Image, x: Int, y: Int, w: Int, h: Int): Image {
        val safeX = x.coerceIn(0, source.width.toInt() - 1)
        val safeY = y.coerceIn(0, source.height.toInt() - 1)
        val safeW = w.coerceAtMost(source.width.toInt() - safeX).coerceAtLeast(1)
        val safeH = h.coerceAtMost(source.height.toInt() - safeY).coerceAtLeast(1)

        val output = WritableImage(safeW, safeH)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        // Copiamos píxel a píxel la región seleccionada
        for (j in 0 until safeH) {
            for (i in 0 until safeW) {
                val color = reader.getColor(safeX + i, safeY + j)
                writer.setColor(i, j, color)
            }
        }
        return output
    }
}