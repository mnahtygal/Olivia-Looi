#!/usr/bin/env python3
"""Local ADB capture driver. Standard library only; never creates placeholder PNGs."""
import argparse
import datetime as dt
import json
import os
import re
from pathlib import Path
import shutil
import signal
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
INVENTORY = ROOT / 'app/src/androidTest/assets/looloo_screenshots.json'
PACKAGE = 'com.nahtygal.olivialooi'
TEST_PACKAGE = PACKAGE + '.test'
INSTRUMENTATION_EMPTY_ACTIVITY = 'androidx.test.core.app.InstrumentationActivityInvoker$EmptyActivity'
PROFILES = ('tablet', 'phone', 'emulator')
CATEGORIES = ('home', 'games', 'learn', 'music', 'stories', 'all')
INSTALL_TIMEOUT_SECONDS = 120


def sanitize_public(value, limit=240):
    """Keep diagnostics useful without exporting paths, control characters, or traces."""
    value = str(value or '')
    value = re.sub(r'[/\\][^\s:]+', '<path>', value)
    value = re.sub(r'[\r\n\t]+', ' ', value)
    value = ''.join(ch if ch.isprintable() else '?' for ch in value)
    return value.strip()[:limit]


def inventory():
    rows = json.loads(INVENTORY.read_text())
    for key in ('id', 'filename'):
        if len({r[key] for r in rows}) != len(rows):
            raise ValueError(f'Duplicate inventory {key}')
    if len(rows) != 65 or any(not r['title'].strip() for r in rows):
        raise ValueError('Expected 65 titled inventory entries')
    return rows


def select_entries(category):
    if category not in CATEGORIES:
        raise ValueError('Unknown category')
    return [r for r in inventory() if category == 'all' or r['category'] == category]


def select_device(output, requested=None):
    devices = {}
    for line in output.splitlines():
        parts = line.split()
        if len(parts) >= 2 and not line.startswith(('List ', '*')):
            devices[parts[0]] = parts[1]
    if requested:
        if devices.get(requested) != 'device':
            raise ValueError('Requested device is missing, offline, or unauthorized; check adb devices -l')
        return requested
    physical = [s for s in devices if not s.startswith('emulator-')]
    if len(physical) == 1:
        if devices[physical[0]] != 'device':
            raise ValueError('Physical device is offline or unauthorized; unlock it and authorize USB debugging')
        return physical[0]
    if len(physical) > 1:
        raise ValueError('Multiple physical devices: pass --serial explicitly')
    emulators = [s for s, state in devices.items() if state == 'device' and s.startswith('emulator-')]
    if len(emulators) == 1:
        return emulators[0]
    raise ValueError('No unambiguous authorized device. Connect/unlock a device and pass --serial')


def parser():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--serial')
    p.add_argument('--profile', choices=PROFILES, default='tablet')
    p.add_argument('--output', type=Path, default=ROOT / 'build/looloo-screenshots')
    install = p.add_mutually_exclusive_group()
    install.add_argument('--install', dest='install', action='store_true')
    install.add_argument('--no-install', dest='install', action='store_false')
    p.set_defaults(install=False)
    p.add_argument('--category', choices=CATEGORIES, default='all')
    p.add_argument('--clean', action='store_true', help='Remove only known PNGs in the selected profile')
    p.add_argument('--include-utilities', action='store_true', help='Opt in to Apps and OS app-settings captures; review before publication')
    p.add_argument('--list', action='store_true', help='List inventory without accessing ADB')
    return p


def run(command, **kwargs):
    return subprocess.run(command, check=True, **kwargs)


