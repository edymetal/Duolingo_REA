"""Percorre os sete formatos em um emulador; não limpa dados de estudo."""
import hashlib
import json
import re
import subprocess
import time
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
ADB='D:/Users/Edney/Android/Sdk/platform-tools/adb.exe'
PACKAGE='dev.edney.lingolocal'


def run(*args):
    return subprocess.run([ADB,'-s','emulator-5554',*args],capture_output=True,check=True,timeout=60).stdout


def state():
    run('shell','uiautomator','dump','/sdcard/lingo-activities.xml')
    return ET.fromstring(run('exec-out','cat','/sdcard/lingo-activities.xml'))


def find(tree,value):
    for n in tree.iter('node'):
        if n.get('enabled')=='true' and value.casefold() in (n.get('text','').casefold(),n.get('content-desc','').casefold()):return n
    raise AssertionError('Elemento ausente: '+value+'\n'+'\n'.join(n.get('text','') for n in tree.iter('node')))


def click(node):
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
    run('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))


def wait_for(value):
    for attempt in range(5):
        tree=state()
        try:return find(tree,value)
        except AssertionError:
            if attempt==4:raise
            time.sleep(.4)


def tap(value):click(wait_for(value))


def text():return '\n'.join(n.get('text','') for n in state().iter('node'))


def screenshot(name):
    time.sleep(.8)
    state()
    (ROOT/'reports'/('activity-'+name+'.png')).write_bytes(run('exec-out','screencap','-p'))


def main():
    previous=run('shell','settings','get','global','airplane_mode_on').strip()
    wifi_was_enabled=b'Wifi is enabled' in run('shell','cmd','wifi','status')
    report={'apk_sha256':hashlib.sha256((ROOT/'dist/Lingo-Edy.apk').read_bytes()).hexdigest(),'passed':False,'formats':[]}
    try:
        run('shell','cmd','connectivity','airplane-mode','enable')
        run('shell','svc','wifi','disable')
        assert b'Wifi is disabled' in run('shell','cmd','wifi','status')
        run('shell','am','force-stop',PACKAGE);run('shell','am','start','-W','-n',PACKAGE+'/.MainActivity')
        tap('Praticar lições');tap('Começar')
        wait_for('Progresso: 1 de 7');wait_for('store')
        screenshot('choice');tap('store');tap('CONTINUAR');assert 'Resposta correta:' in text();tap('CONTINUAR')
        report['formats'].append('choice_incorrect');print('choice passed',flush=True)
        wait_for('Progresso: 2 de 7');screenshot('cloze');tap('want');tap('CONTINUAR');assert 'Muito bem!' in text();tap('CONTINUAR')
        report['formats'].append('cloze');print('cloze passed',flush=True)
        wait_for('Progresso: 3 de 7');screenshot('speaking');tap('Gravar minha fala')
        permission=state()
        deny=[n for n in permission.iter('node') if n.get('resource-id','').endswith('permission_deny_button')]
        if deny:
            click(deny[0]);assert 'Microfone não autorizado' in text();report['permission_denial_handled']=True
        # Emulador autorizado para testar a inicialização real do modelo e AudioRecord.
        run('shell','pm','grant',PACKAGE,'android.permission.RECORD_AUDIO')
        tap('Gravar minha fala');time.sleep(8)
        speech_text=text();assert any(word in speech_text for word in ['Ouvindo','Não consegui ouvir','Toque no microfone']),speech_text
        assert 'Não foi possível' not in speech_text
        report['microphone_model_started']=True
        tap('NÃO POSSO FALAR AGORA');tap('Aquela camisa verde é bonita.');tap('VERIFICAR');tap('CONTINUAR')
        report['formats'].append('speaking_with_listening_alternative');print('speaking passed',flush=True)
        wait_for('Progresso: 4 de 7');screenshot('image-cloze');tap('hat');assert 'I want a red hat.' in text();tap('VERIFICAR');tap('CONTINUAR')
        report['formats'].append('image_cloze');print('image_cloze passed',flush=True)
        wait_for('Progresso: 5 de 7');screenshot('images');tap('verde');tap('VERIFICAR');assert 'Muito bem!' in text();tap('CONTINUAR')
        report['formats'].append('image_choice');print('image_choice passed',flush=True)
        wait_for('Progresso: 6 de 7');screenshot('bank');tree=state();click(find(tree,'Quanto'));tap('Quanto')
        # O primeiro "Quanto" agora é a palavra da resposta; tocá-lo devolve ao banco.
        tree=state()
        for word in ['Quanto','custa','esta','saia']:click(find(tree,word))
        tap('VERIFICAR');assert 'Muito bem!' in text();tap('CONTINUAR')
        report['formats'].append('translate_bank_with_undo');print('bank passed',flush=True)
        wait_for('Progresso: 7 de 7');screenshot('pairs');tree=state()
        # Um par errado deve continuar disponível, sem avançar a atividade.
        click(find(tree,'gostar de'));click(find(tree,'need'));time.sleep(.6)
        for left,right in [('gostar de','like'),('está ensolarado',"it's sunny"),('precisar','need'),('querer','want'),('saia','skirt')]:
            click(find(tree,left));click(find(tree,right))
        tap('VERIFICAR');assert 'Pares concluídos' in text();tap('CONTINUAR')
        assert 'Lição concluída!' in text();screenshot('complete');tap('Voltar à unidade')
        tap('Meu progresso');assert 'Cores e roupas · melhor: 5/7' in text();tap('Voltar')
        run('shell','am','force-stop',PACKAGE);run('shell','am','start','-W','-n',PACKAGE+'/.MainActivity')
        tap('Meu progresso');assert 'Cores e roupas · melhor: 5/7' in text();tap('Voltar')
        report['formats'].append('pairs_with_error_recovery');report['progress_persisted']=True
        logs=run('logcat','-d','-s','LingoCharacter:I','chromium:E','AndroidRuntime:E').decode('utf-8','replace')
        report['character_logs']=[line for line in logs.splitlines() if 'LingoCharacter' in line][-12:]
        assert all('ready '+name in logs for name in ['oscar','lily','zari']),logs[-4000:]
        report['offline']=True;report['passed']=True
    except Exception as error:
        report['error']=str(error);screenshot('failure');raise
    finally:
        (ROOT/'reports/reference-activities-validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        if previous!=b'1':run('shell','cmd','connectivity','airplane-mode','disable')
        if wifi_was_enabled:run('shell','svc','wifi','enable')
    print('All seven activity formats passed.',flush=True)


if __name__=='__main__':main()
