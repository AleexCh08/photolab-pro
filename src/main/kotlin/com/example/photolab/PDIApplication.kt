package com.example.photolab

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import java.net.URL
import nu.pattern.OpenCV

class PDIApplication : Application() {
    override fun start(stage: Stage) {
        OpenCV.loadLocally()
        val fxmlUrl: URL? = PDIApplication::class.java.getResource("main-view.fxml")

        val fxmlLoader = FXMLLoader(fxmlUrl)
        val scene = Scene(fxmlLoader.load(), 1440.0, 900.0)
        val iconStream = PDIApplication::class.java.getResourceAsStream("icon.png")

        stage.icons.add(Image(iconStream))
        stage.title = "PhotoLab PDI Pro"
        stage.scene = scene
        stage.isMaximized = true
        stage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(PDIApplication::class.java, *args)
}

