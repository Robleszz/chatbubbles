# Chat Bubbles (Fabric 1.21.11, client-side)

## Compilar
1. JDK 21 instalado.
2. Lo más fácil: genera la plantilla oficial en https://fabricmc.net/develop (1.21.11, Yarn, Java 21)
   y copia encima `src/`, `build.gradle` y `gradle.properties` de este proyecto
   (así te trae el gradle wrapper y las versiones correctas).
3. En la raíz del proyecto:
   - Probar en el juego:  `./gradlew runClient`
   - Generar el .jar:     `./gradlew build`  -> `build/libs/chatbubbles-1.0.0.jar`
4. En el cliente instala: Fabric Loader, Fabric API, Cloth Config (+ Mod Menu para abrir la config).

Config: Mod Menu -> Chat Bubbles, o `config/chatbubbles.json`.

## Obtener el .jar sin instalar nada (GitHub)
1. Crea un repo en github.com y sube todo el contenido de esta carpeta (incluida `.github`).
2. Pestaña **Actions** -> workflow **build** -> espera ~3 min.
3. Entra a la ejecución y descarga **chatbubbles-jar** (usa el .jar que NO termina en -sources).
