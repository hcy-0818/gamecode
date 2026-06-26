package com.example.common.context;

import lombok.Data;

/**
 * 用户上下文 — ThreadLocal 存储当前请求用户信息
 */
public class UserContext {

    private static final ThreadLocal<UserInfo> currentUser = new ThreadLocal<>();

    public static void set(UserInfo userInfo) {
        currentUser.set(userInfo);
    }

    public static UserInfo get() {
        return currentUser.get();
    }

    public static Long getUserId() {
        UserInfo userInfo = currentUser.get();
        return userInfo != null ? userInfo.getUserId() : null;
    }

    public static String getUsername() {
        UserInfo userInfo = currentUser.get();
        return userInfo != null ? userInfo.getUsername() : null;
    }

    public static String getRole() {
        UserInfo userInfo = currentUser.get();
        return userInfo != null ? userInfo.getRole() : null;
    }

    public static void clear() {
        currentUser.remove();
    }

    @Data
    public static class UserInfo {
        private Long userId;
        private String username;
        private String role;

        public UserInfo() {}

        public UserInfo(Long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }
    }
}
