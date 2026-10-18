param(
    [ValidateSet('Setup', 'Decode', 'Build', 'Verify')]
    [string]$Action = 'Verify',
    [string]$JavaHome,
    [string]$AndroidSdk,
    [ValidateRange(1, 16)][int]$Jobs = 4
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repo = Split-Path $PSScriptRoot -Parent
$toolDir = Join-Path $repo '.tools'
$jar = Join-Path $toolDir 'apktool_3.0.3.jar'
$jarHash = 'dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423'
$apk = Join-Path $repo 'APK/Duolingo.apk'
$apkHash = '806c41968c9748f91afb77d6c08093c8ab6b0e2eefdbbb50c9b29c1bc7cd4baa'
$decoded = Join-Path $repo 'decoded'
$dist = Join-Path $repo 'dist'

function Invoke-Checked([string]$Program, [string[]]$Arguments) {
    & $Program @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Program terminou com código $LASTEXITCODE" }
}
function Assert-Hash([string]$Path, [string]$Expected) {
    if (!(Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Arquivo ausente: $Path" }
    if ((Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant() -ne $Expected) {
        throw "SHA-256 diferente do esperado: $Path"
    }
}
function Resolve-Java {
    $candidates = @($JavaHome, $env:JAVA_HOME,
        'D:/Program Files/Android/Android Studio/jbr',
        'C:/Program Files/Android/Android Studio/jbr')
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path (Join-Path $candidate 'bin/java.exe'))) { return $candidate }
    }
    $command = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($command) { return Split-Path (Split-Path $command.Source -Parent) -Parent }
    throw 'Informe -JavaHome com um JDK 17+ (o jbr do Android Studio pode ser usado).'
}
function Resolve-BuildTools {
    $candidates = @($AndroidSdk, $env:ANDROID_HOME, $env:ANDROID_SDK_ROOT,
        'D:/Users/Edney/Android/Sdk', (Join-Path $env:LOCALAPPDATA 'Android/Sdk'))
    foreach ($candidate in $candidates) {
        if (!$candidate) { continue }
        $folder = Join-Path $candidate 'build-tools'
        if (!(Test-Path $folder)) { continue }
        $versions = Get-ChildItem -LiteralPath $folder -Directory |
            Where-Object { $_.Name -match '^\d+\.\d+\.\d+$' } |
            Sort-Object { [version]$_.Name } -Descending
        foreach ($version in $versions) {
            if ((Test-Path (Join-Path $version.FullName 'apksigner.bat')) -and
                (Test-Path (Join-Path $version.FullName 'zipalign.exe'))) { return $version.FullName }
        }
    }
    throw 'Informe -AndroidSdk com um SDK que contenha build-tools, zipalign e apksigner.'
}

New-Item -ItemType Directory -Force $toolDir | Out-Null
if ($Action -eq 'Setup') {
    if (!(Test-Path -LiteralPath $jar)) {
        $temporary = "$jar.download"
        Invoke-WebRequest 'https://github.com/iBotPeaches/Apktool/releases/download/v3.0.3/apktool_3.0.3.jar' -OutFile $temporary
        Assert-Hash $temporary $jarHash
        Move-Item -LiteralPath $temporary -Destination $jar
    }
    Assert-Hash $jar $jarHash
    Write-Host "Apktool 3.0.3 validado em $jar"
    return
}

$jdk = Resolve-Java
$java = Join-Path $jdk 'bin/java.exe'
if ($Action -eq 'Decode') {
    Assert-Hash $apk $apkHash
    Assert-Hash $jar $jarHash
    if (Test-Path -LiteralPath $decoded) { throw 'decoded já existe. Preserve suas edições; renomeie a pasta antes de decodificar novamente.' }
    Invoke-Checked $java @('-Xmx4g', '-jar', $jar, 'decode', $apk, '--output', $decoded, '--jobs', "$Jobs", '--frame-path', (Join-Path $toolDir 'framework'))
    return
}

$buildTools = Resolve-BuildTools
# apksigner.bat usa JAVA_HOME somente nesta execução.
$previousJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = $jdk
    if ($Action -eq 'Verify') {
        $signed = Join-Path $dist 'Duolingo-base-editable.apk'
        if (!(Test-Path -LiteralPath $signed)) { throw 'Execute Build antes de Verify.' }
        Invoke-Checked (Join-Path $buildTools 'zipalign.exe') @('-c', '-P', '16', '-v', '4', $signed)
        Invoke-Checked (Join-Path $buildTools 'apksigner.bat') @('verify', '--verbose', '--print-certs', $signed)
        return
    }
    Assert-Hash $jar $jarHash
    if (!(Test-Path (Join-Path $decoded 'apktool.yml'))) { throw 'Execute Decode primeiro.' }
    $overrides = Join-Path $repo 'overrides'
    Get-ChildItem -LiteralPath $overrides -Recurse -File | ForEach-Object {
        $relative = $_.FullName.Substring($overrides.Length + 1)
        if ($relative -eq 'README.md') { return }
        if ($relative -notmatch '^(res|assets|lib|unknown|smali[^\\/]*|kotlin)[\\/]' -and
            $relative -notin @('AndroidManifest.xml', 'apktool.yml')) {
            throw "Caminho não permitido em overrides: $relative"
        }
        $destination = Join-Path $decoded $relative
        New-Item -ItemType Directory -Force (Split-Path $destination -Parent) | Out-Null
        Copy-Item -LiteralPath $_.FullName -Destination $destination
    }
    New-Item -ItemType Directory -Force $dist | Out-Null
    $unsigned = Join-Path $dist 'Duolingo-unsigned.apk'
    $aligned = Join-Path $dist 'Duolingo-aligned.apk'
    $signed = Join-Path $dist 'Duolingo-base-editable.apk'
    Invoke-Checked $java @('-Xmx4g', '-jar', $jar, 'build', $decoded, '--output', $unsigned, '--jobs', "$Jobs", '--frame-path', (Join-Path $toolDir 'framework'))
    Invoke-Checked (Join-Path $buildTools 'zipalign.exe') @('-f', '-P', '16', '4', $unsigned, $aligned)
    $keystore = Join-Path $toolDir 'development.keystore'
    if (!(Test-Path -LiteralPath $keystore)) {
        Invoke-Checked (Join-Path $jdk 'bin/keytool.exe') @('-genkeypair', '-keystore', $keystore, '-storepass', 'android', '-keypass', 'android', '-alias', 'local-development', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '3650', '-dname', 'CN=Local APK Development', '-noprompt')
    }
    Invoke-Checked (Join-Path $buildTools 'apksigner.bat') @('sign', '--ks', $keystore, '--ks-key-alias', 'local-development', '--ks-pass', 'pass:android', '--key-pass', 'pass:android', '--out', $signed, $aligned)
    Invoke-Checked (Join-Path $buildTools 'apksigner.bat') @('verify', '--verbose', '--print-certs', $signed)
    $hash = (Get-FileHash -LiteralPath $signed -Algorithm SHA256).Hash.ToLowerInvariant()
    Write-Host "APK gerado: $signed`nSHA-256: $hash"
    Write-Warning 'Este é somente o APK base. Ele declara exigir splits de ABI e densidade ausentes no material fornecido. Assinatura válida não comprova instalação ou execução.'
} finally {
    $env:JAVA_HOME = $previousJavaHome
}
