param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [int]$Minutes = 30
)

$ErrorActionPreference = 'Stop'
$samples = [Math]::Ceiling(($Minutes * 60) / 30)
$started = Get-Date
Write-Output "SCREEN_OFF_START=$($started.ToString('o')) SERIAL=$Serial MINUTES=$Minutes"
& adb -s $Serial shell input keyevent 26 | Out-Null

$failures = 0
for ($i = 0; $i -le $samples; $i++) {
    $dump = (& adb -s $Serial shell dumpsys media_session) -join "`n"
    $state = if ($dump -match 'state=PlaybackState \{state=([A-Z]+)') { $Matches[1] } else { 'MISSING' }
    $position = if ($dump -match 'position=([0-9]+)') { $Matches[1] } else { '-1' }
    $title = if ($dump -match 'description=([^,\r\n]+)') { $Matches[1].Trim() } else { 'UNKNOWN' }
    if ($state -ne 'PLAYING') { $failures++ }
    Write-Output "SAMPLE=$i TIME=$((Get-Date).ToString('o')) STATE=$state POSITION_MS=$position TITLE=$title"
    if ($i -lt $samples) { Start-Sleep -Seconds 30 }
}

$ended = Get-Date
Write-Output "SCREEN_OFF_END=$($ended.ToString('o')) FAILURES=$failures"
if ($failures -gt 0) { exit 1 }
