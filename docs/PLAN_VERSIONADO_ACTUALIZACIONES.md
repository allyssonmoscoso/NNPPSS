# Plan · Versionado y actualizaciones automáticas

**Estado:** 2.0.1 publicado con el fix de auto-cleanup y empaquetado · **Versión de desarrollo:** 2.0.0-SNAPSHOT · **Última release publicada:** 2.0.1 · **Fecha:** 2026-11-21.

## 1. Objetivo

Automatizar dos cosas sin convertirlas en un riesgo para el usuario:

1. **Versionado reproducible:** cada release nace de un tag, con el mismo número en el tag, el JAR, el instalador, la UI y el manifiesto de actualización.
2. **Actualizaciones controladas:** la app consulta GitHub, avisa de una versión nueva y ofrece el instalador correcto para su sistema; el usuario decide cuándo descargar e instalar.

La app no debe actualizarse sola en segundo plano ni ejecutar binarios descargados sin confirmación y verificación.

## 2. Situación actual

| Punto | Estado |
|---|---|
| Versión del proyecto | `pom.xml` declara **1.0.0**. |
| Tags | Conviven `v1.0.0`, `1.0.1` y `1.1.0`, sin una convención única. |
| Release de GitHub | `1.1.0` solo adjunta `NNPPSS-1.1.0.zip` (≈1,6 MB); no hay instaladores por plataforma. |
| Workflow | `build.yml` compila, testea y sube artefactos temporales; no crea releases. |
| Actualizaciones | No existe comprobación, versión visible ni enlace de descarga. |
| Instaladores | Los scripts generan `app-image`; `JPACKAGE_TYPE` permite otros tipos, pero no se usan en release. |
| `act` | Ya valida el workflow Linux localmente; el workflow de release también deberá validarse con act cuando sea posible. |

Antes de la primera release nueva hay que **reconciliar** la versión: el código actual es posterior a 1.1.0. Propuesta: próxima versión **2.0.0** (nuevas funciones y rediseño de UI, sin romper compatibilidad de datos).

## 3. Decisiones de producto

| Tema | Propuesta |
|---|---|
| Fuente de verdad | El **tag Git** `vX.Y.Z`; CI inyecta esa versión en Maven y `jpackage`. |
| SemVer | `MAJOR.MINOR.PATCH`; `-beta.N`/`-rc.N` para pre-releases. |
| Canales | **Solo `stable` en 2.0.0**; `beta` queda para una versión posterior. |
| Comprobación | En segundo plano, como máximo una vez cada 24 h; silenciosa si falla. |
| Aviso | Snackbar/banner no modal con “Ver novedades” y “Abrir descarga”. |
| Instalación | **MVP 2.0.0: avisar y abrir la página del release**. Descarga/lanzamiento de instalador se pospone a 2.x. Sin reemplazo silencioso. |
| Instaladores | Windows MSI, macOS DMG, Linux DEB, más ZIP portable por plataforma; se adjuntan al release para descarga manual. |
| Firma | **Sin certificados por ahora**; se publican checksums y se avisa de las advertencias de Windows/macOS. La firma queda como fase futura. |
| Release | Draft automático; un maintainer lo revisa y publica. La app solo ve releases publicadas. |

## 4. Versionado reproducible

### 4.1 Convención

- Tags nuevos: `v2.0.0`, `v2.0.1`, `v2.1.0-beta.1`.
- Los tags antiguos sin `v` no se reescriben; se documenta la transición.
- `CHANGELOG.md` debe contener una sección `## [2.0.0]` antes de crear el tag.
- Un tag no se mueve ni se reutiliza. Si una release sale mal, se publica un patch.

### 4.2 Inyección de versión

Flujo de release:

```text
tag v2.0.0
  → checkout
  → VERSION=2.0.0
  → mvn versions:set -DnewVersion=$VERSION -DgenerateBackupPoms=false
  → mvn -B clean verify
  → jpackage --app-version $VERSION ...
  → JAR/manifest, instalador, UI y update-manifest.json con 2.0.0
```

- El `pom.xml` de desarrollo puede quedar como `2.0.0-SNAPSHOT`.
- `jpackage` debe recibir `--app-version`; hoy los scripts no lo pasan.
- El JAR llevará `Implementation-Version` en el manifiesto.
- La app obtiene la versión con `BuildInfo.currentVersion()`:
  - `Package.getImplementationVersion()` si existe;
  - fallback `dev` en ejecución desde clases.
