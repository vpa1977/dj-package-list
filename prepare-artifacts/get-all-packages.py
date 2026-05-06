#!/usr/bin/env python3
"""
Ubuntu Upload Monitor
Tracks package uploads to the Ubuntu main archive since the last successful execution.
"""

import argparse
import json
import yaml
import os
import sys
import tarfile
import tempfile
import urllib.request
from datetime import datetime, timezone, timedelta
from launchpadlib.launchpad import Launchpad

try:
    from debian import deb822
except ImportError:
    deb822 = None

STATE_FILE = '.state.json'
CACHE_DIR = os.path.expanduser('~/.launchpadlib/cache/')

def load_state():
    """Load the last run timestamp from the state file."""
    if os.path.exists(STATE_FILE):
        try:
            with open(STATE_FILE, 'r') as f:
                return json.load(f)
        except (json.JSONDecodeError, IOError):
            pass
    return {}

def save_state(state):
    """Save the current run timestamp to the state file."""
    with open(STATE_FILE, 'w') as f:
        json.dump(state, f, indent=2)

def parse_depends(depends_str):
    """
    Parse a dependency string like 'libc6 (>= 2.15), base-files'
    and return a list of package names.
    """
    if not depends_str:
        return []
    packages = []
    # Split by comma and clean up components
    for part in depends_str.split(','):
        # Take the first word and strip any trailing/leading symbols/parens
        part = part.strip()
        if len(part) == 0:
            continue
        name = part.split()[0].split('<')[0].split('(')[0].rstrip(':').strip()
        if name:
            packages.append(name)
    return packages

def get_depends(lp, package_name):
    """
    Retrieves forward dependencies of a source package from Launchpad by
    downloading its Debian control file.

    Returns a dict: { 'source': { name: [build-depends] }, 'binaries': { name: [depends] } }
    """
    if deb822 is None:
        raise ImportError("The 'python-debian' package is required for this function.")

    ubuntu = lp.distributions['ubuntu']
    archive = ubuntu.main_archive
    try:
        sources = archive.getPublishedSources(source_name=package_name, status='Published', exact_match=True)
    except Exception:
        return None

    if not sources:
        return None

    # Take the most recent published version
    source = sources[0]
    urls = source.sourceFileUrls()
    target_url = None
    # Prioritize specialized Debian tarballs
    for url in urls:
        if any(url.endswith(ext) for ext in ['.debian.tar.xz', '.debian.tar.gz', '.diff.gz']):
            target_url = url
            break

    if not target_url:
        return None

    result = {'source': {}, 'binaries': {}}

    with tempfile.TemporaryDirectory() as tmpdir:
        dest_path = os.path.join(tmpdir, "package_source.tar")
        try:
            urllib.request.urlretrieve(target_url, dest_path)

            with tarfile.open(dest_path) as tar:
                # Search for debian/control within the archive
                control_content = None
                for member in tar.getmembers():
                    if member.name.endswith('debian/control'):
                        f = tar.extractfile(member)
                        if f:
                            control_content = f.read().decode('utf-8', errors='ignore')
                            break

                if control_content:
                    for i, section in enumerate(deb822.Packages.iter_paragraphs(control_content)):
                        if i == 0:
                            # Source paragraph
                            src_name = section.get('Source', package_name)
                            bd = section.get('Build-Depends')
                            result['source'][src_name] = parse_depends(bd)
                        else:
                            # Binary paragraphs
                            bin_name = section.get('Package')
                            deps = section.get('Depends')
                            if bin_name:
                                result['binaries'][bin_name] = parse_depends(deps)
        except Exception as e:
            print(f"Error processing {target_url}: {e}", file=sys.stderr)
            return None

    return result


def dump_depends_yaml(package_name, depends):
    """
    Dump the result of get_depends() as a YAML snippet:

    package-name:
      source:
        - build-depend1
      binaries:
        - binary-name:
          - depend1
    """
    source_list = []
    for deps in depends.get('source', {}).values():
        source_list.extend(deps)

    binaries_list = []
    for bin_name, deps in depends.get('binaries', {}).items():
        binaries_list.append({bin_name: deps})

    snippet = {
        package_name: {
            'source': source_list,
            'binaries': binaries_list,
        }
    }
    print(yaml.dump(snippet, default_flow_style=False, allow_unicode=True), end='')


def main():
    parser = argparse.ArgumentParser(description="Monitor Ubuntu uploads since last run.")
    parser.add_argument('--since', help="Timestamp to start monitoring from (ISO 8601 format, e.g., 2026-04-19T00:00:00Z)")
    parser.add_argument('--no-save', action='store_true', help="Display results without updating the last run timestamp.")
    parser.add_argument('--check-deps', help="Fetch and display dependencies for a specific package name.")
    args = parser.parse_args()

    state = load_state()
    last_run = state.get('last_run')

    # Determine start time
    if args.since:
        start_time = args.since
    elif last_run:
        start_time = last_run
    else:
        # Default for first run: 1/1/1970
        dt = datetime(1970, 1, 1, 0, 0, 0, tzinfo=timezone.utc)
        start_time = dt.strftime('%Y-%m-%dT%H:%M:%SZ')
        print(f"First run detected. Monitoring since {start_time}.")
        print("Note: You can specify a starting timestamp using --since 'YYYY-MM-DDTHH:MM:SSZ'")

    print(f"Connecting to Launchpad and fetching uploads since {start_time}...")

    try:
        # Authentication: login_with will prompt via browser if no credentials found
        lp = Launchpad.login_with('ubuntu-upload-monitor', 'production', CACHE_DIR, version='devel')

        ubuntu = lp.distributions['ubuntu']
        archive = ubuntu.main_archive

        # Retrieve published sources
        sources = archive.getPublishedSources(created_since_date=start_time, status='Published')

        for source in sources:
            dump_depends_yaml(source.source_package_name, get_depends(lp, source.source_package_name))

        if not args.no_save:
            # We record the current time to be used as start_time for the next run
            state['last_run'] = datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
            save_state(state)
            print(f"State saved. Next run will start from {state['last_run']}.")

    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