def find_adb():
    candidates = [os.environ.get('ADB'), shutil.which('adb')]
    for key in ('ANDROID_SDK_ROOT', 'ANDROID_HOME'):
        if os.environ.get(key):
            candidates.append(str(Path(os.environ[key]) / 'platform-tools/adb'))
    props = ROOT / 'local.properties'
    if props.exists():
        for line in props.read_text().splitlines():
            if line.startswith('sdk.dir='):
                candidates.append(str(Path(line.split('=', 1)[1]) / 'platform-tools/adb'))
    for candidate in candidates:
        if candidate and Path(candidate).is_file():
            try:
                run([candidate, 'version'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
                return candidate
            except (OSError, subprocess.CalledProcessError):
                continue
    raise ValueError('No executable adb found. Set ADB to a platform-tools binary compatible with this host CPU.')


def manifest_entries(rows, device_results, metadata, profile):
    known = {r['id'] for r in rows}
    if set(device_results) - known:
        raise ValueError('Device returned unknown screen IDs')
    result = []
    for row in rows:
        captured = device_results.get(row['id'], {'status': 'failed', 'notes': 'No device result'})
        result.append({**row, **metadata, **captured, 'filename': f"{profile}/{row['filename']}", 'screen_id': row['id']})
    return result


def atomic_text(path, text):
    temporary = path.with_name(path.name + '.tmp')
    temporary.write_text(text)
    temporary.replace(path)


def write_manifests(output, entries):
    atomic_text(output / 'manifest.json', json.dumps({'schema_version': 1, 'entries': entries}, indent=2) + '\n')
    captured = [r for r in entries if r['status'] == 'captured']
    atomic_text(output / 'showcase_order.txt', ''.join(r['filename'] + '\n' for r in captured))
    atomic_text(output / 'showcase_manifest.json', json.dumps([
        {k: r[k] for k in ('filename', 'title', 'duration_seconds')} | {'transition': 'gentle fade'} for r in captured
    ], indent=2) + '\n')


def cleanup_device(command):
    """Best effort, bounded ADB calls; do not reboot or touch unrelated app processes."""
    errors = []
    for parts in (('shell', 'am', 'force-stop', PACKAGE),
                  ('shell', 'am', 'force-stop', TEST_PACKAGE)):
        try:
            command(*parts)
        except (OSError, subprocess.SubprocessError) as error:
            errors.append(type(error).__name__)
    try:
        recents = command('shell', 'dumpsys', 'activity', 'recents')
        for task_id in instrumentation_task_ids(recents):
            try:
                command('shell', 'am', 'stack', 'remove', task_id)
            except (OSError, subprocess.SubprocessError) as error:
                errors.append(type(error).__name__)
    except (OSError, subprocess.SubprocessError) as error:
        errors.append(type(error).__name__)
    for parts in (('shell', 'input', 'keyevent', 'KEYCODE_HOME'),
                  ('shell', 'am', 'start', '-a', 'android.intent.action.MAIN',
                   '-c', 'android.intent.category.HOME')):
        try:
            command(*parts)
        except (OSError, subprocess.SubprocessError) as error:
            errors.append(type(error).__name__)
    return errors


def instrumentation_task_ids(recents):
    """Find only ActivityScenario's test-package EmptyActivity tasks."""
    task_ids = set()
    blocks = re.split(r'(?=\* Recent #|Task\{)', recents or '')
    for block in blocks:
        if TEST_PACKAGE not in block or INSTRUMENTATION_EMPTY_ACTIVITY not in block:
            continue
        for pattern in (r'\btaskId=(\d+)\b', r'\bTask\{[^\n#]*#(\d+)\b'):
            match = re.search(pattern, block)
            if match:
                task_ids.add(match.group(1))
                break
    return sorted(task_ids, key=int)


def capture_batch(rows, metadata, profile, output, command, instrument, utilities=False):
    """A fresh Android instrumentation process per fixture contains fatal UI-thread crashes."""
    results = {r['id']: {'status': 'blocked', 'notes': 'Not attempted'} for r in rows}
    (output / 'logs').mkdir(parents=True, exist_ok=True)
    def flush():
        entries = manifest_entries(rows, results, metadata, profile)
        write_manifests(output, entries)
        return entries
    flush()
    for row in rows:
        (output / profile / row['filename']).unlink(missing_ok=True)
    for row in rows:
        screen_id = row['id']
        destination = output / profile / row['filename']
        temporary = destination.with_name(destination.name + '.tmp')
        record = {'status': 'failed', 'notes': f'{screen_id}: capture did not finish',
                  'log_file': f'logs/{screen_id}.log', 'detail_log': f'logs/{screen_id}.log'}
        try:
            destination.unlink(missing_ok=True)
            temporary.unlink(missing_ok=True)
            if not row['public_default'] and not utilities:
                record = {'status': 'skipped', 'notes': 'Utility screen excluded by default; opt in after privacy review'}
                continue
            # Clear only this fixture's device outputs. Earlier PNGs remain intact.
            command('shell', 'run-as', PACKAGE, 'rm', '-f', 'files/looloo-capture/results.json',
                    'files/looloo-capture/' + row['filename'])
            process = instrument(screen_id)
            log = (process.stdout or '') + (process.stderr or '')
            atomic_text(output / 'logs' / (screen_id + '.log'), log)
            crashed = process.returncode != 0 or any(marker in log for marker in (
                'FAILURES!!!', 'INSTRUMENTATION_FAILED', 'Process crashed', 'shortMsg=Process'))
            try:
                device = json.loads(command('exec-out', 'run-as', PACKAGE, 'cat', 'files/looloo-capture/results.json'))
            except (subprocess.SubprocessError, json.JSONDecodeError):
                device = {}
            # The test process writes the underlying exception to app-private storage;
            # retain it locally for diagnosis while keeping only its relative path public.
            try:
                detail = command('exec-out', 'run-as', PACKAGE, 'cat',
                                 'files/looloo-capture/logs/' + screen_id + '.log', binary=True)
                if detail:
                    (output / 'logs' / (screen_id + '.log')).write_bytes(
                        (log.encode() + b'\n--- device detail ---\n' + (detail if isinstance(detail, bytes) else detail.encode())))
            except (OSError, subprocess.SubprocessError):
                pass
            if not isinstance(device, dict) or set(device) - {screen_id}:
                raise ValueError('Unexpected fixture result IDs')
            reported = device.get(screen_id)
            if reported is not None:
                if not isinstance(reported, dict) or reported.get('status') not in ('captured', 'failed', 'skipped'):
                    raise ValueError('Malformed fixture status')
                record.update(reported)
            # A failing test still writes the fixture record before its final JUnit
            # assertion. Prefer that record: the assertion is only the process-level
            # summary and must not mask the underlying Android fixture exception.
            if reported is None:
                match = re.search(r'\b([A-Za-z][A-Za-z0-9]{0,90}(?:Exception|Error))\b', log)
                kind = match.group(1) if match else ('InstrumentationCrash' if crashed else 'MissingFixtureResult')
                message = sanitize_public(log.splitlines()[-1] if log.splitlines() else kind)
                record.update(status='failed', error_type=kind, original_error_type=kind,
                              error_message=message, original_error_message=message, capture_stage='instrumentation',
                              log_file=f'logs/{screen_id}.log',
                              detail_log=f'logs/{screen_id}.log',
                              notes=f'{screen_id}: {kind}; inspect the private local detail log')
            if record.get('status') == 'failed':
                record.setdefault('capture_stage', 'instrumentation_result')
                record.setdefault('original_error_type', record.get('error_type', 'InstrumentationReportedFailure'))
                record.setdefault('original_error_message', sanitize_public(record.get('notes', 'Fixture reported failure')))
                record.setdefault('error_type', record.get('original_error_type'))
                record.setdefault('error_message', record.get('original_error_message'))
                record.setdefault('log_file', f'logs/{screen_id}.log')
                record.setdefault('detail_log', f'logs/{screen_id}.log')
            if record['status'] == 'captured':
                data = command('exec-out', 'run-as', PACKAGE, 'cat',
                               'files/looloo-capture/' + row['filename'], binary=True)
                if not data.startswith(b'\x89PNG\r\n\x1a\n'):
                    raise ValueError('Device output was not a PNG')
                temporary.write_bytes(data)
                temporary.replace(destination)
        except KeyboardInterrupt:
            atomic_text(output / 'logs' / (screen_id + '.log'), 'Capture interrupted; device cleanup attempted.\n')
            record.update(status='failed', error_type='Interrupted', notes=f'{screen_id}: capture interrupted; later fixtures were not attempted')
            raise
        except (OSError, subprocess.SubprocessError, ValueError) as error:
            # Full details remain local; no raw paths, keys, or infrastructure enter the manifest.
            kind = type(error).__name__
            record.update(status='failed', error_type=kind, original_error_type=kind,
                          error_message=sanitize_public(str(error)), original_error_message=sanitize_public(str(error)), capture_stage='host_validation',
                          log_file=f'logs/{screen_id}.log',
                          detail_log=f'logs/{screen_id}.log',
                          notes=f'{screen_id}: {kind}; inspect the private local detail log')
            detail = str(error)
            if isinstance(error, subprocess.TimeoutExpired):
                detail = 'Instrumentation/ADB timed out. Device cleanup attempted.\n' + str(error.stdout or '') + str(error.stderr or '')
            atomic_text(output / 'logs' / (screen_id + '.log'), detail)
        finally:
            try:
                if record['status'] != 'captured':
                    destination.unlink(missing_ok=True)
                temporary.unlink(missing_ok=True)
                # Persist before cleanup too, in case a disconnected device stalls cleanup.
                results[screen_id] = record
                flush()
            finally:
                # Cleanup must still run if a host filesystem operation fails.
                errors = cleanup_device(command)
                record['cleanup'] = 'best_effort_failed' if errors else 'completed'
                if errors:
                    record['cleanup_errors'] = errors
            flush()
    return flush()


def main(argv=None):
    args = parser().parse_args(argv)
    rows = select_entries(args.category)
    if args.list:
        print('\n'.join(f"{r['filename']} [{r['category']}]" for r in rows))
        return 0
    output = args.output.resolve()
    for profile in PROFILES:
        (output / profile).mkdir(parents=True, exist_ok=True)
    try:
        adb = find_adb()
        serial = select_device(run([adb, 'devices', '-l'], stdout=subprocess.PIPE, text=True).stdout, args.serial)
    except (ValueError, OSError, subprocess.CalledProcessError):
        blocked = {r['id']: {'status': 'blocked', 'notes': 'ADB/device preflight failed; no screenshot captured'} for r in rows}
        write_manifests(output, manifest_entries(rows, blocked, {
            'device_serial': args.serial, 'device_model': None, 'app_version': None,
            'app_label': 'LooLoo', 'orientation': 'portrait', 'profile': args.profile,
            'timestamp': dt.datetime.now(dt.timezone.utc).isoformat(),
        }, args.profile))
        raise
    def command(*parts, binary=False):
        return run([adb, '-s', serial, *parts], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=not binary,
                   timeout=INSTALL_TIMEOUT_SECONDS if parts[0] == 'install' else 10).stdout
    pending = {r['id']: {'status': 'blocked', 'notes': 'Capture not started or setup failed'} for r in rows}
    write_manifests(output, manifest_entries(rows, pending, {
        'device_serial': serial, 'device_model': None, 'app_version': None, 'app_label': 'LooLoo',
        'orientation': 'portrait', 'profile': args.profile, 'timestamp': dt.datetime.now(dt.timezone.utc).isoformat(),
    }, args.profile))
    try:
        if args.clean:
            for row in inventory():
                (output / args.profile / row['filename']).unlink(missing_ok=True)
        if args.install:
            run([str(ROOT / 'gradlew'), 'assembleDebug', 'assembleDebugAndroidTest'], cwd=ROOT)
            for path in ('app/build/outputs/apk/debug/app-debug.apk', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'):
                command('install', '-r', str(ROOT / path))
        runners = command('shell', 'pm', 'list', 'instrumentation')
        runner = PACKAGE + '.test/androidx.test.runner.AndroidJUnitRunner'
        if runner not in runners:
            raise ValueError('Capture instrumentation is not installed; rerun with --install')
        metadata = {'device_serial': serial, 'device_model': command('shell', 'getprop', 'ro.product.model').strip(),
                    'orientation': 'portrait', 'timestamp': dt.datetime.now(dt.timezone.utc).isoformat(), 'profile': args.profile}
        cleanup_device(command)
        def instrument(screen_id):
            return subprocess.run([adb, '-s', serial, 'shell', 'am', 'instrument', '-w', '-r',
                '-e', 'class', PACKAGE + '.screenshots.ShowcaseCaptureTest#captureInventory',
                '-e', 'screen_id', screen_id, '-e', 'utilities', str(args.include_utilities).lower(), runner],
                capture_output=True, text=True, timeout=120)
        entries = capture_batch(rows, metadata, args.profile, output, command, instrument, args.include_utilities)
        counts = {s: sum(r['status'] == s for r in entries) for s in ('captured', 'skipped', 'failed')}
        print(f"Capture complete: {counts['captured']} captured, {counts['skipped']} skipped, {counts['failed']} failed. Output: {output}")
        return 1 if counts['failed'] else 0
    finally:
        cleanup_device(command)



if __name__ == '__main__':
    def interrupted(signum, frame):
        raise KeyboardInterrupt
    signal.signal(signal.SIGTERM, interrupted)
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print('Capture interrupted; cleanup attempted and partial manifest preserved.', file=sys.stderr)
        sys.exit(130)
    except (ValueError, OSError, subprocess.SubprocessError) as error:
        print(f'Capture stopped: {error}', file=sys.stderr)
        sys.exit(1)
