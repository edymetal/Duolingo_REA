"""Importa somente en_pt do arquivo oficial STAPLE obtido pelo fluxo autorizado."""
import argparse
import hashlib
import io
import json
import math
import re
import tarfile
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EXPECTED_MD5 = 'e48db7b891a4a737eeb2512af6505c7c'
EXPECTED_SHA256 = '17a830b95b0738353ae65d83ab4010f0be57e99e656443f63b34e63265d578fe'
DATASET_URL = 'https://doi.org/10.7910/DVN/38OJR6'


def records(lines, split):
    current = None
    for number, raw in enumerate(lines, 1):
        line = raw.rstrip('\r\n')
        if not line.strip():
            continue
        if line.startswith('prompt_'):
            identity, separator, prompt = line.partition('|')
            if not separator or not re.fullmatch(r'prompt_[0-9a-f]+', identity) or not prompt.strip():
                raise ValueError(f'{split}:{number}: cabeçalho de exercício inválido')
            if current is not None:
                if not current['accepted_translations']:
                    raise ValueError(f'{split}:{number}: exercício sem respostas')
                yield current
            current = {
                'id': identity,
                'dataset_split': split,
                'source_language': 'en',
                'target_language': 'pt',
                'prompt': prompt,
                'accepted_translations': [],
                'source': 'STAPLE 2020 / Harvard Dataverse V6',
                'license': 'CC BY-NC 4.0',
                'curriculum_unit': None,
                'cefr_level': None,
                'audio': None
            }
        else:
            if current is None or '|' not in line:
                raise ValueError(f'{split}:{number}: resposta sem cabeçalho ou peso')
            text, weight_text = line.rsplit('|', 1)
            weight = float(weight_text)
            if not text.strip() or not math.isfinite(weight) or not 0 <= weight <= 1:
                raise ValueError(f'{split}:{number}: resposta ou peso inválido')
            current['accepted_translations'].append({'text': text, 'weight': weight})
    if current is not None:
        if not current['accepted_translations']:
            raise ValueError(f'{split}: último exercício sem respostas')
        yield current


def import_corpus(archive_path, destination):
    with archive_path.open('rb') as handle:
        md5 = hashlib.file_digest(handle, 'md5').hexdigest()
    if md5 != EXPECTED_MD5:
        raise ValueError('O MD5 do arquivo não coincide com o catálogo oficial selecionado')
    with archive_path.open('rb') as handle:
        sha256 = hashlib.file_digest(handle, 'sha256').hexdigest()
    if sha256 != EXPECTED_SHA256:
        raise ValueError('O SHA-256 não coincide com o arquivo de referência verificado')
    destination.mkdir(parents=True, exist_ok=True)
    output = destination / 'staple.en-pt.jsonl'
    temporary = output.with_suffix('.jsonl.tmp')
    seen_ids = set()
    seen_prompts = set()
    counts = {}
    with tarfile.open(archive_path, 'r:gz') as archive, temporary.open('w', encoding='utf-8', newline='\n') as writer:
        # Leitura direta de membros selecionados: não extrai caminhos do TAR.
        selected = [m for m in archive.getmembers() if m.isfile() and re.fullmatch(r'staple-2020/en_pt/(train|dev|test)\.en_pt\.[0-9-]+\.gold\.txt', m.name)]
        split_counts = Counter(m.name.split('/')[-1].split('.')[0] for m in selected)
        if split_counts != Counter({'train': 1, 'dev': 1, 'test': 1}):
            raise ValueError('O arquivo deve conter exatamente um gold por conjunto en_pt')
        for member in sorted(selected, key=lambda m: m.name):
            split = member.name.split('/')[-1].split('.')[0]
            prompt_count = 0
            translation_count = 0
            with io.TextIOWrapper(archive.extractfile(member), encoding='utf-8') as lines:
                for record in records(lines, split):
                    if record['id'] in seen_ids:
                        raise ValueError('Identificador de exercício repetido no corpus')
                    seen_ids.add(record['id'])
                    seen_prompts.add(record['prompt'])
                    prompt_count += 1
                    translation_count += len(record['accepted_translations'])
                    writer.write(json.dumps(record, ensure_ascii=False, separators=(',', ':')) + '\n')
            counts[split] = {'prompts': prompt_count, 'accepted_translations': translation_count, 'member': member.name}
    if {key: value['prompts'] for key, value in counts.items()} != {'train': 4000, 'dev': 500, 'test': 500}:
        raise ValueError('Quantidade de enunciados diferente da publicação oficial')
    temporary.replace(output)
    report = {
        'dataset': 'STAPLE 2020',
        'dataset_url': DATASET_URL,
        'catalog_version': '6.0',
        'citation': 'Duolingo, 2020, Data for the 2020 Duolingo Shared Task on Simultaneous Translation And Paraphrase for Language Education (STAPLE), Harvard Dataverse, V6. doi:10.7910/DVN/38OJR6',
        'license': 'CC BY-NC 4.0',
        'license_url': 'https://creativecommons.org/licenses/by-nc/4.0/',
        'archive_md5': md5,
        'archive_sha256': sha256,
        'published_md5_matches': True,
        'track': 'en_pt',
        'splits': counts,
        'total_prompts': sum(x['prompts'] for x in counts.values()),
        'unique_prompt_texts': len(seen_prompts),
        'total_accepted_translations': sum(x['accepted_translations'] for x in counts.values()),
        'output': str(output.relative_to(ROOT)) if output.is_relative_to(ROOT) else str(output),
        'output_sha256': hashlib.file_digest(output.open('rb'), 'sha256').hexdigest(),
        'transformations': ['Selecionados apenas os arquivos gold en_pt; baselines e outros idiomas excluídos', 'Preservados enunciados, variantes, pesos e divisão train/dev/test', 'Convertido formato de blocos para JSONL; pesos convertidos em números'],
        'limitations': ['Amostra histórica; não representa o curso atual completo', 'Sem associação validada a unidades ou níveis CEFR', 'Sem áudios e sem dados da conta do usuário', 'Não é um curso executável; conteúdo ainda precisa de curadoria pedagógica']
    }
    (ROOT / 'reports' / 'staple-import.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    attribution = '# Origem do conteúdo\n\n' + report['citation'] + '\n\n' + DATASET_URL + '\n\nLicença declarada: CC BY-NC 4.0\nhttps://creativecommons.org/licenses/by-nc/4.0/\n\n' + '\n'.join(report['transformations']) + '\n'
    (destination / 'ATTRIBUTION.md').write_text(attribution, encoding='utf-8')
    print(json.dumps({key: report[key] for key in ('total_prompts', 'total_accepted_translations', 'unique_prompt_texts', 'output')}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--archive', type=Path, default=ROOT / '.tools/datasets/staple-2020-full-data.tar.gz')
    parser.add_argument('--output', type=Path, default=ROOT / 'content/en-pt/generated')
    args = parser.parse_args()
    import_corpus(args.archive.resolve(), args.output.resolve())
