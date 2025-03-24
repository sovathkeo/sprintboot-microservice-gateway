package kh.com.cellcard.common.helper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonObjectHelper {

    public static JsonElement getAsJsonElement(JsonElement jsonElement, String jsonKeys) {
        if (StringHelper.isNullOrEmpty(jsonKeys)) {
            return new JsonObject();
        }
        if (!jsonKeys.contains(".")) {
            return jsonElement.getAsJsonObject().get(jsonKeys);
        }

        JsonElement element = jsonElement;
        var keys = jsonKeys.split("\\.");

        for (String key : keys) {
            element = element.getAsJsonObject().get(key);
        }
        return element;
    }

    public static String getAsStringOrEmpty(JsonElement jsonElement, String keys) {
        if (StringHelper.isNullOrEmpty(keys) || isJsonElementNull(jsonElement)) {
            return "";
        }
        if (!keys.contains(".")) {
            var element =  jsonElement.getAsJsonObject().get(keys);
            return getAsStringOrEmpty(element);
        }

        JsonElement element = jsonElement;
        var keyArr = keys.split("\\.");

        for (String key : keyArr) {
            element = element.getAsJsonObject().get(key);
        }
        return getAsStringOrEmpty(element);
    }

    public static Number getAsNumber(JsonElement jsonElement, String keys) {
        if (StringHelper.isNullOrEmpty(keys) || isJsonElementNull(jsonElement)) {
            return null;
        }
        if (!keys.contains(".")) {
            var element =  jsonElement.getAsJsonObject().get(keys);
            return getAsNumberOrNull(element);
        }

        JsonElement element = jsonElement;
        var keyArr = keys.split("\\.");

        for (String key : keyArr) {
            element = element.getAsJsonObject().get(key);
        }
        return getAsNumberOrNull(element);
    }

    public static JsonArray getAsJsonArray(JsonElement jsonElement, String jsonKeys) {
        if (StringHelper.isNullOrEmpty(jsonKeys)) {
            return new JsonArray();
        }
        JsonElement element;
        if (!jsonKeys.contains(".")) {
            element = jsonElement.getAsJsonObject().get(jsonKeys);
            return getAsArrayOrEmpty(element);
        }

        element = jsonElement;
        var keys = jsonKeys.split("\\.");

        for (String key : keys) {
            element = element.getAsJsonObject().get(key);
        }
        return getAsArrayOrEmpty(element);
    }

    public static boolean isJsonElementNull(JsonElement element) {
        return element == null || element.isJsonNull();
    }

    public static String getAsStringOrEmpty(JsonElement element) {
        return isJsonElementNull(element) ? "" : element.getAsString();
    }

    public static Number getAsNumberOrNull(JsonElement element) {
        return isJsonElementNull(element) || StringHelper.isNullOrEmpty(element.getAsString()) ? null : element.getAsNumber();
    }

    public static boolean isJsonArrayNullOrEmpty(JsonArray jsonArray) {
        return jsonArray == null || jsonArray.isEmpty() || jsonArray.isJsonNull();
    }

    public static JsonArray getAsArrayOrEmpty(JsonElement jsonArray) {
        return isJsonElementNull(jsonArray) ? new JsonArray() : jsonArray.getAsJsonArray();
    }
}
