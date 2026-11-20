# Runs the Linux leg of .github/workflows/build.yml locally with act.
#
# Windows and macOS runners cannot be emulated by act on a Windows host. The
# Ubuntu job is the one that validates Maven, JavaFX/xvfb, Swing checks and
# the Unix app-image packaging. Windows packaging is validated directly with
# scripts/package-windows.ps1 on a Windows host.
param(
    [switch]$DryRun
)
$ErrorActionPreference = "Stop"


if (-not (Get-Command act -ErrorAction SilentlyContinue)) {
    throw "act is not installed or not on PATH."
}
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker is required by act."
}

$network = "nnppss-act"
$existing = docker network ls --filter "name=^/$network$" --format "{{.Name}}"
if ([string]::IsNullOrWhiteSpace($existing)) {
    docker network create $network | Out-Null
}

$arguments = @(
    "-j", "build",
    "--matrix", "os:ubuntu-latest",
    "--network", $network,
    "--artifact-server-path", ".act-artifacts"
)
if ($DryRun) {
    $arguments += "-n"
}

Write-Host "Running: act $($arguments -join ' ')"
& act @arguments
if ($LASTEXITCODE -ne 0) {
    throw "act failed with exit code $LASTEXITCODE"
}

Write-Host "Workflow completed. Artifacts (if uploaded): .act-artifacts"
