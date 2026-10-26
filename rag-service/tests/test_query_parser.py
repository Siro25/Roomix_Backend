import pytest

from app.services.query_parser import parse_query

LOCATIONS = {
    "city": ["Hà Nội", "TP Hồ Chí Minh"],
    "district": ["Cầu Giấy", "Quận 1", "Quận 10", "Quận 7"],
    "ward": ["Dịch Vọng", "Bến Nghé"],
}


@pytest.mark.parametrize("text, low, high", [
    ("phòng dưới 4 triệu", None, 4_000_000),
    ("tìm phòng từ 2 đến 3 triệu", 2_000_000, 3_000_000),
    ("phòng 2-3tr", 2_000_000, 3_000_000),
    ("giá trên 3.500.000 đ", 3_500_000, None),
    ("dưới 4 triệu rưỡi", None, 4_500_000),
    ("phòng không quá 3tr5", None, 3_500_000),
    ("phòng từ 2 triệu trở lên", 2_000_000, None),
    ("tìm phòng đẹp", None, None),
])
def test_price_parsing(text, low, high):
    parsed = parse_query(text)
    assert parsed.min_price == low
    assert parsed.max_price == high


def test_price_approximate_range():
    parsed = parse_query("phòng tầm 3tr5")
    assert parsed.min_price == pytest.approx(2_800_000)
    assert parsed.max_price == pytest.approx(4_200_000)


def test_area_parsing_does_not_leak_into_price():
    parsed = parse_query("phòng trên 20m2 dưới 3 triệu")
    assert (parsed.min_area, parsed.max_area) == (20, None)
    assert (parsed.min_price, parsed.max_price) == (None, 3_000_000)
    assert parse_query("phòng 25m2").min_area is None


@pytest.mark.parametrize("text, district", [
    ("phòng ở quận 1", "Quận 1"),       # must not match Quận 10
    ("phòng ở quận 10", "Quận 10"),
    ("thuê phòng q7", "Quận 7"),
    ("phòng gần cau giay", "Cầu Giấy"),  # accents optional
    ("phòng ở Cầu Giấy giá rẻ", "Cầu Giấy"),
])
def test_district_matching(text, district):
    assert parse_query(text, LOCATIONS).district == district


def test_ward_and_city_matching():
    parsed = parse_query("phòng ở phường Dịch Vọng, Hà Nội", LOCATIONS)
    assert parsed.ward == "Dịch Vọng"
    assert parsed.city == "Hà Nội"


def test_no_locations_known_means_no_location_filter():
    parsed = parse_query("phòng ở Cầu Giấy")
    assert (parsed.city, parsed.district, parsed.ward) == (None, None, None)
