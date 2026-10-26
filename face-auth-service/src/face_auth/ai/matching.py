"""Tính điểm so khớp, dùng được cả trong kiểm thử không có GPU."""
import math


def normalize(vector):
    if not vector or any(not math.isfinite(value) for value in vector):
        raise ValueError("Embedding không hợp lệ")
    length = math.sqrt(sum(value * value for value in vector))
    if length <= 1e-8:
        raise ValueError("Embedding rỗng")
    return [value / length for value in vector]


def mean_vector(vectors):
    if not vectors or len({len(v) for v in vectors}) != 1:
        raise ValueError("Các embedding phải cùng kích thước")
    return normalize([sum(column) / len(vectors) for column in zip(*vectors)])


def similarity(first, second):
    if len(first) != len(second):
        raise ValueError("Hai embedding khác kích thước")
    cosine = sum(a * b for a, b in zip(first, second))
    distance = math.sqrt(sum((a - b) ** 2 for a, b in zip(first, second)))
    # Giữ công thức trong pipeline cũ; đây là điểm, không phải xác suất.
    return 0.7 * cosine + 0.3 * max(0.0, 1.0 - distance / 2.0)


def identify(vector, profiles, threshold, margin):
    ranked = sorted(
        ((similarity(vector, reference), admin_id) for admin_id, reference in profiles.items()),
        key=lambda item: item[0],
        reverse=True,
    )
    if not ranked or ranked[0][0] < threshold:
        return None, "FACE_NOT_RECOGNIZED"
    if len(ranked) > 1 and ranked[0][0] - ranked[1][0] < margin:
        return None, "AMBIGUOUS_FACE"
    return ranked[0][1], None
