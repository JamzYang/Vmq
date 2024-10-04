package com.vone.mq.config;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * @author yang
 * @createTime 2024年09月19日 01:01:00
 */
@Component
public class RequestLoggingFilter implements Filter {

    private static final Log logger = LogFactory.getLog(RequestLoggingFilter.class);
    private static final int MAX_LOG_LENGTH = 2048; // 2KB

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper((HttpServletRequest) request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper((HttpServletResponse) response);

        long startTime = System.currentTimeMillis();

        // 打印请求 URL 和参数
        logRequest(requestWrapper);

        chain.doFilter(requestWrapper, responseWrapper);

        // 打印响应结果
        logResponse(responseWrapper, System.currentTimeMillis() - startTime);

        responseWrapper.copyBodyToResponse();
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String url = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        if (queryString != null) {
            url += "?" + queryString;
        }
        String method = request.getMethod();
        String params = getRequestParams(request);
        
        logger.info(String.format("Request: %s %s, Parameters: %s", method, url, truncate(params)));
    }

    private void logResponse(ContentCachingResponseWrapper response, long duration) throws IOException {
        String content = getResponseContent(response);
        logger.info(String.format("Response (in %d ms): %s", duration, truncate(content)));
    }

    private String getRequestParams(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        return parameterMap.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + Arrays.toString(entry.getValue()))
                .collect(Collectors.joining(", "));
    }

    private String getResponseContent(ContentCachingResponseWrapper response) throws IOException {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            try {
                return new String(content, response.getCharacterEncoding());
            } catch (UnsupportedEncodingException e) {
                return "[无法解码响应内容]";
            }
        }
        return "[无响应内容]";
    }

    private String truncate(String content) {
        if (content.length() <= MAX_LOG_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_LOG_LENGTH) + "... (已截断)";
    }
}