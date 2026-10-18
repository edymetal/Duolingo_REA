"""Inventário estático local complementar ao REA. Não executa o aplicativo."""
import hashlib
import json
import re
import zipfile
from collections import Counter
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'


def inspect():
    apk = ROOT / 'APK' / 'Duolingo.apk'
    decoded = ROOT / 'decoded'
    manifest = ET.parse(decoded / 'AndroidManifest.xml').getroot()
    application = manifest.find('application')
    sdk = manifest.find('uses-sdk')
    config = (decoded / 'apktool.yml').read_text(encoding='utf-8')
    def config_value(name):
        match = re.search(r'^\s+' + re.escape(name) + r':\s*(.+)$', config, re.M)
        return match.group(1).strip().strip("\"'") if match else None
    with zipfile.ZipFile(apk) as archive:
        entries = archive.infolist()
        dex = [e.filename for e in entries if re.fullmatch(r'classes\d*\.dex', e.filename)]
        libraries = [e.filename for e in entries if e.filename.startswith('lib/')]
        assets = [e.filename for e in entries if e.filename.startswith('assets/')]
        by_extension = Counter(Path(e.filename).suffix or '(sem extensão)' for e in entries)
    components = {}
    for kind in ('activity', 'activity-alias', 'service', 'receiver', 'provider'):
        components[kind] = [node.get(ANDROID + 'name') for node in application.findall(kind)]
    launchers = []
    for node in application:
        for intent in node.findall('intent-filter'):
            actions = [a.get(ANDROID + 'name') for a in intent.findall('action')]
            categories = [a.get(ANDROID + 'name') for a in intent.findall('category')]
            if 'android.intent.action.MAIN' in actions and 'android.intent.category.LAUNCHER' in categories:
                launchers.append(node.get(ANDROID + 'name'))
    smali_counts = {p.name: sum(1 for _ in p.rglob('*.smali')) for p in sorted(decoded.glob('smali*')) if p.is_dir()}
    namespaces = Counter()
    for folder in sorted(decoded.glob('smali*')):
        app_path = folder / 'com' / 'duolingo'
        if app_path.exists():
            for path in app_path.rglob('*.smali'):
                relative = path.relative_to(app_path)
                namespaces[relative.parts[0] if len(relative.parts) > 1 else '(raiz)'] += 1
    result = {
        'source': 'Python zipfile + AndroidManifest.xml decodificado pelo Apktool; não é evidência REA',
        'apk_sha256': hashlib.file_digest(apk.open('rb'), 'sha256').hexdigest(),
        'package': manifest.get('package'),
        'version_name': manifest.get(ANDROID + 'versionName') or config_value('versionName'),
        'version_code': manifest.get(ANDROID + 'versionCode') or config_value('versionCode'),
        'sdk': {'minSdkVersion': config_value('minSdkVersion'), 'targetSdkVersion': config_value('targetSdkVersion')} if sdk is None else {k.removeprefix(ANDROID): v for k, v in sdk.attrib.items()},
        'required_split_types': manifest.get(ANDROID + 'requiredSplitTypes'),
        'requires_splits': any(n.get(ANDROID + 'name') == 'com.android.vending.splits.required' and n.get(ANDROID + 'value') == 'true' for n in application.findall('meta-data')),
        'application_class': application.get(ANDROID + 'name'),
        'application_label': application.get(ANDROID + 'label'),
        'launchers': launchers,
        'permissions': [n.get(ANDROID + 'name') for n in manifest.findall('uses-permission')],
        'components': components,
        'archive_entries': len(entries),
        'dex_files': dex,
        'native_libraries': libraries,
        'asset_count': len(assets),
        'asset_sample': assets[:40],
        'extensions': dict(by_extension.most_common()),
        'smali_classes': smali_counts,
        'app_namespaces': dict(namespaces.most_common()),
        'limitations': [
            'Nomes de classes e recursos não comprovam funcionamento de telas.',
            'Código Smali é bytecode desmontado; não é o código-fonte original.',
            'Não inspeciona backend, conteúdo remoto, execução ou assinatura original.',
            'Apktool pode mover versão e SDK para apktool.yml; consulte esse arquivo se os campos forem nulos.'
        ]
    }
    destination = ROOT / 'reports' / 'apk-inventory.json'
    destination.write_text(json.dumps(result, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(json.dumps({key: result[key] for key in ('package', 'version_name', 'sdk', 'required_split_types', 'archive_entries', 'smali_classes')}, indent=2, ensure_ascii=False))


if __name__ == '__main__':
    inspect()
