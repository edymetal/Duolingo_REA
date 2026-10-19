"""Prepara recursos visuais locais do APK decodificado, sem publicá-los no Git."""
import hashlib
import json
import re
import shutil
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ICONS = ['bea_sitting', 'course_flag_en', 'home_active', 'icon_practice',
         'more_active', 'leagues_active', 'heart_active', 'chest_closed', 'guidebook_white',
         'level_star', 'gem', 'friends_streak_flame', 'contacts_book', 'microphone', 'speaker_blue']


def prepare():
    source = ROOT / 'decoded/res'
    output = ROOT / 'android-app/build/visual-res'
    shutil.copytree(ROOT / 'android-app/res', output, dirs_exist_ok=True)
    drawable = output / 'drawable'
    drawable.mkdir(exist_ok=True)
    colors = {e.attrib['name']: e.text for e in ET.parse(source / 'values/colors.xml').getroot() if e.tag == 'color'}

    def color(match):
        value = colors[match[1]]
        while value.startswith('@color/'):
            value = colors[value.split('/')[-1]]
        return value

    inventory = []
    for name in ICONS:
        path = source / 'drawable' / (name + '.xml')
        data = path.read_bytes()
        xml = re.sub(r'@color/([\w]+)', color, data.decode('utf-8')).replace('?pathIconColor', '#ffffff')
        if '@' in xml or re.search(r'\?[a-zA-Z]', xml.replace('<?xml', '')):
            raise ValueError('Referência não resolvida: ' + name)
        (drawable / ('original_' + name + '.xml')).write_text(xml, encoding='utf-8')
        inventory.append({'source': str(path.relative_to(ROOT)).replace('\\', '/'), 'sha256': hashlib.sha256(data).hexdigest()})
    fonts = ROOT / 'android-app/build/visual-assets/fonts'
    fonts.mkdir(parents=True, exist_ok=True)
    for weight in ['regular', 'bold']:
        path = source / 'font' / ('din_next_for_duolingo' + ('_bold' if weight == 'bold' else '') + '.otf')
        shutil.copyfile(path, fonts / (weight + '.otf'))
        inventory.append({'source': str(path.relative_to(ROOT)).replace('\\', '/'), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest()})
    (ROOT / 'reports/visual-assets.json').write_text(json.dumps({'origin': 'APK fornecido pelo usuário, recursos decodificados locais', 'resources': inventory}, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(f'{len(inventory)} recursos originais preparados localmente.')


if __name__ == '__main__':
    prepare()
