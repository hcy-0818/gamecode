package com.example.account.filter;

import com.example.common.context.UserContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

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
