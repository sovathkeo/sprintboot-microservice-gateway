package kh.com.cellcard.model.auth;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import kh.com.cellcard.common.helper.JsonObjectHelper;

public class TokenIntrospectionResultModel {

    public boolean active;
    public String sub;

    @SerializedName("client_id")
    public String clientId;

    public TokenIntrospectionResultModel(boolean active) {
        this.active = active;
    }

    public static TokenIntrospectionResultModel failed() {
        return new TokenIntrospectionResultModel(false);
    }

    public static TokenIntrospectionResultModel success(JsonObject claims) {
        var s =  new TokenIntrospectionResultModel(true);
        s.sub = JsonObjectHelper.getAsStringOrEmpty(claims, "sub");
        s.clientId = JsonObjectHelper.getAsStringOrEmpty(claims, "client_id");
        return s;
    }
}
