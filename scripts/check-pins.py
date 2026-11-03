#!/usr/bin/env python3
"""Checks every pinned indicator and domain-list source against the newest commit that touched its file.

    scripts/check-pins.py            # report only
    scripts/check-pins.py --apply    # move outdated pins forward and refresh the bundled copies

The app only ever downloads a file from the commit pinned in assets/iocs/*.json and only if its SHA-256 matches.
Fresh indicators therefore reach users through new pins in a release, never through unpinned downloads. With
--apply the new commit and hash are written into the catalog, and for sources shipped inside the APK the asset
file is replaced; review the diff and run the unit tests (KnownGood conflicts, parsers) before committing.
Needs the GitHub CLI (`gh`), logged in, for the API calls.
"""
import hashlib
import json
import subprocess
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
IOCS = ROOT / "app/src/main/assets/iocs"
CATALOGS = ["sources.json", "domainlists.json"]
# Must match FeedCatalog.BUNDLED_REPOS: these files ship inside the APK.
BUNDLED = {
    "mvt-project/mvt-indicators", "AssoEchap/stalkerware-indicators", "eset/malware-ioc",
    "meta/threat-research", "Perflyst/PiHoleBlocklist", "StevenBlack/hosts",
}


def gh_api(path):
    out = subprocess.run(["gh", "api", path], check=True, capture_output=True, text=True).stdout
    return json.loads(out)


def blob(repo, path, ref):
    """Git blob id of the file at [ref]; equal ids mean equal bytes, so nothing has to be downloaded to compare."""
    try:
        return gh_api(f"repos/{repo}/contents/{path}?ref={ref}")["sha"]
    except subprocess.CalledProcessError:
        return None


def latest_commit(repo, path):
    commits = gh_api(f"repos/{repo}/commits?path={path}&per_page=1")
    if not commits:
        return None, None
    c = commits[0]
    return c["sha"], c["commit"]["committer"]["date"][:10]


def fetch(repo, commit, path):
    url = f"https://raw.githubusercontent.com/{repo}/{commit}/{path}"
    with urllib.request.urlopen(url, timeout=60) as r:
        return r.read()


def asset_name(source):
    # Same rule as FeedSource.fileName.
    ext = source["path"].rsplit(".", 1)[-1] if "." in source["path"] else "dat"
    return f'{source["id"]}.{ext}'


def main():
    apply = "--apply" in sys.argv
    outdated = 0
    for catalog in CATALOGS:
        file = IOCS / catalog
        text = file.read_text()
        sources = json.loads(text)
        indent = 1 if text.startswith("[\n {") else 2
        changed = False
        for s in sources:
            sha, date = latest_commit(s["repo"], s["path"])
            if sha is None:
                print(f'GONE     {s["id"]}: {s["path"]} no longer in {s["repo"]}')
                outdated += 1
                continue
            newest, pinned = blob(s["repo"], s["path"], sha), blob(s["repo"], s["path"], s["commit"])
            if newest is None or pinned is None:
                print(f'ERROR    {s["id"]}: could not read the file at both commits')
                outdated += 1
                continue
            if newest == pinned:
                print(f'current  {s["id"]}')
                continue
            outdated += 1
            print(f'NEWER    {s["id"]}: {s["commit"][:10]} -> {sha[:10]} ({date})')
            if not apply:
                continue
            data = fetch(s["repo"], sha, s["path"])
            s["commit"] = sha
            s["sha256"] = hashlib.sha256(data).hexdigest()
            if s["repo"] in BUNDLED:
                (IOCS / asset_name(s)).write_bytes(data)
            changed = True
        if changed:
            file.write_text(json.dumps(sources, indent=indent, ensure_ascii=False) + "\n")
    print(f"{outdated} outdated" + (" (applied, review the diff and run the tests)" if apply and outdated else ""))


if __name__ == "__main__":
    main()
