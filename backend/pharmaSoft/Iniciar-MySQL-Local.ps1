param([string]$CarpetaMySQL=(Join-Path $PSScriptRoot '..\..\..\mysql-local'))
$ErrorActionPreference='Stop'
$CarpetaMySQL=[IO.Path]::GetFullPath($CarpetaMySQL)
$base=Join-Path $CarpetaMySQL 'mysql-8.4.11-winx64'
$data=Join-Path $CarpetaMySQL 'data'
if(-not(Test-Path "$base\bin\mysqld.exe") -or -not(Test-Path $data)){throw 'No se encuentra la instalación local. Instala MySQL y configura DB_URL para tu servidor.'}
$escuchando=Get-NetTCPConnection -State Listen -LocalPort 3306 -ErrorAction SilentlyContinue
if($escuchando){Write-Output 'El puerto MySQL 3306 ya está activo.';return}
$proceso=Start-Process "$base\bin\mysqld.exe" -WindowStyle Hidden -PassThru -ArgumentList @('--no-defaults',"--basedir=`"$base`"","--datadir=`"$data`"",'--bind-address=127.0.0.1','--port=3306','--mysqlx=OFF',"--log-error=`"$CarpetaMySQL\mysql.log`"")
for($i=0;$i -lt 30;$i++){
    Start-Sleep -Milliseconds 500
    $proceso.Refresh()
    if($proceso.HasExited){throw 'MySQL no inició. Revisa mysql-local\mysql.log.'}
    if(Get-NetTCPConnection -State Listen -LocalPort 3306 -ErrorAction SilentlyContinue){Write-Output 'MySQL listo en 127.0.0.1:3306.';return}
}
throw 'MySQL no respondió dentro del tiempo esperado.'
