import unittest

from src.face_auth.admin_identity import stored_admin_id, validate_admin_id
from src.face_auth.ai.matching import identify


ADMIN_UUID = "10000000-0000-4000-8000-000000000005"


class AdminIdentityTests(unittest.TestCase):
    def test_uuid_is_kept_as_string(self):
        self.assertEqual(validate_admin_id(ADMIN_UUID), ADMIN_UUID)
        self.assertEqual(stored_admin_id(ADMIN_UUID), ADMIN_UUID)

    def test_old_numeric_ids_keep_their_json_number_type(self):
        self.assertEqual(validate_admin_id(7), 7)
        self.assertEqual(stored_admin_id("7"), 7)

    def test_invalid_core_ids_are_rejected(self):
        for value in (True, None, "7", 0, -1, 2 ** 63, {}, "admin", "0" * 32,
                      "00000000-0000-0000-0000-000000000000", ADMIN_UUID.replace("-", "")):
            with self.subTest(value=value), self.assertRaises(ValueError):
                validate_admin_id(value)

    def test_equal_scores_with_mixed_id_types_are_ambiguous(self):
        self.assertEqual(identify([1.0, 0.0], {7: [1.0, 0.0], ADMIN_UUID: [1.0, 0.0]}, .8, .05),
                         (None, "AMBIGUOUS_FACE"))
