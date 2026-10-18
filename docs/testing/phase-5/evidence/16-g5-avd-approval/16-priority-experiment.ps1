$ErrorActionPreference = 'Stop'
$evidence = Join-Path $PWD 'docs\testing\phase-5\evidence\16-g5-avd-approval'
$target = Get-Process -Id 18996
$metadata = Get-CimInstance -ClassName Win32_Process -Filter 'ProcessId = 18996'
$sdkEmulator = 'C:\Users\CHI NGUYEN\AppData\Local\Android\Sdk\emulator\'
if (-not $target.Path.StartsWith($sdkEmulator, [StringComparison]::OrdinalIgnoreCase) -or $metadata.CommandLine -notmatch 'Medium_Phone') {
    throw 'Target must be the verified Medium_Phone emulator process'
}
$original = $target.PriorityClass
$record = [ordered]@{ProcessId=$target.Id;Process=$target.ProcessName;OriginalPriority=$original.ToString();Experiment='AboveNormal';Restored=$false;Scope='This AVD process only; no global Windows settings or app thresholds changed'}
$record | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $evidence '16-priority-environment.json')
try {
    $target.PriorityClass = [Diagnostics.ProcessPriorityClass]::AboveNormal
    $target.Refresh()
    if ($target.PriorityClass -ne [Diagnostics.ProcessPriorityClass]::AboveNormal) { throw 'Priority change failed' }
    $log = Join-Path $evidence '16-priority-batch.txt'
    'COMMAND: py -3.13 -X utf8 docs/testing/phase-5/evidence/run_android.py --output docs/testing/phase-5/evidence/18-g5-priority-cohort --classes StopLatencyTest,CadenceTest' | Set-Content -LiteralPath $log
    & py -3.13 -X utf8 docs/testing/phase-5/evidence/run_android.py --output docs/testing/phase-5/evidence/18-g5-priority-cohort --classes StopLatencyTest,CadenceTest 2>&1 | Tee-Object -FilePath $log -Append
    $result = $LASTEXITCODE
    "EXIT_CODE: $result" | Tee-Object -FilePath $log -Append
    if ($result -ne 0) { throw "Instrumentation failed ($result)" }
} finally {
    $target.Refresh()
    if (-not $target.HasExited) {
        $target.PriorityClass = $original
        $target.Refresh()
        $record['Restored'] = $target.PriorityClass -eq $original
    }
    $record | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $evidence '16-priority-environment.json')
}
