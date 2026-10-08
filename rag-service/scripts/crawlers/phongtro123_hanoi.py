#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Công cụ thu thập dữ liệu phòng trọ Hà Nội từ phongtro123.com
Hỗ trợ lấy các trường:
- Tiêu đề (Title)
- Mô tả chi tiết (Description)
- Ngõ / Ngách / Số nhà (Alley)
- Phường / Xã (Ward)
- Quận / Huyện (District)
- Thành phố (City)
- Tiện nghi (Amenities)
- Dịch vụ (Điện, nước, mạng, vệ sinh, gửi xe...)
- Số điện thoại (Phone number)
- Giá thuê & Diện tích
- Người đăng & Ngày đăng
- Link bài viết (URL)
"""

import sys
import os
import re
import json
import time
import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed

# Reconfigure stdout for utf-8 on Windows
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

import requests
from bs4 import BeautifulSoup
import pandas as pd


BASE_URL = "https://phongtro123.com"
DEFAULT_CATEGORY_URL = "https://phongtro123.com/tinh-thanh/ha-noi"

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
    "Accept-Language": "vi,en-US;q=0.9,en;q=0.8",
    "Referer": "https://phongtro123.com/"
}


def clean_text(text: str) -> str:
    """Loại bỏ khoảng trắng thừa và ký tự rác."""
    if not text:
        return ""
    text = re.sub(r'[\r\t]+', ' ', text)
    text = re.sub(r' +', ' ', text)
    lines = [line.strip() for line in text.split('\n')]
    return '\n'.join([l for l in lines if l])


def extract_services(description: str) -> dict:
    """Trích xuất chi phí dịch vụ từ nội dung mô tả."""
    services = {
        "dien": "",
        "nuoc": "",
        "mang_internet": "",
        "gui_xe": "",
        "dich_vu_chung": "",
        "tom_tat_dich_vu": ""
    }

    # 1. Điện (tránh bắt nhầm "điện thoại", "xe điện")
    m_dien = re.search(
        r'(?<!xe\s)(?<!số\s)(?<!gọi\s)(?<!liên\s)\b(?:tiền điện|giá điện|điện)\s*[:=–-]?\s*'
        r'([0-9.,kK]+\s*(?:đ|vnđ|k|nghìn|ngàn)?\s*(?:/|\s*trên\s*|\s*mỗi\s*)\s*(?:số|kwh|kw|người|tháng)|giá dân|giá nhà nước|thỏa thuận)',
        description,
        re.IGNORECASE
    )
    if m_dien:
        services["dien"] = m_dien.group(1).strip()

    # 2. Nước
    m_nuoc = re.search(
        r'\b(?:tiền nước|giá nước|nước)\s*[:=–-]?\s*'
        r'([0-9.,kK]+\s*(?:đ|vnđ|k|nghìn|ngàn)?\s*(?:/|\s*trên\s*|\s*mỗi\s*)\s*(?:khối|m3|m³|người|phòng|tháng)(?:\s*\([^\)]+\))?|giá dân|giá nhà nước|thỏa thuận)',
        description,
        re.IGNORECASE
    )
    if m_nuoc:
        services["nuoc"] = m_nuoc.group(1).strip()

    # 3. Mạng / Internet / Wifi
    m_net = re.search(
        r'\b(?:mạng|wifi|net|internet)\s*[:=–-]?\s*'
        r'([0-9.,kK]+\s*(?:đ|vnđ|k|nghìn|ngàn)?\s*(?:/|\s*mỗi\s*)?\s*(?:phòng|người|tháng)?|miễn phí|free)',
        description,
        re.IGNORECASE
    )
    if m_net:
        services["mang_internet"] = m_net.group(1).strip()

    # 4. Gửi xe
    m_xe = re.search(
        r'\b(?:gửi xe|tiền xe|phí xe|xe máy)\s*[:=–-]?\s*'
        r'([0-9.,kK]+\s*(?:đ|vnđ|k|nghìn|ngàn)?\s*(?:/|\s*mỗi\s*)?\s*(?:xe|tháng|người)?|miễn phí|free|để xe tầng 1|sân để xe)',
        description,
        re.IGNORECASE
    )
    if m_xe:
        services["gui_xe"] = m_xe.group(1).strip()

    # 5. Dịch vụ chung / Vệ sinh
    m_dvc = re.search(
        r'\b(?:dvc|dịch vụ chung|phí dịch vụ|vệ sinh|dvụ)\s*[:=–-]?\s*'
        r'([0-9.,kK]+\s*(?:đ|vnđ|k|nghìn|ngàn)?\s*(?:/|\s*mỗi\s*)?\s*(?:phòng|người|tháng)?(?:\s*\([^\)]+\))?)',
        description,
        re.IGNORECASE
    )
    if m_dvc:
        services["dich_vu_chung"] = m_dvc.group(1).strip()

    # Tóm tắt
    parts = []
    if services["dien"]:
        parts.append(f"Điện: {services['dien']}")
    if services["nuoc"]:
        parts.append(f"Nước: {services['nuoc']}")
    if services["mang_internet"]:
        parts.append(f"Internet: {services['mang_internet']}")
    if services["gui_xe"]:
        parts.append(f"Gửi xe: {services['gui_xe']}")
    if services["dich_vu_chung"]:
        parts.append(f"Dịch vụ/Vệ sinh: {services['dich_vu_chung']}")

    services["tom_tat_dich_vu"] = "; ".join(parts) if parts else "Theo thỏa thuận / giá dân"
    return services


def extract_location_info(full_address: str, title: str, description: str) -> dict:
    """Tách thông tin Ngõ, Phường, Quận, Thành phố từ địa chỉ và mô tả."""
    loc = {
        "ngo": "",
        "phuong": "",
        "quan_huyen": "",
        "thanh_pho": "Hà Nội"
    }

    combined = f"{full_address} {title} {description}"

    # 1. Thành phố
    loc["thanh_pho"] = "Hà Nội"

    # 2. Quận / Huyện
    m_quan = re.search(r'(?:quận|huyện|thị xã)\s+([A-Za-zÀ-ỹ0-9\s]+?)(?:,|\.|\n|Hà Nội|TP|$)', full_address, re.IGNORECASE)
    if m_quan:
        loc["quan_huyen"] = m_quan.group(1).strip()
    else:
        # Danh sách quận huyện Hà Nội
        districts = [
            "Ba Đình", "Bắc Từ Liêm", "Cầu Giấy", "Đống Đa", "Hà Đông", "Hai Bà Trưng",
            "Hoàn Kiếm", "Hoàng Mai", "Long Biên", "Nam Từ Liêm", "Tây Hồ", "Thanh Xuân",
            "Sơn Tây", "Ba Vì", "Chương Mỹ", "Đan Phượng", "Đông Anh", "Gia Lâm",
            "Hoài Đức", "Mê Linh", "Mỹ Đức", "Phú Xuyên", "Phúc Thọ", "Quốc Oai",
            "Sóc Sơn", "Thạch Thất", "Thanh Oai", "Thanh Trì", "Thường Tín", "Ứng Hòa"
        ]
        for d in districts:
            if re.search(r'\b' + re.escape(d) + r'\b', combined, re.IGNORECASE):
                loc["quan_huyen"] = d
                break

    # 3. Phường / Xã
    m_phuong = re.search(r'(?:phường|p\.|xã)\s+([A-Za-zÀ-ỹ0-9\s]+?)(?:,|\.|\n|quận|huyện|tp|hà nội|$)', full_address, re.IGNORECASE)
    if m_phuong:
        loc["phuong"] = m_phuong.group(1).strip()
    else:
        # Tìm trong mô tả hoặc tiêu đề
        m_phuong_desc = re.search(r'(?:phường|p\.|xã)\s+([A-Za-zÀ-ỹ0-9\s]+?)(?:,|\.|\n|quận|huyện|tp|hà nội|$)', combined, re.IGNORECASE)
        if m_phuong_desc:
            loc["phuong"] = m_phuong_desc.group(1).strip()

    # 4. Ngõ / Ngách / Hẻm / Số nhà
    # Ưu tiên tìm trong địa chỉ chính thức
    m_ngo = re.search(r'((?:ngách\s+[\w\d/]+(?:\s+ngõ|\s*,\s*ngõ)?\s*)?ngõ\s+[^,;\n]+|(?:hẻm|kiệt)\s+[^,;\n]+)', full_address, re.IGNORECASE)
    if m_ngo:
        loc["ngo"] = m_ngo.group(1).strip()
    else:
        # Kiểm tra định dạng số nhà dạng ngõ như 48/22
        m_slash = re.search(r'^(\d+[A-Za-z]?(?:/\d+[A-Za-z]?)+)', full_address)
        if m_slash:
            parts = m_slash.group(1).split('/')
            loc["ngo"] = f"Ngõ {parts[1]} (Số {parts[0]}/{parts[1]})"
        else:
            # Tìm trong tiêu đề hoặc mô tả
            m_ngo_desc = re.search(r'((?:ngách\s+[\w\d/]+(?:\s+ngõ|\s*,\s*ngõ)?\s*)?ngõ\s+[\w\d/]+(?:\s+phố|\s+đường)?\s*[\w\dÀ-ỹ\s]*?)(?:,|\.|\n|quận|phường|$)', combined, re.IGNORECASE)
            if m_ngo_desc:
                loc["ngo"] = m_ngo_desc.group(1).strip()
            else:
                # Nếu mặt phố
                if re.search(r'\bmặt\s+(?:phố|đường)\b', combined, re.IGNORECASE):
                    loc["ngo"] = "Mặt phố (Không có ngõ)"
                else:
                    loc["ngo"] = ""

    return loc


def extract_amenities(soup: BeautifulSoup, title: str, description: str) -> list:
    """Trích xuất tiện nghi của phòng trọ."""
    amenities = []

    # 1. Từ khối "Nổi bật" trên trang web (kiểm tra trạng thái kích hoạt)
    for h2 in soup.find_all(["h2", "h3"]):
        if any(k in h2.text.lower() for k in ["nổi bật", "tiện nghi", "tiện ích"]):
            parent = h2.find_parent("div") or h2.find_parent("section")
            if parent:
                for item in parent.find_all("div", class_="col-3"):
                    div = item.find("div")
                    if div:
                        style = div.get("style", "")
                        # Thuộc tính có kích hoạt thì opacity không bằng 0.1
                        if "0.1" not in style and "opacity" not in style and "disabled" not in div.get("class", []):
                            txt = div.text.strip()
                            if txt and txt not in amenities:
                                amenities.append(txt)

    # 2. Bổ sung từ khóa trong Tiêu đề và Mô tả
    common_keywords = {
        "Đầy đủ nội thất": [r"full\s*nội\s*thất", r"đầy\s*đủ\s*nội\s*thất", r"full\s*đồ"],
        "Điều hòa": [r"điều\s*hòa", r"máy\s*lạnh", r"ac\b"],
        "Nóng lạnh": [r"nóng\s*lạnh", r"bình\s*nóng\s*lạnh"],
        "Tủ lạnh": [r"tủ\s*lạnh"],
        "Máy giặt": [r"máy\s*giặt"],
        "Giường nệm": [r"giường", r"đệm"],
        "Tủ quần áo": [r"tủ\s*quần\s*áo", r"tủ\s*đồ"],
        "Bếp nấu / Kệ bếp": [r"kệ\s*bếp", r"tủ\s*bếp", r"bếp\s*nấu", r"nấu\s*ăn"],
        "Thang máy": [r"thang\s*máy"],
        "Gác xép / Gác lửng": [r"gác\s*xép", r"gác\s*lửng", r"có\s*gác"],
        "Ban công / Cửa sổ": [r"ban\s*công", r"cửa\s*sổ"],
        "Vệ sinh khép kín": [r"khép\s*kín", r"wc\s*riêng", r"vệ\s*sinh\s*riêng"],
        "Không chung chủ": [r"không\s*chung\s*chủ"],
        "Giờ giấc tự do": [r"giờ\s*giấc\s*tự\s*do", r"tự\s*do\s*thời\s*gian"],
        "Chỗ để xe / Hầm để xe": [r"để\s*xe", r"hầm\s*xe", r"nhà\s*xe"],
        "An ninh / Khóa vân tay / Camera": [r"khóa\s*vân\s*tay", r"camera", r"bảo\s*vệ"]
    }

    full_text = f"{title} {description}"
    for label, patterns in common_keywords.items():
        if label not in amenities:
            for pat in patterns:
                if re.search(pat, full_text, re.IGNORECASE):
                    amenities.append(label)
                    break

    return amenities


def extract_phone_number(soup: BeautifulSoup, description: str) -> str:
    """Trích xuất số điện thoại chủ nhà / người đăng."""
    # 1. Từ JSON-LD Hostel
    for s in soup.find_all("script", type="application/ld+json"):
        try:
            data = json.loads(s.string)
            if data.get("@type") == "Hostel" and data.get("telephone"):
                phone = str(data["telephone"]).strip()
                if re.match(r'^0\d{8,10}$', phone):
                    return phone
        except Exception:
            pass

    # 2. Từ khối Thông tin liên hệ (tránh hotline chăm sóc khách hàng ở chân trang)
    for h2 in soup.find_all(["h2", "h3"]):
        if "liên hệ" in h2.text.lower():
            sec = h2.find_parent(["section", "div", "aside"])
            if sec:
                # Nút tel
                tel = sec.select_one('a[href^="tel:"]')
                if tel:
                    p = re.sub(r'[^\d]', '', tel.get("href", ""))
                    if p.startswith("0") and len(p) >= 10:
                        return p
                # Nút Zalo
                zalo = sec.select_one('a[href*="zalo.me/"]')
                if zalo:
                    m = re.search(r'zalo\.me/(\d+)', zalo["href"])
                    if m:
                        return m.group(1)

    # 3. Nút data-phone
    for btn in soup.select('[data-phone]'):
        p = btn.get("data-phone", "").strip()
        if p.startswith("0") and len(p) >= 10:
            return p

    # 4. Tìm số điện thoại trong phần mô tả
    m = re.search(r'(?:liên\s*hệ|lh|đt|sđt|zalo|hotline|gọi|chủ\s*nhà)?\s*[:.-]?\s*(0[3|5|7|8|9][0-9]{8})\b', description, re.IGNORECASE)
    if m:
        return m.group(1)

    return ""


def parse_detail_page(url: str, session: requests.Session) -> dict:
    """Tải và trích xuất toàn bộ dữ liệu của một bài đăng phòng trọ."""
    try:
        resp = session.get(url, headers=HEADERS, timeout=15)
        if resp.status_code != 200:
            return None

        soup = BeautifulSoup(resp.text, "html.parser")

        # Khởi tạo data
        item = {
            "id": "",
            "tieu_de": "",
            "gia_thue": "",
            "dien_tich": "",
            "dia_chi_day_du": "",
            "ngo": "",
            "phuong": "",
            "quan_huyen": "",
            "thanh_pho": "Hà Nội",
            "so_dien_thoai": "",
            "tien_nghi": "",
            "dich_vu": "",
            "gia_dien": "",
            "gia_nuoc": "",
            "mang_internet": "",
            "phi_dich_vu_khac": "",
            "mo_ta": "",
            "nguoi_dang": "",
            "ngay_dang": "",
            "url": url
        }

        # ID bài viết từ URL
        id_match = re.search(r'-pr(\d+)\.html', url)
        if id_match:
            item["id"] = f"#{id_match.group(1)}"

        # 1. Tiêu đề
        h1 = soup.find("h1")
        if h1:
            item["tieu_de"] = h1.text.strip()

        # 2. Thông tin từ bảng thuộc tính (Specs table)
        specs = {}
        for tr in soup.find_all("tr"):
            tds = tr.find_all(["td", "th"])
            if len(tds) >= 2:
                k = tds[0].text.strip().rstrip(":").lower()
                v = tds[1].text.strip()
                specs[k] = v

        if "địa chỉ" in specs:
            item["dia_chi_day_du"] = specs["địa chỉ"]
        if "quận huyện" in specs:
            item["quan_huyen"] = specs["quận huyện"].replace("Cho thuê phòng trọ", "").strip()
        if "tỉnh thành" in specs:
            item["thanh_pho"] = specs["tỉnh thành"]
        if "ngày đăng" in specs:
            item["ngay_dang"] = specs["ngày đăng"]

        # 3. Metadata từ JSON-LD
        for s in soup.find_all("script", type="application/ld+json"):
            try:
                js = json.loads(s.string)
                if js.get("@type") == "Hostel":
                    if "priceRange" in js and not item["gia_thue"]:
                        val = js["priceRange"]
                        if val.isdigit():
                            item["gia_thue"] = f"{int(val):,} đ/tháng".replace(",", ".")
                        else:
                            item["gia_thue"] = val
                    if "address" in js and isinstance(js["address"], dict):
                        if not item["dia_chi_day_du"]:
                            item["dia_chi_day_du"] = js["address"].get("streetAddress", "")
                        if "addressLocality" in js["address"]:
                            item["thanh_pho"] = js["address"]["addressLocality"]
            except Exception:
                pass

        # 4. Giá thuê & Diện tích từ Post Header / Post Bar
        for span in soup.select('.post-header span, .post-attribute span, [class*="price"], [class*="area"]'):
            text = span.text.strip()
            if "triệu/tháng" in text or "tr/tháng" in text or "đ/tháng" in text:
                if not item["gia_thue"]:
                    item["gia_thue"] = text
            if "m²" in text or "m2" in text:
                if not item["dien_tich"]:
                    item["dien_tich"] = text

        # 5. Mô tả chi tiết
        for h2 in soup.find_all(["h2", "h3"]):
            if "mô tả" in h2.text.lower():
                parent = h2.find_parent("div") or h2.find_parent("section")
                if parent:
                    # Lấy các đoạn text nội dung
                    paragraphs = [p.text.strip() for p in parent.find_all(["p", "div", "li"]) if p.text.strip() and p.name != "h2"]
                    if paragraphs:
                        desc = "\n".join(paragraphs)
                    else:
                        desc = parent.text.strip().replace(h2.text.strip(), "").strip()
                    item["mo_ta"] = clean_text(desc)
                break

        # Nếu chưa tìm thấy mô tả
        if not item["mo_ta"]:
            content_div = soup.find(class_=lambda c: c and any(k in c for k in ["section-post-description", "post-content"]))
            if content_div:
                item["mo_ta"] = clean_text(content_div.text.strip())

        # 6. Người đăng
        for h2 in soup.find_all(["h2", "h3"]):
            if "liên hệ" in h2.text.lower():
                sec = h2.find_parent(["section", "div", "aside"])
                if sec:
                    # Tìm tên người đăng
                    name_div = sec.find(class_=lambda c: c and "fw-medium" in c)
                    if name_div:
                        item["nguoi_dang"] = name_div.text.strip()
                break

        # 7. Số điện thoại
        item["so_dien_thoai"] = extract_phone_number(soup, item["mo_ta"])

        # 8. Địa chỉ, Ngõ, Phường, Quận, Thành phố
        loc = extract_location_info(item["dia_chi_day_du"], item["tieu_de"], item["mo_ta"])
        item["ngo"] = loc["ngo"]
        item["phuong"] = loc["phuong"]
        if not item["quan_huyen"]:
            item["quan_huyen"] = loc["quan_huyen"]
        item["thanh_pho"] = loc["thanh_pho"]

        # 9. Tiện nghi
        amenities_list = extract_amenities(soup, item["tieu_de"], item["mo_ta"])
        item["tien_nghi"] = ", ".join(amenities_list)

        # 10. Dịch vụ (Điện, nước, internet, gửi xe, dvc...)
        serv = extract_services(item["mo_ta"])
        item["dich_vu"] = serv["tom_tat_dich_vu"]
        item["gia_dien"] = serv["dien"]
        item["gia_nuoc"] = serv["nuoc"]
        item["mang_internet"] = serv["mang_internet"]
        item["phi_dich_vu_khac"] = serv["dich_vu_chung"]

        return item
    except Exception as e:
        print(f"[!] Lỗi khi cào URL {url}: {e}")
        return None


def get_post_links_from_page(page_url: str, session: requests.Session) -> list:
    """Lấy danh sách các URL bài đăng chi tiết từ cột chính của trang danh mục (tránh tin sidebar toàn quốc)."""
    post_links = []
    try:
        resp = session.get(page_url, headers=HEADERS, timeout=15)
        if resp.status_code != 200:
            print(f"[!] Không thể tải trang danh mục: {page_url} (HTTP {resp.status_code})")
            return post_links

        soup = BeautifulSoup(resp.text, "html.parser")
        
        # Chỉ quét trong cột danh sách chính, bỏ qua sidebar tin nổi bật toàn quốc
        main_col = soup.select_one("main .col-lg-8, main .col-md-9, #left-col, .post-listing, ul.post-listing")
        search_scope = main_col if main_col else soup

        for a in search_scope.find_all("a", href=True):
            href = a["href"]
            # Các bài đăng chi tiết kết thúc bằng -pr{id}.html
            if re.search(r'-pr\d+\.html$', href):
                full_url = href if href.startswith("http") else f"{BASE_URL}{href}"
                if full_url not in post_links:
                    post_links.append(full_url)
    except Exception as e:
        print(f"[!] Lỗi tải danh sách bài viết từ {page_url}: {e}")

    return post_links


def crawl_hanoi_phongtro(start_page: int = 1, end_page: int = 1, output_file: str = "data/market/phongtro_hanoi.xlsx", delay: float = 0.5, max_workers: int = 4) -> pd.DataFrame:
    """
    Hàm chính điều phối quá trình cào dữ liệu từ phongtro123.com Hà Nội theo dải trang (từ start_page đến end_page).
    """
    if start_page > end_page:
        start_page, end_page = end_page, start_page

    total_pages = end_page - start_page + 1

    print("=" * 60)
    print("🚀 BẮT ĐẦU CÀO DỮ LIỆU PHÒNG TRỌ HÀ NỘI TỪ PHONGTRO123.COM")
    print(f"📄 Dải trang cần quét: Từ trang {start_page} đến trang {end_page} (Tổng: {total_pages} trang)")
    print(f"💾 File đầu ra: {output_file}")
    print("=" * 60)

    session = requests.Session()
    all_links = []

    # 1. Thu thập URL bài viết từ dải trang danh mục
    for page in range(start_page, end_page + 1):
        if page == 1:
            page_url = DEFAULT_CATEGORY_URL
        else:
            page_url = f"{DEFAULT_CATEGORY_URL}?page={page}"

        print(f"\n🔍 Đang quét trang {page}/{end_page}: {page_url}")
        links = get_post_links_from_page(page_url, session)
        print(f"   -> Tìm thấy {len(links)} tin đăng tại trang {page}.")
        for l in links:
            if l not in all_links:
                all_links.append(l)

        if page < end_page:
            time.sleep(delay)

    print(f"\n✨ Tổng số tin đăng cần thu thập chi tiết: {len(all_links)}")
    if not all_links:
        print("[!] Không tìm thấy bài đăng nào trong dải trang đã chọn. Kết thúc.")
        return pd.DataFrame()

    # 2. Thu thập chi tiết từng bài đăng bằng ThreadPool
    results = []
    print(f"\n📥 Đang tiến hành tải thông tin chi tiết ({max_workers} luồng)...")

    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        futures = {executor.submit(parse_detail_page, url, session): url for url in all_links}
        completed = 0
        total = len(futures)

        for future in as_completed(futures):
            completed += 1
            res = future.result()
            if res:
                results.append(res)
                print(f"[{completed}/{total}] ✔ Đã cào: {res['tieu_de'][:45]}... | SĐT: {res['so_dien_thoai'] or 'Chưa rõ'}")
            else:
                print(f"[{completed}/{total}] ✘ Thất bại")
            time.sleep(delay / max_workers)

    # 3. Xuất file kết quả
    if not results:
        print("[!] Không thu thập được dữ liệu chi tiết nào.")
        return pd.DataFrame()

    df = pd.DataFrame(results)

    # Đảm bảo số điện thoại giữ đúng số 0 ở đầu (chuỗi text)
    if "so_dien_thoai" in df.columns:
        df["so_dien_thoai"] = df["so_dien_thoai"].astype(str).apply(
            lambda x: f"0{x}" if x and x.isdigit() and len(x) == 9 else ("" if x in ["nan", "None"] else str(x))
        )

    # Đổi thứ tự cột cho thân thiện và chuẩn hóa (đã loại bỏ id, ngay_dang, url và mo_ta)
    columns_order = [
        "tieu_de", "so_dien_thoai", "nguoi_dang", "gia_thue", "dien_tich",
        "ngo", "phuong", "quan_huyen", "thanh_pho", "dia_chi_day_du",
        "tien_nghi", "dich_vu", "gia_dien", "gia_nuoc", "mang_internet", "phi_dich_vu_khac"
    ]
    df = df[[c for c in columns_order if c in df.columns]]

    # Lưu ra các định dạng
    output_dir = os.path.dirname(os.path.abspath(output_file))
    os.makedirs(output_dir, exist_ok=True)
    file_ext = os.path.splitext(output_file)[1].lower()
    if file_ext in [".xlsx", ".xls"]:
        df.to_excel(output_file, index=False, engine="openpyxl")
    elif file_ext == ".json":
        df.to_json(output_file, orient="records", force_ascii=False, indent=2)
    else:
        df.to_csv(output_file, index=False, encoding="utf-8-sig")

    # Lưu thêm bản CSV và JSON tự động để người dùng tiện sử dụng
    base_name = os.path.splitext(output_file)[0]
    csv_file = f"{base_name}.csv"
    json_file = f"{base_name}.json"
    if file_ext != ".csv":
        df.to_csv(csv_file, index=False, encoding="utf-8-sig")
    if file_ext != ".json":
        df.to_json(json_file, orient="records", force_ascii=False, indent=2)

    print("\n" + "=" * 60)
    print("🎉 HOÀN THÀNH THU THẬP DỮ LIỆU!")
    print(f"📊 Tổng số phòng trọ cào thành công: {len(df)}")
    print(f"📁 File Excel đã lưu: {os.path.abspath(output_file)}")
    print(f"📁 File CSV đã lưu:   {os.path.abspath(csv_file)}")
    print(f"📁 File JSON đã lưu:  {os.path.abspath(json_file)}")
    print("=" * 60)

    return df


def main():
    parser = argparse.ArgumentParser(description="Tool cào dữ liệu phòng trọ Hà Nội từ phongtro123.com")
    parser.add_argument("--from-page", type=int, default=None, help="Trang bắt đầu cào (ví dụ: 1)")
    parser.add_argument("--to-page", type=int, default=None, help="Trang kết thúc cào (ví dụ: 5)")
    parser.add_argument("--range", type=str, default="", help="Dải trang dạng '1-5' hoặc '2-10'")
    parser.add_argument("--pages", type=int, default=None, help="Số lượng trang muốn cào tính từ trang 1")
    parser.add_argument("--output", type=str, default="data/market/phongtro_hanoi.xlsx", help="Tên file xuất dữ liệu (.xlsx, .csv, .json)")
    parser.add_argument("--delay", type=float, default=0.5, help="Thời gian chờ giữa các request (giây)")
    parser.add_argument("--workers", type=int, default=4, help="Số luồng cào song song")
    parser.add_argument("--url", type=str, default="", help="Cào riêng 1 link bài viết cụ thể")

    args = parser.parse_args()

    # 1. Cào riêng 1 URL bài viết
    if args.url:
        print(f"🔍 Đang cào riêng 1 bài viết: {args.url}")
        session = requests.Session()
        res = parse_detail_page(args.url, session)
        if res:
            res.pop("id", None)
            res.pop("ngay_dang", None)
            res.pop("url", None)
            print("\nKết quả trích xuất:")
            print(json.dumps(res, ensure_ascii=False, indent=2))
        else:
            print("[!] Cào thất bại.")
        return

    # 2. Xác định start_page và end_page
    start_page = 1
    end_page = 1

    if args.range:
        # Hỗ trợ dạng "2-5" hoặc "2:5"
        parts = re.split(r'[-:,]', args.range)
        if len(parts) == 2 and parts[0].strip().isdigit() and parts[1].strip().isdigit():
            start_page = int(parts[0].strip())
            end_page = int(parts[1].strip())
        elif len(parts) == 1 and parts[0].strip().isdigit():
            start_page = 1
            end_page = int(parts[0].strip())
    elif args.from_page is not None or args.to_page is not None:
        start_page = args.from_page if args.from_page is not None else 1
        end_page = args.to_page if args.to_page is not None else start_page
    elif args.pages is not None:
        start_page = 1
        end_page = args.pages
    else:
        # Nếu chạy trực tiếp `python tool.py` trong terminal tương tác
        if sys.stdin and sys.stdin.isatty():
            print("=" * 60)
            print("   CHỌN DẢI TRANG CẦN CÀO (PHONGTRO123 - HÀ NỘI)")
            print("=" * 60)
            try:
                inp_start = input("👉 Nhập trang bắt đầu [Mặc định 1]: ").strip()
                start_page = int(inp_start) if inp_start.isdigit() else 1
                
                inp_end = input(f"👉 Nhập trang kết thúc [Mặc định {start_page}]: ").strip()
                end_page = int(inp_end) if inp_end.isdigit() else start_page
            except (KeyboardInterrupt, EOFError):
                print("\nĐã hủy.")
                return
        else:
            start_page = 1
            end_page = 1

    crawl_hanoi_phongtro(
        start_page=start_page,
        end_page=end_page,
        output_file=args.output,
        delay=args.delay,
        max_workers=args.workers
    )


if __name__ == "__main__":
    main()
