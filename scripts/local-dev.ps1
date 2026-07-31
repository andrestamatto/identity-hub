[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet("up", "down", "status", "run", "token", "smoke")]
    [string] $Action = "status",

    [string] $EnvironmentFile = "C:\Users\re040282\dev\env\identity-hub.local.env"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $EnvironmentFile -PathType Leaf)) {
    throw "Arquivo de ambiente local não encontrado: $EnvironmentFile"
}

$repositoryPath = Split-Path -Parent $PSScriptRoot

function ConvertTo-WslPath([string] $WindowsPath) {
    $absolutePath = [System.IO.Path]::GetFullPath($WindowsPath)
    if ($absolutePath -notmatch '^([A-Za-z]):\\(.*)$') {
        throw "O harness espera um caminho absoluto do Windows: $absolutePath"
    }
    $drive = $matches[1].ToLowerInvariant()
    $relativePath = $matches[2].Replace('\', '/')
    return "/mnt/$drive/$relativePath"
}

$repositoryWslPath = ConvertTo-WslPath $repositoryPath
$environmentWslPath = ConvertTo-WslPath $EnvironmentFile

if (-not $repositoryWslPath -or -not $environmentWslPath) {
    throw "Não foi possível converter os caminhos para o WSL."
}

wsl.exe -d Ubuntu -- python3 "$repositoryWslPath/scripts/local-dev.py" `
    $Action `
    --repository "$repositoryWslPath" `
    --env-file "$environmentWslPath"

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
