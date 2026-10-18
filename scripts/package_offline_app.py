"""Monta o APK com assets locais; WAVs ficam sem compressão para MediaPlayer."""
import shutil
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def package():
    build = ROOT / 'android-app/build'
    unit = ROOT / 'content/en-pt/generated/unit-01'
    output = build / 'unsigned.apk'
    shutil.copyfile(build / 'resources.apk', output)
    with zipfile.ZipFile(output, 'a') as archive:
        archive.write(build / 'dex/classes.dex', 'classes.dex', compress_type=zipfile.ZIP_DEFLATED)
        archive.write(unit / 'unit.json', 'assets/unit.json', compress_type=zipfile.ZIP_DEFLATED)
        for path in sorted((build / 'visual-assets').rglob('*')):
            if path.is_file():
                archive.write(path, 'assets/' + path.relative_to(build / 'visual-assets').as_posix(), compress_type=zipfile.ZIP_DEFLATED)
        for path in sorted((unit / 'audio').glob('*.wav')):
            archive.write(path, 'assets/audio/' + path.name, compress_type=zipfile.ZIP_STORED)
    print('APK empacotado com unidade e áudios locais.')


if __name__ == '__main__':
    package()
