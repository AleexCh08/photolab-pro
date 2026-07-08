# PhotoLab Pro

Aplicación de escritorio desarrollada en Kotlin y JavaFX enfocada en el Procesamiento Digital de Imágenes (PDI). Permite cargar, editar, aplicar transformaciones matemáticas, analizar las propiedades de los píxeles y exportar los resultados.

## Características Principales

* **Gestión de Archivos:** Lectura y escritura de imágenes PNG, JPG, BMP y soporte nativo para los formatos NetPBM (P1, P2, P3 en ASCII y P4, P5, P6 en Binario) con detección automática.
* **Ajustes de Color y Luz:** Controles deslizantes para modificar el brillo, contraste, saturación y corrección gamma de la imagen mediante transformaciones lineales.
* **Filtros de Convolución:** Implementación de Kernels para detección de bordes (Sobel, Prewitt, Roberts, Scharr), suavizado (Media, Mediana), perfilado (Sharpen, Laplaciano del Gaussiano LoG) y una herramienta para aplicar matrices de coeficientes personalizadas.
* **Operaciones de Píxel:** Conversión a escala de grises (método Promedio y Luma), filtros monocromáticos, alteraciones aditivas/multiplicativas, y efecto negativo.
* **Umbralización:** Segmentación de imágenes mediante umbral binario simple, corte de rango y selección de rango.
* **Transformaciones Geométricas:** Algoritmos de escalado de resolución usando interpolación de Vecino Más Próximo o Bilineal, rotaciones ortogonales (90°, 180°), espejado y un modo interactivo de recorte (Crop).
* **Herramientas de Análisis:** Cálculo y renderizado en tiempo real de Histogramas (RGB y compuestos), Curva Tonal de transferencia y Perfil de Línea interactivo para examinar intensidades por filas.
* **Ruido Artificial:** Generación matemática de ruido Sal y Pimienta y ruido Gaussiano aditivo.
* **Flujo de Trabajo:** Historial de acciones con miniaturas integradas, soporte para arrastrar y soltar (Drag and Drop), e inspector de píxeles al pasar el cursor.

## Tecnologías Utilizadas

* **Lenguaje:** Kotlin
* **Framework UI:** JavaFX (Módulos: `controls`, `fxml`, `swing`)
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


