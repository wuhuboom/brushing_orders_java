package com.order.framework.front;

import com.order.framework.security.filter.JwtAuthenticationTokenFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class MemberApiFilterIsolationTest {
    @Test
    void memberRequestsSkipAdministratorAuthentication() throws Exception {
        var filter = new JwtAuthenticationTokenFilter();
        var request = mock(HttpServletRequest.class);
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);
        when(request.getServletPath()).thenReturn("/api/order");
        // No administrator token service is installed: touching it would fail this test.
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }
}
