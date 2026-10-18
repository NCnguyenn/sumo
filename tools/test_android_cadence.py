"""Check the approved AVD exception without weakening the default G0 gate."""
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


class CadencePolicyTests(unittest.TestCase):
    def check_cohort(self, target, intervals_ns, *, avd_exception=False):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            lines = [f'BEGIN CADENCE{target}']
            timestamp = 0
            raw = b'V1:0123456789abcdef,1,0,0,0,0|S:153,153,0,0,0\n'.hex()
            for interval in [0, *intervals_ns]:
                timestamp += interval
                lines.append(f'wire t0_ns=-1 t1_ns={timestamp} t2_ns={timestamp+1} cause=periodic hex={raw}')
            lines.append(f'END CADENCE{target}')
            (folder / 'CadenceTest.txt').write_text('\n'.join(lines), encoding='utf-8')
            command = [sys.executable, str(Path(__file__).with_name('check_android_cadence.py')),
                       '--results', str(folder), '--output-prefix', str(folder / 'result'), '--targets', str(target)]
            if avd_exception:
                command.append('--g5-avd-50-jitter')
            result = subprocess.run(command, capture_output=True, text=True, timeout=10)
            summary = folder / 'result.json'
            return result, json.loads(summary.read_text()) if summary.exists() else None

    def test_default_keeps_strict_50ms_upper_bound(self):
        result, summary = self.check_cohort(50, [50_000_001] * 1000)
        self.assertEqual(1, result.returncode, result.stderr)
        self.assertEqual(1000, summary['measurements'][0]['above_50'])

    def test_approved_50ms_exception_accepts_60ms_and_retains_strict_violations(self):
        result, summary = self.check_cohort(50, [50_000_000] * 999 + [60_000_000], avd_exception=True)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual('G5_AVD_50_PLUS_10', summary['policy'])
        row = summary['measurements'][0]
        self.assertEqual(1000, row['n'])
        self.assertEqual(60, row['upper_limit_ms'])
        self.assertEqual(1, row['above_50'])
        self.assertEqual(0, row['above_limit'])

    def test_exception_rejects_even_one_sample_above_60ms(self):
        result, _ = self.check_cohort(50, [50_000_000] * 999 + [60_000_001], avd_exception=True)
        self.assertEqual(1, result.returncode, result.stderr)

    def test_exception_keeps_20ms_floor(self):
        result, _ = self.check_cohort(50, [50_000_000] * 999 + [19_999_999], avd_exception=True)
        self.assertEqual(1, result.returncode, result.stderr)

    def test_exception_does_not_change_20_or_30ms_gates(self):
        for target in (20, 30):
            with self.subTest(target=target):
                result, summary = self.check_cohort(target, [50_000_001] * 1000, avd_exception=True)
                self.assertEqual(1, result.returncode, result.stderr)
                self.assertEqual(50, summary['measurements'][0]['upper_limit_ms'])

    def test_exception_keeps_1000_sample_minimum(self):
        result, _ = self.check_cohort(50, [50_000_000] * 999, avd_exception=True)
        self.assertEqual(1, result.returncode, result.stderr)


if __name__ == '__main__':
    unittest.main(verbosity=2)
