"""Prepara a prática das referências, runtimes e modelo de fala locais."""
import hashlib
import json
import shutil
import zipfile
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def prepare():
    cache = ROOT / '.tools/activity-deps'
    cache.mkdir(parents=True, exist_ok=True)
    lock = json.loads((ROOT / 'scripts/activity-dependencies.json').read_text())
    for entry in lock:
        path = cache / entry['file']
        if not path.exists():
            urllib.request.urlretrieve(entry['url'], path)
        if hashlib.sha256(path.read_bytes()).hexdigest() != entry['sha256']:
            raise ValueError('Hash incorreto: ' + entry['file'])
    for name in ['vosk', 'jna']:
        with zipfile.ZipFile(cache / (name + '.aar')) as archive:
            archive.extractall(cache / name)
    if not (cache / 'vosk-model-small-en-us-0.15/am/final.mdl').exists():
        with zipfile.ZipFile(cache / 'model.zip') as archive:
            archive.extractall(cache)
    target = ROOT / 'android-app/build/visual-assets'
    reference = json.loads((ROOT / 'content/en-pt/reference-activities.json').read_text(encoding='utf-8'))
    for q in reference['exercises']:
        q['audio'] = 'audio/prompt_' + hashlib.sha256(q['english'].encode()).hexdigest()[:16] + '.wav'
    (target / 'reference-activities.json').write_text(json.dumps(reference, ensure_ascii=False), encoding='utf-8')
    (target / 'audio').mkdir(exist_ok=True)
    audio = ROOT / 'content/en-pt/generated/reference-audio'
    audio.mkdir(exist_ok=True)
    manifest = [{'id': q['id'], 'text': q['english'], 'path': q['audio']} for q in reference['exercises']]
    (audio / 'audio-manifest.json').write_text(json.dumps(manifest, ensure_ascii=False), encoding='utf-8')
    for wav in audio.glob('audio/*.wav'):
        shutil.copyfile(wav, target / 'audio' / wav.name)
    for name in ['rive.js', 'rive.wasm']:
        shutil.copyfile(cache / name, target / name)
    characters = {'oscar': 'oscar_inlesson_v01_02.riv', 'lily': 'lily_inlesson_v02_19.riv', 'zari': 'zari_inlesson_v01_04.riv'}
    for name, filename in characters.items():
        shutil.copyfile(ROOT / 'decoded/res/raw' / filename, target / (name + '.riv'))
    inventory = [{'source': 'decoded/res/raw/' + filename, 'sha256': hashlib.sha256((target / (name + '.riv')).read_bytes()).hexdigest()} for name, filename in characters.items()]
    (ROOT / 'reports/activity-assets.json').write_text(json.dumps({'characters': inventory, 'runtimes': lock, 'illustrations': 'Vetores locais em IllustrationView.java, desenhados a partir das referências'}, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    shutil.copytree(ROOT / 'android-app/assets', target, dirs_exist_ok=True)
    model = cache / 'vosk-model-small-en-us-0.15'
    shutil.copytree(model, target / 'speech-model', dirs_exist_ok=True)
    native = ROOT / 'android-app/build/native'
    for dependency in ['vosk', 'jna']:
        shutil.copytree(cache / dependency / 'jni', native, dirs_exist_ok=True)
    print('Prática, personagens animados e modelo de fala preparados.')


if __name__ == '__main__':
    prepare()
