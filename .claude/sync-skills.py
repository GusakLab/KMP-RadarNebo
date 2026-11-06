#!/usr/bin/env python3
"""Copy the project's agent skills from a pinned dot-agents commit, or check that the copies match it.

Run from anywhere; paths resolve relative to this file. Needs git and Python 3.10+, nothing else.

    python3 .claude/sync-skills.py           # replace .claude/skills, .claude/licenses and THIRD_PARTY_SKILLS.md
    python3 .claude/sync-skills.py --check   # change nothing; exit 1 if any copy differs from the pinned commit

Each skill must hash to the sha256 that dot-agents' catalog.json records for it at the pinned commit, so a copy can
only come from content that is committed and public. To update: change COMMIT (and SKILLS), run the sync, review
the diff, commit.
"""
import hashlib
import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

REPO_URL = 'https://github.com/w0rxbend/dot-agents'
COMMIT = '5b5c6d7a7981ab2ae94d1ef4c431057694eb8e43'
SKILLS = (
    'andrej-karpathy', 'api-and-interface-design', 'architecture-patterns', 'caveman', 'caveman-review',
    'clean-code-guard', 'code-complete', 'code-review', 'code-review-and-quality', 'code-simplification',
    'database-design', 'debugging-and-error-recovery', 'design-patterns', 'designing-apis', 'documentation-and-adrs',
    'dockerfile-optimise', 'git-workflow-and-versioning', 'github-actions-advanced', 'github-actions-debugger',
    'gof-patterns', 'grasp-knowledge', 'incremental-implementation', 'investigate-first', 'kotlin-coroutines',
    'kotlin-dsl-patterns', 'kotlin-null-safety', 'mobile-design', 'planning-and-task-breakdown', 'ponytail',
    'ponytail-audit', 'ponytail-debt', 'ponytail-review', 'refactoring-guru', 'security-and-hardening',
    'source-driven-development', 'spec-driven-development', 'surgical-patch', 'test-driven-development',
    'testing-strategies', 'verify-and-stop',
)
# The same names dot-agents' scripts/common.py skips when it hashes a skill folder.
IGNORED = {'.git', '.trash', 'node_modules', '__pycache__', '.venv', '.DS_Store', '.usage.json', '.usage.json.lock'}
# Left out of each copy, after the full upstream folder is verified: a skill's root README.md is the upstream
# project's guide to installing it globally on a developer's machine. No skill links to it, and agents load SKILL.md.
EXCLUDED_ROOT_FILES = ('README.md',)

CLAUDE_DIR = Path(__file__).resolve().parent
DOC_NAME = 'THIRD_PARTY_SKILLS.md'


def tree_hash(root: Path) -> str:
    """dot-agents' folder hash: every file's relative path and bytes, in sorted order."""
    digest = hashlib.sha256()
    for path in sorted(root.rglob('*')):
        if any(part in IGNORED for part in path.relative_to(root).parts) or not path.is_file():
            continue
        digest.update(path.relative_to(root).as_posix().encode())
        digest.update(b'\0')
        digest.update(path.read_bytes())
        digest.update(b'\0')
    return digest.hexdigest()


def fetch(into: Path) -> Path:
    repo = into / 'dot-agents'
    subprocess.run(['git', 'clone', '--quiet', '--no-checkout', REPO_URL, str(repo)], check=True)
    subprocess.run(['git', '-C', str(repo), '-c', 'advice.detachedHead=false', 'checkout', '--quiet', COMMIT],
                   check=True)
    return repo


def catalog_entries(repo: Path) -> list[dict]:
    by_name = {entry['name']: entry for entry in json.loads((repo / 'catalog.json').read_text())['skills']}
    missing = [name for name in SKILLS if name not in by_name]
    if missing:
        sys.exit(f'Not in dot-agents catalog.json at {COMMIT}: {", ".join(missing)}')
    entries = [by_name[name] for name in SKILLS]
    for entry in entries:
        if entry['distribution'] != 'vendored':
            sys.exit(f"{entry['name']}: not vendored in dot-agents, so its content is not in the repository")
        actual = tree_hash(repo / entry['path'])
        if actual != entry['sha256']:
            sys.exit(f"{entry['name']}: tree hash {actual} differs from catalog.json's {entry['sha256']} at {COMMIT}")
    return entries


def stage(repo: Path, entries: list[dict], into: Path) -> Path:
    """Builds the exact .claude content (skills, licenses, doc) under `into`."""
    shutil.copytree(repo / 'licenses' / 'upstream', into / 'licenses', dirs_exist_ok=True,
                    ignore=lambda _, names: [n for n in names if n not in {Path(e['license_file']).name
                                                                          for e in entries}])
    for entry in entries:
        skill = into / 'skills' / entry['name']
        shutil.copytree(repo / entry['path'], skill, ignore=shutil.ignore_patterns(*IGNORED))
        for name in EXCLUDED_ROOT_FILES:
            (skill / name).unlink(missing_ok=True)
    license_index = json.loads((repo / 'licenses' / 'upstream' / 'index.json').read_text())
    (into / DOC_NAME).write_text(render_doc(entries, license_index))
    return into


