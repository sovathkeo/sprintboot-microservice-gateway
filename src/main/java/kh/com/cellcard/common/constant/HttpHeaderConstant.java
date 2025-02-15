package kh.com.cellcard.common.constant;

import org.springframework.http.HttpHeaders;

public class HttpHeaderConstant {

    public static final String CORRELATION_ID  = "X-CorrelationId";
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String X_CELLCARD_REQUEST_ID = "X-Cellcard-Request-ID";
    public static final String X_CELLCARD_CHANNEL = "X-Cellcard-Channel";
    public static final String CONTENT_LENGTH = HttpHeaders.CONTENT_LENGTH;
    public static final String X_API_KEY = "X-API-KEY";
    public static final String AUTHORIZATION = "Authorization";
}
