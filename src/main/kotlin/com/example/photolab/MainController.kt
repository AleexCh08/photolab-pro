package com.example.photolab

import javafx.animation.PauseTransition
import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.chart.AreaChart
import javafx.scene.chart.LineChart
import javafx.scene.chart.XYChart
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import javafx.util.Duration
import javafx.scene.input.TransferMode
import javafx.scene.layout.StackPane
import java.io.File
import java.util.Stack
import javax.imageio.ImageIO
import kotlin.math.ln
import kotlin.system.exitProcess
import javafx.scene.shape.Rectangle
import kotlin.math.abs
import javafx.concurrent.Task
import javafx.scene.Cursor
import kotlin.math.pow
import javafx.scene.control.SpinnerValueFactory

class MainController {

    // ===============================
    // SECCIÓN: Enums y variables FXML
    // ===============================

    enum class ThresholdType(val label: String) {
        BINARY("Umbral Simple"),
        CUT_RANGE("Cortar Rango"),
        SELECT_RANGE("Selecciona Rango");

        override fun toString(): String = label
    }

    private enum class DragHandle { NONE, CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    // --- Elementos de UI Generales ---
    @FXML private lateinit var mainImageView: ImageView
    @FXML private lateinit var overlayCanvas: Canvas
    @FXML private lateinit var lblStatus: Label
    @FXML private lateinit var lblImageInfo: Label
    @FXML private lateinit var lblPixelInfo: Label
    @FXML private lateinit var rectPixelColor: Rectangle

    // --- Paneles y Contenedores ---
    @FXML private lateinit var btnCompare: Button
    @FXML private lateinit var imageStackPane: StackPane

    // --- Botones de Acción Global ---
    @FXML private lateinit var btnResetImage: Button
    @FXML private lateinit var btnUndoGlobal: Button
    @FXML private lateinit var menuUndo: MenuItem
    @FXML private lateinit var menuResetImage: MenuItem

    // --- Gráficos ---
    @FXML private lateinit var histogramChart: AreaChart<Number, Number>
    @FXML private lateinit var tonalCurveChart: LineChart<Number, Number>
    @FXML private lateinit var lineProfileChart: LineChart<Number, Number>
    @FXML private lateinit var comboHistogramChannel: ComboBox<String>

    // --- Controles de Brillo/Contraste ---
    @FXML private lateinit var sliderBrightness: Slider
    @FXML private lateinit var sliderContrast: Slider
    @FXML private lateinit var txtBrightness: TextField
    @FXML private lateinit var txtContrast: TextField
    @FXML private lateinit var sliderSaturation: Slider
    @FXML private lateinit var sliderGamma: Slider
    @FXML private lateinit var txtSaturation: TextField
    @FXML private lateinit var txtGamma: TextField

    // --- Controles de Zoom ---
    @FXML private lateinit var sliderZoom: Slider
    @FXML private lateinit var txtZoom: TextField
    @FXML private lateinit var comboInterpolation: ComboBox<TransformService.InterpolationMethod>
    @FXML private lateinit var rbScalePercent: RadioButton
    @FXML private lateinit var rbScalePixels: RadioButton
    @FXML private lateinit var boxScalePercent: HBox
    @FXML private lateinit var boxScalePixels: VBox
    @FXML private lateinit var txtResizeW: TextField
    @FXML private lateinit var txtResizeH: TextField
    @FXML private lateinit var chkKeepAspect: CheckBox

    // --- Controles de Umbralización ---
    @FXML private lateinit var comboThresholdType: ComboBox<ThresholdType>
    @FXML private lateinit var boxSingleThreshold: VBox
    @FXML private lateinit var boxRangeThreshold: VBox
    @FXML private lateinit var sliderThresholdSingle: Slider
    @FXML private lateinit var txtThresholdSingle: TextField
    @FXML private lateinit var sliderThresholdMin: Slider
    @FXML private lateinit var txtThresholdMin: TextField
    @FXML private lateinit var sliderThresholdMax: Slider
    @FXML private lateinit var txtThresholdMax: TextField
    @FXML private lateinit var chkApplyThreshold: CheckBox

    // --- Controles Umbral Adaptativo ---
    @FXML private lateinit var spinAdaptiveBlock: Spinner<Int>
    @FXML private lateinit var sliderAdaptiveC: Slider
    @FXML private lateinit var txtAdaptiveC: TextField

    // --- Variables de Recorte ---
    @FXML private lateinit var cropContainer: VBox
    @FXML private lateinit var rbCropDraw: RadioButton
    @FXML private lateinit var rbCropPan: RadioButton
    @FXML private lateinit var scrollPane: ScrollPane
    @FXML private lateinit var txtCropResW: TextField
    @FXML private lateinit var txtCropResH: TextField

    // --- Variables para el Historial
    @FXML private lateinit var historyPanel: VBox
    @FXML private lateinit var listHistory: ListView<HistoryState>
    @FXML private lateinit var btnRestoreHistory: Button

    // --- Variables de Estado ---
    private var baseImage: Image? = null
    private var originalFile: File? = null
    private val undoStack = Stack<Image>()
    private var currentProfileRow: Int = -1
    private var statusDelay: PauseTransition? = null
    private var isCropMode = false
    private var cropStartX = 0.0
    private var cropStartY = 0.0
    private var cropEndX = 0.0
    private var cropEndY = 0.0
    private var selectedCropRect: IntArray? = null
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var currentVisualRect: DoubleArray? = null
    private var currentHandle = DragHandle.NONE
    private val handleSize = 10.0

    // Variables para la barra lateral
    @FXML private lateinit var toolGroup: ToggleGroup
    @FXML private lateinit var btnToolPan: ToggleButton
    @FXML private lateinit var btnToolCrop: ToggleButton
    @FXML private lateinit var btnToolProfile: ToggleButton

    // Panel de TitledPanes
    @FXML private lateinit var tpAnalysis: TitledPane
    @FXML private lateinit var tpDevelop: TitledPane
    @FXML private lateinit var tpMorphology: TitledPane

    // Controles de Morfología
    @FXML private lateinit var comboMorphShape: ComboBox<MorphologyService.StructShape>
    @FXML private lateinit var spinMorphSize: Spinner<Int>

    // =======================
    // SECCIÓN: Inicialización
    // =======================

    @FXML
    fun initialize() {
        setupBrightnessContrastControls()
        setupThresholdControls()
        setupZoomControls()
        setupHistogramControls()
        setupDragAndDrop()
        setupImageListener()
        setupCompareControls()
        setupPixelInspector()
        setupCropHandlers()
        setupHistoryPanel()

        menuUndo.disableProperty().bind(btnUndoGlobal.disableProperty())
        menuResetImage.disableProperty().bind(btnResetImage.disableProperty())

        toolGroup.selectedToggleProperty().addListener { _, _, newToggle ->
            when (newToggle) {
                btnToolPan -> {
                    exitCropMode()
                    scrollPane.isPannable = true
                    mainImageView.cursor = Cursor.OPEN_HAND
                }
                btnToolCrop -> {
                    scrollPane.isPannable = false
                    onToggleCropMode()
                }
                btnToolProfile -> {
                    exitCropMode()
                    scrollPane.isPannable = false
                    mainImageView.cursor = Cursor.CROSSHAIR
                    tpAnalysis.isExpanded = true
                }
                null -> {
                    btnToolPan.isSelected = true
                }
            }
        }

        tpAnalysis.expandedProperty().addListener { _, _, isExpanded ->
            if (isExpanded && mainImageView.image != null) updateHistogram(mainImageView.image)
        }
        tpDevelop.expandedProperty().addListener { _, _, isExpanded ->
            if (isExpanded && mainImageView.image != null) updateTonalCurveChart(sliderBrightness.value, sliderContrast.value)
        }

        comboMorphShape.items.addAll(
            MorphologyService.StructShape.RECTANGULO,
            MorphologyService.StructShape.CRUZ,
            MorphologyService.StructShape.ELIPSE
        )
        comboMorphShape.value = MorphologyService.StructShape.RECTANGULO
        spinMorphSize.valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(3, 21, 3, 2)
        spinAdaptiveBlock.valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(3, 99, 11, 2)
    }

    private fun setupPixelInspector() {
        mainImageView.setOnMouseMoved { e ->
            updatePixelInfo(e)
        }
        mainImageView.setOnMouseExited {
            lblPixelInfo.text = ""
            rectPixelColor.isVisible = false
        }
    }

    private fun setupCompareControls() {
        btnCompare.isVisible = false
        btnCompare.setOnMousePressed {
            if (undoStack.isNotEmpty()) {
                mainImageView.image = undoStack.peek()
                lblStatus.text = "Visualizando estado anterior..."
                btnCompare.opacity = 1.0
            }
        }

        btnCompare.setOnMouseReleased {
            if (baseImage != null) {
                mainImageView.image = baseImage
                lblStatus.text = "Listo"
                btnCompare.opacity = 0.7
            }
        }
    }

    private fun setupImageListener() {
        mainImageView.imageProperty().addListener { _, _, _ ->
            clearOverlay()
            currentProfileRow = -1
        }
    }

    private fun setupDragAndDrop() {
        imageStackPane.setOnDragOver { event ->
            if (event.dragboard.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY, TransferMode.MOVE)
            }
            event.consume()
        }

        imageStackPane.setOnDragDropped { event ->
            val db = event.dragboard
            var success = false
            if (db.hasFiles()) {
                val file = db.files[0]
                val name = file.name.lowercase()
                if (name.endsWith(".png") || name.endsWith(".bmp") ||
                    name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                    name.endsWith(".ppm") || name.endsWith(".pgm") || name.endsWith(".pbm")) {

                    loadFile(file)
                    success = true
                }
            }
            event.isDropCompleted = success
            event.consume()
        }
    }

