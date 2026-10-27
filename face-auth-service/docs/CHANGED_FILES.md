# File thay đổi sau rà soát

Tạo ngày 27/10/2026. Danh sách gồm cả thay đổi từ các phần trước, không chỉ lần dọn.
Backend và frontend là hai repository riêng. Không commit hoặc push.
Rental application.properties được stage riêng để index không giữ JWT ghi sẵn;
các thay đổi còn lại giữ trạng thái hiện có. M=modified, D=deleted, ??=new.
Không coi file bị xóa trong working tree là đã bị xóa khỏi lịch sử Git.

## Roomix_Backend

```text
 M .dockerignore
 M .gitignore
 M DOCKER.md
 M api-gateway/src/main/java/com/example/api_gateway/filter/AuthenticationFilter.java
 M api-gateway/src/main/resources/application.yml
 M compose.yml
 M core-service/docs/ADMIN_AUTH.md
 M core-service/docs/FACE_AUTH.md
 M core-service/docs/LOCAL_SETUP.md
 M core-service/docs/MAIN_INTEGRATION.md
 M core-service/docs/TELEGRAM.md
 M core-service/scripts/run_local.ps1
 M face-auth-service/.gitignore
 M face-auth-service/README.md
 M face-auth-service/docs/AI_LIVENESS.md
 M face-auth-service/docs/DATABASE.md
 M face-auth-service/docs/FILE_MAP.md
 M face-auth-service/docs/HANDOFF.md
 M face-auth-service/docs/RUNTIME.md
 M face-auth-service/docs/TELEGRAM.md
 D face-auth-service/legacy/README.md
 D face-auth-service/legacy/README.original.md
 D face-auth-service/legacy/backend/camera_service.py
 D face-auth-service/legacy/backend/database.py
 D face-auth-service/legacy/backend/migrate_pkl.py
 D face-auth-service/legacy/backend/models.py
 D face-auth-service/legacy/backend/requirements.txt
 D face-auth-service/legacy/backend/security_service.py
 D face-auth-service/legacy/cv/attendance.py
 D face-auth-service/legacy/cv/face_processor.py
 D face-auth-service/legacy/cv/main.py
 D face-auth-service/legacy/cv/notifier.py
 D face-auth-service/legacy/cv/requirements.txt
 D face-auth-service/legacy/cv/security_config.json
 D face-auth-service/legacy/cv/user_database.py
 D face-auth-service/legacy/dashboard/css/style.css
 D face-auth-service/legacy/dashboard/index.html
 D face-auth-service/legacy/dashboard/js/app.js
 D face-auth-service/legacy/maven/.mvn/wrapper/maven-wrapper.properties
 D face-auth-service/legacy/maven/mvnw
 D face-auth-service/legacy/maven/mvnw.cmd
 D face-auth-service/legacy/maven/pom.xml
 M face-auth-service/src/face_auth/ai/face_service.py
 M face-auth-service/src/face_auth/config.py
 M face-auth-service/src/face_auth/services/face_workflow.py
 M face-auth-service/src/face_auth/web/demo.mjs
 M face-auth-service/src/face_auth/web/face_camera.mjs
 D face-auth-service/src/main/java/com/example/face_auth_service/config/OpenApiConfig.java
 D face-auth-service/tests/legacy/__init__.py
 D face-auth-service/tests/legacy/test_telegram.py
 M face-auth-service/tests/test_face_service.py
 M face-auth-service/tests/test_workflow.py
 M face-auth-service/tests/web/face_camera.test.mjs
M  rental-service/src/main/resources/application.properties
?? .env.example
?? api-gateway/docs/FACE_ADMIN.md
?? api-gateway/src/test/java/com/example/api_gateway/GatewayFaceRoutingTests.java
?? api-gateway/src/test/java/com/example/api_gateway/filter/AuthenticationFilterTests.java
?? face-auth-service/.dockerignore
?? face-auth-service/Dockerfile
?? face-auth-service/docs/CHANGED_FILES.md
?? face-auth-service/docs/COMPOSE_TEST.md
?? face-auth-service/docs/COMPOSE_WEBCAM.md
?? face-auth-service/docs/DOCKER.md
?? face-auth-service/docs/FINAL_REVIEW.md
?? face-auth-service/docs/LOCAL_ACCEPTANCE.md
?? face-auth-service/docs/PROGRESS.md
?? face-auth-service/scripts/audit_git.py
?? face-auth-service/scripts/check_compose.py
?? face-auth-service/tests/compose/compose_face_fixture.py
?? face-auth-service/tests/test_docker_config.py
```

## Roomix

```text
 M .env.example
 M .gitignore
 M docs/AUTH_FLOW.md
 M next.config.mjs
 M package-lock.json
 M package.json
 M src/app/admin/layout.tsx
 M src/app/admin/login/page.tsx
 M src/components/auth/LoginForm.tsx
 M src/components/layouts/AdminSidebar.tsx
?? docs/ADMIN_AUTH_INTEGRATION.md
?? postcss.config.mjs
?? src/app/admin/face/page.tsx
?? src/components/auth/AdminAuthProvider.tsx
?? src/components/auth/AdminFaceLogin.tsx
?? src/services/admin-auth.ts
?? src/services/face-camera.d.mts
?? src/services/face-camera.mjs
?? src/services/face-messages.ts
?? tests/admin-auth.test.mjs
?? tests/face-camera.test.mjs
```
