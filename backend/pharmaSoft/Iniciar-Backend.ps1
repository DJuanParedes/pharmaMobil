param([string]$ArchivoClaves)
$ErrorActionPreference='Stop'
if($ArchivoClaves){
    $claves=Get-Content -LiteralPath $ArchivoClaves -Raw | ConvertFrom-Json
    $env:DB_PASSWORD=$claves.DB_PASSWORD
    $env:JWT_SECRET=$claves.JWT_SECRET
}
if(-not $env:DB_PASSWORD -or -not $env:JWT_SECRET){throw 'Define DB_PASSWORD y JWT_SECRET, o indica -ArchivoClaves con un JSON local.'}
$env:SPRING_PROFILES_ACTIVE='mysql'
Push-Location $PSScriptRoot
try {
    $jar=Join-Path $PSScriptRoot 'target\PharmaBackend-0.0.1-SNAPSHOT.jar'
    if(-not(Test-Path $jar)){& .\mvnw.cmd -DskipTests package; if($LASTEXITCODE -ne 0){throw 'Error de compilación'}}
    & java -jar $jar
} finally {Pop-Location}
