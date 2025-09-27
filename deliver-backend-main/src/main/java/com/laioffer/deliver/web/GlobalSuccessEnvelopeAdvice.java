package com.laioffer.deliver.web;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalSuccessEnvelopeAdvice implements ResponseBodyAdvice<Object> {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // 显式跳过：标注了 @NoWrap 的类或方法
        return !(returnType.getContainingClass().isAnnotationPresent(NoWrap.class)
                || (returnType.getMethod() != null
                && returnType.getMethod().isAnnotationPresent(NoWrap.class)));
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        // 1) 基于请求路径的跳过规则（Swagger/OpenAPI 等）
        String path = request.getURI().getPath();
        if (startsWith(path, "/v3/api-docs/**")
                || startsWith(path, "/swagger-ui/**")
                || startsWith(path, "/swagger-resources/**")
                || startsWith(path, "/actuator/**")) {
            return body;
        }

        // 2) 非 JSON-ish 的响应跳过（文件下载、图片、文本等）
        if (!isJsonLike(selectedContentType) || body == null) return body;

        // 3) 一些常见类型直接跳过（字符串、二进制、ResponseEntity 容器等）
        if (body instanceof CharSequence || body instanceof byte[] || body instanceof ResponseEntity) return body;

        // 4) 错误体跳过（与你当前 GlobalExceptionHandler 一致：{code,message,status}）
        if (body instanceof Map<?, ?> map &&
                map.containsKey("code") && map.containsKey("message")) {
            return body;
        }
        // 若已包含 status（防止重复包）
        if (body instanceof Map<?, ?> map2 && map2.containsKey("status")) {
            return body;
        }

        // 5) 读取当前 HTTP 状态码
        int status = 200;
        if (response instanceof ServletServerHttpResponse httpResp) {
            status = httpResp.getServletResponse().getStatus();
        }

        // 6) 包装输出：{"status": <httpStatus>, "data": <original>}
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("data", body);
        return out;
    }

    private static boolean isJsonLike(MediaType mediaType) {
        if (mediaType == null) return false;
        // 兼容 application/json、application/*+json、application/problem+json 等
        String subtype = mediaType.getSubtype();
        return MediaType.APPLICATION_JSON.includes(mediaType)
                || (subtype != null && subtype.contains("json"));
    }

    private static boolean startsWith(String path, String pattern) {
        return PATH_MATCHER.match(pattern, path);
    }
}
