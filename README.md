# PhotoLab Pro

Aplicación de escritorio desarrollada en Kotlin y JavaFX enfocada en el Procesamiento Digital de Imágenes (PDI). Permite cargar, editar, aplicar transformaciones matemáticas, analizar las propiedades de los píxeles y exportar los resultados a través de una interfaz profesional.

## Características Principales

* **Interfaz Profesional (Nueva):** Diseño modular con tema oscuro nativo (Dark Mode), barra de herramientas lateral con íconos vectoriales (SVG), y paneles de ajustes colapsables a la derecha para un flujo de trabajo ágil estilo editores avanzados.
* **Gestión de Archivos:** Lectura y escritura de imágenes PNG, JPG, BMP y soporte nativo para los formatos NetPBM (P1, P2, P3 en ASCII y P4, P5, P6 en Binario) con detección automática. Apertura predeterminada interactiva desde el directorio de imágenes del sistema operativo.
* **Ajustes de Color y Luz:** Controles deslizantes para modificar el brillo, contraste, saturación y corrección gamma de la imagen mediante transformaciones lineales.
* **Filtros de Convolución:** Implementación de Kernels para detección de bordes (Sobel, Prewitt, Roberts, Scharr), suavizado (Media, Mediana), perfilado (Sharpen, Laplaciano del Gaussiano LoG) y una herramienta para aplicar matrices de coeficientes personalizadas.
* **Morfología Matemática (Nuevo):** Operaciones de Erosión, Dilatación, Apertura y Cierre con elementos estructurantes completamente personalizables (forma rectangular, cruz o elipse, y tamaño dinámico impar).
* **Segmentación y Umbralización (Mejorado):** Segmentación de imágenes mediante umbral binario simple, corte de rango y selección de rango. Incorpora algoritmos avanzados como el método de Otsu (umbral óptimo global automático) y Umbral Adaptativo (Gaussiano local).
* **Cuantización de Color (Nuevo):** Herramientas para la reducción geométrica y estadística de paletas de colores a través de recorte de bits, algoritmo de popularidad y Clustering con K-Means.
* **Transformaciones Geométricas:** Escalado de resolución usando interpolación de Vecino Más Próximo o Bilineal, rotaciones ortogonales (90°, 180°), espejado y un modo interactivo de recorte (Crop) integrado nativamente en la barra de herramientas.
* **Herramientas de Análisis:** Cálculo y renderizado en tiempo real de Histogramas (RGB y compuestos), Curva Tonal de transferencia y Perfil de Línea interactivo para examinar intensidades por filas.
* **Ruido Artificial:** Generación matemática de ruido Sal y Pimienta y ruido Gaussiano aditivo.
* **Flujo de Trabajo (Mejorado):** Historial de acciones con miniaturas integradas, soporte para arrastrar y soltar (Drag and Drop), e inspector de píxeles al pasar el cursor. Se suma comparación en vivo interactiva y atajos de teclado globales (Ctrl+Z, Ctrl+R).

## Tecnologías Utilizadas

* **Lenguaje:** Kotlin
* **Framework UI:** JavaFX (Módulos: `controls`, `fxml`, `swing`)
* **Visión Artificial:** OpenCV (a través del wrapper OpenPnP `nu.pattern.OpenCV`)
* **Sistema de Construcción:** Gradle
* **Compatibilidad:** Requiere Java/JDK 21

## Instalación y Uso

Asegúrate de tener instalado el JDK 21. Clona el repositorio y utiliza el wrapper de Gradle incluido en la raíz del proyecto para compilar y ejecutar.

**Para ejecutar en entorno de desarrollo:**
```bash
./gradlew run
```

**Para empaquetar y crear un ejecutable nativo de la aplicación:**
```bash
./gradlew jlink
```
**O para generar el instalador:**
```bash
./gradlew jpackage
```

## Autor
Desarrollado por AleexCh.


