# Fluxa | YouTube Client Open Source 

<p align="center">
  <img src="art/Fluxa-horizontal.png" alt="Fluxa Logo" width="600" height="600">
</p>

<p align="center">
  <strong>Un cliente alternativo de YouTube para Android: rápido, privado, inteligente y libre de anuncios.</strong>
</p>

<p align="center">
  <a href="https://github.com/makkispacejam99/Fluxa/releases/latest">
    <img src="https://img.shields.io/github/v/release/makkispacejam99/Fluxa?style=for-the-badge&color=8A2BE2" alt="Latest Release">
  </a>
  <a href="https://ko-fi.com/makkispacejam">
    <img src="https://img.shields.io/badge/Ko--fi-Apoyar-FF5E5B?style=for-the-badge&logo=kofi&logoColor=white" alt="Apóyame en Ko-fi">
  </a>
  <img src="https://img.shields.io/github/license/makkispacejam99/Fluxa?style=for-the-badge&color=4CAF50" alt="GPL-3.0 License">
</p>

---

## Filosofía del Proyecto

Fluxa nace bajo tres pilares fundamentales: **Simplicidad, Rápidez y Útilidad**. 

En un ecosistema saturado de aplicaciones pesadas que ralentizan los dispositivos y rastrean cada interacción, Fluxa ofrece una alternativa ligera e independiente. No depende de los Servicios de Google Play (`GMS`), ni la API oficial de YouTube. En su lugar, utiliza ingeniería inversa limpia a través del motor de **NewPipe Extractor** para devolverle el control total de la experiencia multimedia al usuario. Con un algoritmo personalizado en base a tus suscripciones, keywords y sistema de puntuación. 

---

## Características Principales

* **Privacidad Absoluta (Sin Cuenta de Google):** Disfruta de todo el contenido de YouTube de forma anónima. No necesitas iniciar sesión con un correo electrónico, protegiendo tu historial de rastreos y perfiles comerciales.
* **100% Libre de Anuncios:** Una interfaz limpia y enfocada en el contenido, eliminando las interrupciones publicitarias antes, durante y después de los videos.
* **Reproducción Adaptativa al Máximo (DASH):** Soporte de calidad de video adaptativo hasta **1080p**.
* **Feed con Algoritmo Personalizado:** Feed totalmente personalizado. Fluxa procesa de forma local un algoritmo inteligente basado estrictamente en los canales a los que estás suscrito, sin manipulación externa de tendencias, utilizando unicamente tus keywords, visualizaciones y contenido relacionado almacenados de forma local en tu base de datos personal.
* **Traducción Dinámica con IA:** ¿Videos en inglés, japonés o portugues? La app traduce los títulos y descripciones de forma totalmente automática e instantánea al idioma configurado en tus ajustes mediante inteligencia artificial local.
* **Copias de Seguridad Locales:** Exporta e importa tus suscripciones, listas de reproducción e historial en cualquier momento mediante un archivo de respaldo local. Tu información se queda contigo.
* **Subtítulos Avanzados:** Motor de subtítulos nativo integrado que lee, mapea y renderiza perfectamente las pistas multimedia para que no te pierdas ningún detalle.
* **Ultra Ligero:** Consumo mínimo de memoria RAM y almacenamiento, optimizado especialmente para funcionar de miedo incluso en teléfonos de gama baja.

---

## Arquitectura y Tecnologías

Fluxa está construida desde cero utilizando las herramientas más modernas y eficientes para el desarrollo nativo en Android:

* **Lenguaje:** [Kotlin](https://kotlinlang.org/) — Código moderno, seguro y optimizado.
* **UI Engine:** [Jetpack Compose](https://developer.android.com/jetpack/compose) — Interfaz declarativa, animaciones fluidas y layouts minimalistas libres de herencia pesada XML.
* **Media Player:** [ExoPlayer](https://developer.android.com/media/exoplayer) — El reproductor de alto rendimiento de Google adaptado de forma personalizada para flujos DASH multiplexados.
* **Core Extractor:** [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) — El potente motor de scraping open-source que analiza y extrae feeds, metadatos y enlaces de streaming de YouTube de forma silenciosa.
* **Traductor IA:** [Google ML Kit (On-Device Translation)](https://developers.google.com/ml-kit/language/translation) — Modelos de lenguaje de machine learning que se ejecutan directamente en el procesador del dispositivo, garantizando traducciones gratuitas, rápidas y privadas en modo offline.
* **Base de Datos:** [Room Database](https://developer.android.com/training/data-storage/room) — Persistencia de datos local robusta con arquitectura reactiva para tus listas y suscripciones.

---

## Instalación

Para instalar y probar el proyecto en tu dispositivo Android:

1. Ve a la sección de **[Releases (Lanzamientos)](https://github.com/makkispacejam99/Fluxa/releases)** en este repositorio.
2. Descarga el archivo APK de la versión estable más reciente.
3. Si es la primera vez que instalas un APK externo, asegúrate de habilitar el permiso de **"Instalar aplicaciones de fuentes desconocidas"** en los ajustes de tu navegador o gestor de archivos.
4. ¡Abre Fluxa y disfruta de una experiencia libre!

---

## Bugs

Si encuentra algún error, por favor abra un *Issue* en este repositorio detallando el problema y adjuntando el **Logcat** para poder solucionarlo lo antes posible.

---

## Apoyanos (Donaciones)

Fluxa es un proyecto desarrollado con pasión, dedicación, y amor por el software libre, con el único objetivo de ofrecer una herramienta útil a la comunidad. No hay muros de pago, ni suscripciones premium falsas, ni anuncios ocultos. Si usas la aplicación o alguno de mis futuros proyectos, valoras el esfuerzo invertido en optimizarla y quieres apoyarme para que siga creciendo, ¡puedes invitarme a un café! Toda ayuda me motiva muchísimo a continuar refinando el código.

💖 **[Invítame un café en Ko-fi](https://ko-fi.com/makkispacejam)**

---

## Licencia

Este proyecto es software libre y está licenciado bajo la **GNU General Public License v3.0 (GPL-3.0)**. 

---
Hecho con dedicación, código y diseño por **[MakkiDev (makkispacejam99)](https://github.com/makkispacejam99)**. 
