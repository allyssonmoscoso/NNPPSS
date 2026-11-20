# Plan · Versionado y actualizaciones automáticas

**Estado:** propuesta para aprobación · **Base actual:** `pom.xml` 1.0.0, últimos tags `1.0.1`/`1.1.0` · **Fecha:** 2026-11-20.

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

Antes de la primera release nueva hay que **reconciliar** la versión: el código actual es posterior a 1.1.0. Propuesta: próxima versión **1.2.0** (nuevas funciones y rediseño de UI, sin romper compatibilidad de datos).

## 3. Decisiones de producto

| Tema | Propuesta |
|---|---|
| Fuente de verdad | El **tag Git** `vX.Y.Z`; CI inyecta esa versión en Maven y `jpackage`. |
| SemVer | `MAJOR.MINOR.PATCH`; `-beta.N`/`-rc.N` para pre-releases. |
| Canales | `stable` (por defecto) y `beta` (opt-in). |
| Comprobación | En segundo plano, como máximo una vez cada 24 h; silenciosa si falla. |
| Aviso | Snackbar/banner no modal con “Ver novedades” y “Actualizar”. |
| Instalación | Fase inicial: abrir release/descargar instalador y lanzarlo con confirmación. Sin reemplazo silencioso. |
| Instaladores | Windows MSI, macOS DMG, Linux DEB, más ZIP portable por plataforma. |
| Firma | Recomendada desde el inicio: Authenticode y Developer ID/notarización; sin firma, avisar al usuario. |
| Release | Draft automático; un maintainer lo revisa y publica. La app solo ve releases publicadas. |

## 4. Versionado reproducible

### 4.1 Convención

- Tags nuevos: `v1.2.0`, `v1.2.1`, `v1.3.0-beta.1`.
- Los tags antiguos sin `v` no se reescriben; se documenta la transición.
- `CHANGELOG.md` debe contener una sección `## [1.2.0]` antes de crear el tag.
- Un tag no se mueve ni se reutiliza. Si una release sale mal, se publica un patch.

### 4.2 Inyección de versión

Flujo de release:

```text
tag v1.2.0
  → checkout
  → VERSION=1.2.0
  → mvn versions:set -DnewVersion=$VERSION -DgenerateBackupPoms=false
  → mvn -B clean verify
  → jpackage --app-version $VERSION ...
  → JAR/manifest, instalador, UI y update-manifest.json con 1.2.0
```

- El `pom.xml` de desarrollo puede quedar como `1.2.0-SNAPSHOT`.
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
     - `NNPPSS-1.2.0-windows-x64.msi`
     - `NNPPSS-1.2.0-macos-arm64.dmg`
     - `NNPPSS-1.2.0-linux-x64.deb`
     - `NNPPSS-1.2.0-<os>-<arch>-portable.zip`
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
  "version": "1.2.0",
  "tag": "v1.2.0",
  "channel": "stable",
  "publishedAt": "2026-11-25T12:00:00Z",
  "notesUrl": "https://github.com/allyssonmoscoso/NNPPSS/releases/tag/v1.2.0",
  "assets": {
    "windows-x64": {
      "url": "https://github.com/.../NNPPSS-1.2.0-windows-x64.msi",
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
- `updates.checkOnStartup` (por defecto `true`) y `updates.channel` (`stable`/`beta`).
- `updates.skippedVersion` permite “recordar más tarde”.
- No envía datos personales; solo GET al manifiesto.

### UI

- **Ajustes → Actualizaciones**:
  - versión actual;
  - “Buscar actualizaciones”;
  - auto-check on/off;
  - canal stable/beta;
  - estado: al día / actualización disponible / error de red.
- **Banner/snackbar** al detectar versión nueva:
  - “NNPPSS 1.2.0 disponible”;
  - “Ver novedades”;
  - “Actualizar”;
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
- Firmar instaladores:
  - Windows Authenticode;
  - macOS Developer ID + notarización;
  - Linux checksums/firma GPG opcional.
- Si no hay firma, mostrar advertencia y no prometer actualización silenciosa.
- El token de GitHub solo existe en CI (`contents: write`), nunca en la app.

## 9. Compatibilidad y migración

- Próxima versión: **1.2.0**.
- No reescribir tags antiguos; documentar que `v1.0.0`, `1.0.1` y `1.1.0` son legado.
- La release 1.1.0 actual solo tiene un ZIP; los clientes antiguos no tienen updater y deberán actualizar manualmente a 1.2.0.
- La app nueva debe leer datos/config de 1.0/1.1 sin migraciones destructivas.
- Un release fallido se corrige con un patch (`1.2.1`), no moviendo el tag.
- Activar protección de tags `v*` y permiso de release solo para maintainers.

## 10. Fases y estimación

| Fase | Entregable | Días |
|---|---|---:|
| V0 | Reconciliar versión, tags y política SemVer | 0,5 |
| V1 | Versión inyectada en Maven/UI/JAR | 1 |
| V2 | Workflow `release.yml`, instaladores y manifiesto | 3 |
| V3 | `UpdateService` + sección Ajustes + banner | 2 |
| V4 | Descarga, checksum y lanzamiento de instalador | 2 |
| V5 | Firma/notarización y canal beta | 2–4 |
| V6 | Tests, `act`, release draft y validación | 1,5 |
| **Total** |  | **~10–14 días** |

## 11. Pruebas

### Automatizadas

- `VersionTest`: comparación SemVer, pre-releases, versiones inválidas.
- `UpdateManifestTest`: parseo, asset por SO/arquitectura, campos obligatorios.
- `UpdateServiceTest`: manifiesto más nuevo/igual/más viejo, timeout, checksum inválido, canal beta.
- `BuildInfoTest`: versión desde manifiesto JAR y fallback.
- `FxmlSmokeTest`: sección Actualizaciones.
- `mvn clean verify` verde.

### CI/release

- `act` para la parte Linux del workflow de release (con `workflow_dispatch` y versión de prueba).
- Crear un tag de prueba `v1.2.0-rc.1`, generar draft, revisar assets/checksums/manifiesto.
- Instalar MSI/DMG/DEB en máquinas limpias.
- Verificar que 1.1.0 no ve la beta si está en stable.
- Verificar que 1.2.0 ve 1.2.1 y descarga el asset correcto.
- Verificar checksum corrupto y cancelación.

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

## 13. Decisiones a confirmar

1. ¿Confirmas **tag como fuente de verdad** y próxima versión **1.2.0**?
2. ¿MVP de actualización = **avisar + abrir release**, y después descargar/lanzar instalador con checksum?
3. ¿Instaladores **MSI + DMG + DEB + ZIP portable**, o solo portables al principio?
4. ¿Hay certificados de firma Windows/macOS disponibles?
5. ¿Quieres canal **beta** desde el inicio o solo stable en 1.2.0?

Con estas respuestas se puede empezar por V0/V1 sin rehacer el diseño.