- La UI muestra la versión en **Ajustes → Actualizaciones** y en “Acerca de”.
- Un job de validación comprueba:
  - tag con formato SemVer;
  - `VERSION` del `pom` tras la inyección igual al tag;
  - sección del CHANGELOG presente.

## 5. Release automática en GitHub

Workflow nuevo `.github/workflows/release.yml`, separado de `build.yml`:

```yaml
on:
  push:
    tags: ['v*']
  workflow_dispatch:
    inputs:
      version: { required: true }
      draft: { type: boolean, default: true }

permissions:
  contents: write
```

### 5.1 Jobs

1. **validate**
   - Valida tag/versión/CHANGELOG.
   - Determina canal (`beta` si contiene `-`).
2. **build** (matriz)
   - `windows-latest`, `macos-latest`, `ubuntu-latest`.
   - Setup Java 25 y Maven.
   - Linux: xvfb + GTK/ALSA.
   - `mvn -B clean verify`.
   - `mvn versions:set`.
   - `jpackage`:
     - Windows: MSI y ZIP portable.
     - macOS: DMG (arquitectura del runner) y ZIP.
     - Linux: DEB y `tar.gz`/ZIP portable.
   - Nombres:
     - `NNPPSS-2.0.0-windows-x64.msi`
     - `NNPPSS-2.0.0-macos-arm64.dmg`
     - `NNPPSS-2.0.0-linux-x64.deb`
     - `NNPPSS-2.0.0-<os>-<arch>-portable.zip`
3. **release**
   - Descarga artefactos.
   - Genera `SHA256SUMS.txt`.
   - Genera `update-manifest.json`.
   - Crea un **draft release** con notas extraídas del CHANGELOG.
   - Adjunta instaladores, portables, checksums y manifiesto.
   - Publica solo cuando un maintainer lo aprueba.

### 5.2 Instaladores jpackage

| Plataforma | Tipo | Notas |
|---|---|---|
| Windows | `msi` | Requiere WiX en el runner; añadir `--win-menu`, `--win-shortcut`, `--win-dir-chooser`. |
| macOS | `dmg` | `--mac-package-name`; firma/notarización si hay certificados. |
| Linux | `deb` | Necesita `fakeroot`/`dpkg`; `--linux-shortcut`. |
| Portable | ZIP del app-image | Mantener el comportamiento actual para quien no quiera instalar. |

- `scripts/package-windows.ps1` y `package-unix.sh` deben aceptar:
  - `NNPPSS_VERSION`;
  - `JPACKAGE_TYPE`;
  - nombre de salida estable.
- La app-image actual sigue siendo útil para pruebas y para el ZIP portable.

### 5.3 Manifiesto de actualización

Asset estable del release:

```json
{
  "version": "2.0.0",
  "tag": "v2.0.0",
  "channel": "stable",
  "publishedAt": "2026-11-25T12:00:00Z",
  "notesUrl": "https://github.com/allyssonmoscoso/NNPPSS/releases/tag/v2.0.0",
  "assets": {
    "windows-x64": {
      "url": "https://github.com/.../NNPPSS-2.0.0-windows-x64.msi",
      "sha256": "...",
      "size": 12345678,
      "kind": "msi"
    },
    "macos-arm64": { "...": "..." },
    "linux-x64": { "...": "..." }
  }
}
```

- La app consulta:
  `https://github.com/allyssonmoscoso/NNPPSS/releases/latest/download/update-manifest.json`
  evitando el límite de la API de GitHub.
- El manifiesto se genera en CI, no a mano.
- `latest` solo apunta a releases estables; las beta se ofrecen solo si el usuario activa el canal beta.

## 6. Comprobación desde la app

Nuevo `UpdateService` sin JavaFX:

```text
checkForUpdates(channel, currentVersion, lastCheck)
  → descarga update-manifest.json (HTTPS, timeout 5 s)
  → compara SemVer
  → selecciona asset OS/arquitectura
  → devuelve Optional<UpdateInfo>
```

- Se ejecuta en un hilo de fondo al arrancar, después de mostrar la UI.
- Frecuencia máxima: una vez cada 24 h; se guarda `updates.lastCheck`.
- Si falla la red, no muestra error ni molesta; solo log.
- `updates.checkOnStartup` (por defecto `true`); el canal `beta` queda para una fase futura.
- `updates.skippedVersion` permite “recordar más tarde”.
- No envía datos personales; solo GET al manifiesto.

