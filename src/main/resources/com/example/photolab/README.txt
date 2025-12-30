Tarea 1 - Procesamiento Digital de Imágenes
PhotoLab PDI Pro
Integrante: Alexander Churio - CI: 23708325

Consideraciones puntuales:
1. La barra inferior muestra: a la izquierda el estado (se actualiza dinámicamente), en el centro el Inspector de
Píxeles (Coordenadas, RGB, Hex y Muestra de Color) y a la derecha las dimensiones/bits de la imagen.
2. Hay mensajes para el usuario cada vez que se realice una acción en la barra inferior a la izquierda.
3. Hay un botón de reiniciar imagen en el menú "Imagen" para quitar todos los filtros.
4. El cálculo del gradiente se hace implícitamente al aplicar una convolución.
5. El brillo y contraste están junto a la curva tonal (Menú Ver).
6. Para no sobrecargar el panel derecho, dividí el uso de las herramientas "Histograma, curva tonal y umbralización",
por separado, "Zoom y curva de perfil". Su visualización es exclusiva por grupos.
7. De los ítems opcionales, implementé el del kernel personalizado (Menú Avanzado), donde se pueden elegir
variantes de los filtros básicos.
8. En los operadores de borde, implementé también el de Scharr aparte de los 3 solicitados.
9. Todos los filtros convolucionan tanto en X como en Y al mismo tiempo, uso la técnica de clamping para los bordes.
10. Implementé un "Deshacer" con la clase Stack (Pila) que proporciona Java, al aplicar una operación se guarda
esta operación en la pila (estados completos en memoria RAM), si sobrepasa de 15 operaciones el programa se vuelve lento.
11. El filtro Laplaciano del Gaussiano (LoG) hace lo siguiente en este orden: aplica un Gaussiano para suavizar y borrar
ruido, luego, aplica Laplaciano para detectar bordes.
12. La lógica de los sliders del brillo, contraste y umbralización es: se actualiza en tiempo real durante el arrastre,
luego al soltar el slider se hace el cálculo nuevo de colores y curvas (histograma).
13. Al guardar en formato .bmp la imagen la mayoría de las veces se guarda, hay casos puntuales en las que no.
14. Se puede arrastrar y soltar una imagen en el visualizador.
15. Agregué un botón de comparar (ubicado en la barra superior), permite alternar visualmente entre la imagen original
y la editada mientras se mantiene presionado, ideal para verificar cambios sutiles.
16. Implementé un soporte NetPBM completo, soporta lectura y escritura de formatos NetPBM (P1, P2, P3) en modo ASCII y
formatos binarios (P4, P5, P6), detectando automáticamente el tipo.
17. Algunas de las imágenes usadas para probar mi aplicación están en la carpeta "Imagenes".
...