    private fun setupHistogramControls() {
        comboHistogramChannel.items.addAll("RGB (Compuesto)", "Rojo", "Verde", "Azul", "Todos")
        comboHistogramChannel.value = "RGB (Compuesto)"

        comboHistogramChannel.valueProperty().addListener { _, _, _ ->
            if (mainImageView.image != null && tpAnalysis.isExpanded) {
                updateHistogram(mainImageView.image)
            }
        }
    }

    private fun setupBrightnessContrastControls() {
        sliderBrightness.valueProperty().addListener { _, _, n ->
            onSliderChanged(updateCharts = false)
            if(!txtBrightness.isFocused) txtBrightness.text = "%.2f".format(n)
        }
        sliderContrast.valueProperty().addListener { _, _, n ->
            onSliderChanged(updateCharts = false)
            if(!txtContrast.isFocused) txtContrast.text = "%.2f".format(n)
        }
        val onRelease = javafx.event.EventHandler<MouseEvent> {
            onSliderChanged(updateCharts = true)
        }
        val commonListener = { _: Any, _: Any, _: Any ->
            onSliderChanged(updateCharts = false)
        }

        sliderSaturation.valueProperty().addListener(commonListener)
        sliderSaturation.valueProperty().addListener { _, _, n -> if(!txtSaturation.isFocused) txtSaturation.text = "%.2f".format(n) }

        sliderGamma.valueProperty().addListener(commonListener)
        sliderGamma.valueProperty().addListener { _, _, n -> if(!txtGamma.isFocused) txtGamma.text = "%.2f".format(n) }


        sliderBrightness.onMouseReleased = onRelease
        sliderContrast.onMouseReleased = onRelease
        sliderSaturation.onMouseReleased = onRelease
        sliderGamma.onMouseReleased = onRelease

        setupTextField(txtBrightness, sliderBrightness)
        setupTextField(txtContrast, sliderContrast)
        setupTextField(txtSaturation, sliderSaturation)
        setupTextField(txtGamma, sliderGamma)
    }

    private fun setupThresholdControls() {
        comboThresholdType.items.addAll(ThresholdType.entries.toTypedArray())
        comboThresholdType.value = ThresholdType.BINARY

        comboThresholdType.valueProperty().addListener { _, _, _ -> onThresholdTypeChanged() }
        chkApplyThreshold.selectedProperty().addListener { _, _, isSelected -> toggleThresholdControls(isSelected); applyThreshold() }

        val onRelease = javafx.event.EventHandler<MouseEvent> {
            applyThreshold(updateCharts = true)
        }

        sliderThresholdSingle.onMouseReleased = onRelease
        sliderThresholdMin.onMouseReleased = onRelease
        sliderThresholdMax.onMouseReleased = onRelease

        sliderThresholdSingle.valueProperty().addListener { _, _, n ->
            applyThreshold(updateCharts = false)
            if(!txtThresholdSingle.isFocused) txtThresholdSingle.text = "${n.toInt()}"
        }

        val gap = 10.0

        sliderThresholdMin.valueProperty().addListener { _, _, n ->
            val minVal = n.toDouble()
            if (sliderThresholdMax.value < minVal + gap) {
                sliderThresholdMax.value = (minVal + gap).coerceAtMost(255.0)
            }
            applyThreshold(updateCharts = false)
            if (!txtThresholdMin.isFocused) txtThresholdMin.text = "${minVal.toInt()}"
        }

        sliderThresholdMax.valueProperty().addListener { _, _, n ->
            val maxVal = n.toDouble()
            if (sliderThresholdMin.value > maxVal - gap) {
                sliderThresholdMin.value = (maxVal - gap).coerceAtLeast(0.0)
            }
            applyThreshold(updateCharts = false)
            if (!txtThresholdMax.isFocused) txtThresholdMax.text = "${maxVal.toInt()}"
        }

        setupTextFieldInt(txtThresholdSingle, sliderThresholdSingle)
        setupTextFieldInt(txtThresholdMin, sliderThresholdMin)
        setupTextFieldInt(txtThresholdMax, sliderThresholdMax)
        setupTextFieldDoubleNoDecimals(txtAdaptiveC, sliderAdaptiveC)
        sliderAdaptiveC.valueProperty().addListener { _, _, n -> if(!txtAdaptiveC.isFocused) txtAdaptiveC.text = "%.1f".format(n) }
    }

    private fun setupZoomControls() {
        comboInterpolation.items.addAll(TransformService.InterpolationMethod.entries.toTypedArray())
        comboInterpolation.value = TransformService.InterpolationMethod.NEAREST_NEIGHBOR

        sliderZoom.valueProperty().addListener { _, _, n ->
            if (!txtZoom.isFocused) {
                txtZoom.text = "%.0f".format(n)
            }
        }
        setupTextFieldDoubleNoDecimals(txtZoom, sliderZoom)

        val toggleGroup = ToggleGroup()
        rbScalePercent.toggleGroup = toggleGroup
        rbScalePixels.toggleGroup = toggleGroup

        toggleGroup.selectedToggleProperty().addListener { _, _, _ ->
            val isPixels = rbScalePixels.isSelected
            boxScalePercent.isVisible = !isPixels
            boxScalePercent.isManaged = !isPixels
            boxScalePixels.isVisible = isPixels
            boxScalePixels.isManaged = isPixels

            if (isPixels && baseImage != null) {
                txtResizeW.text = baseImage!!.width.toInt().toString()
                txtResizeH.text = baseImage!!.height.toInt().toString()
            }
        }

        txtResizeW.textProperty().addListener { _, _, newValue ->
            if (txtResizeW.isFocused && chkKeepAspect.isSelected && baseImage != null) {
                val w = newValue.toIntOrNull() ?: return@addListener
                val aspect = baseImage!!.height / baseImage!!.width
                txtResizeH.text = (w * aspect).toInt().toString()
            }
        }

        txtResizeH.textProperty().addListener { _, _, newValue ->
            if (txtResizeH.isFocused && chkKeepAspect.isSelected && baseImage != null) {
                val h = newValue.toIntOrNull() ?: return@addListener
                val aspect = baseImage!!.width / baseImage!!.height
                txtResizeW.text = (h * aspect).toInt().toString()
            }
        }
    }

    // ============================
    // SECCIÓN: Gestión de archivos
    // ============================

