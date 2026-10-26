package com.team4.core.notifications;

public enum AuthAlertType {
    PASSWORD_LOGIN_SUCCEEDED("Đăng nhập mật khẩu thành công"),
    PASSWORD_LOGIN_FAILED("Đăng nhập mật khẩu thất bại"),
    FACE_LOGIN_SUCCEEDED("Đăng nhập khuôn mặt thành công");

    private final String title;
    AuthAlertType(String title) { this.title = title; }
    public String title() { return title; }
}
