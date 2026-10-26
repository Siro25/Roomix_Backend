"""Standalone face recognition, enrollment, and Telegram photo alerts."""
import cv2
import os
import time
import argparse
from face_processor import FaceProcessor
from user_database import UserDatabase
from attendance import AttendanceManager
from notifier import TelegramNotifier

RECOGNITION_THRESHOLD = 0.80
DEBOUNCE_SECONDS = 30
UNKNOWN_COOLDOWN_SEC = 5.0
DB_PATH = "user_database.pkl"
ENROLL_LIMIT = 20
ENROLL_MIN_CONFIDENCE = 0.8

def run_recognition_mode(args):
    db = UserDatabase(DB_PATH)
    processor = FaceProcessor(recognition_threshold=args.threshold)
    processor.set_database(db)
    attendance = AttendanceManager(debounce_seconds=DEBOUNCE_SECONDS)
    notifier = TelegramNotifier.from_config(args.security_config)
    cap = cv2.VideoCapture(args.camera)
    last_unknown = 0
    try:
        while cap.isOpened():
            ok, frame = cap.read()
            if not ok:
                break
            frame = cv2.flip(frame, 1)
            results = processor.process_frame(frame, skip_mesh=True)
            for result in results:
                if result.is_known:
                    attendance.check_and_log(result.user_id, result.name, result.mssv)
            if any(not r.is_known and r.quality_score >= 0.3 for r in results):
                now = time.monotonic()
                if now - last_unknown >= UNKNOWN_COOLDOWN_SEC:
                    last_unknown = now
                    notifier.send_alert("🚨 Phát hiện khuôn mặt chưa đăng ký!", frame)
            cv2.imshow("Face Recognition", FaceProcessor.draw_results(frame, results))
            if cv2.waitKey(1) & 0xFF == ord("q"):
                break
    finally:
        cap.release()
        cv2.destroyAllWindows()


def run_enroll_mode(args):
    """
    Chế độ enroll: Đăng ký khuôn mặt mới.
    Hybrid UI: YOLOv8 bbox vàng + MediaPipe mesh xanh + Progress bar.
    Chỉ lưu khi: confidence > 0.8 VÀ mặt chính diện.
    """
    print("\n" + "=" * 55)
    print("  SMART ATTENDANCE SYSTEM")
    print("  Che do: ENROLL KHUON MAT")
    print("  Model: YOLOv8 + MediaPipe + FaceNet")
    print("=" * 55)

    db = UserDatabase(DB_PATH)
    processor = FaceProcessor(recognition_threshold=args.threshold)
    processor.set_database(db)

    if db.users:
        db.print_users()

    print("\nChon hanh dong:")
    print("  1. Them nguoi moi + enroll bang webcam")
    print("  2. Them nguoi moi + enroll tu thu muc anh")
    print("  3. Enroll them anh cho nguoi da co")
    print("  4. Thoat")

    choice = input("\nLua chon (1-4): ").strip()

    if choice == "1":
        ho_ten = input("Nhap ho ten: ").strip()
        mssv = input("Nhap MSSV: ").strip()

        if not ho_ten or not mssv:
            print("[!] Can nhap day du ho ten va MSSV!")
            return

        user_id = db.add_user(ho_ten, mssv)
        _enroll_webcam(processor, db, user_id, ho_ten, args.camera)

        if db.get_user(user_id) and len(db.get_user(user_id).embeddings) == 0:
            db.remove_user(user_id)
            print(f"\n[!] Khong co anh nao, da xoa user.")

    elif choice == "2":
        ho_ten = input("Nhap ho ten: ").strip()
        mssv = input("Nhap MSSV: ").strip()
        folder = input("Duong dan thu muc anh: ").strip()

        if not ho_ten or not mssv or not folder:
            print("[!] Can nhap day du thong tin!")
            return

        user_id = db.add_user(ho_ten, mssv)
        count = processor.enroll_from_folder(folder, user_id)

        if count > 0:
            processor.invalidate_cache()
            print(f"\n[OK] Enroll hoan tat: {ho_ten} -> {count} anh")
        else:
            db.remove_user(user_id)
            print(f"\n[!] Khong enroll duoc anh nao!")

    elif choice == "3":
        db.print_users()
        try:
            user_id = int(input("Nhap User ID can enroll them: "))
        except ValueError:
            print("[!] ID khong hop le!")
            return

        user = db.get_user(user_id)
        if not user:
            print(f"[!] Khong tim thay User ID={user_id}")
            return

        _enroll_webcam(processor, db, user_id, user.ho_ten, args.camera)


