param([switch]$BuildOnly)
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
$folder=Join-Path $repo '.tools/recognition-test'
$sdk='D:/Users/Edney/Android/Sdk'
$jdk='D:/Program Files/Android/Android Studio/jbr/bin'
$tools=Join-Path $sdk 'build-tools/36.1.0'
$android=Join-Path $sdk 'platforms/android-36/android.jar'
$vosk=Join-Path $repo '.tools/activity-deps/vosk/classes.jar'
$jna=Join-Path $repo '.tools/activity-deps/jna/classes.jar'
$classpath=@($android,$vosk,$jna) -join ';'
New-Item -ItemType Directory -Force $folder,(Join-Path $folder 'classes'),(Join-Path $folder 'dex') | Out-Null
function Run([string]$exe,[string[]]$arguments){ & $exe @arguments; if($LASTEXITCODE -ne 0){throw "Falha em $exe"} }
$manifest='<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="dev.edney.lingolocal.tests"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="34"/><instrumentation android:name="dev.edney.lingolocal.tests.RecognitionInstrumentation" android:targetPackage="dev.edney.lingolocal"/><application android:label="Lingo Edy speech tests"/></manifest>'
[IO.File]::WriteAllText((Join-Path $folder 'AndroidManifest.xml'),$manifest)
Run (Join-Path $jdk 'javac.exe') @('--release','8','-encoding','UTF-8','-classpath',$classpath,'-d',(Join-Path $folder 'classes'),(Join-Path $repo 'tests/android/RecognitionInstrumentation.java'))
Run (Join-Path $jdk 'jar.exe') @('--create','--file',(Join-Path $folder 'classes.jar'),'-C',(Join-Path $folder 'classes'),'.')
$previous=$env:JAVA_HOME
$previousMode=$null
$wifiWasEnabled=$false
try {
    $env:JAVA_HOME=Split-Path $jdk -Parent
    Run (Join-Path $tools 'd8.bat') @('--lib',$android,'--classpath',$vosk,'--classpath',$jna,'--min-api','26','--output',(Join-Path $folder 'dex'),(Join-Path $folder 'classes.jar'))
    Run (Join-Path $tools 'aapt2.exe') @('link','-o',(Join-Path $folder 'test.apk'),'--manifest',(Join-Path $folder 'AndroidManifest.xml'),'-I',$android)
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    Add-Type -AssemblyName System.IO.Compression
    $zip=[IO.Compression.ZipFile]::Open((Join-Path $folder 'test.apk'),[IO.Compression.ZipArchiveMode]::Update)
    try { [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,(Join-Path $folder 'dex/classes.dex'),'classes.dex') | Out-Null } finally { $zip.Dispose() }
    Run (Join-Path $tools 'apksigner.bat') @('sign','--ks',(Join-Path $repo '.tools/development.keystore'),'--ks-key-alias','local-development','--ks-pass','pass:android','--out',(Join-Path $folder 'signed.apk'),(Join-Path $folder 'test.apk'))
    if(!$BuildOnly) {
        $adb=Join-Path $sdk 'platform-tools/adb.exe'
        Run $adb @('-s','emulator-5554','install','-r',(Join-Path $folder 'signed.apk'))
        $previousMode=(& $adb -s emulator-5554 shell settings get global airplane_mode_on).Trim()
        $wifiWasEnabled=((& $adb -s emulator-5554 shell cmd wifi status) -join "`n") -match 'Wifi is enabled'
        Run $adb @('-s','emulator-5554','shell','cmd','connectivity','airplane-mode','enable')
        Run $adb @('-s','emulator-5554','shell','svc','wifi','disable')
        $result=& $adb -s emulator-5554 shell am instrument -w -r 'dev.edney.lingolocal.tests/dev.edney.lingolocal.tests.RecognitionInstrumentation'
        $result | Write-Output
        $installedPath=((& $adb -s emulator-5554 shell pm path dev.edney.lingolocal) -replace '^package:','').Trim()
        $installedHash=((& $adb -s emulator-5554 shell sha256sum $installedPath) -split '\s+')[0]
        $report=[ordered]@{ apk_sha256=$installedHash; airplane_mode=$true; scope='Motor Android real com WAV local: frase positiva, áudio diferente e silêncio. Não mede sotaques ou qualidade fonética humana.'; output=$result; passed=(($result -join "`n") -match 'success=true') }
        $report | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $repo 'reports/speech-android-validation.json') -Encoding utf8
        if(!$report.passed){throw 'Validação do reconhecimento Android falhou'}
    }
} finally {
    $env:JAVA_HOME=$previous
    if($null -ne $previousMode -and $previousMode -ne '1') { & $adb -s emulator-5554 shell cmd connectivity airplane-mode disable }
    if($wifiWasEnabled) { & $adb -s emulator-5554 shell svc wifi enable }
}
