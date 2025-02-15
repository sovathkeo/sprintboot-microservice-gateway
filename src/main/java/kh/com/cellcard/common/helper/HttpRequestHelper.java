package kh.com.cellcard.common.helper;

import kh.com.cellcard.common.constant.HttpHeaderConstant;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

public class HttpRequestHelper {

    public static boolean hasBody(ServerHttpRequest request) {
        return request.getHeaders().containsKey(HttpHeaderConstant.CONTENT_LENGTH);
    }

    public static boolean containKey(ServerWebExchange request, String key) {
        if (request == null || StringHelper.isNullOrEmpty(key)) {
            return false;
        }
        return request.getRequest().getHeaders().containsKey(key) || request.getRequest().getHeaders().containsKey(key.toLowerCase());
    }

    public static List<String> getHeaders(ServerWebExchange request, String headerName) {
        if (containKey(request, headerName)) {
            return request
                .getRequest()
                .getHeaders()
                .get(headerName);
        }
        return List.of();
    }

    /*
    * String getHeaderOrDefault
    * Parameter
    *   1. ServerWebExchange request,
    *   2. String headerName,
    *   3. String defaultValue
    * Return the value of the header key if present else return defaultValue
    * Note : comparison is ignored-case
    * */
    public static String getHeaderOrDefault(ServerWebExchange request, String headerName, String defaultValue) {
        var header = getHeaders(request, headerName)
            .stream()
            .findFirst();
        return header.orElse(defaultValue);
    }

    public static String getClientRemoteAddress(ServerWebExchange exchange) {
        var inetAddress = exchange.getRequest().getRemoteAddress();
        if (null != inetAddress) {
            return inetAddress.getAddress().getHostAddress();
        }
        return "";
    }
}
