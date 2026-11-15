package com.gawt.gawt_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class BrevoEmailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Value("${brevo.api-key}")
    private String apiKey;

    /** Sender address; must be verified in Brevo (BREVO_SENDER_EMAIL in production). */
    @Value("${brevo.sender-email}")
    private String senderEmail;

    public void sendOtpEmail(String toEmail, String firstName, String otp, long expiryMinutes) {
        try {
            URL url = new URL(BREVO_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("api-key", apiKey);  // Use the instance variable
            conn.setDoOutput(true);

            String subject = "Your OTP Code";

            // Use String.format or StringBuilder for cleaner code
            String htmlContent = String.format(
                    "<p>Hi %s,</p><p>Your OTP code is: <b>%s</b></p><p>This code will expire in %d minutes.</p>",
                    firstName, otp, expiryMinutes
            );

            // Use ObjectMapper or proper JSON escaping
            String jsonPayload = String.format(
                    "{\"sender\":{\"name\":\"GAWT App\",\"email\":\"%s\"},\"to\":[{\"email\":\"%s\"}],\"subject\":\"%s\",\"htmlContent\":\"%s\"}",
                    escapeJson(senderEmail), toEmail, subject, escapeJson(htmlContent)
            );

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == 201 || responseCode == 200) {
                System.out.println("OTP email sent successfully to " + toEmail);
            } else {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder error = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        error.append(line.trim());
                    }
                    System.err.println("Error Response: " + error.toString());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper method to escape JSON strings properly
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}