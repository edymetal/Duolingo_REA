"""Monta o APK com assets locais; WAVs ficam sem compressão para MediaPlayer."""
import shutil
import zipfile
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def package():
    build = ROOT / 'android-app/build'
    unit = ROOT / 'content/en-pt/generated/unit-01'
    output = build / 'unsigned.apk'
    reference = json.loads((build / 'visual-assets/reference-activities.json').read_text(encoding='utf-8'))
    for entry in reference['exercises']:
        if not (build / 'visual-assets' / entry['audio']).exists():
            raise FileNotFoundError('Gere os áudios da prática de referência antes de compilar: ' + entry['audio'])
    shutil.copyfile(build / 'resources.apk', output)
    with zipfile.ZipFile(output, 'a') as archive:
        archive.write(build / 'dex/classes.dex', 'classes.dex', compress_type=zipfile.ZIP_DEFLATED)
        archive.write(unit / 'unit.json', 'assets/unit.json', compress_type=zipfile.ZIP_DEFLATED)
        for path in sorted((build / 'visual-assets').rglob('*')):
            if path.is_file():
                archive.write(path, 'assets/' + path.relative_to(build / 'visual-assets').as_posix(), compress_type=zipfile.ZIP_STORED if path.suffix == '.wav' else zipfile.ZIP_DEFLATED)
        for path in sorted((build / 'native').rglob('*.so')):
            archive.write(path, 'lib/' + path.relative_to(build / 'native').as_posix(), compress_type=zipfile.ZIP_STORED)
        for path in sorted((unit / 'audio').glob('*.wav')):
            archive.write(path, 'assets/audio/' + path.name, compress_type=zipfile.ZIP_STORED)
    print('APK empacotado com unidade e áudios locais.')


if __name__ == '__main__':
    package()
