import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

MODULE = Path(__file__).resolve().parents[1] / 'looloo_screenshots.py'
spec = importlib.util.spec_from_file_location('capture', MODULE)
capture = importlib.util.module_from_spec(spec)
spec.loader.exec_module(capture)


class ScreenshotTests(unittest.TestCase):
    def test_inventory_has_65_unique_ids(self):
        rows = capture.inventory()
        self.assertEqual(65, len(rows))
        self.assertEqual(65, len({r['id'] for r in rows}))

    def test_unique_deterministic_filenames(self):
        rows = capture.inventory()
        self.assertEqual(65, len({r['filename'] for r in rows}))
        for i, r in enumerate(rows, 1):
            self.assertEqual(f"{i:02d}_{r['id']}.png", r['filename'])

    def test_titles_are_nonblank(self):
        self.assertTrue(all(r['title'].strip() for r in capture.inventory()))

    def test_category_filter(self):
        selected = []
        for category in capture.CATEGORIES[:-1]:
            rows = capture.select_entries(category)
            self.assertTrue(rows)
            self.assertTrue(all(r['category'] == category for r in rows))
            selected.extend(r['id'] for r in rows)
        self.assertEqual(65, len(set(selected)))
        self.assertEqual(65, len(capture.select_entries('all')))

    def test_unknown_category(self):
        with self.assertRaises(ValueError):
            capture.select_entries('private')

    def test_profiles(self):
        for profile in capture.PROFILES:
            self.assertEqual(profile, capture.parser().parse_args(['--profile', profile]).profile)

    def test_unknown_profile(self):
        with self.assertRaises(SystemExit):
            capture.parser().parse_args(['--profile', 'unknown'])

    def test_multiple_physical_devices_refuse_guessing(self):
        with self.assertRaisesRegex(ValueError, 'Multiple'):
            capture.select_device('List of devices attached\na device\nb device\n')

    def test_explicit_device_and_unavailable_states(self):
        self.assertEqual('b', capture.select_device('a device\nb device', 'b'))
        for state in ('offline', 'unauthorized'):
            with self.assertRaises(ValueError):
                capture.select_device(f'b {state}', 'b')
        with self.assertRaises(ValueError):
            capture.select_device('', 'missing')

    def test_auto_selection_prefers_one_physical_and_supports_emulator(self):
        self.assertEqual('a', capture.select_device('a device\nemulator-5554 device'))
        self.assertEqual('emulator-5554', capture.select_device('emulator-5554 device'))
        with self.assertRaises(ValueError):
            capture.select_device('emulator-5554 device\nemulator-5556 device')

    def test_manifest_matches_ids_and_marks_missing_results_failed(self):
        rows = capture.select_entries('home')
        entries = capture.manifest_entries(rows, {rows[0]['id']: {'status': 'captured'}}, {}, 'tablet')
        self.assertEqual([r['id'] for r in rows], [r['screen_id'] for r in entries])
        self.assertEqual('captured', entries[0]['status'])
        self.assertTrue(all(e['status'] == 'failed' for e in entries[1:]))

    def test_unknown_device_results_rejected(self):
        with self.assertRaises(ValueError):
            capture.manifest_entries(capture.inventory(), {'unknown': {}}, {}, 'phone')

    def test_showcase_only_contains_captured_known_screens(self):
        rows = capture.inventory()
        entries = capture.manifest_entries(rows, {rows[0]['id']: {'status': 'captured'}}, {}, 'tablet')
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory)
            capture.write_manifests(output, entries)
            self.assertEqual([f"tablet/{rows[0]['filename']}"], (output / 'showcase_order.txt').read_text().splitlines())
            self.assertEqual(65, len(json.loads((output / 'manifest.json').read_text())['entries']))

    def test_utilities_require_opt_in(self):
        self.assertEqual({'apps_main', 'settings_main'}, {r['id'] for r in capture.inventory() if not r['public_default']})
        self.assertFalse(capture.parser().parse_args([]).include_utilities)

    def test_no_production_dependency_on_capture_tooling(self):
        for source in (capture.ROOT / 'app/src/main').rglob('*.kt'):
            self.assertNotIn('import com.nahtygal.olivialooi.screenshots', source.read_text())
        self.assertNotIn('CaptureActivity', (capture.ROOT / 'app/src/main/AndroidManifest.xml').read_text())

    def test_no_network_libraries_in_tooling(self):
        sources = [MODULE] + list((capture.ROOT / 'app/src/androidTest/java/com/nahtygal/olivialooi/screenshots').glob('*.kt'))
        for source in sources:
            text = source.read_text()
            for forbidden in ('import requests', 'import urllib', 'import okhttp', 'import retrofit', 'import java.net'):
                self.assertNotIn(forbidden, text)

    def test_preflight_failure_records_blocked_inventory_without_pngs(self):
        with tempfile.TemporaryDirectory() as directory:
            with patch.object(capture, 'find_adb', side_effect=ValueError('unavailable')):
                with self.assertRaises(ValueError):
                    capture.main(['--output', directory])
            output = Path(directory)
            rows = json.loads((output / 'manifest.json').read_text())['entries']
            self.assertEqual(65, len(rows))
            self.assertTrue(all(r['status'] == 'blocked' for r in rows))
            self.assertFalse(list(output.rglob('*.png')))
            self.assertEqual('', (output / 'showcase_order.txt').read_text())

    def test_unauthorized_physical_device_does_not_silently_choose_emulator(self):
        with self.assertRaises(ValueError):
            capture.select_device('phone unauthorized\nemulator-5554 device')
        with self.assertRaisesRegex(ValueError, 'Multiple'):
            capture.select_device('phone unauthorized\nother device')

    def test_shell_strict_mode(self):
        self.assertIn('set -euo pipefail', (capture.ROOT / 'scripts/capture_looloo_screenshots.sh').read_text())

    def test_capture_does_not_wait_for_physical_audio_initialization(self):
        source = (capture.ROOT / 'app/src/androidTest/java/com/nahtygal/olivialooi/screenshots/ShowcaseCaptureTest.kt').read_text()
        self.assertNotIn('textPresent("Getting the")', source)
        self.assertNotIn('captureStage = "audio_ready"', source)


if __name__ == '__main__':
    unittest.main()
