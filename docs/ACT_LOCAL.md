# Ejecutar los workflows de GitHub en local con act

El repositorio incluye `act` como herramienta de validación local. `act` usa
Docker y **solo puede emular runners Linux**: los jobs `windows-latest` y
`macos-latest` deben ejecutarse en GitHub o en un host de ese sistema.

## Requisitos

- `act` 0.2.98 o superior.
- Docker en ejecución.
- La imagen de runner:

```powershell
docker pull catthehacker/ubuntu:act-latest
```

## Uso rápido

Windows:

```powershell
pwsh -NoProfile -File scripts/act-local.ps1
```

Linux/macOS:

```sh
sh scripts/act-local.sh
```

El script:

1. Crea la red `nnppss-act` (necesaria en Docker Desktop).
2. Ejecuta solo la matriz `ubuntu-latest`.
3. Guarda los artefactos de `upload-artifact` en `.act-artifacts/`.

Equivalente manual:

```powershell
act -j build --matrix os:ubuntu-latest `
  --network nnppss-act `
  --artifact-server-path .act-artifacts
```

## Qué valida

- `actions/checkout`.
- `actions/setup-java` con Temurin 25.
- Instalación de xvfb, Maven y dependencias GTK/ALSA de JavaFX en Linux.
- `mvn -B clean verify`.
- Gate de Swing/AWT y archivos `.form`.
- `scripts/package-unix.sh` y `jpackage`.
- `actions/upload-artifact`.

## Validación realizada

La ejecución local del 2026-11-20 terminó con:

- `mvn -B clean verify`: **45 tests, 0 fallos**.
- Gate de Swing/AWT: correcto.
- `scripts/package-unix.sh` + `jpackage`: app image Linux creada.
- `actions/upload-artifact@v4`: `NNPPSS-ubuntu-latest.zip` de 63.5 MB en
  `.act-artifacts/`.

Comando usado:

```powershell
act -j build --matrix os:ubuntu-latest `
  --network act-net `
  --artifact-server-path .act-artifacts
```

### Corrección necesaria para act

El `.gitignore` usaba patrones como `app/`, `*.properties` y `*.json`, que
también excluían `src/main/java/.../app/` y los bundles de mensajes al copiar
el workspace con act. Se cambiaron a patrones anclados a la raíz
(`/app/`, `/lib/`, `/db/`, `/games/`, `/target/`) y se retiró `*.properties`.
GitHub no se veía afectado porque los archivos están versionados, pero act sí
los omitía.

`setup-java@v4` emite una advertencia de deprecación; sigue funcionando. La
migración a `@v5` queda pendiente de validación en una próxima ejecución.

## Limitaciones

- **No ejecutar toda la matriz** (`act -j build` sin `--matrix`): act usaría
  Linux para los tres jobs y produciría paquetes Linux con nombres Windows/
  macOS. Siempre filtrar `--matrix os:ubuntu-latest`.
- Windows y macOS no se emulan. El empaquetado Windows se valida con:
  ```powershell
  pwsh -NoProfile -File scripts/package-windows.ps1
  ```
  El de macOS requiere un host macOS real.
- En Docker Desktop, `--network host` puede quedarse colgado al crear el
  contenedor. Por eso los scripts usan la red `nnppss-act`.
- Si act deja un volumen bloqueado tras una interrupción:
  ```powershell
  docker container prune -f
  docker volume prune -f
  ```

## Validación estática

Sin Docker ni descargar imágenes:

```powershell
act -n -j build --matrix os:ubuntu-latest
```
