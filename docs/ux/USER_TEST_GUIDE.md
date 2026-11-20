# Guion de prueba con usuarios · NNPPSS 1.1
**Objetivo:** validar que una persona nueva completa el flujo principal sin
ayuda y detectar fricción antes de cerrar UX6.

## Participantes

- 5 personas, idealmente 2 con experiencia en emuladores/PS Vita y 3 sin ella.
- Sesiones individuales de 25–35 minutos, presenciales o por videollamada.
- Consentimiento para grabar pantalla y voz; los datos se anonimizan.

## Material

- Build de `dist/NNPPSS/NNPPSS.exe` (o el artefacto de la plataforma).
- Catálogo de prueba con al menos una biblioteca configurada.
- Cronómetro y hoja de observación.
- Enlace al cuestionario SUS.

## Guion de introducción

> Estamos probando la interfaz, no a ti. No hay respuestas correctas. Piensa en
> voz alta mientras haces las tareas; si algo no se entiende, eso es justo lo
> que necesitamos saber. Puedes parar en cualquier momento.

## Tareas

| # | Tarea | Éxito esperado | Métrica |
|---|---|---|---|
| 1 | Encuentra un juego de PS Vita y descárgalo. | Descarga iniciada sin ayuda. | Tiempo, clics, errores |
| 2 | Pausa la descarga, colapsa el dock y vuelve a mostrarlo. | Estado pausado y dock restaurado. | Éxito, dudas |
| 3 | Añade otro juego a la cola, quita el primero y abre la cola. | Cola coherente y sin duplicados. | Éxito, clics |
| 4 | Busca un juego que no exista y vuelve al catálogo completo. | Encuentra “Limpiar filtros”. | Éxito, tiempo |
| 5 | Cambia el tema y comprueba que se mantiene. | Tema aplicado sin reiniciar. | Éxito |
| 6 | Abre ajustes, prueba una URL y guarda. | Estado OK/Error comprensible. | Éxito, errores |
| 7 | Cierra la app y vuelve a abrirla. | No se pierden cola ni descargas. | Éxito |

## Preguntas post-tarea

1. ¿Qué fue lo primero que miraste?
2. ¿Qué esperabas que hiciera “Descargar”?
3. ¿Encontraste cómo pausar o quitar una descarga?
4. ¿Qué te resultó confuso o lento?
5. Del 1 al 5, ¿qué tan fácil fue completar el flujo?
6. ¿Qué cambiarías primero?

## SUS

Administrar el cuestionario SUS de 10 ítems (escala 1–5) después de las tareas.
Objetivo: **SUS ≥ 75**. Registrar también:

- Tiempo total.
- Número de errores recuperables.
- Ayudas solicitadas.
- Comentarios literales relevantes.

## Hoja de observación

| Participante | Tarea | Éxito | Tiempo | Errores | Ayuda | Comentario |
|---|---|---|---|---|---|---|
| P1 | 1 |  |  |  |  |  |
| P1 | 2 |  |  |  |  |  |
| … | … |  |  |  |  |  |

## Criterios de cierre

- 5/5 completan las tareas 1 y 4 sin ayuda.
- 4/5 completan pausa, cola y ajustes sin ayuda.
- SUS ≥ 75.
- Cero bloqueos o regresiones funcionales.
- Los hallazgos se priorizan en el backlog y se corrigen antes de cerrar UX6.

## Fuera de alcance

- Extracción real con `pkg2zip` si el binario no está presente.
- Cambio de idioma en caliente (documentado como reinicio).
- Pruebas de red lenta o modo offline; se cubren en una sesión técnica aparte.
