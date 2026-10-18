"""Teste de interface no emulador, com rede desativada. Não opera celulares físicos."""
import argparse
import hashlib
import json
import re
import subprocess
import time
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'dev.edney.lingolocal'


def main(adb, serial, resume_final_checks=False):
    if not re.fullmatch(r'emulator-\d+', serial):
        raise ValueError('Somente um emulador pode ser usado neste teste')
    unit = json.loads((ROOT / 'content/en-pt/generated/unit-01/unit.json').read_text(encoding='utf-8'))
    def run(*args, timeout=45):
        return subprocess.run([adb, '-s', serial, *args], capture_output=True, check=True, timeout=timeout).stdout
    def state():
        run('shell', 'uiautomator', 'dump', '/sdcard/lingo-local-test.xml')
        data = run('exec-out', 'cat', '/sdcard/lingo-local-test.xml')
        return ET.fromstring(data)
    def text(tree):
        return '\n'.join(node.get('text', '') for node in tree.iter('node'))
    def click_node(node):
        values = [int(x) for x in re.findall(r'\d+', node.get('bounds'))]
        run('shell', 'input', 'tap', str((values[0]+values[2])//2), str((values[1]+values[3])//2))
    def find(tree, value, exact=False):
        candidates = [n for n in tree.iter('node') if n.get('enabled') == 'true' and (n.get('text', '').casefold() == value.casefold() if exact else value.casefold() in n.get('text', '').casefold())]
        if not candidates:
            raise AssertionError('Elemento esperado ausente: ' + value + '\n' + text(tree))
        return candidates[0]
    def tap(value, exact=False):
        tree = state(); click_node(find(tree, value, exact)); return tree
    def screenshot(name):
        folder = '.tools' if 'failure' in name else 'reports'
        (ROOT / folder / name).write_bytes(run('exec-out', 'screencap', '-p'))
    def cap(value):
        return value[:1].upper()+value[1:]
    previous_mode = run('shell', 'settings', 'get', 'global', 'airplane_mode_on').decode().strip()
    report = {'package': PACKAGE, 'serial': serial, 'airplane_mode': True, 'lessons_completed': 0, 'questions_checked': 0, 'passed': False}
    if resume_final_checks:
        previous_report = json.loads((ROOT / 'reports/offline-app-validation.json').read_text(encoding='utf-8'))
        assert previous_report['lessons_completed'] == 3 and previous_report['questions_checked'] == 18 and previous_report.get('mistakes_reviewed')
        for key in ('lessons_completed', 'questions_checked', 'mistakes_reviewed', 'full_flow_apk_sha256'):
            if key in previous_report:
                report[key] = previous_report[key]
        report['full_flow_reused'] = True
    else:
        report['full_flow_apk_sha256'] = hashlib.sha256((ROOT / 'dist/Lingo-Local.apk').read_bytes()).hexdigest()
    try:
        run('shell', 'cmd', 'connectivity', 'airplane-mode', 'enable')
        assert run('shell', 'settings', 'get', 'global', 'airplane_mode_on').strip() == b'1'
        wifi_status = run('shell', 'cmd', 'wifi', 'status').decode('utf-8', 'replace')
        assert 'Wifi is disabled' in wifi_status, 'Desative o Wi-Fi do emulador antes do teste offline'
        report['wifi_disabled'] = True
        run('shell', 'am', 'force-stop', PACKAGE)
        run('shell', 'am', 'start', '-W', '-n', PACKAGE+'/.MainActivity')
        home = state(); assert 'Lingo Local' in text(home)
        screenshot('offline-app-home.png')
        for lesson_index, lesson in enumerate([] if resume_final_checks else unit['lessons']):
            tap(lesson['title']); tap('Começar', exact=True)
            for index, exercise in enumerate(lesson['exercises']):
                tree = state()
                if exercise['type'] == 'word_order':
                    nodes = list(tree.iter('node'))
                    used = set()
                    for token in exercise['english'].split():
                        candidates = [(i, n) for i, n in enumerate(nodes) if i not in used and n.get('text') == token and n.get('class') == 'android.widget.Button' and n.get('enabled') == 'true']
                        if not candidates:
                            raise AssertionError('Palavra ausente no banco: '+token)
                        selected_index, node = candidates[0]; used.add(selected_index); click_node(node)
                else:
                    answer = lesson['exercises'][1]['portuguese'] if lesson_index == 0 and index == 0 else exercise['portuguese']
                    click_node(find(tree, cap(answer), exact=True))
                tap('Verificar', exact=True)
                feedback = text(state())
                expected = 'Vamos aprender juntos.' if lesson_index == 0 and index == 0 else 'Muito bem!'
                assert expected in feedback, feedback
                if lesson_index == 0 and index == 2:
                    screenshot('offline-app-exercise.png')
                report['questions_checked'] += 1
                tap('Continuar', exact=True)
            assert 'Lição concluída!' in text(state())
            screenshot('offline-app-complete.png')
            tap('Voltar à unidade', exact=True)
            report['lessons_completed'] += 1
            print('Lesson completed:', lesson_index+1, flush=True)
        assert '45 XP' in text(state())
        if not resume_final_checks:
            tap('Revisar meus erros')
            first = unit['lessons'][0]['exercises'][0]
            tap(cap(first['portuguese']), exact=True); tap('Verificar', exact=True); tap('Continuar', exact=True)
            assert 'Revisão concluída' in text(state())
            tap('Voltar à unidade', exact=True)
        assert 'Revisar meus erros  ·  0' in text(state())
        report['mistakes_reviewed'] = True
        # Reprodução de WAV empacotado, com o mesmo modo avião do teste.
        tap(unit['lessons'][0]['title']); tap('Começar', exact=True)
        tree = state(); click_node(find(tree, 'Ouvir em inglês'))
        pid = run('shell', 'pidof', PACKAGE).decode().strip()
        playback_lines = []
        for attempt in range(12):
            audio_state = run('shell', 'dumpsys', 'audio').decode('utf-8', 'replace')
            identity_lines = [line for line in audio_state.splitlines() if '/'+pid in line and 'piid:' in line]
            player_ids = {match for line in identity_lines for match in re.findall(r'piid:(\d+)', line)}
            playback_lines = [line for line in audio_state.splitlines() if any(re.search(r'piid:'+re.escape(player_id)+r'\b', line) for player_id in player_ids)]
            if any('state:started' in line or 'event:started' in line for line in playback_lines):
                break
            time.sleep(.3)
        report['audio_service_evidence'] = playback_lines
        report['audio_player_observed'] = any('state:started' in line or 'event:started' in line for line in playback_lines)
        if not report['audio_player_observed']:
            (ROOT / '.tools/audio-playback-diagnostic.txt').write_text(audio_state, encoding='utf-8')
            raise AssertionError('Reprodução não confirmada pelo serviço de áudio')
        run('shell', 'am', 'force-stop', PACKAGE)
        run('shell', 'am', 'start', '-W', '-n', PACKAGE+'/.MainActivity')
        home = text(state()); assert '3 de 3 lições' in home and '45 XP' in home and 'melhor: 5/6' in home
        report['progress_persisted_after_restart'] = True
        screenshot('offline-app-progress.png')
        report['final_apk_sha256'] = hashlib.sha256((ROOT / 'dist/Lingo-Local.apk').read_bytes()).hexdigest()
        report['passed'] = True
    except Exception as error:
        report['error'] = str(error)
        try:
            screenshot('offline-app-failure.png')
        except Exception:
            pass
        raise
    finally:
        (ROOT / 'reports/offline-app-validation.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
        if previous_mode != '1':
            run('shell', 'cmd', 'connectivity', 'airplane-mode', 'disable')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default='D:/Users/Edney/Android/Sdk/platform-tools/adb.exe')
    parser.add_argument('--serial', default='emulator-5554')
    parser.add_argument('--resume-final-checks', action='store_true', help='Reutiliza os 18 exercícios já aprovados e repete apenas áudio e persistência')
    options = parser.parse_args()
    main(options.adb, options.serial, options.resume_final_checks)
