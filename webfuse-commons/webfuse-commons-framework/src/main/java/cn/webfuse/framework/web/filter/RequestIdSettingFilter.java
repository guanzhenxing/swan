package cn.webfuse.framework.web.filter;

import cn.webfuse.framework.kit.HttpServletKits;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在Header中添加X-Request-Id的值
 */
@WebFilter
public class RequestIdSettingFilter implements Filter {


    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        RequestIdSettingRequestWrapper requestWrapper = new RequestIdSettingRequestWrapper((HttpServletRequest) servletRequest);
        requestWrapper.addHeader("X-Request-Id", HttpServletKits.buildRequestId());
        filterChain.doFilter(requestWrapper, servletResponse);
    }

    @Override
    public void destroy() {

    }

    private static class RequestIdSettingRequestWrapper extends HttpServletRequestWrapper {

        private Map<String, String> customHeaders;

        public RequestIdSettingRequestWrapper(HttpServletRequest request) {
            super(request);
            customHeaders = new ConcurrentHashMap<>();
        }

        public void addHeader(String name, String value) {
            customHeaders.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            String value = this.customHeaders.get(name);
            if (value != null) {
                return value;
            }
            return ((HttpServletRequest) getRequest()).getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> set = new HashSet<>(customHeaders.keySet());
            Enumeration<String> enumeration = ((HttpServletRequest) getRequest()).getHeaderNames();
            while (enumeration.hasMoreElements()) {
                String name = enumeration.nextElement();
                set.add(name);
            }
            return Collections.enumeration(set);
        }
    }
}



