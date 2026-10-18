import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
from import_staple import records


class RecordParserTests(unittest.TestCase):
    def test_preserves_variants_weights_and_accents(self):
        result = list(records(['prompt_ab12|A short example.\n', 'Um exemplo curto.|0.75\n', 'Exemplo breve.|0.25\n'], 'train'))
        self.assertEqual(len(result), 1)
        self.assertEqual(result[0]['accepted_translations'], [
            {'text': 'Um exemplo curto.', 'weight': 0.75},
            {'text': 'Exemplo breve.', 'weight': 0.25}
        ])
        self.assertIsNone(result[0]['cefr_level'])
        self.assertIsNone(result[0]['curriculum_unit'])

    def test_preserves_pipe_inside_translation(self):
        result = list(records(['prompt_ab12|Example\n', 'a|b|1\n'], 'train'))
        self.assertEqual(result[0]['accepted_translations'][0]['text'], 'a|b')

    def test_rejects_truncated_record(self):
        with self.assertRaises(ValueError):
            list(records(['prompt_ab12|Example\n'], 'train'))

    def test_rejects_response_without_header(self):
        with self.assertRaises(ValueError):
            list(records(['Resposta|1\n'], 'train'))

    def test_rejects_nonfinite_or_out_of_range_weights(self):
        for weight in ('NaN', 'Infinity', '-0.1', '1.1'):
            with self.subTest(weight=weight), self.assertRaises(ValueError):
                list(records(['prompt_ab12|Example\n', f'Resposta|{weight}\n'], 'train'))


if __name__ == '__main__':
    unittest.main()
