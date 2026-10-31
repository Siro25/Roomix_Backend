package com.team4.core.face;

public class FaceAuthFailure extends RuntimeException {
    private final int status;
    public FaceAuthFailure(int status, String code) { super(code); this.status = status; }
    public int status() { return status; }
}