def _enroll_webcam(processor, db, user_id, name, camera_index):
    """
    Mở webcam để enroll khuôn mặt.
    Hybrid UI: bbox vàng + mesh xanh + progress bar.
    Chỉ lưu khi confidence > 0.8 VÀ mặt chính diện (MediaPipe).
    """
    print(f"\n[INFO] Mo webcam cho: {name}")
    print(f"  === ENROLL MODE ===")
    print(f"  - AUTO: Tu chup moi 3 giay (bam 'a' bat/tat)")
    print(f"  - CLICK CHUOT TRAI vao cua so camera")
    print(f"  - Nhan SPACE hoac 's' de chup thu cong")
    print(f"  - Chi luu khi mat CHINH DIEN + confidence > {ENROLL_MIN_CONFIDENCE}")
    print(f"  - Nhan 'q' hoac ESC = Hoan tat")
    print(f"  - Muc tieu: {ENROLL_LIMIT} anh\n")

    cap = cv2.VideoCapture(camera_index)
    if not cap.isOpened():
        print("[!] Khong mo duoc camera!")
        print(f"  Thu: python main.py --enroll --camera 1")
        return

    # Warmup camera
    print("[INFO] Dang khoi tao camera...")
    for _ in range(10):
        cap.read()
        time.sleep(0.05)

    # Tạo thư mục dataset (lưu ảnh crop)
    dataset_dir = os.path.join("dataset", name.replace(" ", "_"))
    os.makedirs(dataset_dir, exist_ok=True)

    # --- State ---
    enroll_count = 0
    should_capture = [False]
    auto_mode = [True]
    last_auto_time = time.time()
    AUTO_INTERVAL = 3.0

    # --- Mouse callback ---
    def on_mouse(event, x, y, flags, param):
        if event == cv2.EVENT_LBUTTONDOWN:
            should_capture[0] = True
            print("  [CLICK] Da nhan click chuot!")

    window_name = "Enroll - Click de chup"
    cv2.namedWindow(window_name, cv2.WINDOW_AUTOSIZE)
    cv2.setMouseCallback(window_name, on_mouse)

    print("[OK] Camera san sang!")
    print(f"[AUTO] Auto-capture: BAT (moi {AUTO_INTERVAL:.0f}s)\n")

    while enroll_count < ENROLL_LIMIT:
        ret, frame = cap.read()
        if not ret:
            time.sleep(0.05)
            continue

        # === FLIP CAMERA (mirror) ===
        frame = cv2.flip(frame, 1)

        # === Kiểm tra auto-capture ===
        if auto_mode[0]:
            elapsed = time.time() - last_auto_time
            if elapsed >= AUTO_INTERVAL:
                should_capture[0] = True
                last_auto_time = time.time()

        # === YOLO detect + MediaPipe mesh cho hiển thị ===
        try:
            boxes, probs = processor.detect_faces(frame)
        except Exception as e:
            print(f"[ERROR] Detection: {e}")
            boxes, probs = [], []

        # Lấy landmarks cho tất cả faces
        face_data = []
        for box, prob in zip(boxes, probs):
            landmarks = processor.get_face_mesh(frame, box)
            is_frontal = processor.is_frontal_face(landmarks)
            face_data.append((box, prob, landmarks, is_frontal))

        # === Xử lý chụp ===
        if should_capture[0] and face_data:
            should_capture[0] = False

            # Tìm face lớn nhất (gần nhất)
            best_face = max(face_data,
                            key=lambda f: (f[0][2]-f[0][0]) * (f[0][3]-f[0][1]))
            box, prob, landmarks, is_frontal = best_face

            if prob < ENROLL_MIN_CONFIDENCE:
                print(f"  >>> Confidence qua thap ({prob:.2f} < {ENROLL_MIN_CONFIDENCE})")
            elif not is_frontal:
                print(f"  >>> Mat KHONG chinh dien! Hay nhin thang vao camera.")
            else:
                print(f"  [PROCESSING] Dang trich xuat khuon mat...")

                success = processor.enroll_from_frame(frame, user_id)

                if success:
                    enroll_count += 1
                    print(f"  >>> OK! Da chup {enroll_count}/{ENROLL_LIMIT}")

                    # Lưu ảnh crop vào dataset
                    x1, y1, x2, y2 = box
                    face_crop = frame[y1:y2, x1:x2]
                    if face_crop.size > 0:
                        crop_path = os.path.join(
                            dataset_dir,
                            f"{name.replace(' ', '_')}_{enroll_count:04d}.jpg"
                        )
                        cv2.imwrite(crop_path, face_crop)

                    # Flash xanh
                    h, w = frame.shape[:2]
                    flash = frame.copy()
                    cv2.rectangle(flash, (0, 0), (w, h), (0, 255, 0), 15)
                    cv2.putText(flash, f"OK! ({enroll_count}/{ENROLL_LIMIT})",
                                (w // 2 - 100, h // 2),
                                cv2.FONT_HERSHEY_SIMPLEX, 1.0, (0, 255, 0), 3)
                    cv2.imshow(window_name, flash)
                    cv2.waitKey(300)
                else:
                    print(f"  >>> FaceNet khong detect duoc mat trong crop!")
                    h, w = frame.shape[:2]
                    flash = frame.copy()
                    cv2.rectangle(flash, (0, 0), (w, h), (0, 0, 255), 10)
                    cv2.putText(flash, "KHONG THAY MAT!",
                                (w // 2 - 120, h // 2),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 255), 3)
                    cv2.imshow(window_name, flash)
                    cv2.waitKey(300)

            continue

        elif should_capture[0] and not face_data:
            should_capture[0] = False
            print("  >>> Khong thay khuon mat nao!")
            continue

        # === Vẽ HUD ===
        display = frame.copy()
        h, w = display.shape[:2]

        # Vẽ YOLO bbox vàng + MediaPipe mesh xanh
        for box, prob, landmarks, is_frontal in face_data:
            x1, y1, x2, y2 = box

            # Bbox vàng
            cv2.rectangle(display, (x1, y1), (x2, y2), (0, 220, 255), 2)

            # Corner accents
            cl = min(20, (x2 - x1) // 4, (y2 - y1) // 4)
            for cx, cy in [(x1, y1), (x2, y1), (x1, y2), (x2, y2)]:
                dx = cl if cx == x1 else -cl
                dy = cl if cy == y1 else -cl
                cv2.line(display, (cx, cy), (cx + dx, cy),
                         (0, 220, 255), 3)
                cv2.line(display, (cx, cy), (cx, cy + dy),
                         (0, 220, 255), 3)

            # Face Mesh xanh
            if landmarks:
                FaceProcessor.draw_face_mesh(display, landmarks,
                                             color=(0, 255, 0), thickness=1)

            # Confidence + Frontal status
            conf_text = f"{prob*100:.0f}%"
            front_text = "FRONTAL" if is_frontal else "NOT FRONTAL"
            front_color = (0, 255, 0) if is_frontal else (0, 0, 255)

            cv2.putText(display, conf_text, (x1, y1 - 8),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 220, 255), 1)
            cv2.putText(display, front_text, (x1, y2 + 18),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.45, front_color, 1)

        # Mode text
        mode_text = "AUTO" if auto_mode[0] else "MANUAL"
        cv2.putText(display, f"Mode: {mode_text} | 'a'=toggle | 'q'=xong",
                    (10, 25), cv2.FONT_HERSHEY_SIMPLEX,
                    0.45, (200, 200, 200), 1)

        if auto_mode[0]:
            remaining = AUTO_INTERVAL - (time.time() - last_auto_time)
            cv2.putText(display, f"Auto chup sau: {max(0,remaining):.1f}s",
                        (10, 48), cv2.FONT_HERSHEY_SIMPLEX,
                        0.45, (0, 255, 255), 1)

        # === PROGRESS BAR + ENROLL LABEL ===
        FaceProcessor.draw_enroll_ui(display, name, enroll_count, ENROLL_LIMIT)

        cv2.imshow(window_name, display)

        # === Xử lý phím ===
        key = cv2.waitKey(30)

        if key == -1:
            continue

        key = key & 0xFF

        if key == ord('q') or key == 27:  # q hoặc ESC
            break
        elif key == ord('s') or key == 32:  # s hoặc SPACE
            should_capture[0] = True
            print("  [KEY] Nhan phim chup!")
        elif key == ord('a'):  # Toggle auto
            auto_mode[0] = not auto_mode[0]
            last_auto_time = time.time()
            status = "BAT" if auto_mode[0] else "TAT"
            print(f"  [AUTO] Auto-capture: {status}")

    cap.release()
    cv2.destroyAllWindows()

    if enroll_count > 0:
        processor.invalidate_cache()
        print(f"\n{'='*45}")
        print(f"  ENROLL HOAN TAT!")
        print(f"  Ten:  {name}")
        print(f"  So anh: {enroll_count}")
        print(f"  Luu tai: {dataset_dir}")
        print(f"{'='*45}")
    else:
        print(f"\n[!] Khong chup duoc anh nao.")


def run_manage_mode(args):
    """Chế độ quản lý: Xem/xóa người dùng."""
    print("\n" + "=" * 55)
    print("  SMART ATTENDANCE SYSTEM")
    print("  Che do: QUAN LY NGUOI DUNG")
    print("=" * 55)

    db = UserDatabase(DB_PATH)

    while True:
        db.print_users()
        print("Chon hanh dong:")
        print("  1. Xem chi tiet nguoi dung")
        print("  2. Xoa nguoi dung")
        print("  3. Xoa toan bo database")
        print("  4. Thoat")

        choice = input("\nLua chon (1-4): ").strip()

        if choice == "1":
            try:
                uid = int(input("Nhap User ID: "))
            except ValueError:
                continue
            user = db.get_user(uid)
            if user:
                print(f"\n  ID:     {user.user_id}")
                print(f"  Ten:    {user.ho_ten}")
                print(f"  MSSV:   {user.mssv}")
                print(f"  Faces:  {len(user.embeddings)}")
                print(f"  Tao:    {user.created_at}\n")
            else:
                print(f"  [!] Khong tim thay!")

        elif choice == "2":
            try:
                uid = int(input("Nhap User ID can xoa: "))
            except ValueError:
                continue
            confirm = input(f"Xac nhan xoa ID={uid}? (y/n): ")
            if confirm.lower() == 'y':
                db.remove_user(uid)

        elif choice == "3":
            confirm = input("XOA TOAN BO DATABASE? (yes/no): ")
            if confirm.lower() == 'yes':
                if os.path.exists(DB_PATH):
                    os.remove(DB_PATH)
                    db = UserDatabase(DB_PATH)
                    print("[OK] Da xoa toan bo!")

        elif choice == "4":
            break


def main():
    parser = argparse.ArgumentParser(
        description="Smart Attendance System - Hybrid Face Recognition",
        formatter_class=argparse.RawTextHelpFormatter
    )

    parser.add_argument(
        "--camera", type=int, default=0,
        help="Camera index cho webcam (mac dinh: 0)"
    )
    parser.add_argument(
        "--threshold", type=float, default=RECOGNITION_THRESHOLD,
        help=f"Nguong nhan dien 0.0-1.0 (mac dinh: {RECOGNITION_THRESHOLD})"
    )
    parser.add_argument(
        "--enroll", action="store_true",
        help="Che do enroll khuon mat moi"
    )
    parser.add_argument(
        "--manage", action="store_true",
        help="Che do quan ly nguoi dung"
    )
    parser.add_argument(
        "--security-config", type=str, default="security_config.json",
        dest="security_config",
        help="Duong dan file cau hinh an ninh (mac dinh: security_config.json)"
    )

    args = parser.parse_args()

    if args.enroll:
        run_enroll_mode(args)
    elif args.manage:
        run_manage_mode(args)
    else:
        run_recognition_mode(args)


if __name__ == "__main__":
    main()