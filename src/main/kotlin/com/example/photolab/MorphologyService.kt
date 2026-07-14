package com.example.photolab

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

object MorphologyService {

    enum class MorphOp { EROSION, DILATACION, APERTURA, CIERRE }
    enum class StructShape { RECTANGULO, CRUZ, ELIPSE, PERSONALIZADO }

    fun applyMorphology(
        source: Image,
        op: MorphOp,
        shape: StructShape,
        kernelSize: Int,
        customKernel: ByteArray? = null,
        customW: Int = 0,
        customH: Int = 0
    ): Image {
        val srcMat = imageToMat(source)
        val dstMat = Mat()

        // 1. Definir el Elemento Estructurante
        val element: Mat = if (shape == StructShape.PERSONALIZADO && customKernel != null) {
            // Crear matriz personalizada (CV_8U)
            val mat = Mat(customH, customW, CvType.CV_8U)
            mat.put(0, 0, customKernel)
            mat
        } else {
            // Crear forma estándar de OpenCV
            val cvShape = when (shape) {
                StructShape.RECTANGULO -> Imgproc.MORPH_RECT
                StructShape.CRUZ -> Imgproc.MORPH_CROSS
                StructShape.ELIPSE -> Imgproc.MORPH_ELLIPSE
                else -> Imgproc.MORPH_RECT
            }
            // El tamaño debe ser impar para tener centro
            val kSize = if (kernelSize % 2 == 0) kernelSize + 1 else kernelSize
            Imgproc.getStructuringElement(cvShape, Size(kSize.toDouble(), kSize.toDouble()))
        }

        // 2. Aplicar la operación morfológica
        when (op) {
            MorphOp.EROSION -> Imgproc.erode(srcMat, dstMat, element)
            MorphOp.DILATACION -> Imgproc.dilate(srcMat, dstMat, element)
            MorphOp.APERTURA -> Imgproc.morphologyEx(srcMat, dstMat, Imgproc.MORPH_OPEN, element)
            MorphOp.CIERRE -> Imgproc.morphologyEx(srcMat, dstMat, Imgproc.MORPH_CLOSE, element)
        }

        Imgproc.cvtColor(dstMat, dstMat, Imgproc.COLOR_BGR2RGB)
        return matToImage(dstMat)
    }

    // --- Helpers de conversión (Reutilizados para seguridad) ---
    private fun imageToMat(image: Image): Mat {
        val bImage: BufferedImage = SwingFXUtils.fromFXImage(image, null)
        val bImageConverted = BufferedImage(bImage.width, bImage.height, BufferedImage.TYPE_3BYTE_BGR)
        val g = bImageConverted.createGraphics(); g.drawImage(bImage, 0, 0, null); g.dispose()
        val data = (bImageConverted.raster.dataBuffer as DataBufferByte).data
        val mat = Mat(bImageConverted.height, bImageConverted.width, CvType.CV_8UC3)
        mat.put(0, 0, data)
        return mat
    }

    private fun matToImage(mat: Mat): Image {
        val mat8u = Mat(); mat.convertTo(mat8u, CvType.CV_8U)
        val data = ByteArray(mat8u.rows() * mat8u.cols() * mat8u.channels())
        mat8u.get(0, 0, data)
        val image = BufferedImage(mat8u.cols(), mat8u.rows(), BufferedImage.TYPE_3BYTE_BGR)
        image.raster.setDataElements(0, 0, mat8u.cols(), mat8u.rows(), data)
        return SwingFXUtils.toFXImage(image, null)
    }
}