### UI

- **Ajustes → Actualizaciones**:
  - versión actual;
  - “Buscar actualizaciones”;
  - auto-check on/off;
  - estado: al día / actualización disponible / error de red.
- **Banner/snackbar** al detectar versión nueva:
  - “NNPPSS 2.0.0 disponible”;
  - “Ver novedades”;
  - “Abrir descarga”;
  - “Recordar más tarde”.
- No bloquear el arranque ni abrir un modal automático.
- La versión actual se muestra también en “Acerca de”.

## 7. Instalación de la actualización

### Fase A · Notificar y abrir release (MVP)

- Botón “Actualizar” abre la página del release en el navegador.
- Es la opción más segura y multiplataforma.
- No descarga ni ejecuta nada.

### Fase B · Descargar con verificación

- Descargar el asset correspondiente a un archivo temporal.
- Verificar tamaño y SHA-256 del manifiesto.
- Mostrar progreso y permitir cancelar.
- Si el checksum no coincide, borrar el archivo y mostrar error.

### Fase C · Lanzar el instalador

- Windows: cerrar la app y lanzar `msiexec /i <archivo>.msi`; el UAC lo gestiona Windows.
- macOS: abrir el `.dmg`; el usuario arrastra la app. No intentar reemplazar una app en ejecución.
- Linux: abrir el `.deb` con el gestor del sistema; no ejecutar `apt` con privilegios desde la app.
- Siempre pedir confirmación antes de cerrar y ejecutar.
- Si el usuario cancela, no se pierde nada.

### Fase D · Actualización in-place (solo si se justifica)

No es prioridad. Requiere updater específico por plataforma, bloqueo de ejecutable y recuperación ante fallos. Se evaluará después de validar A–C.

## 8. Seguridad

- Solo HTTPS y solo el repositorio oficial.
- Verificar SHA-256 del asset contra el manifiesto.
- No aceptar URLs arbitrarias del usuario.
- No elevar privilegios silenciosamente.
- No ejecutar el instalador sin confirmación explícita.
- Para 2.0.0 **no hay certificados de firma disponibles**: se publican
  `SHA256SUMS.txt` y el manifiesto, y la UI/release avisa de las advertencias
  de Windows SmartScreen y macOS Gatekeeper.
- Firma futura: Windows Authenticode, macOS Developer ID/notarización y
  checksums/firma GPG opcional en Linux.
- El token de GitHub solo existe en CI (`contents: write`), nunca en la app.

## 9. Compatibilidad y migración

- Próxima versión: **2.0.0**.
- No reescribir tags antiguos; documentar que `v1.0.0`, `1.0.1` y `1.1.0` son legado.
- La release 1.1.0 actual solo tiene un ZIP; los clientes antiguos no tienen updater y deberán actualizar manualmente a 2.0.0.
- La app nueva debe leer datos/config de 1.0/1.1 sin migraciones destructivas.
- Un release fallido se corrige con un patch (`2.0.1`), no moviendo el tag.
- Activar protección de tags `v*` y permiso de release solo para maintainers.

## 10. Fases y estimación

| Fase | Entregable | Días |
|---|---|---:|
| V0 | Reconciliar versión, tags y política SemVer | 0,5 |
| V1 | Versión inyectada en Maven/UI/JAR | 1 |
| V2 | Workflow `release.yml`, instaladores y manifiesto | 3 |
| V3 | `UpdateService` + sección Ajustes + banner | 2 |
| V4 | Tests, `act`, release draft y validación | 1,5 |
| **Total 2.0.0** |  | **~8 días** |
| Futuro | Descarga/checksum/lanzamiento de instalador | 2 |
| Futuro | Firma/notarización y canal beta | 2–4 |

## 11. Pruebas

### Automatizadas

- `VersionTest`: comparación SemVer, pre-releases, versiones inválidas.
- `UpdateManifestTest`: parseo, asset por SO/arquitectura, campos obligatorios.
- `UpdateServiceTest`: manifiesto más nuevo/igual/más viejo, timeout, checksum inválido; canal beta queda para fase futura.
- `BuildInfoTest`: versión desde manifiesto JAR y fallback.
- `FxmlSmokeTest`: sección Actualizaciones.
- `mvn clean verify` verde.