    @FXML
    fun onOpenClick() {
        val stage = mainImageView.scene.window
        val fileChooser = FileChooser()
        fileChooser.title = "Abrir Imagen"

        val initialDir = File(System.getProperty("user.home"), "Pictures")
        if (initialDir.exists() && initialDir.isDirectory) {
            fileChooser.initialDirectory = initialDir
        }

        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("Todos", "*.png", "*.bmp", "*.ppm", "*.pgm", "*.pbm", "*.jpg"),
            FileChooser.ExtensionFilter("PNG y BMP", "*.png", "*.bmp"),
            FileChooser.ExtensionFilter("NetPBM", "*.ppm", "*.pgm", "*.pbm")
        )
        val file = fileChooser.showOpenDialog(stage)
        if (file != null) loadFile(file)
    }

    private fun loadFile(file: File) {
        try {
            val name = file.name.lowercase()
            val image: Image = if (name.endsWith(".ppm") || name.endsWith(".pgm") || name.endsWith(".pbm")) {
                NetPbmManager.load(file)
            } else {
                java.io.FileInputStream(file).use { stream ->
                    Image(stream)
                }
            }

            if (!image.isError) {
                originalFile = file
                btnResetImage.isDisable = true

                undoStack.clear()
                btnCompare.isVisible = false
                btnUndoGlobal.isDisable = true

                sliderBrightness.value = 0.0; sliderContrast.value = 1.0
                txtBrightness.text = "0.00"; txtContrast.text = "1.00"

                chkApplyThreshold.isSelected = false
                sliderThresholdSingle.value = 128.0; txtThresholdSingle.text = "128"

                lineProfileChart.data.clear()
                clearOverlay()
                currentProfileRow = -1

                baseImage = image
                mainImageView.image = image
                listHistory.items.clear()

                updateStatus("Abierto: ${file.name}")
                updateImageInfo(image)
            } else {
                updateStatus("Error al leer imagen.")
            }
        } catch (e: Exception) {
            updateStatus("Error: ${e.message}")
        }
    }

    @FXML
    fun onSaveClick(): Boolean {
        if (mainImageView.image == null) {
            updateStatus("No hay ninguna imagen para guardar.")
            return false
        }

        val stage = mainImageView.scene.window
        val fileChooser = FileChooser()
        fileChooser.title = "Guardar Imagen"
        fileChooser.initialFileName = "imagen_editada"

        val initialDir = File(System.getProperty("user.home"), "Pictures")
        if (initialDir.exists() && initialDir.isDirectory) {
            fileChooser.initialDirectory = initialDir
        }

        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("Imagen PNG", "*.png"),
            FileChooser.ExtensionFilter("Imagen BMP", "*.bmp"),
            FileChooser.ExtensionFilter("NetPBM", "*.ppm", "*.pgm", "*.pbm")
        )
        val file = fileChooser.showSaveDialog(stage)

        if (file != null) {
            saveFile(file)
            return true
        }
        return false
    }

    private fun saveFile(file: File) {
        try {
            val image = mainImageView.image ?: return
            var finalFile = file
            val nameLower = file.name.lowercase()

            if (!nameLower.endsWith(".png") && !nameLower.endsWith(".bmp") &&
                !nameLower.endsWith(".ppm") && !nameLower.endsWith(".pgm") && !nameLower.endsWith(".pbm")) {

                finalFile = File(file.parent, "${file.name}.png")
            }

            val name = finalFile.name.lowercase()
            val type = ImageAnalysis.analyzeImageType(image)
            updateStatus("Guardando como ${type.name} en ${finalFile.name}")

            if (name.endsWith(".ppm") || name.endsWith(".pgm") || name.endsWith(".pbm")) {
                NetPbmManager.saveAscii(finalFile, image, type)
            } else {
                val bImageFull = SwingFXUtils.fromFXImage(image, null)

                val optimizedImage = when (type) {
                    ImageAnalysis.ImageType.BINARY -> {
                        val binImg = java.awt.image.BufferedImage(bImageFull.width, bImageFull.height, java.awt.image.BufferedImage.TYPE_BYTE_BINARY)
                        val g = binImg.createGraphics()
                        g.drawImage(bImageFull, 0, 0, null)
                        g.dispose()
                        binImg
                    }
                    ImageAnalysis.ImageType.GRAYSCALE -> {
                        val grayImg = java.awt.image.BufferedImage(bImageFull.width, bImageFull.height, java.awt.image.BufferedImage.TYPE_BYTE_GRAY)
                        val g = grayImg.createGraphics()
                        g.drawImage(bImageFull, 0, 0, null)
                        g.dispose()
                        grayImg
                    }
                    ImageAnalysis.ImageType.COLOR -> {
                        if (name.endsWith(".bmp")) {
                            val rgbImg = java.awt.image.BufferedImage(
                                bImageFull.width,
                                bImageFull.height,
                                java.awt.image.BufferedImage.TYPE_INT_RGB
                            )
                            val g = rgbImg.createGraphics()
                            g.drawImage(bImageFull, 0, 0, null)
                            g.dispose()
                            rgbImg
                        } else {
                            bImageFull
                        }
                    }
                }

                val format = if (name.endsWith(".bmp")) "bmp" else "png"
                val success = ImageIO.write(optimizedImage, format, finalFile)
                if (!success) {
                    throw java.io.IOException("No se pudo guardar la imagen. Posible error de formato o bloqueo.")
                }
            }
            updateStatus("Imagen guardada correctamente (${finalFile.name}).")
        } catch (e: Exception) {
            updateStatus("Error al guardar: ${e.message}")
            e.printStackTrace()
        }
    }

    @FXML
    fun onCloseImageClick() {
        mainImageView.image = null
        baseImage = null
        updateStatus("Imagen cerrada.")
        lblImageInfo.text = ""
        histogramChart.data.clear()
        tonalCurveChart.data.clear()

        lineProfileChart.data.clear()
        clearOverlay()
        currentProfileRow = -1

        sliderBrightness.value = 0.0
        sliderContrast.value = 1.0
        txtBrightness.text = "0.00"
        txtContrast.text = "1.00"

        txtResizeW.text = "0"
        txtResizeH.text = "0"
        sliderZoom.value = 100.0
        txtZoom.text = "100"

        listHistory.items.clear()
        undoStack.clear()
        btnCompare.isVisible = false
        btnUndoGlobal.isDisable = true
        btnResetImage.isDisable = true
    }

    @FXML
    fun onExitClick() {
        if (baseImage == null) {
            exitProcess(0)
        }

        val alert = Alert(Alert.AlertType.CONFIRMATION)
        alert.title = "Salir"
        alert.headerText = "Hay una imagen abierta"
        alert.dialogPane.style = "-fx-base: #383838; -fx-background-color: #383838; -fx-font-family: 'Segoe UI', sans-serif;"
        alert.contentText = "¿Desea guardar los cambios antes de salir?"

        val btnSave = ButtonType("Guardar y Salir")
        val btnExit = ButtonType("Salir sin Guardar")
        val btnCancel = ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE)

        alert.buttonTypes.setAll(btnSave, btnExit, btnCancel)
        val result = alert.showAndWait().orElse(btnCancel)

        when (result) {
            btnSave -> {
                if (onSaveClick()) exitProcess(0)
            }
            btnExit -> exitProcess(0)
            else -> return
        }
    }
    // =====================================
    // SECCIÓN: Transformaciones geométricas
    // =====================================

    @FXML
    fun onApplyZoomClick() {
        val currentBase = baseImage ?: return
        val method = comboInterpolation.value ?: return
        val newImage: Image
        val actionName: String

        if (rbScalePercent.isSelected) {
            val scalePercentage = sliderZoom.value
            val scaleFactor = scalePercentage / 100.0
            if (scaleFactor == 1.0) {
                updateStatus("Escala 100%, no se han aplicado cambios.")
                return
            }
            newImage = TransformService.scaleImage(currentBase, scaleFactor, method)
            actionName = "Escala ${scalePercentage.toInt()}%"
        } else {
            val newW = txtResizeW.text.toIntOrNull() ?: currentBase.width.toInt()
            val newH = txtResizeH.text.toIntOrNull() ?: currentBase.height.toInt()
            if (newW == currentBase.width.toInt() && newH == currentBase.height.toInt()) {
                updateStatus("Las dimensiones ingresadas son idénticas a las originales.")
                return
            }
            newImage = TransformService.resizeImage(currentBase, newW, newH, method)
            actionName = "Redimensión ${newW}x${newH}"
        }

        updateBaseAfterFilter(newImage, actionName)
        updateStatus("$actionName aplicado. Nueva resolución: ${newImage.width.toInt()}x${newImage.height.toInt()}")
    }

    @FXML fun onRotate180Click() = applyGeometricTransform("Rotación 180°") { TransformService.rotate180(it) }
    @FXML fun onRotate90RightClick() = applyGeometricTransform("Rotación 90° Derecha") { TransformService.rotate90Right(it) }
    @FXML fun onRotate90LeftClick() = applyGeometricTransform("Rotación 90° Izquierda") { TransformService.rotate90Left(it) }
    @FXML fun onFlipHorizontalClick() = applyGeometricTransform("Voltear Horizontal") { TransformService.flipHorizontal(it) }
    @FXML fun onFlipVerticalClick() = applyGeometricTransform("Voltear Vertical") { TransformService.flipVertical(it) }

    private fun applyGeometricTransform(name: String, transform: (Image) -> Image) {
        val currentBase = baseImage ?: run {
            updateStatus("No hay imagen cargada para rotar.")
            return
        }
        val newImage = transform(currentBase)
        updateBaseAfterFilter(newImage, name)
        updateStatus("$name aplicada. Dimensiones: ${newImage.width.toInt()}x${newImage.height.toInt()}")
    }

    // ===============================
    // SECCIÓN: Filtros de convolución
    // ===============================

    @FXML fun onFilterSobelClick() { applyEdgeFilter("Sobel", FilterService.EdgeFilterType.SOBEL) }
    @FXML fun onFilterPrewittClick() { applyEdgeFilter("Prewitt", FilterService.EdgeFilterType.PREWITT) }
    @FXML fun onFilterRobertsClick() { applyEdgeFilter("Roberts", FilterService.EdgeFilterType.ROBERTS) }
    @FXML fun onFilterScharrClick() { applyEdgeFilter("Scharr", FilterService.EdgeFilterType.SCHARR) }

    private fun applyEdgeFilter(name: String, type: FilterService.EdgeFilterType) {
        val currentBase = baseImage ?: run { updateStatus("No hay imagen cargada."); return }
        applyFilterInBackground(name) {
            FilterService.applyEdgeDetection(currentBase, type)
        }
    }

    @FXML
    fun onFilterSharpenClick() {
        showKernelConfigDialog("Perfilado (Sharpening)") { w, h ->
            applyFilterInBackground("Perfilado") {
                val kernel = FilterService.generateSharpenKernel(w, h)
                FilterService.applyConvolution(baseImage!!, kernel, w, h)
            }
        }
    }

    @FXML
    fun onFilterAverageClick() {
        showKernelConfigDialog("Filtro Promedio") { w, h ->
            applyFilterInBackground("Filtro Promedio") {
                val kernel = FilterService.generateAverageKernel(w, h)
                FilterService.applyConvolution(baseImage!!, kernel, w, h)
            }
        }
    }

    @FXML
    fun onFilterMedianClick() {
        showKernelConfigDialog("Filtro Mediana") { w, h ->
            applyFilterInBackground("Filtro Mediana") {
                FilterService.applyMedianFilter(baseImage!!, w, h)
            }
        }
    }

    @FXML
    fun onFilterLoGClick() {
        showKernelConfigDialog("Laplaciano del Gaussiano") { w, h ->
            applyFilterInBackground("Filtro LoG") {
                val kernel = FilterService.generateLoGKernel(w, h)
                FilterService.applyConvolution(baseImage!!, kernel, w, h)
            }
        }
    }

    private fun showKernelConfigDialog(title: String, onApply: (Int, Int) -> Unit) {
        if (baseImage == null) { updateStatus("No hay imagen cargada."); return }

        val dialog = Dialog<Pair<Int, Int>>()
        dialog.title = title
        dialog.headerText = "Configurar Tamaño del Kernel (Matriz)"
        dialog.dialogPane.style = "-fx-base: #383838; -fx-background-color: #383838; -fx-font-family: 'Segoe UI', sans-serif;"
        dialog.dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

        val spinW = Spinner<Int>(1, 7, 3)
        val spinH = Spinner<Int>(1, 7, 3)

        val grid = GridPane()
        grid.hgap = 10.0; grid.vgap = 10.0; grid.padding = Insets(20.0, 150.0, 10.0, 10.0)
        grid.add(Label("Filas (Alto):"), 0, 0); grid.add(spinH, 1, 0)
        grid.add(Label("Columnas (Ancho):"), 0, 1); grid.add(spinW, 1, 1)
        grid.add(Label("Mínimo: 2x1 o 1x2"), 1, 2)

        dialog.dialogPane.content = grid

        val okButton = dialog.dialogPane.lookupButton(ButtonType.OK)
        okButton.isDisable = false

        val validate = {
            val w = spinW.value
            val h = spinH.value
            val isValid = (w * h) >= 2
            okButton.isDisable = !isValid
        }

        spinW.valueProperty().addListener { _,_,_ -> validate() }
        spinH.valueProperty().addListener { _,_,_ -> validate() }
        dialog.setResultConverter { if (it == ButtonType.OK) Pair(spinW.value, spinH.value) else null }
        dialog.showAndWait().ifPresent { size ->
            onApply(size.first, size.second)
        }
    }

    // --- Filtro Personalizado ---
    private data class CustomFilterPreset(val name: String, val w: Int, val h: Int, val data: DoubleArray) {
        override fun toString(): String = name
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CustomFilterPreset

            if (w != other.w) return false
            if (h != other.h) return false
            if (name != other.name) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = w
            result = 31 * result + h
            result = 31 * result + name.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    @FXML
    fun onCustomFilterClick() {
        if (baseImage == null) { updateStatus("No hay imagen cargada."); return }

        val dialog = Dialog<Triple<Int, Int, DoubleArray>>()
        dialog.title = "Filtro de Convolución Personalizado"
        dialog.headerText = "Define tu propio Kernel"
        dialog.dialogPane.style = "-fx-base: #383838; -fx-background-color: #383838; -fx-font-family: 'Segoe UI', sans-serif;"
        dialog.dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

        val spinW = Spinner<Int>(1, 10, 3)
        val spinH = Spinner<Int>(1, 10, 3)

        val matrixGrid = GridPane()
        matrixGrid.hgap = 5.0
        matrixGrid.vgap = 5.0
        matrixGrid.alignment = Pos.CENTER

        val textFields = ArrayList<TextField>()

        val comboPresets = ComboBox<CustomFilterPreset>()
        val customItem = CustomFilterPreset("Personalizado...", 3, 3, DoubleArray(0))
        comboPresets.items.add(customItem)
        comboPresets.value = customItem

        comboPresets.items.addAll(
            CustomFilterPreset("Sobel 5x5 (Vertical)", 5, 5, doubleArrayOf(
                -1.0, -2.0, 0.0, 2.0, 1.0,
                -4.0, -8.0, 0.0, 8.0, 4.0,
                -6.0, -12.0, 0.0, 12.0, 6.0,
                -4.0, -8.0, 0.0, 8.0, 4.0,
                -1.0, -2.0, 0.0, 2.0, 1.0
            )),
            CustomFilterPreset("Sobel 5x5 (Horizontal)", 5, 5, doubleArrayOf(
                -1.0, -4.0, -6.0, -4.0, -1.0,
                -2.0, -8.0, -12.0, -8.0, -2.0,
                0.0,  0.0,   0.0,  0.0,  0.0,
                2.0,  8.0,  12.0,  8.0,  2.0,
                1.0,  4.0,   6.0,  4.0,  1.0
            )),
            CustomFilterPreset("Prewitt 5x5 (Vertical)", 5, 5, doubleArrayOf(
                -2.0, -1.0, 0.0, 1.0, 2.0,
                -2.0, -1.0, 0.0, 1.0, 2.0,
                -2.0, -1.0, 0.0, 1.0, 2.0,
                -2.0, -1.0, 0.0, 1.0, 2.0,
                -2.0, -1.0, 0.0, 1.0, 2.0
            )),
            CustomFilterPreset("Prewitt 5x5 (Horizontal)", 5, 5, doubleArrayOf(
                -2.0, -2.0, -2.0, -2.0, -2.0,
                -1.0, -1.0, -1.0, -1.0, -1.0,
                0.0,  0.0,  0.0,  0.0,  0.0,
                1.0,  1.0,  1.0,  1.0,  1.0,
                2.0,  2.0,  2.0,  2.0,  2.0
            )),
            CustomFilterPreset("Sobel 7x7 (Vertical)", 7, 7, doubleArrayOf(
                -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0,
                -4.0, -3.0, -2.0, 0.0, 2.0, 3.0, 4.0,
                -5.0, -4.0, -3.0, 0.0, 3.0, 4.0, 5.0,
                -6.0, -5.0, -4.0, 0.0, 4.0, 5.0, 6.0,
                -5.0, -4.0, -3.0, 0.0, 3.0, 4.0, 5.0,
                -4.0, -3.0, -2.0, 0.0, 2.0, 3.0, 4.0,
                -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0
            )),
            CustomFilterPreset("Sobel 7x7 (Horizontal)", 7, 7, doubleArrayOf(
                -3.0, -4.0, -5.0, -6.0, -5.0, -4.0, -3.0,
                -2.0, -3.0, -4.0, -5.0, -4.0, -3.0, -2.0,
                -1.0, -2.0, -3.0, -4.0, -3.0, -2.0, -1.0,
                0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,
                1.0,  2.0,  3.0,  4.0,  3.0,  2.0,  1.0,
                2.0,  3.0,  4.0,  5.0,  4.0,  3.0,  2.0,
                3.0,  4.0,  5.0,  6.0,  5.0,  4.0,  3.0
            )),
            CustomFilterPreset("Desenfoque Gaussiano 5x5", 5, 5, doubleArrayOf(
                1/256.0, 4/256.0, 6/256.0, 4/256.0, 1/256.0,
                4/256.0, 16/256.0, 24/256.0, 16/256.0, 4/256.0,
                6/256.0, 24/256.0, 36/256.0, 24/256.0, 6/256.0,
                4/256.0, 16/256.0, 24/256.0, 16/256.0, 4/256.0,
                1/256.0, 4/256.0, 6/256.0, 4/256.0, 1/256.0
            )),
            CustomFilterPreset("Laplaciano 5x5", 5, 5, doubleArrayOf(
                -1.0, -1.0, -1.0, -1.0, -1.0,
                -1.0, -1.0, -1.0, -1.0, -1.0,
                -1.0, -1.0, 24.0, -1.0, -1.0,
                -1.0, -1.0, -1.0, -1.0, -1.0,
                -1.0, -1.0, -1.0, -1.0, -1.0
            )),
            CustomFilterPreset("Unsharp Mask 5x5 (Enfoque)", 5, 5, doubleArrayOf(
                -1/256.0, -4/256.0, -6/256.0, -4/256.0, -1/256.0,
                -4/256.0, -16/256.0, -24/256.0, -16/256.0, -4/256.0,
                -6/256.0, -24/256.0, 476/256.0, -24/256.0, -6/256.0,
                -4/256.0, -16/256.0, -24/256.0, -16/256.0, -4/256.0,
                -1/256.0, -4/256.0, -6/256.0, -4/256.0, -1/256.0
            )),
            CustomFilterPreset("Efecto 3D 3x3", 3, 3, doubleArrayOf(
                -2.0, -1.0, 0.0,
                -1.0,  1.0, 1.0,
                0.0,  1.0, 2.0
            )),
            CustomFilterPreset("Cómic 3x3", 3, 3, doubleArrayOf(
                -4.0, -4.0, -4.0,
                -4.0, 33.0, -4.0,
                -4.0, -4.0, -4.0
            ))
        )

        fun updateMatrixGrid(w: Int, h: Int, values: DoubleArray?) {
            matrixGrid.children.clear()
            textFields.clear()

            for (y in 0 until h) {
                for (x in 0 until w) {
                    val tf = TextField()
                    tf.prefWidth = 50.0
                    tf.alignment = Pos.CENTER

                    val idx = y * w + x
                    val value = if (values != null && idx < values.size) values[idx] else 0.0
                    if (value % 1.0 == 0.0) tf.text = "%.0f".format(value)
                    else tf.text = "%.3f".format(value)
                    matrixGrid.add(tf, x, y)
                    textFields.add(tf)
                }
            }
        }

        comboPresets.setOnAction {
            val p = comboPresets.value
            if (p != null) {
                spinW.valueFactory.value = p.w
                spinH.valueFactory.value = p.h
                updateMatrixGrid(p.w, p.h, p.data)
            }
        }

        spinW.valueProperty().addListener { _,_,newW -> updateMatrixGrid(newW, spinH.value, null) }
        spinH.valueProperty().addListener { _,_,newH -> updateMatrixGrid(spinW.value, newH, null) }

        updateMatrixGrid(3, 3, null)

        val rootBox = VBox(15.0)
        rootBox.padding = Insets(20.0)

        val sizeBox = HBox(10.0, Label("Ancho:"), spinW, Label("Alto:"), spinH)
        sizeBox.alignment = Pos.CENTER_LEFT

        val presetBox = HBox(10.0, Label("Predefinidos:"), comboPresets)
        presetBox.alignment = Pos.CENTER_LEFT

        val scrollPane = ScrollPane(matrixGrid)
        scrollPane.prefHeight = 200.0
        scrollPane.prefWidth = 400.0
        scrollPane.isFitToWidth = true
        scrollPane.style = "-fx-background-color:transparent;"

        rootBox.children.addAll(presetBox, Separator(), sizeBox, Label("Matriz de Coeficientes:"), scrollPane)
        dialog.dialogPane.content = rootBox

        val btnOk = dialog.dialogPane.lookupButton(ButtonType.OK)
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION) { event ->
            var hasError = false
            for (tf in textFields) {
                try {
                    tf.text.replace(",", ".").toDouble()
                    tf.style = ""
                } catch (_: NumberFormatException) {
                    hasError = true
                    tf.style = "-fx-border-color: red; -fx-border-width: 1px;"
                }
            }

            if (hasError) {
                event.consume()
                val alert = Alert(Alert.AlertType.ERROR)
                alert.title = "Error de Formato"
                alert.headerText = "Valores inválidos en la matriz"
                dialog.dialogPane.style = "-fx-base: #383838; -fx-background-color: #383838; -fx-font-family: 'Segoe UI', sans-serif;"
                alert.contentText = "Por favor, revisa las celdas marcadas en rojo. Deben ser números válidos."
                alert.showAndWait()
            }
        }

        dialog.setResultConverter { btn ->
            if (btn == ButtonType.OK) {
                val w = spinW.value
                val h = spinH.value
                val data = DoubleArray(w * h)
                var sum = 0.0

                for (i in textFields.indices) {
                    val value = textFields[i].text.replace(",", ".").toDouble()
                    data[i] = value
                    sum += value
                }
                if (abs(sum) > 1.0) {
                    for (i in data.indices) {
                        data[i] /= sum
                    }
                }
                Triple(w, h, data)
            } else null
        }

        dialog.showAndWait().ifPresent { result ->
            val (w, h, kernel) = result
            updateStatus("Aplicando filtro personalizado ${w}x${h}")

            applyFilterInBackground("Filtro Personalizado") {
                FilterService.applyConvolution(baseImage!!, kernel, w, h)
            }
        }
    }

    // =============================
    // SECCIÓN: Operaciones de píxel
    // =============================

    private fun onSliderChanged(updateCharts: Boolean) {
        if (baseImage == null) return

        val b = sliderBrightness.value
        val c = sliderContrast.value
        val s = sliderSaturation.value
        val g = sliderGamma.value

        val newImage = PixelOperations.applyAdjustments(baseImage!!, s, g, b, c)
        mainImageView.image = newImage

        updateImageInfo(newImage, updateCharts)

        if (updateCharts && tpDevelop.isExpanded) {
            updateTonalCurveChart(b, c, g)
        }
    }

    @FXML
    fun onApplyBrightnessContrast() {
        val currentBase = baseImage ?: return
        val b = sliderBrightness.value
        val c = sliderContrast.value
        val s = sliderSaturation.value
        val g = sliderGamma.value

        val newImage = PixelOperations.applyAdjustments(currentBase, s, g, b, c)
        updateBaseAfterFilter(newImage, "Revelado (Color/Luz)")
        updateStatus("Ajustes de revelado aplicados.")
    }

    private fun onThresholdTypeChanged() {
        val type = comboThresholdType.value ?: return
        boxSingleThreshold.isVisible = (type == ThresholdType.BINARY)
        boxRangeThreshold.isVisible = (type != ThresholdType.BINARY)
        if (baseImage != null) applyThreshold()
    }

    @FXML
    fun onCalculateOtsuClick() {
        if (baseImage == null) return
        val otsuVal = SegmentationService.calculateOtsuThreshold(baseImage!!)
        sliderThresholdSingle.value = otsuVal
        updateStatus("Otsu calculado: ${otsuVal.toInt()}")
    }

    @FXML
    fun onApplyAdaptiveClick() {
        if (baseImage == null) return
        val blockSize = spinAdaptiveBlock.value
        val cVal = sliderAdaptiveC.value
        applyFilterInBackground("Umbral Adaptativo (${blockSize}x${blockSize}, C=$cVal)") {
            SegmentationService.applyAdaptiveThreshold(baseImage!!, blockSize, cVal)
        }
    }

    private fun applyThreshold(updateCharts: Boolean = true) {
        val currentBase = baseImage ?: return

        if (!chkApplyThreshold.isSelected) {
            mainImageView.image = currentBase
            updateImageInfo(currentBase, updateCharts = true)
            updateStatus("Umbralización desactivada (Vista original).")
            return
        }

        val type = comboThresholdType.value ?: return
        val newImage = when (type) {
            ThresholdType.BINARY -> {
                val t = sliderThresholdSingle.value / 255.0
                PixelOperations.thresholdBinary(currentBase, t)
            }
            ThresholdType.CUT_RANGE -> {
                val min = sliderThresholdMin.value / 255.0
                val max = sliderThresholdMax.value / 255.0
                PixelOperations.thresholdCutRange(currentBase, min, max)
            }
            ThresholdType.SELECT_RANGE -> {
                val min = sliderThresholdMin.value / 255.0
                val max = sliderThresholdMax.value / 255.0
                PixelOperations.thresholdSelectRange(currentBase, min, max)
            }
        }

        mainImageView.image = newImage
        updateImageInfo(newImage, updateCharts = updateCharts)
        updateStatus("Umbralización aplicada.")
        btnResetImage.isDisable = false
    }

    @FXML fun onGrayscaleAvgClick() = applyGenericFilter("Escala Grises (Promedio)") { img, _ -> PixelOperations.toGrayscale(img, PixelOperations.GrayscaleMethod.AVERAGE) }
    @FXML fun onGrayscaleLumaClick() = applyGenericFilter("Escala Grises (Luma)") { img, _ -> PixelOperations.toGrayscale(img, PixelOperations.GrayscaleMethod.LUMA) }
    @FXML fun onColorTintClick() = pickColorAndApply("Escala Monocromática") { img, color -> PixelOperations.toColorScale(img, color) }
    @FXML fun onColorMultiplyClick() = pickColorAndApply("Filtro Multiplicativo") { img, color -> PixelOperations.applyColorFilter(img, color) }
    @FXML fun onColorAddClick() = pickColorAndApply("Filtro Aditivo") { img, color -> PixelOperations.applyColorAdd(img, color) }
    @FXML fun onNegativeClick() = applyGenericFilter("Negativo") { img, _ -> PixelOperations.applyNegative(img) }

    @FXML
    fun onQuantizeBitsClick() {
        showIntParameterDialog("Reducción de Bits", "Configurar profundidad de color", "Bits por canal (1-8):", 8, 4) { value ->
            applyFilterInBackground("Reducción de Bits ($value bits)") {
                QuantizationService.reduceBitDepth(baseImage!!, value)
            }
        }
    }

    @FXML
    fun onQuantizePopularityClick() {
        showIntParameterDialog("Algoritmo de Popularidad", "Configurar tamaño de paleta", "Colores (2-256):", 256, 64) { value ->
            applyFilterInBackground("Popularidad ($value colores)") {
                QuantizationService.applyPopularityQuantization(baseImage!!, value)
            }
        }
    }

    @FXML
    fun onQuantizeKMeansClick() {
        showIntParameterDialog("K-Means Clustering", "Configurar número de clústeres", "K (2-64):", 64, 16) { value ->
            applyFilterInBackground("K-Means (K=$value)") {
                QuantizationService.applyKMeansQuantization(baseImage!!, value)
            }
        }
    }

    @FXML
    fun onNoiseSaltPepperClick() {
        showParameterDialog("Ruido Sal y Pimienta", "Configurar densidad", "Porcentaje de ruido:", 0.2, 0.05) { value ->
            applyFilterInBackground("Ruido Sal y Pimienta") {
                PixelOperations.generateSaltPepperNoise(baseImage!!, value)
            }
        }
    }

    @FXML
    fun onNoiseGaussianClick() {
        showParameterDialog("Ruido Gaussiano", "Configurar desviación estándar (Sigma)", "Intensidad (Sigma):", 0.5, 0.1) { value ->
            applyFilterInBackground("Ruido Gaussiano") {
                PixelOperations.generateGaussianNoise(baseImage!!, value)
            }
        }
    }

    @FXML fun onMorphErosionClick() { tpMorphology.isExpanded = true; executeMorphology("Erosión", MorphologyService.MorphOp.EROSION) }
    @FXML fun onMorphDilationClick() { tpMorphology.isExpanded = true; executeMorphology("Dilatación", MorphologyService.MorphOp.DILATACION) }
    @FXML fun onMorphOpeningClick() { tpMorphology.isExpanded = true; executeMorphology("Apertura", MorphologyService.MorphOp.APERTURA) }
    @FXML fun onMorphClosingClick() { tpMorphology.isExpanded = true; executeMorphology("Cierre", MorphologyService.MorphOp.CIERRE) }

    private fun executeMorphology(title: String, op: MorphologyService.MorphOp) {
        if (baseImage == null) { updateStatus("No hay imagen cargada."); return }

        val size = spinMorphSize.value
        val shape = comboMorphShape.value

        applyFilterInBackground("$title (${size}x${size}, $shape)") {
            MorphologyService.applyMorphology(baseImage!!, op, shape, size)
        }
    }

    // ==========================
    // SECCIÓN: Gestión de estado
    // ==========================

    @FXML
    fun onResetImageClick() {
        if (originalFile != null) {
            loadFile(originalFile!!)
            updateStatus("Imagen restaurada.")
        }
    }

    @FXML
    fun onResetBrightnessContrast() {
        sliderBrightness.value = 0.0
        sliderContrast.value = 1.0
        sliderSaturation.value = 1.0
        sliderGamma.value = 1.0

        txtBrightness.text = "0.00"
        txtContrast.text = "1.00"
        txtSaturation.text = "1.00"
        txtGamma.text = "1.00"

        if (mainImageView.image != null) {
            updateTonalCurveChart(0.0, 1.0, 1.0)
            mainImageView.image = baseImage
            updateImageInfo(baseImage!!, updateCharts = true)
        }
        updateStatus("Valores de revelado reiniciados.")
    }

    @FXML
    fun onUndoClick() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.pop()
            if (listHistory.items.isNotEmpty()) {
                listHistory.items.removeAt(0)
            }

            updateBaseAfterFilter(previous, "Deshacer", saveHistory = false)

            if (undoStack.isEmpty()) {
                btnUndoGlobal.isDisable = true
                btnResetImage.isDisable = true
            }
            updateStatus("Cambio deshecho.")
        }
    }

    private fun updateBaseAfterFilter(newImage: Image, actionName: String = "Filtro/Ajuste", saveHistory: Boolean = true) {
        if (saveHistory && baseImage != null) {
            undoStack.push(baseImage)
            btnUndoGlobal.isDisable = false
        }

        if (saveHistory) {
            val thumb = TransformService.scaleImage(newImage, 100.0 / newImage.width, TransformService.InterpolationMethod.NEAREST_NEIGHBOR)
            val state = HistoryState(thumb, newImage, actionName)
            listHistory.items.add(0, state)
        }

        baseImage = newImage

        sliderBrightness.value = 0.0
        sliderContrast.value = 1.0
        txtBrightness.text = "0.00"
        txtContrast.text = "1.00"
        sliderThresholdSingle.value = 128.0; txtThresholdSingle.text = "128"
        sliderThresholdMin.value = 64.0; txtThresholdMin.text = "64"
        sliderThresholdMax.value = 192.0; txtThresholdMax.text = "192"
        sliderSaturation.value = 1.0
        sliderGamma.value = 1.0
        txtSaturation.text = "1.00"
        txtGamma.text = "1.00"

        chkApplyThreshold.isSelected = false

        mainImageView.image = newImage
        updateImageInfo(newImage, updateCharts = true)

        btnResetImage.isDisable = false
        btnCompare.isVisible = undoStack.isNotEmpty()
    }


    // =========================================
    // SECCIÓN: Análisis, gráficas e información
    // =========================================

    private fun updateImageInfo(image: Image, updateCharts: Boolean = true) {
        val w = image.width.toInt()
        val h = image.height.toInt()
        if (updateCharts) {
            val uniqueColors = ImageAnalysis.countUniqueColors(image)
            val type = ImageAnalysis.analyzeImageType(image)
            val bpp = when (type) {
                ImageAnalysis.ImageType.BINARY -> "1 bit"
                ImageAnalysis.ImageType.GRAYSCALE -> "8 bits"
                ImageAnalysis.ImageType.COLOR -> "32 bits"
            }
            lblImageInfo.text = "$w x $h px  |  $bpp  |  $uniqueColors colores"

            if (tpAnalysis.isExpanded) updateHistogram(image)
            if (tpDevelop.isExpanded) updateTonalCurveChart(sliderBrightness.value, sliderContrast.value)
        }
    }

    private fun updateHistogram(image: Image) {
        histogramChart.data.clear()
        val hist = ImageAnalysis.calculateHistogram(image)
        val mode = comboHistogramChannel.value ?: "RGB (Compuesto)"

        val seriesRed = XYChart.Series<Number, Number>().apply { name = "Rojo" }
        val seriesGreen = XYChart.Series<Number, Number>().apply { name = "Verde" }
        val seriesBlue = XYChart.Series<Number, Number>().apply { name = "Azul" }
        val seriesGray = XYChart.Series<Number, Number>().apply { name = "Gris" }

        for (i in 0 until 256) {
            val vR = if(hist.red[i]>0) ln(hist.red[i].toDouble()+1) else 0.0
            val vG = if(hist.green[i]>0) ln(hist.green[i].toDouble()+1) else 0.0
            val vB = if(hist.blue[i]>0) ln(hist.blue[i].toDouble()+1) else 0.0
            val vGray = if(hist.gray[i]>0) ln(hist.gray[i].toDouble()+1) else 0.0

            seriesRed.data.add(XYChart.Data(i, vR))
            seriesGreen.data.add(XYChart.Data(i, vG))
            seriesBlue.data.add(XYChart.Data(i, vB))
            seriesGray.data.add(XYChart.Data(i, vGray))
        }

        when (mode) {
            "RGB (Compuesto)" -> {
                histogramChart.data.add(seriesGray)
                seriesGray.node?.lookup(".chart-series-area-line")?.style = "-fx-stroke: #dddddd; -fx-stroke-width: 1px;"
                seriesGray.node?.lookup(".chart-series-area-fill")?.style = "-fx-fill: rgba(150, 150, 150, 0.4);"
            }
            "Rojo" -> {
                histogramChart.data.add(seriesRed)
                seriesRed.node?.lookup(".chart-series-area-line")?.style = "-fx-stroke: rgba(255, 50, 50, 1.0); -fx-stroke-width: 1px;"
                seriesRed.node?.lookup(".chart-series-area-fill")?.style = "-fx-fill: rgba(255, 50, 50, 0.3);"
            }
            "Verde" -> {
                histogramChart.data.add(seriesGreen)
                seriesGreen.node?.lookup(".chart-series-area-line")?.style = "-fx-stroke: rgba(50, 255, 50, 1.0); -fx-stroke-width: 1px;"
                seriesGreen.node?.lookup(".chart-series-area-fill")?.style = "-fx-fill: rgba(50, 255, 50, 0.3);"
            }
            "Azul" -> {
                histogramChart.data.add(seriesBlue)
                seriesBlue.node?.lookup(".chart-series-area-line")?.style = "-fx-stroke: rgba(50, 50, 255, 1.0); -fx-stroke-width: 1px;"
                seriesBlue.node?.lookup(".chart-series-area-fill")?.style = "-fx-fill: rgba(50, 50, 255, 0.3);"
            }
            "Todos" -> {
                histogramChart.data.addAll(seriesRed, seriesGreen, seriesBlue)
                seriesRed.node?.lookup(".chart-series-area-line")?.style = "-fx-stroke: rgba(255, 0, 0, 0.8); -fx-stroke-width: 1px;"
                seriesRed.node?.lookup(".chart-series-area-fill")?.style = "-fx-fill: rgba(255, 0, 0, 0.1);"
                seriesGreen.node?.lookup(".chart-series-area-line")?.style = "-fx-stroke: rgba(0, 255, 0, 0.8); -fx-stroke-width: 1px;"
                seriesGreen.node?.lookup(".chart-series-area-fill")?.style = "-fx-fill: rgba(0, 255, 0, 0.1);"
                seriesBlue.node?.lookup(".chart-series-area-line")?.style = "-fx-stroke: rgba(0, 0, 255, 0.8); -fx-stroke-width: 1px;"
                seriesBlue.node?.lookup(".chart-series-area-fill")?.style = "-fx-fill: rgba(0, 0, 255, 0.1);"
            }
        }
    }

    private fun updateTonalCurveChart(brightness: Double, contrast: Double, gamma: Double = 1.0) {
        tonalCurveChart.data.clear()
        val series = XYChart.Series<Number, Number>().apply { name = "Transferencia" }

        for (x in 0..255 step 8) {
            val inputNormalized = x / 255.0

            val afterGamma = inputNormalized.pow(1.0 / gamma)

            val outputNormalized = contrast * (afterGamma - 0.5) + 0.5 + brightness
            val y = (outputNormalized.coerceIn(0.0, 1.0) * 255)
            series.data.add(XYChart.Data(x, y))
        }

        tonalCurveChart.data.add(series)
        series.node?.lookup(".chart-series-line")?.style = "-fx-stroke: #510885; -fx-stroke-width: 1.5px;"
    }

    private fun updateLineProfileChart(rowY: Int) {
        val image = mainImageView.image ?: return
        val w = image.width.toInt()
        val reader = image.pixelReader

        lineProfileChart.data.clear()

        val series = XYChart.Series<Number, Number>()
        series.name = "Intensidad"

        for (x in 0 until w) {
            val c = reader.getColor(x, rowY)
            val intensity = (0.21 * c.red + 0.72 * c.green + 0.07 * c.blue) * 255.0
            series.data.add(XYChart.Data(x, intensity))
        }

        lineProfileChart.data.add(series)
        series.node?.lookup(".chart-series-line")?.style = "-fx-stroke: #510885; -fx-stroke-width: 1px;"
    }

    // =================================
    // SECCIÓN: Interacción con el mouse
    // =================================

    @FXML
    fun onMainImageClick(event: MouseEvent) {
        if (!btnToolProfile.isSelected || mainImageView.image == null) return

        val image = mainImageView.image
        val bounds = mainImageView.boundsInLocal

        val clickX = event.x
        val clickY = event.y

        val scaleX = image.width / bounds.width
        val scaleY = image.height / bounds.height

        val pixelX = (clickX * scaleX).toInt()
        val pixelY = (clickY * scaleY).toInt()

        if (pixelX in 0 until image.width.toInt() && pixelY in 0 until image.height.toInt()) {
            currentProfileRow = pixelY
            drawProfileLine(pixelY, clickY)
            updateLineProfileChart(pixelY)
        }
    }

    private fun drawProfileLine(rowY: Int, preciseY: Double? = null) {
        val gc: GraphicsContext = overlayCanvas.graphicsContext2D
        val image = mainImageView.image ?: return
        val bounds = mainImageView.layoutBounds

        gc.clearRect(0.0, 0.0, overlayCanvas.width, overlayCanvas.height)

        overlayCanvas.width = bounds.width
        overlayCanvas.height = bounds.height

        val scaleY = bounds.height / image.height
        val visualY = preciseY ?: (rowY * scaleY + (scaleY / 2))

        gc.stroke = Color.RED
        gc.lineWidth = 1.5
        gc.strokeLine(0.0, visualY, bounds.width, visualY)
    }

    private fun clearOverlay() {
        val gc = overlayCanvas.graphicsContext2D
        gc.clearRect(0.0, 0.0, overlayCanvas.width, overlayCanvas.height)
    }

    @FXML
    fun onClearLineProfileClick() {
        clearOverlay()
        lineProfileChart.data.clear()
        currentProfileRow = -1
        updateStatus("Selección de perfil limpiada.")
    }

    // Configura los eventos de arrastre para dibujar el rectángulo de selección
    private fun setupCropHandlers() {
        val group = ToggleGroup()
        rbCropDraw.toggleGroup = group
        rbCropPan.toggleGroup = group

        group.selectedToggleProperty().addListener { _, _, _ ->
            if (rbCropPan.isSelected) {
                scrollPane.isPannable = true
                mainImageView.cursor = Cursor.OPEN_HAND
                currentHandle = DragHandle.NONE
            } else {
                scrollPane.isPannable = false
                mainImageView.cursor = Cursor.DEFAULT
            }
        }

        mainImageView.setOnMouseMoved { e ->
            updatePixelInfo(e)

            if (!isCropMode || rbCropPan.isSelected || currentVisualRect == null) {
                if (!isCropMode) mainImageView.cursor = Cursor.DEFAULT
                return@setOnMouseMoved
            }

            val handle = getHandleForPoint(e.x, e.y, currentVisualRect!!)
            mainImageView.cursor = when (handle) {
                DragHandle.CENTER -> Cursor.MOVE
                DragHandle.TOP_LEFT -> Cursor.NW_RESIZE
                DragHandle.TOP_RIGHT -> Cursor.NE_RESIZE
                DragHandle.BOTTOM_LEFT -> Cursor.SW_RESIZE
                DragHandle.BOTTOM_RIGHT -> Cursor.SE_RESIZE
                else -> Cursor.CROSSHAIR
            }
        }

        mainImageView.setOnMousePressed { e ->
            listHistory.selectionModel.clearSelection()
            if (!isCropMode || rbCropPan.isSelected || mainImageView.image == null) return@setOnMousePressed

            val rect = currentVisualRect
            currentHandle = if (rect != null) {
                getHandleForPoint(e.x, e.y, rect)
            } else {
                DragHandle.NONE
            }

            if (currentHandle == DragHandle.NONE) {
                cropStartX = e.x.coerceIn(0.0, mainImageView.layoutBounds.width)
                cropStartY = e.y.coerceIn(0.0, mainImageView.layoutBounds.height)
                selectedCropRect = null
                currentVisualRect = null
                txtCropResW.text = "0"; txtCropResH.text = "0"
            } else {
                lastMouseX = e.x
                lastMouseY = e.y
            }
        }

        mainImageView.setOnMouseDragged { e ->
            if (!isCropMode || rbCropPan.isSelected || mainImageView.image == null) return@setOnMouseDragged

            val bounds = mainImageView.layoutBounds
            val mouseX = e.x.coerceIn(0.0, bounds.width)
            val mouseY = e.y.coerceIn(0.0, bounds.height)

            if (currentHandle == DragHandle.NONE) {
                cropEndX = mouseX
                cropEndY = mouseY
                drawCropSelection(cropStartX, cropStartY, cropEndX, cropEndY)

            } else if (currentVisualRect != null) {
                val dx = mouseX - lastMouseX
                val dy = mouseY - lastMouseY

                var curX = currentVisualRect!![0]
                var curY = currentVisualRect!![1]
                var curW = currentVisualRect!![2]
                var curH = currentVisualRect!![3]

                when (currentHandle) {
                    DragHandle.CENTER -> {
                        curX += dx
                        curY += dy
                        curX = curX.coerceIn(0.0, bounds.width - curW)
                        curY = curY.coerceIn(0.0, bounds.height - curH)
                    }
                    DragHandle.TOP_LEFT -> {
                        val newX = (curX + dx).coerceIn(0.0, curX + curW - 10)
                        val newY = (curY + dy).coerceIn(0.0, curY + curH - 10)
                        val deltaW = curX - newX
                        val deltaH = curY - newY
                        curX = newX; curY = newY
                        curW += deltaW; curH += deltaH
                    }
                    DragHandle.TOP_RIGHT -> {
                        curY = (curY + dy).coerceIn(0.0, curY + curH - 10)
                        curH += (currentVisualRect!![1] - curY)
                        curW = (mouseX - curX).coerceAtLeast(10.0)
                    }
                    DragHandle.BOTTOM_LEFT -> {
                        curX = (curX + dx).coerceIn(0.0, curX + curW - 10)
                        curW += (currentVisualRect!![0] - curX)
                        curH = (mouseY - curY).coerceAtLeast(10.0)
                    }
                    DragHandle.BOTTOM_RIGHT -> {
                        curW = (mouseX - curX).coerceAtLeast(10.0)
                        curH = (mouseY - curY).coerceAtLeast(10.0)
                    }
                    else -> {}
                }

                cropStartX = curX; cropStartY = curY
                cropEndX = curX + curW; cropEndY = curY + curH

                currentVisualRect!![0] = curX; currentVisualRect!![1] = curY
                currentVisualRect!![2] = curW; currentVisualRect!![3] = curH

                lastMouseX = mouseX
                lastMouseY = mouseY

                drawCropSelection(curX, curY, curX + curW, curY + curH)
                calculateCropCoordinates()
            }
        }

        mainImageView.setOnMouseReleased { e ->
            if (!isCropMode || rbCropPan.isSelected || mainImageView.image == null) return@setOnMouseReleased

            if (currentHandle == DragHandle.NONE) {
                cropEndX = e.x.coerceIn(0.0, mainImageView.layoutBounds.width)
                cropEndY = e.y.coerceIn(0.0, mainImageView.layoutBounds.height)
            }
            calculateCropCoordinates()
        }
    }

    private fun getHandleForPoint(x: Double, y: Double, rect: DoubleArray): DragHandle {
        val rx = rect[0]
        val ry = rect[1]
        val rw = rect[2]
        val rh = rect[3]
        val hitSize = handleSize * 1.5

        if (abs(x - rx) < hitSize && abs(y - ry) < hitSize) return DragHandle.TOP_LEFT
        if (abs(x - (rx + rw)) < hitSize && abs(y - ry) < hitSize) return DragHandle.TOP_RIGHT
        if (abs(x - rx) < hitSize && abs(y - (ry + rh)) < hitSize) return DragHandle.BOTTOM_LEFT
        if (abs(x - (rx + rw)) < hitSize && abs(y - (ry + rh)) < hitSize) return DragHandle.BOTTOM_RIGHT

        if (x >= rx && x <= rx + rw && y >= ry && y <= ry + rh) return DragHandle.CENTER

        return DragHandle.NONE
    }

    private fun calculateCropCoordinates() {
        val image = mainImageView.image ?: return
        val bounds = mainImageView.layoutBounds
        val scaleX = image.width / bounds.width
        val scaleY = image.height / bounds.height

        val visX = minOf(cropStartX, cropEndX)
        val visY = minOf(cropStartY, cropEndY)
        val visW = abs(cropEndX - cropStartX)
        val visH = abs(cropEndY - cropStartY)

        val realX = (visX * scaleX).toInt().coerceIn(0, image.width.toInt())
        val realY = (visY * scaleY).toInt().coerceIn(0, image.height.toInt())
        val realW = (visW * scaleX).toInt().coerceAtMost(image.width.toInt() - realX)
        val realH = (visH * scaleY).toInt().coerceAtMost(image.height.toInt() - realY)

        if (realW > 0 && realH > 0) {
            selectedCropRect = intArrayOf(realX, realY, realW, realH)

            txtCropResW.text = "$realW"
            txtCropResH.text = "$realH"
            currentVisualRect = doubleArrayOf(visX, visY, visW, visH)
        }
    }

    // Dibuja el rectángulo amarillo sobre el Canvas
    private fun drawCropSelection(x1: Double, y1: Double, x2: Double, y2: Double) {
        val gc = overlayCanvas.graphicsContext2D
        val cvW = overlayCanvas.width
        val cvH = overlayCanvas.height

        gc.clearRect(0.0, 0.0, cvW, cvH)

        overlayCanvas.width = mainImageView.layoutBounds.width
        overlayCanvas.height = mainImageView.layoutBounds.height

        val x = minOf(x1, x2)
        val y = minOf(y1, y2)
        val w = abs(x2 - x1)
        val h = abs(y2 - y1)

        gc.fill = Color.rgb(0, 0, 0, 0.6)
        gc.fillRect(0.0, 0.0, overlayCanvas.width, overlayCanvas.height)

        gc.clearRect(x, y, w, h)

        // 3. Bordes y Guías
        gc.stroke = Color.WHITE
        gc.lineWidth = 1.0
        gc.setLineDashes(0.0)
        gc.strokeRect(x, y, w, h)

        gc.stroke = Color.rgb(255, 255, 255, 0.3)
        gc.setLineDashes(3.0)
        gc.strokeLine(x + w / 3, y, x + w / 3, y + h)
        gc.strokeLine(x + 2 * w / 3, y, x + 2 * w / 3, y + h)
        gc.strokeLine(x, y + h / 3, x + w, y + h / 3)
        gc.strokeLine(x, y + 2 * h / 3, x + w, y + 2 * h / 3)

        gc.fill = Color.WHITE
        gc.setLineDashes(0.0)
        val hs = handleSize
        val half = hs / 2

        gc.fillRect(x - half, y - half, hs, hs) // Top-Left
        gc.fillRect(x + w - half, y - half, hs, hs) // Top-Right
        gc.fillRect(x - half, y + h - half, hs, hs) // Bottom-Left
        gc.fillRect(x + w - half, y + h - half, hs, hs) // Bottom-Right
    }

    @FXML
    fun onToggleCropMode() {
        isCropMode = true

        cropContainer.isVisible = true
        cropContainer.isManaged = true
        rbCropDraw.isSelected = true
        scrollPane.isPannable = false
        mainImageView.cursor = Cursor.CROSSHAIR

        updateStatus("Modo Recorte: Arrastre el mouse para seleccionar área.")
    }

    @FXML
    fun onApplyCrop() {
        val rect = selectedCropRect
        val currentImage = baseImage

        if (rect != null && currentImage != null) {
            val (x, y, w, h) = rect
            val croppedImage = TransformService.cropImage(currentImage, x, y, w, h)
            updateBaseAfterFilter(croppedImage, "Recorte ${w}x${h}")
            updateStatus("Recorte aplicado: ${w}x${h}")

            exitCropMode()
        } else {
            updateStatus("Seleccione un área válida primero.")
        }
    }

    @FXML
    fun onClearCropSelection() {
        clearOverlay()
        selectedCropRect = null
        currentVisualRect = null
        txtCropResW.text = "0"
        txtCropResH.text = "0"
    }

    private fun exitCropMode() {
        isCropMode = false
        cropContainer.isVisible = false
        cropContainer.isManaged = false
        clearOverlay()
        selectedCropRect = null
        currentVisualRect = null
        txtCropResW.text = "0"
        txtCropResH.text = "0"
        updateStatus("Modo Recorte finalizado.")
    }

    // =============================
    // SECCIÓN: Utilidades y helpers
    // =============================

    private fun updateStatus(message: String) {
        lblStatus.text = message
        statusDelay?.stop()

        val pause = PauseTransition(Duration.seconds(5.0))
        pause.setOnFinished {
            lblStatus.text = "Listo"
        }
        pause.play()
        statusDelay = pause
    }

    private fun pickColorAndApply(opName: String, transform: (Image, Color) -> Image) {
        val currentImage = mainImageView.image ?: run {
            updateStatus("No hay imagen cargada.")
            return }
        val dialog = Dialog<Color>()
        dialog.title = opName
        dialog.headerText = "Selecciona el color:"
        dialog.dialogPane.style = "-fx-base: #383838; -fx-background-color: #383838; -fx-font-family: 'Segoe UI', sans-serif;"
        dialog.dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
        val colorPicker = ColorPicker(Color.CYAN)
        val box = VBox(colorPicker)
        box.alignment = Pos.CENTER
        box.padding = Insets(20.0)
        dialog.dialogPane.content = box
        dialog.setResultConverter { if (it == ButtonType.OK) colorPicker.value else null }
        dialog.showAndWait().ifPresent { color ->
            val newImage = transform(currentImage, color)
            updateBaseAfterFilter(newImage, opName)
            updateStatus("Aplicado: $opName")
        }
    }

    private fun applyGenericFilter(opName: String, transform: (Image, Void?) -> Image) {
        val currentImage = mainImageView.image ?: run {
            updateStatus("No hay imagen cargada.")
            return }
        val newImage = transform(currentImage, null)
        updateBaseAfterFilter(newImage, opName)
        updateStatus("Aplicado: $opName")
    }

    // --- Helpers para TextFields numéricos ---
    private fun setupTextFieldDoubleNoDecimals(textField: TextField, slider: Slider) {
        val commit = { commitEditDouble(textField, slider) }
        textField.setOnAction { commit() }
        textField.focusedProperty().addListener { _, _, isFocused -> if (!isFocused) commit() }
    }

    private fun commitEditDouble(textField: TextField, slider: Slider, format: String = "%.0f") {
        try {
            val value = textField.text.replace(",", ".").toDouble()
            if (value >= slider.min && value <= slider.max) slider.value = value
            else textField.text = format.format(slider.value)
        } catch (_: NumberFormatException) { textField.text = format.format(slider.value) }
    }

    private fun toggleThresholdControls(enable: Boolean) {
        comboThresholdType.isDisable = !enable
        boxSingleThreshold.isDisable = !enable
        boxRangeThreshold.isDisable = !enable
    }

    private fun setupTextFieldInt(textField: TextField, slider: Slider) {
        textField.setOnAction { commitEditInt(textField, slider) }
        textField.focusedProperty().addListener { _, _, isFocused -> if (!isFocused) commitEditInt(textField, slider) }
    }

    private fun commitEditInt(textField: TextField, slider: Slider) {
        try {
            val value = textField.text.toInt()
            if (value >= slider.min && value <= slider.max) slider.value = value.toDouble()
            else textField.text = "${slider.value.toInt()}"
        } catch (_: NumberFormatException) {
            textField.text = "${slider.value.toInt()}"
        }
    }

    private fun setupTextField(textField: TextField, slider: Slider) {
        textField.setOnAction { commitEdit(textField, slider) }
        textField.focusedProperty().addListener { _, _, isFocused ->
            if (!isFocused) commitEdit(textField, slider)
        }
    }

    private fun commitEdit(textField: TextField, slider: Slider) {
        try {
            val valStr = textField.text.replace(",", ".")
            val value = valStr.toDouble()

            if (value >= slider.min && value <= slider.max) {
                slider.value = value
            } else {
                textField.text = "%.2f".format(slider.value)
                updateStatus("Valor fuera de rango (${slider.min} a ${slider.max})")
            }
        } catch (_: NumberFormatException) {
            textField.text = "%.2f".format(slider.value)
        }
    }

    // Helper para ejecutar filtros en segundo plano sin congelar la UI
    private fun applyFilterInBackground(filterName: String, process: () -> Image) {
        mainImageView.scene?.cursor = Cursor.WAIT
        lblStatus.text = "Procesando $filterName..."

        val task = object : Task<Image>() {
            override fun call(): Image {
                return process()
            }
        }

        task.setOnSucceeded {
            val resultImage = task.value
            updateBaseAfterFilter(resultImage, "Filtro: $filterName")
            updateStatus("$filterName aplicado.")
            mainImageView.scene?.cursor = Cursor.DEFAULT
        }

        task.setOnFailed {
            updateStatus("Error al aplicar $filterName.")
            task.exception.printStackTrace()
            mainImageView.scene?.cursor = Cursor.DEFAULT
        }

        Thread(task).start()
    }

    // Helper extraído para evitar duplicar código entre el Inspector y el Recorte
    private fun updatePixelInfo(e: MouseEvent) {
        val img = mainImageView.image ?: return
        val bounds = mainImageView.layoutBounds

        val scaleX = img.width / bounds.width
        val scaleY = img.height / bounds.height
        val px = (e.x * scaleX).toInt()
        val py = (e.y * scaleY).toInt()

        if (px in 0 until img.width.toInt() && py in 0 until img.height.toInt()) {
            val c = img.pixelReader.getColor(px, py)
            val r = (c.red * 255).toInt()
            val g = (c.green * 255).toInt()
            val b = (c.blue * 255).toInt()
            val hex = String.format("#%02X%02X%02X", r, g, b)

            lblPixelInfo.text = "Pos($px,$py) | RGB($r, $g, $b) | Hex: $hex |"
            rectPixelColor.fill = c
            rectPixelColor.isVisible = true
        }
    }

    // Helper para pedir un valor numérico simple (Double)
    private fun showParameterDialog(title: String, header: String, label: String, max: Double, default: Double, onApply: (Double) -> Unit) {
        if (baseImage == null) { updateStatus("No hay imagen cargada."); return }

        val dialog = Dialog<Double>()
        dialog.title = title
        dialog.headerText = header
        dialog.dialogPane.style = "-fx-base: #383838; -fx-background-color: #383838; -fx-font-family: 'Segoe UI', sans-serif;"
        dialog.dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

        // Usamos 0.0 fijo como mínimo
        val slider = Slider(0.0, max, default)
        slider.isShowTickLabels = true
        slider.isShowTickMarks = true

        val lblValue = Label("%.2f".format(default))
        lblValue.style = "-fx-font-weight: bold;"

        slider.valueProperty().addListener { _, _, n ->
            lblValue.text = "%.2f".format(n.toDouble())
        }

        val box = VBox(10.0)
        box.alignment = Pos.CENTER_LEFT
        box.padding = Insets(20.0)
        box.children.addAll(Label(label), slider, lblValue)

        dialog.dialogPane.content = box

        dialog.setResultConverter { if (it == ButtonType.OK) slider.value else null }
        dialog.showAndWait().ifPresent { value ->
            onApply(value)
        }
    }

    private fun showIntParameterDialog(title: String, header: String, label: String, max: Int, default: Int, onApply: (Int) -> Unit) {
        if (baseImage == null) { updateStatus("No hay imagen cargada."); return }

        val dialog = Dialog<Int>()
        dialog.title = title
        dialog.headerText = header
        dialog.dialogPane.style = "-fx-base: #383838; -fx-background-color: #383838; -fx-font-family: 'Segoe UI', sans-serif;"
        dialog.dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

        val slider = Slider(1.0, max.toDouble(), default.toDouble())
        slider.isShowTickLabels = true
        slider.isShowTickMarks = true
        slider.majorTickUnit = (max / 4).toDouble().coerceAtLeast(1.0)
        slider.minorTickCount = 0
        slider.isSnapToTicks = true // Fuerza al slider a saltar solo de entero en entero

        val lblValue = Label(default.toString())
        lblValue.style = "-fx-font-weight: bold;"

        slider.valueProperty().addListener { _, _, n ->
            lblValue.text = n.toInt().toString()
        }

        val box = VBox(10.0)
        box.alignment = Pos.CENTER_LEFT
        box.padding = Insets(20.0)
        box.children.addAll(Label(label), slider, lblValue)

        dialog.dialogPane.content = box

        dialog.setResultConverter { if (it == ButtonType.OK) slider.value.toInt() else null }
        dialog.showAndWait().ifPresent { value ->
            onApply(value)
        }
    }

    private data class HistoryState(val thumbnail: Image, val fullImage: Image, val actionName: String) {
        override fun toString(): String = actionName
    }

    private fun setupHistoryPanel() {
        listHistory.setCellFactory {
            object : ListCell<HistoryState>() {
                private val imageView = ImageView()
                override fun updateItem(item: HistoryState?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        text = null; graphic = null
                        style = "-fx-background-color: transparent;"
                    } else {
                        imageView.image = item.thumbnail
                        imageView.fitWidth = 40.0; imageView.fitHeight = 40.0
                        imageView.isPreserveRatio = true
                        text = item.actionName
                        graphic = imageView

                        if (isSelected) {
                            style = "-fx-background-color: #0078D7; -fx-text-fill: white; -fx-padding: 5;"
                            textFill = Color.WHITE
                        } else {
                            style = "-fx-background-color: transparent; -fx-padding: 5;"
                            textFill = Color.LIGHTGRAY
                        }
                    }
                }
            }
        }

        listHistory.setOnMouseClicked { e ->
            if (e.clickCount == 2 && listHistory.selectionModel.selectedItem != null) {
                onRestoreHistoryClick()
            }
        }

        btnRestoreHistory.disableProperty().bind(listHistory.selectionModel.selectedItemProperty().isNull())

        historyPanel.setOnMousePressed { e ->
            if (!listHistory.boundsInParent.contains(e.x, e.y)) {
                listHistory.selectionModel.clearSelection()
            }
        }

        listHistory.selectionModel.selectedItemProperty().addListener { _, _, _ -> listHistory.refresh() }
    }

    @FXML
    fun onRestoreHistoryClick() {
        val selectedState = listHistory.selectionModel.selectedItem
        if (selectedState != null) {
            baseImage = selectedState.fullImage
            mainImageView.image = baseImage

            val selectedIndex = listHistory.selectionModel.selectedIndex
            if (selectedIndex > 0) {
                listHistory.items.remove(0, selectedIndex)
            }

            undoStack.clear()

            updateImageInfo(baseImage!!, updateCharts = true)
            updateStatus("Restaurado a: ${selectedState.actionName}")
        }
    }

}