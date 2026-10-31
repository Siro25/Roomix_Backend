"""Read-only Git review. Prints paths/categories, never secret values.

Run with the Face runtime Python. By default inspect backend and sibling Roomix.
This supplements review; it cannot prove that every possible secret is absent.
"""
import argparse
import json
from pathlib import Path
import re
import subprocess


FACE = Path(__file__).resolve().parents[1]


def git(root, *args):
    return subprocess.check_output(["git", "-C", str(root), *args])


def local_secrets(roots):
    values = set()
    for root in roots:
        for directory in (root, root / "core-service", root / "face-auth-service"):
            for path in directory.glob(".env*"):
                if not path.is_file() or path.name.endswith(".example"):
                    continue
                for line in path.read_text(encoding="utf-8-sig").splitlines():
                    key, sep, value = line.partition("=")
                    if sep and re.search(r"SECRET|TOKEN|PASSWORD|API_KEY", key):
                        value = value.strip().strip("\"'")
                        if len(value) >= 12 and not value.startswith("${"):
                            values.add(value.encode())
    return values


def review(root, secrets):
    names = set(filter(None, git(root, "ls-files", "--cached", "--others",
                                "--exclude-standard", "-z").decode().split("\0")))
    findings = []
    media = []
    signatures = {
        "telegram-token": rb"\b[0-9]{7,12}:AA[A-Za-z0-9_-]{30,}\b",
        "private-key": rb"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----",
        "google-api-key": rb"\bAIza[0-9A-Za-z_-]{30,}\b",
    }
    # Review the index separately: a clean working copy can still hide a secret
    # in a previously staged version of the same file.
    staged = git(root, "ls-files", "--stage", "-z").decode().split("\0")
    process = subprocess.Popen(["git", "-C", str(root), "cat-file", "--batch"],
                               stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    try:
        for entry in filter(None, staged):
            metadata, name = entry.split("\t", 1)
            oid = metadata.split()[1]
            process.stdin.write((oid + "\n").encode())
            process.stdin.flush()
            header = process.stdout.readline().split()
            content = process.stdout.read(int(header[2]))
            process.stdout.read(1)
            if any(value in content for value in secrets):
                findings.append({"path": name, "kind": "staged-known-local-secret"})
            for kind, pattern in signatures.items():
                if re.search(pattern, content):
                    findings.append({"path": name, "kind": "staged-" + kind})
    finally:
        process.stdin.close()
        process.wait()
    generated = {"node_modules", ".next", ".tmp", "__pycache__", "target"}
    private_suffixes = {".pkl", ".pickle", ".npy", ".npz", ".db", ".sqlite",
                        ".sqlite3", ".pt", ".pth", ".task", ".dump", ".backup"}
    for name in sorted(names):
        path = root / name
        if not path.is_file():
            continue
        parts = Path(name).parts
        if (any(p in generated or p.startswith(".venv") for p in parts)
                or (path.name.startswith(".env") and path.name != ".env.example")
                or path.suffix.lower() in private_suffixes):
            findings.append({"path": name, "kind": "private-or-generated-file"})
        content = path.read_bytes()
        if any(value in content for value in secrets):
            findings.append({"path": name, "kind": "known-local-secret"})
        for kind, pattern in signatures.items():
            if re.search(pattern, content):
                findings.append({"path": name, "kind": kind})
        if path.suffix.lower() in {".jpg", ".jpeg", ".png", ".webp", ".mp4", ".zip"}:
            media.append(name)
        if re.search(rb"(?m)^(?:<{7}|>{7}) ", content):
            findings.append({"path": name, "kind": "merge-marker"})
    checks = []
    for name in (".env", ".env.local", ".env.production", ".tmp/private.txt",
                 "node_modules/private.txt", ".env.example"):
        ignored = subprocess.run(["git", "-C", str(root), "check-ignore", "-q", name]).returncode == 0
        checks.append({"path": name, "ignored": ignored})
    for check in checks:
        expected = check["path"] != ".env.example"
        if check["ignored"] != expected:
            findings.append({"path": check["path"], "kind": "ignore-rule-mismatch"})
    return {"root": str(root), "candidate_count": len(names), "findings": findings,
            "media_for_manual_review": media, "ignore_checks": checks,
            "changes": git(root, "status", "--short").decode().splitlines()}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=FACE / ".tmp" / "final-git-review.json")
    args = parser.parse_args()
    roots = [FACE.parent, FACE.parent.parent / "Roomix"]
    report = [review(root, local_secrets(roots)) for root in roots]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    for item in report:
        print(Path(item["root"]).name, "candidates=", item["candidate_count"],
              "findings=", len(item["findings"]), "media=", len(item["media_for_manual_review"]))
        for finding in item["findings"]:
            print(finding["kind"], finding["path"])
    return int(any(item["findings"] for item in report))


if __name__ == "__main__":
    raise SystemExit(main())
