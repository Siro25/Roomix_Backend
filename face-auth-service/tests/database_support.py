"""Mỗi test dùng schema PostgreSQL riêng, không xóa dữ liệu ứng dụng."""
import os
import unittest
import uuid
import psycopg
from psycopg import sql
from src.face_auth.db.storage import Store


def create_test_store(test):
    url = os.environ.get("TEST_DATABASE_URL", "")
    if not url:
        raise unittest.SkipTest("Cần TEST_DATABASE_URL để kiểm thử PostgreSQL thật")
    schema = "face_test_" + uuid.uuid4().hex
    store = Store(url, schema=schema)

    def remove_schema():
        # Chỉ xóa schema ngẫu nhiên do chính test này tạo ra.
        with psycopg.connect(url) as db:
            db.execute(sql.SQL("DROP SCHEMA {} CASCADE").format(sql.Identifier(schema)))

    test.addCleanup(remove_schema)
    return store
