package com.team4.core.auth;

public class AuthFailure extends RuntimeException {
    private final int status;

    public AuthFailure(int status) {
        super(status == 401 ? "UNAUTHORIZED" : "FORBIDDEN");
        this.status = status;
    }

    public int status() { return status; }
}
