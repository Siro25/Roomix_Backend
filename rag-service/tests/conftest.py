import os
import tempfile

# Must run before `app` is imported: isolate Chroma and force offline mode so
# tests never touch the real vector DB, the Gemini API or core-service.
os.environ["CHROMA_PERSIST_DIRECTORY"] = tempfile.mkdtemp(prefix="roomix_chroma_test_")
os.environ["GEMINI_API_KEY"] = ""
os.environ["CORE_SERVICE_URL"] = ""

import pytest  # noqa: E402


@pytest.fixture(autouse=True)
def clean_store():
    from app.core.vector_store import vector_store

    for col in (vector_store.rooms_col, vector_store.policies_col):
        ids = col.get()["ids"]
        if ids:
            col.delete(ids=ids)
    yield
