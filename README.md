# 🎮 Duelo de Reflejos (Reflex Game)

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple?style=flat&logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-blue?style=flat&logo=android)
![Android SDK](https://img.shields.io/badge/Min%20SDK-24-green)
![Status](https://img.shields.io/badge/Status-Active-success)

Una aplicación nativa de Android desarrollada en **Kotlin** y **Jetpack Compose** que pone a prueba tus reflejos. Compite contra un amigo en el mismo dispositivo o conéctate vía **Bluetooth** para jugar en dispositivos separados.

---

## ✨ Características Principales

### 🕹️ Modos de Juego
1.  **Modo Clásico:** El primero en llegar a 5 puntos gana.
2.  **Modo Contrarreloj:** Consigue la mayor cantidad de puntos en 60 segundos.
3.  **Modo Confusión:** El color del texto no coincide con la palabra escrita (Efecto Stroop). ¡No te dejes engañar!

### 📡 Multijugador
* **Local:** Dos jugadores en una sola pantalla (pantalla dividida).
* **Bluetooth:** Conexión inalámbrica entre dos dispositivos (Arquitectura Cliente-Servidor). Sincronización de estado de juego en tiempo real.

### ⚙️ Personalización y Persistencia
* **Temas Personalizados:** Cambia la apariencia de la app (Sistema, IPN, ESCOM).
* **Guardado de Partidas:** Guarda tus resultados en formatos `.json`, `.xml` o `.txt`.
* **Historial y Estadísticas:** Registro de victorias persistente usando **Room Database**.
* **Exportación:** Exporta tus partidas guardadas a la carpeta de Descargas del dispositivo.

---

## 🛠️ Tecnologías Utilizadas

* **Lenguaje:** Kotlin
* **UI Toolkit:** Jetpack Compose (Material 3)
* **Arquitectura:** MVVM (Model-View-ViewModel)
* **Base de Datos Local:** Room Database
* **Preferencias:** DataStore Preferences
* **Conectividad:** Android Bluetooth API (Classic/RFCOMM)
* **Serialización:** Gson & XMLSerializer

---

## 📱 Instalación

### Requisitos Previos
* Android Studio Koala o superior (recomendado).
* JDK 17 o superior.
* Dispositivo físico Android con versión 7.0 (API 24) o superior para pruebas de Bluetooth.

### Pasos para compilar
1.  **Clonar el repositorio:**
    ```bash
    git clone [https://github.com/leay21/juego.git](https://github.com/leay21/juego.git)
    ```
2.  **Abrir en Android Studio:**
    Selecciona la carpeta raíz del proyecto.
3.  **Sincronizar Gradle:**
    Espera a que se descarguen las dependencias.
4.  **Ejecutar:**
    Conecta tu dispositivo y presiona `Run` (▶️).

---

## 📖 Guía de Uso

### 1. Jugar en Modo Local
* En el menú principal, selecciona **"Jugar Local"**.
* Elige uno de los tres modos de juego.
* Coloca el dispositivo en una superficie plana entre los dos jugadores.
* Presiona tu lado de la pantalla cuando aparezca el color indicado.

### 2. Jugar vía Bluetooth
**Importante:** Asegúrate de tener el Bluetooth y la Ubicación (GPS) activados.

**Dispositivo A (Anfitrión/Host):**
1.  Ve a **"Multijugador (Bluetooth)"**.
2.  Presiona el botón **"Ser Anfitrión"**.
3.  (Opcional) Selecciona el modo de juego en la lista inferior.
4.  Espera a que el otro jugador se conecte.

**Dispositivo B (Cliente):**
1.  Ve a **"Multijugador (Bluetooth)"**.
2.  Presiona **"Buscar Partidas"**.
3.  Selecciona el nombre del dispositivo Anfitrión en la lista.
4.  ¡El juego iniciará automáticamente en ambos dispositivos!

### 3. Gestión de Partidas
* Ve a **"Ajustes y Partidas Guardadas"**.
* Aquí puedes cambiar el tema de la aplicación.
* Visualiza tus partidas anteriores.
* Usa los iconos para **Exportar**, **Ver detalles** o **Eliminar** partidas.

---

## 🔒 Permisos Requeridos

La aplicación solicita los siguientes permisos para funcionar correctamente:

| Permiso | Razón |
| :--- | :--- |
| `BLUETOOTH` / `BLUETOOTH_ADMIN` | Conexión básica para dispositivos Android 11 o inferior. |
| `BLUETOOTH_SCAN` | Buscar dispositivos cercanos (Android 12+). |
| `BLUETOOTH_CONNECT` | Conectarse a dispositivos (Android 12+). |
| `BLUETOOTH_ADVERTISE` | Actuar como servidor Bluetooth (Android 12+). |
| `ACCESS_FINE_LOCATION` | Requisito del sistema para escanear Bluetooth en versiones antiguas. |

---

## 🙋 Autor

Toral Alvarez Yael Adair
