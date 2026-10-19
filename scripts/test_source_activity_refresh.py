"""Confere os três formatos anteriores em uma lição completa com a nova interface."""
import hashlib
import json
import re
from test_reference_activities import ROOT,PACKAGE,run,state,find,click,tap,text,wait_for


def main():
    unit=json.loads((ROOT/'content/en-pt/generated/unit-01/unit.json').read_text(encoding='utf-8'))
    lesson=unit['lessons'][0]
    previous=run('shell','settings','get','global','airplane_mode_on').strip()
    wifi_was_enabled=b'Wifi is enabled' in run('shell','cmd','wifi','status')
    report={'apk_sha256':hashlib.sha256((ROOT/'dist/Lingo-Edy.apk').read_bytes()).hexdigest(),'passed':False,'questions_checked':0}
    try:
        run('shell','cmd','connectivity','airplane-mode','enable')
        run('shell','svc','wifi','disable')
        assert b'Wifi is disabled' in run('shell','cmd','wifi','status')
        run('shell','am','force-stop',PACKAGE);run('shell','am','start','-W','-n',PACKAGE+'/.MainActivity')
        tap('Meu progresso');before=text();already_done=lesson['title']+' · melhor:' in before
        tap('Voltar');before_home=text()
        xp_before=int(re.search(r'(\d+) XP',before_home).group(1))
        tap(lesson['title']);tap('Começar')
        for i,q in enumerate(lesson['exercises']):
            wait_for(f'Progresso: {i+1} de 6')
            if q['type']=='word_order':
                tree=state();used=set()
                for word in q['english'].split():
                    nodes=[(index,n) for index,n in enumerate(tree.iter('node')) if index not in used and n.get('text')==word and n.get('class')=='android.widget.Button' and n.get('enabled')=='true']
                    assert nodes,'Palavra ausente: '+word
                    index,node=nodes[0];used.add(index);click(node)
            else:
                if q['type']=='listening' and i==2:
                    tap('Ouvir: Toque para ouvir')
                tap(q['portuguese'][:1].upper()+q['portuguese'][1:])
            tap('VERIFICAR');assert 'Muito bem!' in text();tap('CONTINUAR');report['questions_checked']+=1
            print('Source exercise passed:',i+1,flush=True)
        assert '6 de 6 respostas corretas' in text();tap('Voltar à unidade')
        xp_expected=xp_before+(0 if already_done else 15)
        assert str(xp_expected)+' XP' in text()
        tap('Meu progresso');assert lesson['title']+' · melhor: 6/6' in text();tap('Voltar')
        run('shell','am','force-stop',PACKAGE);run('shell','am','start','-W','-n',PACKAGE+'/.MainActivity')
        tap('Meu progresso');assert lesson['title']+' · melhor: 6/6' in text();tap('Voltar')
        report['offline']=True;report['score_persisted']=True;report['xp_before']=xp_before;report['xp_after']=xp_expected;report['passed']=True
    except Exception as error:
        report['error']=str(error);raise
    finally:
        (ROOT/'reports/source-activity-refresh-validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        if previous!=b'1':run('shell','cmd','connectivity','airplane-mode','disable')
        if wifi_was_enabled:run('shell','svc','wifi','enable')


if __name__=='__main__':main()
