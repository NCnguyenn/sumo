$ErrorActionPreference = 'Stop'
$evidence = Join-Path $PWD 'docs\testing\phase-5\evidence\16-g5-avd-approval'
$target = Get-Process -Id 18996
$metadata = Get-CimInstance -ClassName Win32_Process -Filter 'ProcessId = 18996'
if ($metadata.CommandLine -notmatch 'Medium_Phone' -or $target.ProcessName -ne 'qemu-system-x86_64-headless') { throw 'Wrong emulator process' }
Add-Type -TypeDefinition @'
using System;
using System.ComponentModel;
using System.Diagnostics;
using System.Runtime.InteropServices;
public static class SumoPowerExperiment {
    [StructLayout(LayoutKind.Sequential)] public struct State { public uint Version; public uint ControlMask; public uint StateMask; }
    [DllImport("kernel32.dll", SetLastError=true)] private static extern bool GetProcessInformation(IntPtr p,int c,ref State s,uint n);
    [DllImport("kernel32.dll", SetLastError=true)] private static extern bool SetProcessInformation(IntPtr p,int c,ref State s,uint n);
    public static State Read(int pid) {
        using(var p=Process.GetProcessById(pid)) {var s=new State{Version=1};if(!GetProcessInformation(p.Handle,4,ref s,12))throw new Win32Exception(Marshal.GetLastWin32Error());return s;}
    }
    public static void Write(int pid, State s) {
        using(var p=Process.GetProcessById(pid)) {if(!SetProcessInformation(p.Handle,4,ref s,12))throw new Win32Exception(Marshal.GetLastWin32Error());}
    }
}
'@
$original = [SumoPowerExperiment]::Read($target.Id)
$record = [ordered]@{ProcessId=$target.Id;Original=$original;Applied=$null;Restored=$false;Scope='Only QEMU power throttling; priority stays Normal; no system-wide timer or power-plan changes'}
try {
    $desired = [SumoPowerExperiment+State]::new()
    $desired.Version=1; $desired.ControlMask=5; $desired.StateMask=0
    [SumoPowerExperiment]::Write($target.Id,$desired)
    $record['Applied'] = [SumoPowerExperiment]::Read($target.Id)
    $record | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $evidence '24-power-environment.json')
    $log = Join-Path $evidence '24-power-batch.txt'
    'COMMAND: py -3.13 -X utf8 docs/testing/phase-5/evidence/run_android.py --output docs/testing/phase-5/evidence/21-g5-power-cohort --classes StopLatencyTest,CadenceTest' | Set-Content -LiteralPath $log
    & py -3.13 -X utf8 docs/testing/phase-5/evidence/run_android.py --output docs/testing/phase-5/evidence/21-g5-power-cohort --classes StopLatencyTest,CadenceTest 2>&1 | Tee-Object -FilePath $log -Append
    $result=$LASTEXITCODE
    "EXIT_CODE: $result" | Tee-Object -FilePath $log -Append
    if($result -ne 0){throw "Instrumentation failed ($result)"}
} finally {
    [SumoPowerExperiment]::Write($target.Id,$original)
    $restored=[SumoPowerExperiment]::Read($target.Id)
    $record['Restored']=($restored.ControlMask -eq $original.ControlMask -and $restored.StateMask -eq $original.StateMask)
    $record | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $evidence '24-power-environment.json')
}
