$ErrorActionPreference = 'Stop'
$workspace = 'C:\Users\CHI NGUYEN\AndroidStudioProjects\IoT'
$evidence = Join-Path $workspace 'docs\testing\phase-4\evidence\34-completion'
$results = Join-Path $evidence '03-results'
$adb = 'C:\Users\CHI NGUYEN\AppData\Local\Android\Sdk\platform-tools\adb.exe'
$python = 'C:\Users\CHI NGUYEN\python.exe'
$commandLog = Join-Path $evidence '03-one-acceptance-batch.txt'
$simulator = $null
$logcat = $null
New-Item -ItemType Directory -Path $results -Force | Out-Null
function Invoke-Recorded([string]$program, [string[]]$arguments) {
    "COMMAND: $program $($arguments -join ' ')" | Add-Content -LiteralPath $commandLog
    & $program @arguments 2>&1 | Tee-Object -FilePath $commandLog -Append
    $code = $LASTEXITCODE
    "EXIT_CODE: $code" | Add-Content -LiteralPath $commandLog
    if ($code -ne 0) { throw "Command failed: $program ($code)" }
}
try {
    Invoke-Recorded $adb @('-s','emulator-5554','get-state')
    Invoke-Recorded $adb @('-s','emulator-5554','install','-r',(Join-Path $workspace 'app\build\outputs\apk\debug\app-debug.apk'))
    Invoke-Recorded $adb @('-s','emulator-5554','install','-r',(Join-Path $workspace 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk'))
    $simulatorScript = Join-Path $workspace 'tools\sumo_simulator.py'
    $simulatorLog = Join-Path $results 'simulator.jsonl'
    $simulatorArgs = @('-u',('"'+$simulatorScript+'"'),'--host','127.0.0.1','--port','8888','--scenario','normal','--log',('"'+$simulatorLog+'"'))
    "START: $python $($simulatorArgs -join ' ')" | Add-Content -LiteralPath $commandLog
    $simulator = Start-Process -FilePath $python -ArgumentList $simulatorArgs -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $results 'simulator.stdout.txt') -RedirectStandardError (Join-Path $results 'simulator.stderr.txt')
    $readyUntil = [DateTime]::UtcNow.AddSeconds(5)
    do {
        if ($simulator.HasExited) { throw 'Simulator exited before READY' }
        if ((Get-Content -LiteralPath (Join-Path $results 'simulator.stdout.txt') -Raw -ErrorAction SilentlyContinue) -match 'READY 8888') { break }
        Start-Sleep -Milliseconds 100
    } while ([DateTime]::UtcNow -lt $readyUntil)
    if ((Get-Content -LiteralPath (Join-Path $results 'simulator.stdout.txt') -Raw) -notmatch 'READY 8888') { throw 'Simulator READY timeout' }
    $logcatArgs = @('-s','emulator-5554','logcat','-T','1','-v','monotonic','-s','SumoSession:I')
    "START: $adb $($logcatArgs -join ' ')" | Add-Content -LiteralPath $commandLog
    $logcat = Start-Process -FilePath $adb -ArgumentList $logcatArgs -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $results 'logcat-StopLatencyTest-CadenceTest.txt') -RedirectStandardError (Join-Path $results 'logcat.stderr.txt')
    Invoke-Recorded $adb @('-s','emulator-5554','shell','am','instrument','-w','-r','-e','class','com.aistudy.myapplication.StopLatencyTest,com.aistudy.myapplication.CadenceTest','-e','cadenceTargets','50','com.aistudy.myapplication.test/androidx.test.runner.AndroidJUnitRunner')
    if ((Get-Content -LiteralPath $commandLog -Raw) -notmatch 'OK \(2 tests\)') { throw 'Instrumentation did not report exactly two passing tests' }
} finally {
    foreach ($process in @($logcat,$simulator)) {
        if ($null -ne $process -and -not $process.HasExited) {
            Stop-Process -Id $process.Id
            "CLEANUP: stopped own process $($process.Id)" | Add-Content -LiteralPath $commandLog
        }
    }
}
