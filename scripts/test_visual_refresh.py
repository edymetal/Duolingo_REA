"""Valida navegação e um exercício offline após a troca visual; preserva progresso."""
import hashlib
import argparse
import json
import re
import subprocess
import time
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ADB = 'D:/Users/Edney/Android/Sdk/platform-tools/adb.exe'
PACKAGE = 'dev.edney.lingolocal'


def run(*args):
    return subprocess.run([ADB, '-s', 'emulator-5554', *args], check=True, capture_output=True, timeout=45).stdout


def state():
    run('shell', 'uiautomator', 'dump', '/sdcard/lingo-edy-visual.xml')
    return ET.fromstring(run('exec-out', 'cat', '/sdcard/lingo-edy-visual.xml'))


def text():
    return '\n'.join(n.get('text', '') for n in state().iter('node'))


def tap(value):
    for n in state().iter('node'):
        if n.get('enabled') == 'true' and value.casefold() in (n.get('text', '').casefold(), n.get('content-desc', '').casefold()):
            x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
            run('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
            return
    raise AssertionError('Elemento ausente: ' + value)


def main(final_checks=False):
    unit = json.loads((ROOT / 'content/en-pt/generated/unit-01/unit.json').read_text(encoding='utf-8'))
    previous = run('shell', 'settings', 'get', 'global', 'airplane_mode_on').strip()
    report = {'apk_sha256': hashlib.sha256((ROOT / 'dist/Lingo-Edy.apk').read_bytes()).hexdigest(), 'scope': 'Visual, navegação das três lições, um exercício, áudio e reinício; não repete os 18 exercícios da validação anterior.', 'passed': False}
    if final_checks:
        previous_report = json.loads((ROOT / 'reports/lingo-edy-visual-validation.json').read_text(encoding='utf-8'))
        assert previous_report['passed']
        report['navigation_validated_apk_sha256'] = previous_report.get('navigation_validated_apk_sha256', previous_report['apk_sha256'])
        report['scope'] = 'Navegação reutilizada da compilação anterior; após ajuste dos indicadores de conclusão, repete exercício, áudio e contadores ao reiniciar no APK final.'
    try:
        run('shell', 'cmd', 'connectivity', 'airplane-mode', 'enable')
        assert b'Wifi is disabled' in run('shell', 'cmd', 'wifi', 'status')
        report['offline'] = True
        run('shell', 'am', 'force-stop', PACKAGE)
        run('shell', 'am', 'start', '-W', '-n', PACKAGE+'/.MainActivity')
        assert 'Lingo Edy' in text()
        (ROOT / 'reports/lingo-edy-home.png').write_bytes(run('exec-out', 'screencap', '-p'))
        stats_before = text()
        for lesson in ([] if final_checks else unit['lessons']):
            tap(lesson['title']); assert lesson['tip'] in text(); tap('Voltar')
        if not final_checks:
            tap('Guia da unidade'); assert 'Todos os áudios são locais.' in text(); tap('Entendi')
            tap('Meu progresso'); assert 'de 4 lições' in text(); tap('Voltar')
            tap('Sobre o Lingo Edy'); assert 'STAPLE 2020' in text(); tap('Entendi')
            tap('Revisar meus erros')
            if 'Nenhum erro pendente.' in text():
                tap('Entendi')
            else:
                run('shell', 'am', 'force-stop', PACKAGE); run('shell', 'am', 'start', '-W', '-n', PACKAGE+'/.MainActivity')
        tap(unit['lessons'][0]['title']); tap('Começar')
        tap('Ouvir: '+unit['lessons'][0]['exercises'][0]['english'])
        pid = run('shell', 'pidof', PACKAGE).decode().strip()
        evidence = []
        for attempt in range(12):
            audio = run('shell', 'dumpsys', 'audio').decode('utf-8', 'replace')
            ids = {p for line in audio.splitlines() if '/'+pid in line for p in re.findall(r'piid:(\d+)', line)}
            evidence = [line for line in audio.splitlines() if any(re.search(r'piid:'+p+r'\b', line) for p in ids)]
            if any('started' in line for line in evidence):
                break
            time.sleep(.3)
        assert any('started' in line for line in evidence), 'Reprodução local não observada'
        report['audio_observed'] = True
        report['audio_service_evidence'] = evidence
        answer = unit['lessons'][0]['exercises'][0]['portuguese']
        tap(answer[:1].upper()+answer[1:]); tap('Verificar'); assert 'Muito bem!' in text()
        (ROOT / 'reports/lingo-edy-exercise.png').write_bytes(run('exec-out', 'screencap', '-p'))
        run('shell', 'am', 'force-stop', PACKAGE); run('shell', 'am', 'start', '-W', '-n', PACKAGE+'/.MainActivity')
        assert stats_before == text(), 'Contadores mudaram numa tentativa não concluída'
        report['counters_preserved'] = True
        report['passed'] = True
    finally:
        if previous != b'1':
            run('shell', 'cmd', 'connectivity', 'airplane-mode', 'disable')
        (ROOT / 'reports/lingo-edy-visual-validation.json').write_text(json.dumps(report, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
    print(json.dumps(report, ensure_ascii=False))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--final-checks', action='store_true', help='Reutiliza a navegação aprovada e repete exercício, áudio e reinício')
    main(parser.parse_args().final_checks)
