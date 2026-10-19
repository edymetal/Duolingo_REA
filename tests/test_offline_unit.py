import json
import unittest
import wave
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
UNIT_ROOT = ROOT / 'content/en-pt/generated/unit-01'


@unittest.skipUnless((UNIT_ROOT / 'unit.json').exists(), 'Gere a unidade e os áudios locais primeiro')
class OfflineUnitTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.unit = json.loads((UNIT_ROOT / 'unit.json').read_text(encoding='utf-8'))
        cls.exercises = [e for lesson in cls.unit['lessons'] for e in lesson['exercises']]

    def test_source_sentences_and_variants_preserved(self):
        self.assertEqual(len(self.unit['lessons']), 3)
        self.assertEqual(len(self.exercises), 18)
        self.assertEqual(len({e['id'] for e in self.exercises}), 18)
        with (ROOT / 'content/en-pt/generated/staple.en-pt.jsonl').open(encoding='utf-8') as reader:
            sources = {r['id']: r for r in map(json.loads, reader)}
        for exercise in self.exercises:
            original = sources[exercise['id']]
            self.assertEqual(exercise['english'], original['prompt'])
            self.assertEqual(exercise['accepted_translations'], original['accepted_translations'])
            self.assertIn(exercise['portuguese'], [a['text'] for a in original['accepted_translations']])

    def test_audio_files_are_playable_pcm(self):
        for exercise in self.exercises:
            with wave.open(str(UNIT_ROOT / exercise['audio'])) as audio:
                self.assertEqual(audio.getnchannels(), 1)
                self.assertEqual(audio.getframerate(), 22050)
                self.assertEqual(audio.getsampwidth(), 2)
                self.assertGreater(audio.getnframes(), 22050 // 2)

    def test_manifest_has_no_network_permission(self):
        manifest = ET.parse(ROOT / 'android-app/AndroidManifest.xml').getroot()
        self.assertNotEqual(manifest.get('package'), 'com.duolingo')
        permissions = [p.get('{http://schemas.android.com/apk/res/android}name') for p in manifest.findall('uses-permission')]
        self.assertEqual(permissions, ['android.permission.RECORD_AUDIO'])

    @unittest.skipUnless((ROOT / 'dist/Lingo-Edy.apk').exists(), 'Compile o APK primeiro')
    def test_packaged_audio_is_uncompressed_and_complete(self):
        with zipfile.ZipFile(ROOT / 'dist/Lingo-Edy.apk') as archive:
            packaged = json.loads(archive.read('assets/unit.json'))
            self.assertEqual(packaged, self.unit)
            for exercise in self.exercises:
                info = archive.getinfo('assets/' + exercise['audio'])
                self.assertEqual(info.compress_type, zipfile.ZIP_STORED)
                self.assertEqual(archive.read(info), (UNIT_ROOT / exercise['audio']).read_bytes())


if __name__ == '__main__':
    unittest.main()
