#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Module đánh giá giá phòng trọ so với thị trường nội bộ.

Phân tích dựa trên dữ liệu phòng trọ hiện có trong hệ thống:
- So sánh giá theo quận/huyện
- So sánh giá theo số lượng tiện nghi tương đương
- Tính percentile và phân loại: Rẻ / Hợp lý / Cao / Đắt
- Đưa ra nhận xét tổng quan cho người dùng
"""

import re
import pandas as pd
import numpy as np
from typing import Optional


def parse_price(price_str: str) -> Optional[float]:
    """
    Chuyển chuỗi giá thuê sang số (đơn vị: triệu đồng/tháng).

    Hỗ trợ các định dạng:
    - "3.500.000 đ/tháng" -> 3.5
    - "1.600.000 đ/tháng" -> 1.6
    - "4,5 triệu/tháng" -> 4.5
    - "700.000 đ/tháng" -> 0.7
    """
    if not price_str or not isinstance(price_str, str):
        return None

    price_str = price_str.strip()

    # Dạng "X.XXX.XXX đ/tháng" hoặc "X,XXX,XXX đ/tháng"
    match = re.search(r'([\d.,]+)\s*(?:đ|vnđ|đồng)', price_str, re.IGNORECASE)
    if match:
        num_str = match.group(1)
        # Loại bỏ dấu chấm phân cách hàng nghìn, giữ dấu phẩy làm thập phân
        # VD: "3.500.000" -> 3500000 hoặc "3,500,000" -> 3500000
        if num_str.count('.') >= 2:
            num_str = num_str.replace('.', '')
        elif num_str.count(',') >= 2:
            num_str = num_str.replace(',', '')
        elif '.' in num_str and ',' in num_str:
            num_str = num_str.replace('.', '').replace(',', '.')

        try:
            value = float(num_str)
            return value / 1_000_000  # Chuyển sang triệu
        except ValueError:
            pass

    # Dạng "X triệu/tháng" hoặc "X tr/tháng"
    match = re.search(r'([\d.,]+)\s*(?:triệu|tr)', price_str, re.IGNORECASE)
    if match:
        num_str = match.group(1).replace(',', '.')
        try:
            return float(num_str)
        except ValueError:
            pass

    return None


def count_amenities(amenities_str: str) -> int:
    """Đếm số lượng tiện nghi từ chuỗi tiện nghi (phân tách bởi dấu phẩy)."""
    if not amenities_str or not isinstance(amenities_str, str):
        return 0
    items = [a.strip() for a in amenities_str.split(',') if a.strip()]
    return len(items)


class MarketEvaluator:
    """
    Đánh giá giá phòng trọ so với thị trường nội bộ.

    Sử dụng dữ liệu từ file CSV để tính toán thống kê giá
    theo quận/huyện và mức tiện nghi, từ đó đưa ra đánh giá.
    """

    def __init__(self, csv_path: str):
        """
        Khởi tạo MarketEvaluator từ file CSV dữ liệu phòng trọ.

        Args:
            csv_path: Đường dẫn tới file CSV chứa dữ liệu phòng trọ.
        """
        self.df = pd.read_csv(csv_path, encoding='utf-8-sig')
        self._prepare_data()

    def _prepare_data(self):
        """Tiền xử lý dữ liệu: parse giá, đếm tiện nghi, loại bỏ dòng thiếu giá."""
        self.df['gia_so'] = self.df['gia_thue'].apply(parse_price)
        self.df['so_tien_nghi'] = self.df['tien_nghi'].apply(count_amenities)

        # Chuẩn hoá tên quận/huyện
        self.df['quan_huyen_clean'] = self.df['quan_huyen'].apply(
            lambda x: x.strip().lower() if isinstance(x, str) else ''
        )

        # Chỉ giữ các dòng có giá hợp lệ
        self.df_valid = self.df[self.df['gia_so'].notna()].copy()

        # Phân nhóm tiện nghi: ít (0-5), trung bình (6-10), nhiều (>10)
        self.df_valid['nhom_tien_nghi'] = self.df_valid['so_tien_nghi'].apply(
            lambda x: 'ít' if x <= 5 else ('trung bình' if x <= 10 else 'nhiều')
        )

    def get_district_stats(self, district: str) -> dict:
        """
        Thống kê giá phòng trọ trong một quận/huyện.

        Args:
            district: Tên quận/huyện cần tra cứu.

        Returns:
            Dict chứa min, max, mean, median, count, std. Trả về None nếu không có dữ liệu.
        """
        district_clean = district.strip().lower()
        # Tìm kiếm mềm: chứa tên quận
        mask = self.df_valid['quan_huyen_clean'].str.contains(district_clean, na=False)
        subset = self.df_valid[mask]

        if subset.empty:
            return None

        prices = subset['gia_so']
        return {
            'quan_huyen': district,
            'so_phong': len(subset),
            'gia_min': round(prices.min(), 2),
            'gia_max': round(prices.max(), 2),
            'gia_trung_binh': round(prices.mean(), 2),
            'gia_trung_vi': round(prices.median(), 2),
            'do_lech_chuan': round(prices.std(), 2) if len(prices) > 1 else 0,
        }

    def get_overall_stats(self) -> dict:
        """Thống kê giá toàn bộ dữ liệu trong hệ thống."""
        prices = self.df_valid['gia_so']
        if prices.empty:
            return None

        return {
            'tong_so_phong': len(prices),
            'gia_min': round(prices.min(), 2),
            'gia_max': round(prices.max(), 2),
            'gia_trung_binh': round(prices.mean(), 2),
            'gia_trung_vi': round(prices.median(), 2),
        }

    def evaluate_room(self, gia_thue: str, quan_huyen: str, tien_nghi: str = "",
                      dien_tich: str = "", tieu_de: str = "") -> str:
        """
        Đánh giá giá phòng trọ so với thị trường nội bộ.

        Args:
            gia_thue: Giá thuê dạng chuỗi (VD: "3.500.000 đ/tháng")
            quan_huyen: Quận/huyện của phòng trọ
            tien_nghi: Chuỗi tiện nghi (phân tách bởi dấu phẩy)
            dien_tich: Diện tích phòng (VD: "25m²")
            tieu_de: Tiêu đề bài đăng

        Returns:
            Chuỗi đánh giá chi tiết về giá phòng trọ so với thị trường.
        """
        price = parse_price(gia_thue)
        if price is None:
            return "⚠️ Không thể phân tích giá thuê để đánh giá thị trường."

        result_parts = []
        result_parts.append(f"💰 **Giá phòng**: {gia_thue} (~{price:.1f} triệu/tháng)")

        # === 1. So sánh với toàn bộ thị trường ===
        overall = self.get_overall_stats()
        if overall:
            all_prices = self.df_valid['gia_so'].values
            percentile_all = (np.sum(all_prices <= price) / len(all_prices)) * 100

            result_parts.append(
                f"\n📊 **So với toàn bộ hệ thống** ({overall['tong_so_phong']} phòng):"
                f"\n   • Giá thấp nhất: {overall['gia_min']:.1f} tr — Giá cao nhất: {overall['gia_max']:.1f} tr"
                f"\n   • Giá trung bình: {overall['gia_trung_binh']:.1f} tr — Giá trung vị: {overall['gia_trung_vi']:.1f} tr"
                f"\n   • Phòng này thuộc mức **{self._percentile_label(percentile_all)}** "
                f"(rẻ hơn {percentile_all:.0f}% các phòng trong hệ thống)"
            )

        # === 2. So sánh trong cùng quận/huyện ===
        if quan_huyen:
            district_stats = self.get_district_stats(quan_huyen)
            if district_stats and district_stats['so_phong'] >= 2:
                district_clean = quan_huyen.strip().lower()
                mask = self.df_valid['quan_huyen_clean'].str.contains(district_clean, na=False)
                district_prices = self.df_valid[mask]['gia_so'].values
                percentile_dist = (np.sum(district_prices <= price) / len(district_prices)) * 100

                result_parts.append(
                    f"\n🏘️ **So với khu vực {quan_huyen}** ({district_stats['so_phong']} phòng):"
                    f"\n   • Giá thấp nhất: {district_stats['gia_min']:.1f} tr — Giá cao nhất: {district_stats['gia_max']:.1f} tr"
                    f"\n   • Giá trung bình: {district_stats['gia_trung_binh']:.1f} tr — Giá trung vị: {district_stats['gia_trung_vi']:.1f} tr"
                    f"\n   • Phòng này thuộc mức **{self._percentile_label(percentile_dist)}** trong khu vực"
                )

                # Chênh lệch so với trung bình quận
                diff = price - district_stats['gia_trung_binh']
                if abs(diff) < 0.1:
                    result_parts.append("   • ➡️ Giá gần bằng mức trung bình khu vực")
                elif diff > 0:
                    result_parts.append(f"   • ⬆️ Cao hơn trung bình khu vực {abs(diff):.1f} triệu")
                else:
                    result_parts.append(f"   • ⬇️ Thấp hơn trung bình khu vực {abs(diff):.1f} triệu")
            elif district_stats and district_stats['so_phong'] == 1:
                result_parts.append(
                    f"\n🏘️ **Khu vực {quan_huyen}**: Chỉ có 1 phòng trong hệ thống, "
                    f"chưa đủ dữ liệu để so sánh chi tiết."
                )
            else:
                result_parts.append(
                    f"\n🏘️ **Khu vực {quan_huyen}**: Không có dữ liệu phòng trọ khác để so sánh."
                )

        # === 3. So sánh theo mức tiện nghi tương đương ===
        num_amenities = count_amenities(tien_nghi)
        if num_amenities > 0:
            nhom = 'ít' if num_amenities <= 5 else ('trung bình' if num_amenities <= 10 else 'nhiều')
            mask_nhom = self.df_valid['nhom_tien_nghi'] == nhom
            subset_nhom = self.df_valid[mask_nhom]

            if len(subset_nhom) >= 2:
                nhom_prices = subset_nhom['gia_so'].values
                percentile_nhom = (np.sum(nhom_prices <= price) / len(nhom_prices)) * 100
                nhom_mean = nhom_prices.mean()

                result_parts.append(
                    f"\n🛋️ **So với phòng cùng mức tiện nghi** (nhóm '{nhom}' — {len(subset_nhom)} phòng):"
                    f"\n   • Giá trung bình nhóm: {nhom_mean:.1f} tr"
                    f"\n   • Phòng này thuộc mức **{self._percentile_label(percentile_nhom)}** trong nhóm"
                )

        # === 4. Kết luận tổng quan ===
        verdict = self._final_verdict(price, overall, quan_huyen, tien_nghi)
        result_parts.append(f"\n🏷️ **Kết luận**: {verdict}")

        return "\n".join(result_parts)

    def _percentile_label(self, percentile: float) -> str:
        """Phân loại mức giá dựa trên percentile."""
        if percentile <= 20:
            return "🟢 Rất rẻ"
        elif percentile <= 40:
            return "🟢 Rẻ"
        elif percentile <= 60:
            return "🟡 Hợp lý"
        elif percentile <= 80:
            return "🟠 Hơi cao"
        else:
            return "🔴 Đắt"

    def _final_verdict(self, price: float, overall: dict,
                       quan_huyen: str, tien_nghi: str) -> str:
        """Đưa ra nhận xét tổng hợp dựa trên tất cả các chỉ số."""
        if overall is None:
            return "Không đủ dữ liệu để đánh giá."

        num_amenities = count_amenities(tien_nghi)
        reasons = []

        # So với toàn hệ thống
        all_prices = self.df_valid['gia_so'].values
        percentile_all = (np.sum(all_prices <= price) / len(all_prices)) * 100

        # So với quận
        percentile_dist = None
        if quan_huyen:
            district_clean = quan_huyen.strip().lower()
            mask = self.df_valid['quan_huyen_clean'].str.contains(district_clean, na=False)
            district_prices = self.df_valid[mask]['gia_so'].values
            if len(district_prices) >= 2:
                percentile_dist = (np.sum(district_prices <= price) / len(district_prices)) * 100

        # Phân tích
        if percentile_all <= 25:
            reasons.append("giá thuộc nhóm thấp nhất thị trường")
        elif percentile_all <= 50:
            reasons.append("giá ở mức phải chăng so với thị trường")
        elif percentile_all <= 75:
            reasons.append("giá ở mức trung bình cao")
        else:
            reasons.append("giá thuộc nhóm cao nhất thị trường")

        if percentile_dist is not None:
            if percentile_dist <= 30:
                reasons.append(f"rẻ so với khu vực {quan_huyen}")
            elif percentile_dist >= 70:
                reasons.append(f"đắt so với khu vực {quan_huyen}")
            else:
                reasons.append(f"phù hợp mặt bằng giá khu vực {quan_huyen}")

        if num_amenities >= 10:
            reasons.append("nhiều tiện nghi đi kèm")
        elif num_amenities >= 5:
            reasons.append("tiện nghi ở mức trung bình")
        elif num_amenities > 0:
            reasons.append("ít tiện nghi")

        # Xếp loại tổng
        if percentile_all <= 30 and num_amenities >= 8:
            overall_rating = "⭐ RẤT ĐÁNG CÂN NHẮC — Giá rẻ + nhiều tiện nghi"
        elif percentile_all <= 50 and num_amenities >= 5:
            overall_rating = "✅ HỢP LÝ — Giá tốt so với những gì nhận được"
        elif percentile_all >= 75 and num_amenities < 5:
            overall_rating = "❌ KHÔNG HỢP LÝ — Giá cao nhưng ít tiện nghi, nên cân nhắc thêm"
        elif percentile_all >= 70:
            overall_rating = "⚠️ GIÁ CAO — Tuy nhiên có thể hợp lý nếu vị trí tốt hoặc phòng mới"
        else:
            overall_rating = "✅ GIÁ CHẤP NHẬN ĐƯỢC — Phù hợp mặt bằng chung"

        detail = "; ".join(reasons)
        return f"{overall_rating}\n   📝 Chi tiết: {detail}."

    def find_similar_rooms(self, quan_huyen: str, gia_thue: str,
                           top_n: int = 3) -> str:
        """
        Tìm các phòng tương tự trong cùng quận để gợi ý cho người dùng.

        Args:
            quan_huyen: Quận/huyện cần tìm
            gia_thue: Giá thuê hiện tại để tính khoảng cách giá
            top_n: Số phòng gợi ý tối đa

        Returns:
            Chuỗi mô tả các phòng tương tự.
        """
        price = parse_price(gia_thue)
        if price is None:
            return ""

        district_clean = quan_huyen.strip().lower()
        mask = self.df_valid['quan_huyen_clean'].str.contains(district_clean, na=False)
        subset = self.df_valid[mask].copy()

        if subset.empty:
            return ""

        # Tính khoảng cách giá
        subset['chenh_lech'] = (subset['gia_so'] - price).abs()
        # Loại bỏ chính phòng đang xét (nếu trùng giá chính xác)
        similar = subset.sort_values('chenh_lech').head(top_n + 1)
        # Lọc ra các phòng khác (không trùng giá hoàn toàn hoặc lấy top_n)
        similar = similar.head(top_n)

        if similar.empty:
            return ""

        lines = [f"\n🔍 **Phòng tương tự trong {quan_huyen}**:"]
        for _, row in similar.iterrows():
            title = row.get('tieu_de', 'Không rõ')
            if len(title) > 50:
                title = title[:50] + "..."
            lines.append(
                f"   • {title} — {row['gia_thue']} "
                f"({row.get('so_tien_nghi', 0)} tiện nghi)"
            )

        return "\n".join(lines)


def create_evaluation_context(csv_path: str, documents: list) -> str:
    """
    Tạo ngữ cảnh đánh giá giá thị trường từ các documents retrieved.

    Được gọi trong RAG chain để bổ sung đánh giá vào context trước khi gửi cho LLM.

    Args:
        csv_path: Đường dẫn file CSV dữ liệu
        documents: Danh sách documents từ retriever

    Returns:
        Chuỗi đánh giá giá cho tất cả các phòng được tìm thấy.
    """
    evaluator = MarketEvaluator(csv_path)
    evaluations = []

    for doc in documents:
        content = doc.page_content if hasattr(doc, 'page_content') else str(doc)

        # Trích xuất thông tin từ document content
        gia_thue = _extract_field(content, r'gia_thue[:\s]+(.+?)(?:\n|$)')
        quan_huyen = _extract_field(content, r'quan_huyen[:\s]+(.+?)(?:\n|$)')
        tien_nghi = _extract_field(content, r'tien_nghi[:\s]+(.+?)(?:\n|$)')
        tieu_de = _extract_field(content, r'tieu_de[:\s]+(.+?)(?:\n|$)')
        dien_tich = _extract_field(content, r'dien_tich[:\s]+(.+?)(?:\n|$)')

        if gia_thue and quan_huyen:
            eval_result = evaluator.evaluate_room(
                gia_thue=gia_thue,
                quan_huyen=quan_huyen,
                tien_nghi=tien_nghi or "",
                dien_tich=dien_tich or "",
                tieu_de=tieu_de or ""
            )
            similar = evaluator.find_similar_rooms(quan_huyen, gia_thue)
            evaluations.append(f"\n--- ĐÁNH GIÁ GIÁ THỊ TRƯỜNG ---\n{eval_result}{similar}")

    return "\n\n".join(evaluations) if evaluations else ""


def _extract_field(text: str, pattern: str) -> Optional[str]:
    """Trích xuất field từ nội dung document bằng regex."""
    match = re.search(pattern, text, re.IGNORECASE)
    if match:
        return match.group(1).strip()
    return None
