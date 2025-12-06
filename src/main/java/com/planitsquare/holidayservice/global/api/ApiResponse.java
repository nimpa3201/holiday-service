package com.planitsquare.holidayservice.global.api;

import lombok.Getter;

@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data;

    private ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data);
    }

    public static <T> ApiResponse<T> error(T data) {
        return new ApiResponse<>(false, data);
    }

    public static <T> ApiResponse<T> fail() {
        return new ApiResponse<>(false, null);
    }
}
