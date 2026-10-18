param(
    [string]$JavaHome = 'D:/Program Files/Android/Android Studio/jbr',
    [string]$AndroidSdk = 'D:/Users/Edney/Android/Sdk',
    [string]$BuildToolsVersion = '36.1.0',
    [switch]$Install,
    [string]$EmulatorSerial = 'emulator-5554'
)
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
$jdkBin=Join-Path $JavaHome 'bin'
$tools=Join-Path $AndroidSdk "build-tools/$BuildToolsVersion"
$androidJar=Join-Path $AndroidSdk 'platforms/android-36/android.jar'
$build=Join-Path $repo 'android-app/build'
$source=Join-Path $repo 'android-app'
$unit=Join-Path $repo 'content/en-pt/generated/unit-01'
$dist=Join-Path $repo 'dist'
function Run([string]$Program,[string[]]$Arguments) {
    & $Program @Arguments
    if($LASTEXITCODE -ne 0) { throw "$Program terminou com código $LASTEXITCODE" }
}
foreach($required in @((Join-Path $jdkBin 'javac.exe'),(Join-Path $tools 'aapt2.exe'),$androidJar,(Join-Path $unit 'unit.json'))) {
    if(!(Test-Path -LiteralPath $required)) { throw "Arquivo necessário ausente: $required" }
}
$unitContent=Get-Content (Join-Path $unit 'unit.json') -Raw -Encoding utf8 | ConvertFrom-Json
foreach($lesson in $unitContent.lessons) { foreach($exercise in $lesson.exercises) {
    if(!(Test-Path -LiteralPath (Join-Path $unit $exercise.audio))) { throw 'Gere os áudios com generate_unit_audio.ps1 antes de compilar.' }
} }
New-Item -ItemType Directory -Force $build,(Join-Path $build 'classes'),(Join-Path $build 'dex'),(Join-Path $build 'generated'),$dist | Out-Null
$previousJavaHome=$env:JAVA_HOME
try {
    $env:JAVA_HOME=$JavaHome
    $python=Get-Command python.exe -ErrorAction SilentlyContinue
    $pythonPath=if($python) { $python.Source } else { 'D:/Program Files/Python313/python.exe' }
    Run $pythonPath @((Join-Path $repo 'scripts/prepare_visual_assets.py'))
    Run (Join-Path $tools 'aapt2.exe') @('compile','--dir',(Join-Path $build 'visual-res'),'-o',(Join-Path $build 'resources.zip'))
    Run (Join-Path $tools 'aapt2.exe') @('link','-o',(Join-Path $build 'resources.apk'),'--manifest',(Join-Path $source 'AndroidManifest.xml'),'-I',$androidJar,(Join-Path $build 'resources.zip'))
    $javaFiles=@(Get-ChildItem (Join-Path $source 'src') -Recurse -Filter '*.java' | ForEach-Object FullName)
    Run (Join-Path $jdkBin 'javac.exe') (@('--release','8','-encoding','UTF-8','-classpath',$androidJar,'-d',(Join-Path $build 'classes'))+$javaFiles)
    Run (Join-Path $jdkBin 'jar.exe') @('--create','--file',(Join-Path $build 'classes.jar'),'-C',(Join-Path $build 'classes'),'.')
    Run (Join-Path $tools 'd8.bat') @('--lib',$androidJar,'--min-api','26','--output',(Join-Path $build 'dex'),(Join-Path $build 'classes.jar'))
    # O empacotamento usa o Python já presente no ambiente ou explicitamente no PATH.
    $python=Get-Command python.exe -ErrorAction SilentlyContinue
    $pythonPath=if($python) { $python.Source } else { 'D:/Program Files/Python313/python.exe' }
    Run $pythonPath @((Join-Path $repo 'scripts/package_offline_app.py'))
    $unsigned=Join-Path $build 'unsigned.apk'
    $aligned=Join-Path $build 'aligned.apk'
    $signed=Join-Path $dist 'Lingo-Edy.apk'
    Run (Join-Path $tools 'zipalign.exe') @('-f','-P','16','4',$unsigned,$aligned)
    $key=Join-Path $repo '.tools/development.keystore'
    if(!(Test-Path -LiteralPath $key)) {
        New-Item -ItemType Directory -Force (Split-Path $key -Parent) | Out-Null
        Run (Join-Path $jdkBin 'keytool.exe') @('-genkeypair','-keystore',$key,'-storepass','android','-keypass','android','-alias','local-development','-keyalg','RSA','-keysize','2048','-validity','3650','-dname','CN=Local APK Development','-noprompt')
    }
    Run (Join-Path $tools 'apksigner.bat') @('sign','--ks',$key,'--ks-key-alias','local-development','--ks-pass','pass:android','--key-pass','pass:android','--out',$signed,$aligned)
    Run (Join-Path $tools 'apksigner.bat') @('verify','--verbose',$signed)
    Run (Join-Path $tools 'zipalign.exe') @('-c','-P','16','4',$signed)
    if($Install) {
        if($EmulatorSerial -notmatch '^emulator-\d+$') { throw 'Instalação automática limitada a um serial de emulador.' }
        Run (Join-Path $AndroidSdk 'platform-tools/adb.exe') @('-s',$EmulatorSerial,'install','-r',$signed)
        $launch=& (Join-Path $AndroidSdk 'platform-tools/adb.exe') -s $EmulatorSerial shell am start -W -n 'dev.edney.lingolocal/.MainActivity'
        $launch | Write-Output
        if($LASTEXITCODE -ne 0 -or ($launch -join "`n") -notmatch 'Status: ok') { throw 'O APK foi instalado, mas a abertura não foi confirmada. Verifique o emulador.' }
    }
    Write-Output "APK offline: $signed"
} finally { $env:JAVA_HOME=$previousJavaHome }
