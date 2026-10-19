import json
import unittest
import zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]


class ReferenceActivitiesTests(unittest.TestCase):
    def test_all_seven_formats_have_solvable_answers(self):
        unit=json.loads((ROOT/'content/en-pt/reference-activities.json').read_text(encoding='utf-8'))
        entries=unit['exercises']
        self.assertEqual({q['type'] for q in entries},{'choice','cloze','speaking','image_cloze','image_choice','translate_bank','pairs'})
        self.assertEqual(len({q['id'] for q in entries}),7)
        for q in entries:
            if q['type'] in {'choice','cloze','image_cloze','image_choice'}:
                self.assertIn(q['answer'],q['choices'])
                self.assertEqual(len(set(q['choices'])),len(q['choices']))
            if q['type']=='translate_bank':
                for word in q['answer'].split():self.assertIn(word,q['tokens'])
            if q['type']=='pairs':
                self.assertEqual(len({pair[0] for pair in q['pairs']}),5)
                self.assertEqual(len({pair[1] for pair in q['pairs']}),5)
            if q['type']=='speaking':self.assertIn(q['fallback_answer'],q['choices'])

    @unittest.skipUnless((ROOT/'dist/Lingo-Edy.apk').exists(),'Compile primeiro')
    def test_runtime_model_and_reference_audio_are_packaged(self):
        with zipfile.ZipFile(ROOT/'dist/Lingo-Edy.apk') as apk:
            reference=json.loads(apk.read('assets/reference-activities.json'))
            for q in reference['exercises']:
                self.assertEqual(apk.getinfo('assets/'+q['audio']).compress_type,zipfile.ZIP_STORED)
            for path in ['assets/rive.wasm','assets/oscar.riv','assets/lily.riv','assets/zari.riv','assets/speech-model/am/final.mdl','lib/arm64-v8a/libvosk.so','lib/x86_64/libvosk.so','lib/arm64-v8a/libjnidispatch.so']:
                self.assertGreater(apk.getinfo(path).file_size,1000)


if __name__=='__main__':unittest.main()
