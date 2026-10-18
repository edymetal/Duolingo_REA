"""Recorta uma unidade editorial sem modificar ou reclassificar o corpus original."""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def prepare():
    plan = json.loads((ROOT / 'content/en-pt/unit-01-plan.json').read_text(encoding='utf-8'))
    corpus = ROOT / 'content/en-pt/generated/staple.en-pt.jsonl'
    wanted = {prompt for lesson in plan['lessons'] for prompt in lesson['prompts']}
    found = {}
    with corpus.open(encoding='utf-8') as reader:
        for line in reader:
            record = json.loads(line)
            if record['prompt'] in wanted:
                if record['prompt'] in found:
                    raise ValueError('Enunciado duplicado no corpus selecionado')
                found[record['prompt']] = record
    if set(found) != wanted:
        raise ValueError('Frases ausentes no corpus: ' + repr(wanted - set(found)))
    unit = {key: value for key, value in plan.items() if key != 'lessons'}
    unit.update({
        'schema_version': 1,
        'source': 'STAPLE 2020 / Duolingo / Harvard Dataverse V6',
        'source_url': 'https://doi.org/10.7910/DVN/38OJR6',
        'license': 'CC BY-NC 4.0',
        'audio_origin': 'Síntese local de voz; não são gravações originais do Duolingo.',
        'lessons': []
    })
    for lesson in plan['lessons']:
        result = {key: value for key, value in lesson.items() if key != 'prompts'}
        result['exercises'] = []
        for index, prompt in enumerate(lesson['prompts']):
            source = found[prompt]
            answers = source['accepted_translations']
            primary = max(answers, key=lambda a: a['weight'])['text']
            result['exercises'].append({
                'id': source['id'],
                'type': ['translation', 'translation', 'listening', 'listening', 'word_order', 'word_order'][index],
                'english': prompt,
                'portuguese': primary,
                'accepted_translations': answers,
                'audio': 'audio/' + source['id'] + '.wav',
                'source_split': source['dataset_split']
            })
        unit['lessons'].append(result)
    destination = ROOT / 'content/en-pt/generated/unit-01'
    destination.mkdir(parents=True, exist_ok=True)
    target = destination / 'unit.json'
    target.write_text(json.dumps(unit, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    manifest = [{'id': e['id'], 'text': e['english'], 'path': e['audio']} for l in unit['lessons'] for e in l['exercises']]
    (destination / 'audio-manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'lessons': len(unit['lessons']), 'exercises': len(manifest), 'unit_sha256': hashlib.sha256(target.read_bytes()).hexdigest()}, indent=2))


if __name__ == '__main__':
    prepare()