### CI/release

- `act` para la parte Linux del workflow de release (con `workflow_dispatch` y versión de prueba).
- Crear un tag de prueba `v2.0.0-rc.1`, generar draft, revisar assets/checksums/manifiesto.
- Instalar MSI/DMG/DEB en máquinas limpias.
- Verificar que 1.1.0 no ve una beta (canal estable).
- Verificar que 2.0.0 ve 2.0.1 y abre el asset correcto.
- Verificar checksums y que un checksum corrupto se rechaza en la fase de descarga futura.

## 12. Riesgos

| Riesgo | Mitigación |
|---|---|
| Tags y `pom` divergen | Validación en CI; tag como fuente de verdad. |
| Instaladores sin firma generan advertencias | Firma/notarización o aviso claro. |
| Auto-update bloquea el ejecutable | No reemplazo in-place en MVP; instalador externo. |
| Rate limit de GitHub | Usar asset `releases/latest/download`, cache y comprobación diaria. |
| Release roto afecta a todos | Draft + smoke test + patch, nunca mover tag. |
| Archivos de usuario perdidos | Actualización solo reemplaza binarios; datos en `dataDir` intactos. |
| macOS/Windows no se prueban con act | Validar en runners reales/GitHub y máquinas limpias. |

## 13. Decisiones confirmadas
El 2026-11-20 el usuario confirmó:

1. Próxima versión: **2.0.0**.
2. El **tag Git** es la fuente de verdad.
3. MVP de actualización: **avisar y abrir el release**; descarga/lanzamiento de instalador queda para 2.x.
4. Publicar **MSI + DMG + DEB + ZIP portable**.
5. **Sin certificados de firma por ahora**; checksums y avisos visibles.
6. **Solo canal stable** en 2.0.0; beta queda para una fase futura.
7. Release **draft** con publicación manual.

Con esto se puede empezar por V0/V1 sin más preguntas de producto.

## 14. Estado de implementación

Implementado:

- `pom.xml` en `2.0.0-SNAPSHOT` y `Implementation-Version` en el manifiesto del JAR.
- `BuildInfo` y versión visible en Ajustes.
- `release.yml` con validación de tag/CHANGELOG, matriz de instaladores, checksums, manifiesto y draft release.
- `UpdateService` con lectura del manifiesto, comparación SemVer y pruebas con servidor HTTP local.
- Sección **Ajustes → Actualizaciones**, auto-check al inicio (24 h) y banner con “Ver release”.
- `scripts/package-*` aceptan `NNPPSS_VERSION` y `JPACKAGE_TYPE` (MSI/DMG/DEB/app-image).
- `CHANGELOG` 2.0.1 y tests: **53 verdes**.
- App image Windows y JAR Linux versionados correctamente; el manifiesto del JAR reporta `2.0.1`.
- `release.yml` validado con act: job `validate` + build Linux (`deb`, app-image y upload de artefacto) correctos.

Pendiente para cerrar el objetivo:

- Probar MSI/DMG/DEB en máquinas limpias (los assets de 2.0.1 se generaron y el JAR reporta 2.0.1).
- Firma y canal beta quedan fuera de 2.0.x por decisión.

## 15. Validación

- CI de rama: runs [44800940009](https://github.com/allyssonmoscoso/NNPPSS/actions/runs/44800940009), [44849526121](https://github.com/allyssonmoscoso/NNPPSS/actions/runs/44849526121) y [44851434253](https://github.com/allyssonmoscoso/NNPPSS/actions/runs/44851434253) — verdes en Ubuntu, Windows y macOS.
- Release `v2.0.0`: run [44802330265](https://github.com/allyssonmoscoso/NNPPSS/actions/runs/44802330265) — publicado (sus assets antiguos reportan `2.0.0-SNAPSHOT`; usar 2.0.1).
- Release `v2.0.1`: run [44851464585](https://github.com/allyssonmoscoso/NNPPSS/actions/runs/44851464585) — publicado, JAR `2.0.1`, con MSI, DMG, DEB, ZIP portables, checksums y manifiesto.
- `releases/latest/download/update-manifest.json` devuelve `2.0.1`.
- Act: job `validate` y build Linux (`deb` + app-image + upload) correctos.
- 53 tests locales verdes, incluidos auto-cleanup y comparación `2.0.1 > 2.0.0-SNAPSHOT`.
