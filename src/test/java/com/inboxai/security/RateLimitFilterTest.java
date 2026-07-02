package com.inboxai.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    private static final int CAPACITY = 10;

    private RateLimitFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
    }

    private void post(String path) {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn(path);
    }

    @Test
    void nonProtectedPathPassesThrough() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/inbox");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(contains("ratelimit"));
    }

    @Test
    void loginWithinLimitPassesThrough() throws Exception {
        post("/login");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void loginOverLimitRedirectsWithRatelimitFlag() throws Exception {
        post("/login");
        when(request.getContextPath()).thenReturn("");

        for (int i = 0; i < CAPACITY; i++) {
            filter.doFilterInternal(request, response, chain);
        }
        filter.doFilterInternal(request, response, chain);

        verify(chain, times(CAPACITY)).doFilter(request, response);
        verify(response).setHeader("Retry-After", "60");
        verify(response).sendRedirect("/login?ratelimit");
    }

    @Test
    void registerOverLimitRedirectsWithRatelimitFlag() throws Exception {
        post("/register");
        when(request.getContextPath()).thenReturn("");

        for (int i = 0; i < CAPACITY; i++) {
            filter.doFilterInternal(request, response, chain);
        }
        filter.doFilterInternal(request, response, chain);

        verify(chain, times(CAPACITY)).doFilter(request, response);
        verify(response).sendRedirect("/register?ratelimit");
    }

    @Test
    void differentClientsGetSeparateBuckets() throws Exception {
        post("/login");
        when(request.getContextPath()).thenReturn("");
        for (int i = 0; i < CAPACITY + 1; i++) {
            filter.doFilterInternal(request, response, chain);
        }

        // a different IP is not affected by the exhausted bucket
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");
        filter.doFilterInternal(request, response, chain);

        verify(chain, times(CAPACITY + 1)).doFilter(request, response);
    }

    @Test
    void xForwardedForTakesPrecedenceOverRemoteAddr() throws Exception {
        post("/login");
        when(request.getContextPath()).thenReturn("");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.7, 10.0.0.1");

        for (int i = 0; i < CAPACITY; i++) {
            filter.doFilterInternal(request, response, chain);
        }
        filter.doFilterInternal(request, response, chain);

        // same forwarded client is limited even though remoteAddr never changed
        verify(response).sendRedirect("/login?ratelimit");
    }
}
