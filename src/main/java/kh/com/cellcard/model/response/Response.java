package kh.com.cellcard.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import kh.com.cellcard.common.wrapper.SerializationWrapper;


@JsonIgnoreProperties(value = {"$$beanFactory"})
public class Response {
    @JsonProperty("meta")
    ResponseMeta meta;

    @JsonProperty("data")
    ResponseData data;

    protected Response() {
        this.meta = ResponseMeta.buildMeta();
        this.data = ResponseData.success();
    }

    protected Response(Object additionalData) {
        this.meta = ResponseMeta.buildMeta();
        this.data = ResponseData.success();
        this.data.additionalData = additionalData;
    }

    protected Response(String errorMessage) {
        this.meta = ResponseMeta.buildMeta();
        this.data = ResponseData.success(errorMessage);
    }


    protected Response(String errorCode, String errorMessage, String errorDescription, String correlationId) {
        this.meta = ResponseMeta.buildMeta(correlationId);
        this.data = ResponseData.failed(errorCode, errorMessage, errorDescription);
    }

    protected Response(String errorCode, String errorMessage, String errorDescription, String correlationId, String requestId) {
        this.meta = ResponseMeta.buildMeta(correlationId, requestId);
        this.data = ResponseData.failed(errorCode, errorMessage, errorDescription);
    }

    public static Response success() {
        return new Response();
    }

    public static Response success(Object additionalData) {
        return new Response(additionalData);
    }


    public static Response success(String errorMessage) {
        return new Response(errorMessage);
    }

    // Start build failed response
    public static Response failure(String errorCode, String errorMessage, String errorDescription, String correlationId) {
        return new Response(errorCode, errorMessage, errorDescription, correlationId);
    }

    public static Response unAuthorized(String errorMessage, String errorDescription, String correlationId) {
        return new Response("401", errorMessage, errorDescription, correlationId);
    }

    public byte[] getBytes() {
        return SerializationWrapper.serialize(this).getBytes();
    }
}

