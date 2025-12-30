package com.example.photolab

import javafx.scene.image.Image
import javafx.scene.image.PixelWriter
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import java.io.*

object NetPbmManager {
    // Carga una imagen NetPBM detectando automáticamente su formato.
    fun load(file: File): Image {
        FileInputStream(file).use { fis ->
            val bis = BufferedInputStream(fis)

            val magic = readToken(bis)
            val width = readToken(bis).toInt()
            val height = readToken(bis).toInt()

            val maxVal = if (magic == "P1" || magic == "P4") 1 else readToken(bis).toInt()

            val image = WritableImage(width, height)
            val writer = image.pixelWriter

            when (magic) {
                "P1" -> readASCII(bis, width, height, maxVal, writer, true) // Bitmap ASCII
                "P2" -> readASCII(bis, width, height, maxVal, writer, false) // Grises ASCII
                "P3" -> readASCIIColor(bis, width, height, maxVal, writer)   // RGB ASCII
                "P4" -> readBinaryBitmap(bis, width, height, writer)         // Bitmap Binario
                "P5" -> readBinaryGray(bis, width, height, writer)   // Grises Binario
                "P6" -> readBinaryRGB(bis, width, height, writer)    // RGB Binario
                else -> throw IOException("Formato no soportado: $magic")
            }
            return image
        }
    }

    // Guarda la imagen en formato NetPBM ASCII (P1, P2 o P3).
    // Se elige el formato basado en el análisis previo de la imagen (ImageAnalysis).
    fun saveAscii(file: File, image: Image, type: ImageAnalysis.ImageType) {
        val writer = PrintWriter(FileWriter(file))
        val w = image.width.toInt()
        val h = image.height.toInt()
        val reader = image.pixelReader

        when (type) {
            ImageAnalysis.ImageType.BINARY -> {
                writer.println("P1") // P1 = Bitmap ASCII
                writer.println("$w $h")
            }
            ImageAnalysis.ImageType.GRAYSCALE -> {
                writer.println("P2") // P2 = Grises ASCII
                writer.println("$w $h")
                writer.println("255")
            }
            ImageAnalysis.ImageType.COLOR -> {
                writer.println("P3") // P3 = Color ASCII
                writer.println("$w $h")
                writer.println("255")
            }
        }

        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = reader.getColor(x, y)
                when (type) {
                    ImageAnalysis.ImageType.BINARY -> {
                        val bit = if (c.brightness < 0.5) 1 else 0
                        writer.print("$bit ")
                    }
                    ImageAnalysis.ImageType.GRAYSCALE -> {
                        val gray = (c.red * 255).toInt()
                        writer.print("$gray ")
                    }
                    ImageAnalysis.ImageType.COLOR -> {
                        val r = (c.red * 255).toInt()
                        val g = (c.green * 255).toInt()
                        val b = (c.blue * 255).toInt()
                        writer.print("$r $g $b ")
                    }
                }
            }
            writer.println()
        }
        writer.close()
    }

    // Lee el siguiente "token" (palabra o número) del flujo de bytes. Ignora espacios en blanco y comentarios.
    private fun readToken(stream: InputStream): String {
        val sb = StringBuilder()
        while (true) {
            val b = stream.read()
            if (b == -1) break

            val c = b.toChar()
            if (c == '#') {
                while (true) {
                    val b2 = stream.read()
                    if (b2 == -1 || b2.toChar() == '\n') break
                }
            } else if (Character.isWhitespace(c)) {
                if (sb.isNotEmpty()) return sb.toString()
            } else {
                sb.append(c)
            }
        }
        if (sb.isNotEmpty()) return sb.toString()
        throw EOFException("Fin de archivo inesperado leyendo token")
    }

    // Lee formato P3 (Color ASCII).
    private fun readASCIIColor(bis: BufferedInputStream, w: Int, h: Int, max: Int, writer: PixelWriter) {
        for (y in 0 until h) {
            for (x in 0 until w) {
                val r = readToken(bis).toInt()
                val g = readToken(bis).toInt()
                val b = readToken(bis).toInt()
                writer.setColor(x, y, Color.rgb(scale(r, max), scale(g, max), scale(b, max)))
            }
        }
    }

    // Lee formatos P1 (Bitmap ASCII) y P2 (Grises ASCII).
    private fun readASCII(bis: BufferedInputStream, w: Int, h: Int, max: Int, writer: PixelWriter, isBitmap: Boolean) {
        for (y in 0 until h) {
            for (x in 0 until w) {
                val gray = readToken(bis).toInt()
                val valNorm = if (isBitmap) (1 - gray) * 255 else scale(gray, max) // P1: 1 es negro
                writer.setColor(x, y, Color.rgb(valNorm, valNorm, valNorm))
            }
        }
    }

    // Lee formato P6 (Color Binario). Lee 3 bytes crudos por píxel (R, G, B).
    private fun readBinaryRGB(bis: BufferedInputStream, w: Int, h: Int, writer: PixelWriter) {
        for (y in 0 until h) {
            for (x in 0 until w) {
                val r = bis.read()
                val g = bis.read()
                val b = bis.read()
                writer.setColor(x, y, Color.rgb(r, g, b))
            }
        }
    }

    // Lee formato P5 (Grises Binario). Lee 1 byte crudo por píxel.
    private fun readBinaryGray(bis: BufferedInputStream, w: Int, h: Int, writer: PixelWriter) {
        for (y in 0 until h) {
            for (x in 0 until w) {
                val gray = bis.read()
                writer.setColor(x, y, Color.rgb(gray, gray, gray))
            }
        }
    }

    // Lee formato P4 (Bitmap Binario).
    private fun readBinaryBitmap(bis: BufferedInputStream, w: Int, h: Int, writer: PixelWriter) {
        for (y in 0 until h) {
            var currentByte = 0
            var bitsRead = 0
            for (x in 0 until w) {
                if (bitsRead == 0) {
                    currentByte = bis.read()
                    bitsRead = 8
                }
                val bit = (currentByte shr 7) and 1
                currentByte = currentByte shl 1
                bitsRead--

                val col = if (bit == 1) Color.BLACK else Color.WHITE
                writer.setColor(x, y, col)
            }
        }
    }

    // Normaliza un valor entero al rango [0..255] requerido por Color.rgb.
    private fun scale(v: Int, max: Int): Int = (v.toDouble() / max * 255).toInt().coerceIn(0, 255)
}