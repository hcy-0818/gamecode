package com.example.user.filter;

import com.example.common.context.UserContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 从 Gateway 注入的请求头中提取用户信息，设置到 UserContext
 */
@Component
public class UserContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String userId = httpRequest.getHeader("X-User-Id");
        String username = httpRequest.getHeader("X-Username");
        String role = httpRequest.getHeader("X-User-Role");

        if (userId != null) {
            UserContext.set(new UserContext.UserInfo(Long.valueOf(userId), username, role));
        }

        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
