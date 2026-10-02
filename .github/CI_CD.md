# CI/CD cho Roomix Backend

## 1. Mục tiêu

Workflow `ci-cd.yml` tự động kiểm tra năm service Java và phát hành Docker image lên GitHub Container Registry (GHCR). Không có mật khẩu cơ sở dữ liệu, JWT secret hay Cloudinary secret nào được ghi trực tiếp trong workflow.

## 2. Khi nào workflow chạy?

- Mọi lần `push`: chạy Maven `verify` cho cả năm service.
- Pull request vào `main` hoặc `develop`: chạy Maven `verify`, không phát hành image.
- Push vào `main`: sau khi toàn bộ kiểm thử đạt, phát hành image có tag `latest` và `sha-<commit>`.
- Push Git tag dạng `v*`, ví dụ `v1.2.0`: phát hành tag `1.2.0`, `1.2` và `sha-<commit>`.
- Chạy thủ công bằng `workflow_dispatch`: kiểm thử rồi phát hành image theo SHA.

## 3. Các image được tạo

Với GitHub owner là `siro25`, các image có dạng:

```text
ghcr.io/siro25/roomix-api-gateway
ghcr.io/siro25/roomix-core-service
ghcr.io/siro25/roomix-rental-service
ghcr.io/siro25/roomix-rag-service
ghcr.io/siro25/roomix-face-auth-service
```

Dockerfile dùng hai giai đoạn: JDK 21 để build và JRE 21 để chạy. Ứng dụng chạy bằng user `roomix` không có quyền root.

## 4. Thiết lập GitHub một lần

1. Vào `Settings > Actions > General > Workflow permissions`.
2. Chọn `Read and write permissions` hoặc bảo đảm workflow được cấp `packages: write`.
3. Bật `Allow GitHub Actions to create and approve pull requests` chỉ khi dự án thật sự cần; workflow hiện tại không cần quyền này.
4. Trong `Settings > Branches`, tạo rule cho `main` và yêu cầu các check `Verify ...` phải thành công trước khi merge.

Workflow dùng `GITHUB_TOKEN` có sẵn để đẩy GHCR, vì vậy không cần tạo PAT hay repository secret cho bước publish.

## 5. Chạy image

Đăng nhập GHCR nếu package đang private, sau đó chạy image với các biến môi trường của service. Ví dụ Core Service:

```bash
docker run --rm -p 8081:8081 \
  -e CORE_DB_URL=jdbc:postgresql://host.docker.internal:5432/core_db \
  -e CORE_DB_USERNAME=postgres \
  -e CORE_DB_PASSWORD=your-password \
  -e JWT_SECRET=your-base64-secret \
  ghcr.io/siro25/roomix-core-service:latest
```

Không đưa giá trị secret thật vào repository. Khi triển khai lên VPS, Kubernetes, Render hoặc Railway, khai báo chúng bằng secret/environment của nền tảng.

## 6. Phạm vi CD hiện tại

CD hiện tại dừng ở bước **continuous delivery**: tạo image đã kiểm thử và đưa lên GHCR. Bước tự động thay container đang chạy trên VPS/Kubernetes cần thông tin đích triển khai riêng (host, nền tảng, domain và cơ chế lưu secret), vì vậy chưa được giả định trong repository.
