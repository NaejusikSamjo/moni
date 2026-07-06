package com.moni.user.global.redis;

// GET-then-SET 결과
public enum CasResult {

    SUCCESS(1L),
    NOT_FOUND(0L),
    MISMATCH(-1L);

    private final long code;

    CasResult(long code) {
        this.code = code;
    }

    public static CasResult from(long code) {
        for (CasResult result : values()) {
            if (result.code == code) {
                return result;
            }
        }
        throw new IllegalStateException("알 수 없는 CAS 결과 코드: " + code);
    }
}
