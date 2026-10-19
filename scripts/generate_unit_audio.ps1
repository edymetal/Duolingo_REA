param([string]$Voice = 'Microsoft Zira Desktop', [string]$UnitRoot = '', [string]$ReportName = 'unit-01-audio.json')
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$audioRoot = if($UnitRoot) { $UnitRoot } else { Join-Path $repo 'content/en-pt/generated/unit-01' }
$manifest = Get-Content (Join-Path $audioRoot 'audio-manifest.json') -Raw -Encoding utf8 | ConvertFrom-Json
Add-Type -AssemblyName System.Speech
$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
try {
    $installed = @($synth.GetInstalledVoices() | Where-Object { $_.Enabled -and $_.VoiceInfo.Name -eq $Voice -and $_.VoiceInfo.Culture.Name -like 'en-*' })
    if ($installed.Count -ne 1) { throw 'A voz inglesa solicitada não está instalada. Informe -Voice com uma voz inglesa local.' }
    $synth.SelectVoice($Voice)
    $synth.Rate = -2
    $format = New-Object System.Speech.AudioFormat.SpeechAudioFormatInfo(22050, [System.Speech.AudioFormat.AudioBitsPerSample]::Sixteen, [System.Speech.AudioFormat.AudioChannel]::Mono)
    $generated = @()
    foreach ($entry in $manifest) {
        if ($entry.path -notmatch '^audio/prompt_[a-f0-9]+\.wav$') { throw 'Caminho inesperado no manifesto de áudio' }
        $output = Join-Path $audioRoot $entry.path
        New-Item -ItemType Directory -Force (Split-Path $output -Parent) | Out-Null
        $synth.SetOutputToWaveFile($output, $format)
        $synth.Speak([string]$entry.text)
        $synth.SetOutputToNull()
        $generated += [ordered]@{ id = $entry.id; file = $entry.path; sha256 = (Get-FileHash -LiteralPath $output).Hash.ToLowerInvariant(); bytes = (Get-Item -LiteralPath $output).Length }
    }
    [ordered]@{ engine = 'Windows System.Speech'; voice = $Voice; culture = $installed[0].VoiceInfo.Culture.Name; rate = -2; sample_rate = 22050; network_service_used = $false; files = $generated } | ConvertTo-Json -Depth 6 | Set-Content (Join-Path $repo "reports/$ReportName") -Encoding utf8
    Write-Output "$($generated.Count) áudios locais gerados."
} finally { $synth.Dispose() }
