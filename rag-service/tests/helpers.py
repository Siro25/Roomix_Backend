from app.models.schemas import RoomItem


def make_room(**overrides) -> RoomItem:
    data = dict(
        id="r1",
        title="Phòng trọ gần ĐH Bách Khoa",
        description="Phòng sạch sẽ, an ninh tốt",
        address="12 Tạ Quang Bửu",
        ward="Bách Khoa",
        district="Hai Bà Trưng",
        city="Hà Nội",
        area_sqm=25,
        price=3_500_000,
        deposit=3_500_000,
        electricity_cost=3500,
        water_cost=100000,
        amenities=["Máy lạnh", "Ban công"],
        rules=["Không hút thuốc"],
        contact_phone="0901234567",
        landlord_name="Anh Nam",
    )
    data.update(overrides)
    return RoomItem(**data)
