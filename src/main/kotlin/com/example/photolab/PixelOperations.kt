package com.example.photolab

import javafx.scene.image.Image
import javafx.scene.image.PixelReader
import javafx.scene.image.PixelWriter
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import kotlin.math.pow

object PixelOperations {

    enum class GrayscaleMethod {
        AVERAGE, // Conversión sencilla: (R + G + B) / 3
        LUMA     // Conversión precisa: 0.21R + 0.72G + 0.07B
    }

    // Sección: UMBRALIZACIÓN
    // Binaria Simple
    // Si Luma(píxel) >= Umbral -> Blanco (1.0)
    // Si Luma(píxel) < Umbral  -> Negro (0.0)
    fun thresholdBinary(source: Image, threshold: Double): Image {
        val w = source.width.toInt()
        val h = source.height.toInt()
        val output = WritableImage(w, h)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = reader.getColor(x, y)
                val luma = calculateLuma(c)

                val finalColor = if (luma >= threshold) Color.WHITE else Color.BLACK
                writer.setColor(x, y, Color(finalColor.red, finalColor.green, finalColor.blue, c.opacity))
            }
        }
        return output
    }

    // Umbralización Cortar Rango (Mantener el resto)
    // Mantiene la imagen original, excepto los píxeles que caen dentro del rango [min, max], los cuales se fuerzan a Negro.
    fun thresholdCutRange(source: Image, min: Double, max: Double): Image {
        val w = source.width.toInt()
        val h = source.height.toInt()
        val output = WritableImage(w, h)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = reader.getColor(x, y)
                val luma = calculateLuma(c)

                val finalColor = if (luma in min..max) Color.BLACK else c
                writer.setColor(x, y, Color(finalColor.red, finalColor.green, finalColor.blue, c.opacity))
            }
        }
        return output
    }

    // Umbralización Selección de Rango (Binaria de Rango)
    // Lo que está dentro del rango se vuelve Blanco, lo que está fuera se vuelve Negro
    fun thresholdSelectRange(source: Image, min: Double, max: Double): Image {
        val w = source.width.toInt()
        val h = source.height.toInt()
        val output = WritableImage(w, h)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = reader.getColor(x, y)
                val luma = calculateLuma(c)

                val finalColor = if (luma in min..max) Color.WHITE else Color.BLACK
                writer.setColor(x, y, Color(finalColor.red, finalColor.green, finalColor.blue, c.opacity))
            }
        }
        return output
    }

    // Sección: BRILLO - CONTRASTE - COLOREAR
    // Ajusta el brillo, contraste, saturación y gamma mediante una transformación lineal.
    // Función maestra: Aplica Saturación, Gamma, Brillo y Contraste
    fun applyAdjustments(source: Image, saturation: Double, gamma: Double, brightness: Double, contrast: Double): Image {
        val w = source.width.toInt()
        val h = source.height.toInt()
        val output = WritableImage(w, h)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        val useGamma = gamma != 1.0

        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = reader.getColor(x, y)
                var r = c.red
                var g = c.green
                var b = c.blue

                // 1. SATURACIÓN
                if (saturation != 1.0) {
                    val gray = 0.21 * r + 0.72 * g + 0.07 * b
                    r = gray + (r - gray) * saturation
                    g = gray + (g - gray) * saturation
                    b = gray + (b - gray) * saturation
                }

                // 2. GAMMA (Ajustado a la fórmula de PDI: v^(1/gamma))
                if (useGamma) {
                    val exp = 1.0 / gamma
                    r = if (r > 0) r.pow(exp) else 0.0
                    g = if (g > 0) g.pow(exp) else 0.0
                    b = if (b > 0) b.pow(exp) else 0.0
                }

                // 3. BRILLO Y CONTRASTE
                if (contrast != 1.0 || brightness != 0.0) {
                    r = contrast * (r - 0.5) + 0.5 + brightness
                    g = contrast * (g - 0.5) + 0.5 + brightness
                    b = contrast * (b - 0.5) + 0.5 + brightness
                }

                writer.setColor(x, y, Color(
                    r.coerceIn(0.0, 1.0),
                    g.coerceIn(0.0, 1.0),
                    b.coerceIn(0.0, 1.0),
                    c.opacity
                ))
            }
        }
        return output
    }

    // Escala de Grises: Convierte la imagen a escala de grises usando el modo seleccionado.
    fun toGrayscale(source: Image, method: GrayscaleMethod): Image {
        val width = source.width.toInt()
        val height = source.height.toInt()
        val output = WritableImage(width, height)
        val reader: PixelReader = source.pixelReader
        val writer: PixelWriter = output.pixelWriter

        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = reader.getColor(x, y)

                val grayValue = when (method) {
                    GrayscaleMethod.AVERAGE -> calculateAverage(color)
                    GrayscaleMethod.LUMA -> calculateLuma(color)
                }

                val newColor = Color(grayValue, grayValue, grayValue, color.opacity)
                writer.setColor(x, y, newColor)
            }
        }
        return output
    }

    // Escala de Colores (Monocromática)
    // Convierte la imagen a una escala monocromática de un color específico. Mantiene la luminancia original, pero reemplaza el matiz.
    // Canal_Nuevo = Luma_Original * Canal_Color_Objetivo
    fun toColorScale(source: Image, targetColor: Color): Image {
        val width = source.width.toInt()
        val height = source.height.toInt()
        val output = WritableImage(width, height)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        for (y in 0 until height) {
            for (x in 0 until width) {
                val c = reader.getColor(x, y)
                val luma = calculateLuma(c)
                val newR = luma * targetColor.red
                val newG = luma * targetColor.green
                val newB = luma * targetColor.blue
                writer.setColor(x, y, Color(newR, newG, newB, c.opacity))
            }
        }
        return output
    }

    // Colorear por producto (Multiplicativo). Filtro Sustractivo
    // C_final = C_original * C_filtro
    fun applyColorFilter(source: Image, filterColor: Color): Image {
        val width = source.width.toInt()
        val height = source.height.toInt()
        val output = WritableImage(width, height)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        for (y in 0 until height) {
            for (x in 0 until width) {
                val c = reader.getColor(x, y)
                val r = c.red * filterColor.red
                val g = c.green * filterColor.green
                val b = c.blue * filterColor.blue
                writer.setColor(x, y, Color(r, g, b, c.opacity))
            }
        }
        return output
    }

    // Colorear por suma (Aditivo)
    // C_final = min(1.0, C_original + C_filtro)
    fun applyColorAdd(source: Image, addColor: Color): Image {
        val width = source.width.toInt()
        val height = source.height.toInt()
        val output = WritableImage(width, height)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        for (y in 0 until height) {
            for (x in 0 until width) {
                val c = reader.getColor(x, y)
                val r = (c.red + addColor.red).coerceIn(0.0, 1.0)
                val g = (c.green + addColor.green).coerceIn(0.0, 1.0)
                val b = (c.blue + addColor.blue).coerceIn(0.0, 1.0)
                writer.setColor(x, y, Color(r, g, b, c.opacity))
            }
        }
        return output
    }

    // Filtro Negativo. Invierte los colores de la imagen
    // C_final = 1.0 - C_original
    fun applyNegative(source: Image): Image {
        val width = source.width.toInt()
        val height = source.height.toInt()
        val output = WritableImage(width, height)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        for (y in 0 until height) {
            for (x in 0 until width) {
                val c = reader.getColor(x, y)
                val r = 1.0 - c.red
                val g = 1.0 - c.green
                val b = 1.0 - c.blue
                writer.setColor(x, y, Color(r, g, b, c.opacity))
            }
        }
        return output
    }

    // --- SECCIÓN: GENERACIÓN DE RUIDO ---
    // Ruido Sal y Pimienta
    // intensity: Probabilidad de que un píxel sea afectado (0.0 a 1.0).
    fun generateSaltPepperNoise(source: Image, intensity: Double): Image {
        val w = source.width.toInt()
        val h = source.height.toInt()
        val output = WritableImage(w, h)
        val reader = source.pixelReader
        val writer = output.pixelWriter
        val rand = java.util.Random()

        val threshold = intensity / 2.0

        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = reader.getColor(x, y)
                val chance = rand.nextDouble() // 0.0 a 1.0

                val finalColor = if (chance < threshold) {
                    Color.BLACK
                } else if (chance < intensity) {
                    Color.WHITE
                } else {
                    c
                }
                writer.setColor(x, y, finalColor)
            }
        }
        return output
    }

    // Ruido Gaussiano (Aditivo)
    // sigma: Desviación estándar (intensidad del ruido).
    fun generateGaussianNoise(source: Image, sigma: Double): Image {
        val w = source.width.toInt()
        val h = source.height.toInt()
        val output = WritableImage(w, h)
        val reader = source.pixelReader
        val writer = output.pixelWriter
        val rand = java.util.Random()

        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = reader.getColor(x, y)

                val noise = rand.nextGaussian() * sigma

                val r = (c.red + noise).coerceIn(0.0, 1.0)
                val g = (c.green + noise).coerceIn(0.0, 1.0)
                val b = (c.blue + noise).coerceIn(0.0, 1.0)

                writer.setColor(x, y, Color(r, g, b, c.opacity))
            }
        }
        return output
    }

    // Funciones Auxiliares Privadas
    // Promedio aritmético (No ponderado). Rápido pero inexacto.
    private fun calculateAverage(c: Color): Double {
        return (c.red + c.green + c.blue) / 3.0
    }

    // Cálculo de Luminancia (Luma) estándar. Pondera los canales según la sensibilidad del ojo humano.
    private fun calculateLuma(c: Color): Double {
        return (0.21 * c.red) + (0.72 * c.green) + (0.07 * c.blue)
    }
}