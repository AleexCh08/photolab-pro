package com.example.photolab

import javafx.application.Application
import javafx.concurrent.Task
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import javafx.stage.StageStyle
import nu.pattern.OpenCV
import java.net.URL

class PDIApplication : Application() {
    override fun start(primaryStage: Stage) {
        val splashUrl = PDIApplication::class.java.getResource("splash-view.fxml")
        val fxmlLoader = FXMLLoader(splashUrl)
        val splashScene = Scene(fxmlLoader.load())

        val splashStage = Stage()
        splashStage.initStyle(StageStyle.UNDECORATED)
        splashStage.scene = splashScene

        val iconStream = PDIApplication::class.java.getResourceAsStream("icon.png")
        if (iconStream != null) {
            val icon = Image(iconStream)
            splashStage.icons.add(icon)
            primaryStage.icons.add(icon)
        }

        splashStage.show()

        val initTask = object : Task<Void?>() {
            override fun call(): Void? {
                OpenCV.loadLocally()
                return null
            }
        }

        initTask.setOnSucceeded {
            val mainUrl: URL? = PDIApplication::class.java.getResource("main-view.fxml")
            val mainRoot = FXMLLoader(mainUrl).load<Parent>()
            val mainScene = Scene(mainRoot, 1440.0, 900.0)

            primaryStage.title = "PhotoLab PDI Pro"
            primaryStage.scene = mainScene
            primaryStage.isMaximized = true

            splashStage.hide()
            primaryStage.show()
        }

        initTask.setOnFailed {
            it.source.exception.printStackTrace()
        }
        Thread(initTask).start()
    }
}

fun main(args: Array<String>) {
    Application.launch(PDIApplication::class.java, *args)
}

