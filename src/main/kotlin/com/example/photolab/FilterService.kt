package com.example.photolab

import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import kotlin.math.exp
import kotlin.math.sqrt

object FilterService {

    enum class EdgeFilterType { SOBEL, // Operador estándar. Suaviza ligeramente antes de derivar. Buen equilibrio ante ruido.
        PREWITT, // Operador de gradiente puro. No suaviza. Muy sensible al ruido.
        ROBERTS, // Operador de gradiente diagonal (2x2). El más simple y rápido, pero sensible al ruido.
        SCHARR // Variante de Sobel optimizada para simetría rotacional. Mejor precisión angular.
    }

    // Sección: FILTROS DE BORDE Y GRADIENTE
    // Aplica detección de bordes calculando la magnitud del gradiente.
    // Se convoluciona la imagen con dos máscaras: gradiente horizontal y gradiente vertical.
    // La magnitud del borde en cada píxel se calcula como la hipotenusa: G = sqrt{G_x^2 + G_y^2}
    fun applyEdgeDetection(source: Image, type: EdgeFilterType): Image {
        val w = source.width.toInt()
        val h = source.height.toInt()
        val output = WritableImage(w, h)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        // Definición de Kernels
        val (kernelX, kernelY) = when (type) {
            EdgeFilterType.SOBEL -> Pair(
                doubleArrayOf(-1.0, 0.0, 1.0, -2.0, 0.0, 2.0, -1.0, 0.0, 1.0),
                doubleArrayOf(-1.0, -2.0, -1.0, 0.0, 0.0, 0.0, 1.0, 2.0, 1.0)
            )
            EdgeFilterType.PREWITT -> Pair(
                doubleArrayOf(-1.0, 0.0, 1.0, -1.0, 0.0, 1.0, -1.0, 0.0, 1.0),
                doubleArrayOf(-1.0, -1.0, -1.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
            )
            EdgeFilterType.SCHARR -> Pair(
                doubleArrayOf(-3.0, 0.0, 3.0, -10.0, 0.0, 10.0, -3.0, 0.0, 3.0),
                doubleArrayOf(-3.0, -10.0, -3.0, 0.0, 0.0, 0.0, 3.0, 10.0, 3.0)
            )
            EdgeFilterType.ROBERTS -> Pair(
                doubleArrayOf(1.0, 0.0, 0.0, -1.0),
                doubleArrayOf(0.0, 1.0, -1.0, 0.0)
            )
        }

        val size = if (type == EdgeFilterType.ROBERTS) 2 else 3
        val offset = size / 2

        for (y in 0 until h - (size - 1)) {
            for (x in 0 until w - (size - 1)) {
                var gx = 0.0
                var gy = 0.0

                // Convolución simultánea X e Y
                for (ky in 0 until size) {
                    for (kx in 0 until size) {
                        val px = if (type == EdgeFilterType.ROBERTS) x + kx else (x + kx - offset).coerceIn(0, w - 1)
                        val py = if (type == EdgeFilterType.ROBERTS) y + ky else (y + ky - offset).coerceIn(0, h - 1)

                        val color = reader.getColor(px, py)
                        val gray = calculateLuma(color)
                        val kIndex = ky * size + kx
                        gx += gray * kernelX[kIndex]
                        gy += gray * kernelY[kIndex]
                    }
                }
                val magnitude = sqrt(gx * gx + gy * gy).coerceIn(0.0, 1.0)
                writer.setColor(x, y, Color(magnitude, magnitude, magnitude, 1.0))
            }
        }
        return output
    }

    // Sección: KERNELS Y CONVOLUCIÓN
    // Genera un Kernel de Perfilado (Sharpening).
    // Se resta el promedio de los vecinos al píxel central amplificado. La suma total del kernel se aproxima a 1 para mantener el brillo promedio.
    fun generateSharpenKernel(w: Int, h: Int): DoubleArray {
        val size = w * h
        val kernel = DoubleArray(size)

        val negativeVal = -1.0 / size

        for (i in kernel.indices) {
            kernel[i] = negativeVal
        }

        val cx = w / 2
        val cy = h / 2
        val centerIndex = cy * w + cx

        kernel[centerIndex] = 2.0 + negativeVal
        return kernel
    }

    // Aplica cualquier matriz cuadrada o rectangular a la imagen.
    // Manejo de bordes mediante extensión (repetir el último píxel válido).
    fun applyConvolution(source: Image, kernel: DoubleArray, kW: Int, kH: Int): Image {
        val w = source.width.toInt()
        val h = source.height.toInt()
        val output = WritableImage(w, h)
        val reader = source.pixelReader
        val writer = output.pixelWriter

        val cx = kW / 2
        val cy = kH / 2

        for (y in 0 until h) {
            for (x in 0 until w) {
                var rAcc = 0.0; var gAcc = 0.0; var bAcc = 0.0

                for (ky in 0 until kH) {
                    for (kx in 0 until kW) {
                        val px = (x + kx - cx).coerceIn(0, w - 1)
                        val py = (y + ky - cy).coerceIn(0, h - 1)

                        val color = reader.getColor(px, py)
                        val weight = kernel[ky * kW + kx]

                        rAcc += color.red * weight
                        gAcc += color.green * weight
                        bAcc += color.blue * weight
                    }
                }
                val finalR = rAcc.coerceIn(0.0, 1.0)
                val finalG = gAcc.coerceIn(0.0, 1.0)
                val finalB = bAcc.coerceIn(0.0, 1.0)

                writer.setColor(x, y, Color(finalR, finalG, finalB, reader.getColor(x, y).opacity))
            }
        }
        return output
    }

    // Filtro de Mediana. Para eliminar ruido (Sal y Pimienta) preservando los bordes.
    // Para cada píxel, recolectar los valores de sus vecinos y reemplazar el píxel central con el valor que ocupa la posición media.
    fun applyMedianFilter(source: Image, kW: Int, kH: Int): Image {
        val w = source.width.toInt()
        val h = source.height.toInt()
        val output = WritableImage(w, h)
        val reader = source.pixelReader
        val writer = output.pixelWriter
        val cx = kW / 2
        val cy = kH / 2

        val rList = DoubleArray(kW * kH)
        val gList = DoubleArray(kW * kH)
        val bList = DoubleArray(kW * kH)
        val medianIndex = (kW * kH) / 2

        for (y in 0 until h) {
            for (x in 0 until w) {
                var count = 0
                for (ky in 0 until kH) {
                    for (kx in 0 until kW) {
                        val px = (x + kx - cx).coerceIn(0, w - 1)
                        val py = (y + ky - cy).coerceIn(0, h - 1)
                        val c = reader.getColor(px, py)
                        rList[count] = c.red
                        gList[count] = c.green
                        bList[count] = c.blue
                        count++
                    }
                }

                rList.sort(); gList.sort(); bList.sort()
                writer.setColor(x, y, Color(rList[medianIndex], gList[medianIndex], bList[medianIndex], reader.getColor(x, y).opacity))
            }
        }
        return output
    }

    // Genera un Kernel de Promedio.
    fun generateAverageKernel(w: Int, h: Int): DoubleArray {
        val size = w * h
        val value = 1.0 / size
        return DoubleArray(size) { value }
    }

    // Genera un Kernel Laplaciano del Gaussiano (LoG).
    // Combina suavizado gaussiano (para reducir ruido) con el Laplaciano (segunda derivada) para detectar bordes.
    fun generateLoGKernel(w: Int, h: Int): DoubleArray {
        val kernel = DoubleArray(w * h)
        val cx = w / 2
        val cy = h / 2

        val sigma = (w / 2.0) / 3.0
        var sum = 0.0
        var sumPositives = 0.0

        for (y in 0 until h) {
            for (x in 0 until w) {
                val dx = (x - cx).toDouble()
                val dy = (y - cy).toDouble()
                val distSq = dx*dx + dy*dy

                val sigma2 = sigma * sigma
                val sigma4 = sigma2 * sigma2

                val term1 = -1.0 / (Math.PI * sigma4)
                val term2 = 1.0 - (distSq / (2 * sigma2))
                val term3 = exp(-distSq / (2 * sigma2))

                val value = term1 * term2 * term3
                kernel[y * w + x] = value
                sum += value

                if (value > 0) sumPositives += value
            }
        }

        val mean = sum / (w * h)
        for (i in kernel.indices) {
            kernel[i] -= mean
        }

        val factor = if (sumPositives > 0) 10.0 / sumPositives else 1.0

        for (i in kernel.indices) {
            kernel[i] *= factor
        }

        for (i in kernel.indices) {
            kernel[i] *= -1.0
        }
        return kernel
    }

    // Helper privado para esta sección específica
    private fun calculateLuma(c: Color): Double {
        return (0.21 * c.red) + (0.72 * c.green) + (0.07 * c.blue)
    }
}