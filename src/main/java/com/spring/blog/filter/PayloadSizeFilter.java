package com.spring.blog.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PayloadSizeFilter implements Filter {

    private static final long MAX_SIZE = 1024 * 100; // 100KB

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (httpRequest.getContentLengthLong() > MAX_SIZE) {
            httpResponse.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE); // 413
            httpResponse.getWriter().write("{\"error\": \"Payload too large. Max 100KB allowed.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}