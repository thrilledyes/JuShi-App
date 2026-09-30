package com.jushi.demo.main.chaosuan;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EasyHpcHomeworkClient {
    private static final String BASE_URL = "https://www.easyhpc.net";
    private static final String LOGIN_ENDPOINT = BASE_URL + "/api/v1/user/auth";
    private static final String COURSE_ENDPOINT = BASE_URL + "/api/v1/course";
    private static final String HOMEWORK_ENDPOINT_TEMPLATE = BASE_URL + "/api/v1/course/%s/homework";

    private final Map<String, String> cookies = new LinkedHashMap<>();

    public JSONObject login(String username, String password) throws IOException, JSONException {
        JSONObject payload = new JSONObject();
        payload.put("Username", username);
        payload.put("Password", password);
        payload.put("Token", "");
        payload.put("Sig", "");
        payload.put("SessionId", "");
        payload.put("Scene", "ic_login");

        Object response = request(
                "POST",
                LOGIN_ENDPOINT + "?lang=zh-CN",
                BASE_URL + "/login",
                payload
        );
        if (!(response instanceof JSONObject)) {
            throw new JSONException("Unexpected login response: " + response);
        }

        JSONObject object = (JSONObject) response;
        Object status = firstExisting(object, "Status", "status");
        if (isFailedStatus(status)) {
            throw new IOException("EasyHPC login failed: " + object);
        }
        return object;
    }

    public List<JSONObject> fetchCourses() throws IOException, JSONException {
        String url = COURSE_ENDPOINT + "?"
                + buildQuery("lang", "zh-CN", "role", "student", "joined", "true");
        Object response = request("GET", url, BASE_URL + "/home", null);
        return toObjectList(EasyHpcHomeworkNormalizer.extractArray(response));
    }

    public List<JSONObject> fetchHomeworkPayloads(String courseId) throws IOException, JSONException {
        String endpoint = String.format(Locale.ROOT, HOMEWORK_ENDPOINT_TEMPLATE, encode(courseId));
        String url = endpoint + "?" + buildQuery("lang", "zh-CN", "role", "student");
        Object response = request("GET", url, BASE_URL + "/course/" + encode(courseId), null);
        return toObjectList(EasyHpcHomeworkNormalizer.extractArray(response));
    }

    public List<EasyHpcHomework> fetchAllHomeworks(String username, String password)
            throws IOException, JSONException {
        login(username, password);
        List<EasyHpcHomework> all = new ArrayList<>();

        for (JSONObject course : fetchCourses()) {
            String courseId = EasyHpcHomeworkNormalizer.extractCourseId(course);
            if (isEmpty(courseId)) {
                continue;
            }

            String courseName = EasyHpcHomeworkNormalizer.extractCourseName(course);
            List<JSONObject> tasks = fetchHomeworkPayloads(courseId);
            all.addAll(EasyHpcHomeworkNormalizer.normalize(courseId, courseName, tasks));
        }

        return all;
    }

    private Object request(String method, String url, String referer, JSONObject body)
            throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("Accept", "application/json, text/plain, */*");
        connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Pragma", "no-cache");
        connection.setRequestProperty("Origin", BASE_URL);
        connection.setRequestProperty("Referer", referer);
        connection.setRequestProperty("User-Agent", userAgent());
        connection.setRequestProperty("Sec-Fetch-Dest", "empty");
        connection.setRequestProperty("Sec-Fetch-Mode", "cors");
        connection.setRequestProperty("Sec-Fetch-Site", "same-origin");
        if (!cookies.isEmpty()) {
            connection.setRequestProperty("Cookie", buildCookieHeader());
        }

        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(bytes);
            }
        }

        int code = connection.getResponseCode();
        rememberCookies(connection);
        InputStream stream = code >= 200 && code < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String response = readText(stream);
        connection.disconnect();

        if (code < 200 || code >= 300) {
            throw new IOException("EasyHPC HTTP " + code + ": " + response);
        }
        return new JSONTokener(response).nextValue();
    }

    private void rememberCookies(HttpURLConnection connection) {
        Map<String, List<String>> headers = connection.getHeaderFields();
        if (headers == null) {
            return;
        }

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() == null || !"Set-Cookie".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            for (String cookie : entry.getValue()) {
                int semicolon = cookie.indexOf(';');
                String pair = semicolon >= 0 ? cookie.substring(0, semicolon) : cookie;
                int equals = pair.indexOf('=');
                if (equals > 0) {
                    cookies.put(pair.substring(0, equals), pair.substring(equals + 1));
                }
            }
        }
    }

    private String buildCookieHeader() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }

    private static List<JSONObject> toObjectList(JSONArray array) {
        List<JSONObject> values = new ArrayList<>();
        if (array == null) {
            return values;
        }
        for (int i = 0; i < array.length(); i++) {
            Object value = array.opt(i);
            if (value instanceof JSONObject) {
                values.add((JSONObject) value);
            }
        }
        return values;
    }

    private static String buildQuery(String... pairs) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(encode(pairs[i])).append('=').append(encode(pairs[i + 1]));
        }
        return builder.toString();
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            return value == null ? "" : value;
        }
    }

    private static String readText(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private static Object firstExisting(JSONObject object, String... keys) {
        for (String key : keys) {
            if (object.has(key) && !object.isNull(key)) {
                return object.opt(key);
            }
        }
        return null;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean isFailedStatus(Object status) {
        if (status == null) {
            return false;
        }
        if (status instanceof Number) {
            return ((Number) status).intValue() != 0;
        }
        String value = String.valueOf(status).trim();
        return !value.isEmpty() && !"0".equals(value);
    }

    private static String userAgent() {
        return "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
    }
}
