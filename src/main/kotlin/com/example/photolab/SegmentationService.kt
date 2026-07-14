package com.example.photolab

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

object SegmentationService {
    // MÉTODO 1: OTSU (Devuelve el valor del umbral)
    fun calculateOtsuThreshold(source: Image): Double {
        val mat = imageToMatGray(source)
        val dst = Mat()
        return Imgproc.threshold(
            mat, dst, 0.0, 255.0,
            Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU
        )
    }

    // MÉTODO 2: ADAPTATIVO (Local / Gaussiano)
    fun applyAdaptiveThreshold(source: Image, blockSize: Int, c: Double): Image {
        val mat = imageToMatGray(source)
        val dst = Mat()

        val oddBlockSize = if (blockSize % 2 == 0) blockSize + 1 else blockSize

        Imgproc.adaptiveThreshold(
            mat, dst, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            oddBlockSize,
            c
        )

        return matToImage(dst)
    }

    // Helpers Privados
    private fun imageToMatGray(image: Image): Mat {
        val bImage: BufferedImage = SwingFXUtils.fromFXImage(image, null)
        val bImageGray = BufferedImage(bImage.width, bImage.height, BufferedImage.TYPE_BYTE_GRAY)
        val g = bImageGray.createGraphics()
        g.drawImage(bImage, 0, 0, null)
        g.dispose()

        val data = (bImageGray.raster.dataBuffer as DataBufferByte).data
        val mat = Mat(bImageGray.height, bImageGray.width, CvType.CV_8UC1)
        mat.put(0, 0, data)
        return mat
    }

    private fun matToImage(mat: Mat): Image {
        val mat8u = Mat()
        mat.convertTo(mat8u, CvType.CV_8U)
        val data = ByteArray(mat8u.rows() * mat8u.cols() * mat8u.channels())
        mat8u.get(0, 0, data)

        val type = if (mat.channels() == 1) BufferedImage.TYPE_BYTE_GRAY else BufferedImage.TYPE_3BYTE_BGR
        val image = BufferedImage(mat.cols(), mat.rows(), type)
        image.raster.setDataElements(0, 0, mat.cols(), mat.rows(), data)
        return SwingFXUtils.toFXImage(image, null)
    }
}