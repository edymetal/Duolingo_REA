"""Verifica preservação de classes DEX e assets; não comprova execução."""
import hashlib
import json
import re
import struct
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def dex_classes(data):
    if not data.startswith(b'dex\n'):
        raise ValueError('DEX inválido')
    def uint(offset):
        return struct.unpack_from('<I', data, offset)[0]
    string_count, string_offset = uint(56), uint(60)
    type_count, type_offset = uint(64), uint(68)
    count, offset = uint(96), uint(100)
    names = set()
    for index in range(count):
        type_index = uint(offset + index * 32)
        if type_index >= type_count:
            raise ValueError('Índice de tipo inválido')
        string_index = uint(type_offset + type_index * 4)
        if string_index >= string_count:
            raise ValueError('Índice de string inválido')
        position = uint(string_offset + string_index * 4)
        while data[position] & 0x80:
            position += 1
        position += 1
        end = data.index(0, position)
        names.add(data[position:end])
    return names


def verify():
    original = ROOT / 'APK' / 'Duolingo.apk'
    rebuilt = ROOT / 'dist' / 'Duolingo-base-editable.apk'
    differences = []
    with zipfile.ZipFile(original) as before, zipfile.ZipFile(rebuilt) as after:
        dex_names = sorted(n for n in before.namelist() if re.fullmatch(r'classes\d*\.dex', n))
        rebuilt_dex = sorted(n for n in after.namelist() if re.fullmatch(r'classes\d*\.dex', n))
        if dex_names != rebuilt_dex:
            differences.append('Conjunto de arquivos DEX alterado')
        classes = {}
        for name in dex_names:
            old = dex_classes(before.read(name))
            new = dex_classes(after.read(name))
            classes[name] = {'original': len(old), 'rebuilt': len(new), 'same_descriptors': old == new}
            if old != new:
                differences.append('Classes alteradas: ' + name)
        assets = sorted(n for n in before.namelist() if n.startswith('assets/'))
        after_names = set(after.namelist())
        for name in assets:
            if name not in after_names or hashlib.sha256(before.read(name)).digest() != hashlib.sha256(after.read(name)).digest():
                differences.append('Asset alterado/ausente: ' + name)
    report = {
        'scope': 'Comparação estática da reconstrução de referência, antes de alterações intencionais.',
        'original_sha256': hashlib.file_digest(original.open('rb'), 'sha256').hexdigest(),
        'rebuilt_sha256': hashlib.file_digest(rebuilt.open('rb'), 'sha256').hexdigest(),
        'dex': classes,
        'assets_checked': len(assets),
        'differences': differences,
        'passed': not differences,
        'limitations': ['Não compara semântica de instruções, recursos recompilados, instalação ou execução. Os splits continuam ausentes.']
    }
    (ROOT / 'reports' / 'rebuild-verification.json').write_text(json.dumps(report, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(json.dumps(report, indent=2, ensure_ascii=False))
    if differences:
        raise SystemExit(1)


if __name__ == '__main__':
    verify()
