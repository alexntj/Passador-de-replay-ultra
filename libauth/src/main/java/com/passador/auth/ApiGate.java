package com.passador.auth;

import android.content.Context;
import android.provider.Settings;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class ApiGate {
    private static final String VALIDATE_URL = "https://ghostcheats.de.deplexo.com/api/validate";
    private static final String USER_AGENT = "PrivReplay/2.0";

    private ApiGate() {
    }

    public static Validation validate(
            Context context,
            String key,
            String previousSessionToken,
            String appSlug,
            String appVersion
    ) {
        HttpURLConnection connection = null;
        String sessionToken = previousSessionToken == null ? "" : previousSessionToken;
        try {
            String hwid = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );
            if (hwid == null || hwid.isEmpty()) hwid = "unknown-device";

            JSONObject body = new JSONObject()
                    .put("key", key)
                    .put("hwid", hwid)
                    .put("appSlug", appSlug)
                    .put("appVersion", appVersion);
            if (!sessionToken.isEmpty()) body.put("sessionToken", sessionToken);

            connection = (HttpURLConnection) new URL(VALIDATE_URL).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setInstanceFollowRedirects(false);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Cache-Control", "no-cache");
            if (!sessionToken.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + sessionToken);
                connection.setRequestProperty("X-Session-Token", sessionToken);
            }

            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            InputStream stream = responseCode >= 200 && responseCode <= 299
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            if (stream == null) {
                return new Validation(false, "Servidor sem resposta (HTTP " + responseCode + ").", sessionToken);
            }

            String responseText;
            try (InputStream input = stream) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[4096];
                int count;
                while ((count = input.read(chunk)) != -1) {
                    buffer.write(chunk, 0, count);
                }
                responseText = new String(buffer.toByteArray(), StandardCharsets.UTF_8).trim();
            }
            if (!responseText.startsWith("{")) {
                String contentType = connection.getHeaderField("Content-Type");
                String detail;
                if (responseCode >= 300 && responseCode <= 399) {
                    detail = "redirecionamento HTTP " + responseCode;
                } else if (contentType != null && contentType.toLowerCase().contains("text/html")) {
                    detail = "página HTML recebida";
                } else {
                    detail = "resposta não JSON";
                }
                return new Validation(false,
                        "Servidor retornou " + detail + " (HTTP " + responseCode + "). Confira a URL da API.",
                        sessionToken);
            }

            JSONObject response = new JSONObject(responseText);
            boolean authorized = response.optBoolean("success", false)
                    && "active".equals(response.optString("status"))
                    && !response.optBoolean("killed", false)
                    && !response.optBoolean("banned", false);
            String returnedToken = response.optString("sessionToken", sessionToken);
            return new Validation(
                    authorized,
                    response.optString("message"),
                    returnedToken
            );
        } catch (Exception exception) {
            String message = exception.getMessage();
            if (message == null || message.isEmpty()) message = exception.getClass().getSimpleName();
            return new Validation(false, "Erro de conexão com o servidor: " + message, sessionToken);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    public static final class Validation {
        private final boolean authorized;
        private final String message;
        private final String sessionToken;

        public Validation(boolean authorized, String message, String sessionToken) {
            this.authorized = authorized;
            this.message = message == null ? "" : message;
            this.sessionToken = sessionToken == null ? "" : sessionToken;
        }

        public boolean isAuthorized() {
            return authorized;
        }

        public String getMessage() {
            return message;
        }

        public String getSessionToken() {
            return sessionToken;
        }
    }
}
