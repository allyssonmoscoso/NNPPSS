# Builds a self-contained Windows app image with jpackage.
# Output: dist\NNPPSS\NNPPSS.exe
$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

Write-Host "Building application JAR and runtime dependencies..."
mvn -B -DskipTests package
if ($LASTEXITCODE -ne 0) {
    throw "Maven package failed with exit code $LASTEXITCODE"
}

$appDir = Join-Path $Root "target\app"
$jar = Get-ChildItem -Path (Join-Path $Root "target") -Filter "NNPPSS-*.jar" |
    Where-Object { $_.Name -notmatch "sources|javadoc" } |
    Select-Object -First 1
if (-not $jar) {
    throw "NNPPSS JAR was not produced by Maven"
}

Copy-Item $jar.FullName (Join-Path $appDir "NNPPSS.jar") -Force

# Optional external extractor. It must live in app\lib so that
# PackageService (AppPaths.appDir()/lib) can find it.
$pkg2zip = Join-Path $Root "lib\pkg2zip.exe"
if (Test-Path $pkg2zip) {
    Copy-Item $pkg2zip (Join-Path $appDir "lib\pkg2zip.exe") -Force
    Write-Host "Bundled pkg2zip.exe"
} else {
    Write-Warning "lib\pkg2zip.exe not found; extraction will require pkg2zip on PATH."
}

$distDir = Join-Path $Root "dist"
$imageDir = Join-Path $distDir "NNPPSS"
if (Test-Path $imageDir) {
    Remove-Item $imageDir -Recurse -Force
}

$packageType = $env:JPACKAGE_TYPE
if (-not $packageType) {
    $packageType = "app-image"
}

Write-Host "Running jpackage (type: $packageType)..."
$jpackageArgs = @(
    "--type", $packageType,
    "--name", "NNPPSS",
    "--input", $appDir,
    "--main-jar", "NNPPSS.jar",
    "--main-class", "com.squarepeace.nnppss.Launcher",
    "--dest", $distDir,
    "--java-options", "--enable-native-access=ALL-UNNAMED"
)
if ($env:NNPPSS_VERSION) {
    $jpackageArgs += @("--app-version", $env:NNPPSS_VERSION)
}
if ($packageType -eq "msi") {
    $jpackageArgs += @(
        "--win-menu",
        "--win-shortcut",
        "--win-dir-chooser",
        "--win-upgrade-uuid", "8f7d64a0-3b4a-4d6a-9a1f-2f0d8f4c6b21"
    )
}
& jpackage @jpackageArgs

if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed with exit code $LASTEXITCODE"
}

if (Test-Path $imageDir) {
    Write-Host "App image created: $imageDir"
    Write-Host "Run: $imageDir\NNPPSS.exe"
} else {
    Write-Host "Package created under: $distDir"
    Get-ChildItem $distDir | Select-Object Name, Length
}
