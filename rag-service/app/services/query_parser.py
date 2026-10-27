"""Rule-based extraction of structured search filters from a Vietnamese question.

Vector similarity cannot enforce numeric limits ("dưới 4 triệu", "trên 20m2") or
exact locations, so we pull those out of the text and apply them as hard
filters on the room index. Locations are matched against the districts/wards
that actually exist in the indexed (real) data.
"""
import re
import unicodedata
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple


@dataclass
class ParsedQuery:
    min_price: Optional[float] = None
    max_price: Optional[float] = None
    min_area: Optional[float] = None
    max_area: Optional[float] = None
    city: Optional[str] = None
    district: Optional[str] = None
    ward: Optional[str] = None


def normalize(text: str) -> str:
    """Lowercase, strip accents (đ -> d) and collapse spaces."""
    text = unicodedata.normalize("NFKD", text or "").replace("đ", "d").replace("Đ", "D")
    text = "".join(ch for ch in text if not unicodedata.combining(ch)).lower()
    return re.sub(r"\s+", " ", text).strip()


# ----------------------------------------------------------------------------
# Money
# ----------------------------------------------------------------------------
_PRICE = re.compile(
    r"\s*(\d+(?:[.,]\d+)*)\s*"
    r"(?:(trieu|tr)(\d)|(trieu|tr|m|k|nghin|ngan|dong|vnd|d)\b)?"
    r"(\s*ruoi)?"
)
_MILLION = {"trieu", "tr", "m"}
_THOUSAND = {"k", "nghin", "ngan"}
_VND = {"dong", "vnd", "d"}
_SEPARATOR = re.compile(r"\s*(?:den|toi|-|~)\s*")


def _unit_of(m: "re.Match") -> Optional[str]:
    return m.group(2) or m.group(4)


def _money(m: "re.Match", default_unit: Optional[str] = None) -> float:
    raw, _, decimal_digit, _, half = m.groups()
    unit = _unit_of(m) or default_unit
    if re.fullmatch(r"\d{1,3}(?:\.\d{3})+", raw):
        number = float(raw.replace(".", ""))  # 3.500.000
    else:
        number = float(raw.replace(",", "."))  # 3.5 / 3,5
    if unit in _MILLION:
        value = number * 1_000_000 + (int(decimal_digit) * 100_000 if decimal_digit else 0)
    elif unit in _THOUSAND:
        value = number * 1_000
    elif unit in _VND:
        value = number
    else:
        value = number * 1_000_000 if number < 1000 else number
    if half and (unit in _MILLION or (unit is None and number < 1000)):
        value += 500_000
    return value


def _parse_price(t: str) -> Tuple[Optional[float], Optional[float]]:
    # 1) ranges: "từ 2 đến 3 triệu", "2-3tr"
    for m in re.finditer(r"(?:\btu\s+)?(?<![\d.,])(?=\d)", t):
        first = _PRICE.match(t, m.end())
        if not first:
            continue
        sep = _SEPARATOR.match(t, first.end())
        if not sep:
            continue
        second = _PRICE.match(t, sep.end())
        if not second:
            continue
        unit2 = _unit_of(second)
        if unit2 is None and _unit_of(first) is None:
            continue  # bare "2 - 3": too ambiguous
        low = _money(first, default_unit=unit2)
        high = _money(second)
        if low > high:
            low, high = high, low
        return low, high

    # 2) approximate: "tầm 3 triệu", "khoảng 3tr"
    kw = re.search(r"\b(?:tam|khoang|quanh|gan)\s+(?=\d)", t)
    if kw:
        m = _PRICE.match(t, kw.end())
        if m:
            value = _money(m)
            return value * 0.8, value * 1.2

    low = high = None
    # 3) upper bound
    kw = re.search(r"(?:\b(?:duoi|khong qua|toi da|nho hon|re hon|max)\b|<=?)\s*(?=\d)", t)
    if kw:
        m = _PRICE.match(t, kw.end())
        if m:
            high = _money(m)
    # 4) lower bound
    kw = re.search(r"(?:\b(?:tren|tu|toi thieu|it nhat|lon hon|min)\b|>=?)\s*(?=\d)", t)
    if kw:
        m = _PRICE.match(t, kw.end())
        if m:
            low = _money(m)
    if low is None and high is None:
        m = re.search(r"(?<![\d.,])(\d+(?:[.,]\d+)?)\s*(trieu|tr|m|k)?\s*tro len", t)
        if m:
            low = _money(_PRICE.match(t, m.start()))
    return low, high


# ----------------------------------------------------------------------------
# Area
# ----------------------------------------------------------------------------
_AREA = re.compile(r"(\d+(?:[.,]\d+)?)\s*(?:m2|met vuong|m vuong)\b")


def _parse_area(t: str) -> Tuple[Optional[float], Optional[float], str]:
    min_area = max_area = None
    for m in _AREA.finditer(t):
        value = float(m.group(1).replace(",", "."))
        before = t[max(0, m.start() - 14):m.start()]
        if re.search(r"(duoi|khong qua|toi da|nho hon|<=?)\s*$", before):
            max_area = value
        elif re.search(r"(tren|toi thieu|it nhat|lon hon|tu|>=?)\s*$", before):
            min_area = value
    return min_area, max_area, _AREA.sub(" ", t)


# ----------------------------------------------------------------------------
# Locations
# ----------------------------------------------------------------------------
_ADMIN_PREFIX = re.compile(r"^(?:thanh pho|tp\.?|quan|huyen|thi xa|phuong|xa|thi tran)\s+")


def _aliases(name: str) -> List[str]:
    full = normalize(name)
    aliases = {full}
    stripped = _ADMIN_PREFIX.sub("", full)
    if stripped and not stripped.isdigit():
        aliases.add(stripped)
    number = re.fullmatch(r"quan (\d+)", full)
    if number:
        aliases.update({f"q{number.group(1)}", f"q.{number.group(1)}", f"q {number.group(1)}"})
    return [a for a in aliases if a]


def _find_location(t: str, names: List[str]) -> Optional[str]:
    best: Optional[Tuple[int, str]] = None
    for name in names:
        for alias in _aliases(name):
            if re.search(rf"(?<![a-z0-9]){re.escape(alias)}(?![a-z0-9])", t):
                if best is None or len(alias) > best[0]:
                    best = (len(alias), name)
    return best[1] if best else None


def parse_query(text: str, locations: Optional[Dict[str, List[str]]] = None) -> ParsedQuery:
    t = normalize(text)
    parsed = ParsedQuery()
    parsed.min_area, parsed.max_area, t_without_area = _parse_area(t)
    parsed.min_price, parsed.max_price = _parse_price(t_without_area)
    locations = locations or {}
    parsed.district = _find_location(t, locations.get("district", []))
    parsed.ward = _find_location(t, locations.get("ward", []))
    parsed.city = _find_location(t, locations.get("city", []))
    return parsed