def render_doc(entries: list[dict], license_index: dict) -> str:
    tree = f'{REPO_URL}/tree/{COMMIT}'
    lines = [
        '# Third-party agent skills',
        '',
        f'The skills in [`skills/`](skills/) are copied unchanged from [w0rxbend/dot-agents]({REPO_URL}) at commit',
        f'[`{COMMIT[:7]}`]({tree}), except that each skill\'s root `README.md` is left out: it is the upstream',
        "project's guide to a global install on a developer's machine, and no skill links to it. dot-agents vendors",
        'the skills from the upstream repositories below. Each skill keeps its upstream license; the license texts are',
        'in [`licenses/`](licenses/).',
        '',
        f"`Catalog SHA-256` is the hash that dot-agents' [`catalog.json`]({REPO_URL}/blob/{COMMIT}/catalog.json)",
        f'records for the skill at that commit, computed by [`tree_hash`]({REPO_URL}/blob/{COMMIT}/scripts/common.py).',
        '`sync-skills.py` checks every upstream folder against it before copying, and refuses a mismatch.',
        '',
        'Do not edit a copied skill here. Change it upstream or in dot-agents, then sync from the new commit.',
        '',
        '## Sync and check',
        '',
        'From the repository root, with git and Python 3.10 or later:',
        '',
        '```sh',
        'python3 .claude/sync-skills.py --check   # verify the copies against the pinned commit; changes nothing',
        'python3 .claude/sync-skills.py           # re-copy skills, licenses and this file from the pinned commit',
        '```',
        '',
        'To update the skills, change `COMMIT` (and `SKILLS`) in `sync-skills.py`, run the sync, review the diff and',
        'commit it. This file is generated by the sync: do not edit it by hand.',
        '',
        '## Skills',
        '',
        '| Skill | Upstream | License | Catalog SHA-256 |',
        '| --- | --- | --- | --- |',
    ]
    for entry in entries:
        source = entry['source']
        lines.append(f"| `{entry['name']}` | [{source}](https://github.com/{source}) | {entry['license']} "
                     f"| `{entry['sha256'][:16]}` |")
    lines += ['', '## Licenses', '', '| Upstream | License | Text |', '| --- | --- | --- |']
    sources = {entry['source']: entry for entry in entries}
    for source in sorted(sources, key=str.lower):
        entry = sources[source]
        url = license_index.get(source, {}).get('html_url', f'https://github.com/{source}')
        file_name = Path(entry['license_file']).name
        lines.append(f"| [{source}]({url}) | {entry['license']} | [`licenses/{file_name}`](licenses/{file_name}) |")
    if any(entry['license'] == 'FSL-1.1-ALv2' for entry in entries):
        lines += [
            '',
            'FSL-1.1-ALv2 (Functional Source License) allows use, copying and modification for any purpose except a',
            'competing commercial product, and requires every copy to carry the license text.',
        ]
    return '\n'.join(lines) + '\n'


def differences(staged: Path) -> list[str]:
    problems = []
    for kind in ('skills', 'licenses'):
        expected = {p.name for p in (staged / kind).iterdir()}
        actual = {p.name for p in (CLAUDE_DIR / kind).iterdir()} if (CLAUDE_DIR / kind).is_dir() else set()
        problems += [f'{kind}/{name}: missing' for name in sorted(expected - actual)]
        problems += [f'{kind}/{name}: not from the pinned commit' for name in sorted(actual - expected)]
        for name in sorted(expected & actual):
            staged_path, actual_path = staged / kind / name, CLAUDE_DIR / kind / name
            same = (tree_hash(staged_path) == tree_hash(actual_path) if staged_path.is_dir()
                    else staged_path.read_bytes() == actual_path.read_bytes())
            if not same:
                problems.append(f'{kind}/{name}: differs from the pinned commit')
    doc = CLAUDE_DIR / DOC_NAME
    if not doc.is_file() or doc.read_text() != (staged / DOC_NAME).read_text():
        problems.append(f'{DOC_NAME}: out of date')
    return problems


def replace_with(staged: Path) -> None:
    for kind in ('skills', 'licenses'):
        shutil.rmtree(CLAUDE_DIR / kind, ignore_errors=True)
        shutil.copytree(staged / kind, CLAUDE_DIR / kind)
    shutil.copyfile(staged / DOC_NAME, CLAUDE_DIR / DOC_NAME)


def main() -> None:
    check_only = '--check' in sys.argv[1:]
    with tempfile.TemporaryDirectory() as tmp:
        repo = fetch(Path(tmp))
        staged = stage(repo, catalog_entries(repo), Path(tmp) / 'staged')
        if check_only:
            problems = differences(staged)
            for problem in problems:
                print(problem)
            print(f'{len(SKILLS)} skills match dot-agents {COMMIT[:7]}.' if not problems else 'Run the sync.')
            sys.exit(1 if problems else 0)
        replace_with(staged)
        print(f'Copied {len(SKILLS)} skills from dot-agents {COMMIT[:7]}.')


if __name__ == '__main__':
    main()
