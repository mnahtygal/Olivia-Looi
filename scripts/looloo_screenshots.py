#!/usr/bin/env python3
"""Local ADB capture driver. Standard library only; never creates placeholder PNGs."""
import argparse
import datetime as dt
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
INVENTORY = ROOT / 'app/src/androidTest/assets/looloo_screenshots.json'
PACKAGE = 'com.nahtygal.olivialooi'
PROFILES = ('tablet', 'phone', 'emulator')
CATEGORIES = ('home', 'games', 'learn', 'music', 'stories', 'all')


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


def write_manifests(output, entries):
    (output / 'manifest.json').write_text(json.dumps({'schema_version': 1, 'entries': entries}, indent=2) + '\n')
    captured = [r for r in entries if r['status'] == 'captured']
    (output / 'showcase_order.txt').write_text(''.join(r['filename'] + '\n' for r in captured))
    (output / 'showcase_manifest.json').write_text(json.dumps([
        {k: r[k] for k in ('filename', 'title', 'duration_seconds')} | {'transition': 'gentle fade'} for r in captured
    ], indent=2) + '\n')


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
        return run([adb, '-s', serial, *parts], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=not binary).stdout
    pending = {r['id']: {'status': 'blocked', 'notes': 'Capture not started or setup failed'} for r in rows}
    write_manifests(output, manifest_entries(rows, pending, {
        'device_serial': serial, 'device_model': None, 'app_version': None, 'app_label': 'LooLoo',
        'orientation': 'portrait', 'profile': args.profile, 'timestamp': dt.datetime.now(dt.timezone.utc).isoformat(),
    }, args.profile))
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
    # Ensure a failed new run cannot accidentally reuse old device or host images.
    command('shell', 'run-as', PACKAGE, 'rm', '-rf', 'files/looloo-capture')
    for row in rows:
        (output / args.profile / row['filename']).unlink(missing_ok=True)
    instrumentation = subprocess.run([adb, '-s', serial, 'shell', 'am', 'instrument', '-w', '-r',
        '-e', 'class', PACKAGE + '.screenshots.ShowcaseCaptureTest', '-e', 'category', args.category,
        '-e', 'utilities', str(args.include_utilities).lower(), runner], capture_output=True, text=True)
    # Raw instrumentation can contain device paths; keep it local, never in PNGs.
    (output / 'instrumentation.log').write_text(instrumentation.stdout + instrumentation.stderr)
    try:
        results = json.loads(command('exec-out', 'run-as', PACKAGE, 'cat', 'files/looloo-capture/results.json'))
    except (subprocess.CalledProcessError, json.JSONDecodeError):
        results = {}
    entries = manifest_entries(rows, results, metadata, args.profile)
    for entry, row in zip(entries, rows):
        if entry['status'] != 'captured':
            continue
        try:
            data = command('exec-out', 'run-as', PACKAGE, 'cat', 'files/looloo-capture/' + row['filename'], binary=True)
            if not data.startswith(b'\x89PNG\r\n\x1a\n'):
                raise ValueError('Device output was not a PNG')
            (output / args.profile / row['filename']).write_bytes(data)
        except (subprocess.CalledProcessError, ValueError):
            entry.update(status='failed', notes='PNG export failed')
    write_manifests(output, entries)
    counts = {s: sum(r['status'] == s for r in entries) for s in ('captured', 'skipped', 'failed')}
    print(f"Capture complete: {counts['captured']} captured, {counts['skipped']} skipped, {counts['failed']} failed. Output: {output}")
    return 1 if counts['failed'] or instrumentation.returncode or 'FAILURES!!!' in instrumentation.stdout else 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        print(f'Capture stopped: {error}', file=sys.stderr)
        sys.exit(1)
