package com.otp.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.*;

@Service
public class EmailService {

	private final RestClient restClient;

	@Value("${brevo.api.key}")
	private String apiKey;

	@Value("${brevo.sender.email}")
	private String senderEmail;

	@Value("${brevo.sender.name}")
	private String senderName;

	public EmailService(RestClient restClient) {

		this.restClient = restClient;

	}

	public String sendOtpEmail(String recipientEmail, String otp) {

		String htmlContent = buildEmailHtml(otp);

		Map<String, Object> sender = Map.of("email", senderEmail, "name", senderName);

		Map<String, Object> recipient = Map.of("email", recipientEmail);

		Map<String, Object> requestBody = Map.of("sender", sender, "to", new Map[] { recipient }, "subject",
				"Your OTP Verification Code", "htmlContent", htmlContent);

		BrevoResponse response = restClient.post().uri("https://api.brevo.com/v3/smtp/email").header("api-key", apiKey)
				.header("accept", "application/json").contentType(MediaType.APPLICATION_JSON).body(requestBody)
				.retrieve().body(BrevoResponse.class);

		if (response == null) {

			throw new RuntimeException("Brevo returned an empty response");

		}

		return response.getMessageId();
	}

	private String buildEmailHtml(String otp) {

		return """
				<!DOCTYPE html>
				<html>
				<head>
				    <meta charset="UTF-8">
				    <title>Email Verification</title>
				</head>

				<body style="
				    margin:0;
				    padding:0;
				    background:#f4f6f8;
				    font-family:Arial,sans-serif;
				">

				    <div style="
				        max-width:600px;
				        margin:40px auto;
				        background:white;
				        padding:30px;
				        border-radius:10px;
				        box-shadow:0 2px 10px rgba(0,0,0,0.1);
				    ">

				        <h2 style="text-align:center;">
				            Email Verification
				        </h2>

				        <p>
				            Hello,
				        </p>

				        <p>
				            Use the following OTP to verify your email address:
				        </p>

				        <div style="
				            text-align:center;
				            margin:30px 0;
				        ">

				            <span style="
				                display:inline-block;
				                background:#f1f3f5;
				                padding:15px 30px;
				                font-size:32px;
				                font-weight:bold;
				                letter-spacing:8px;
				                border-radius:8px;
				            ">
				                %s
				            </span>

				        </div>

				        <p>
				            This OTP is valid for <strong>5 minutes</strong>.
				        </p>

				        <p>
				            Do not share this OTP with anyone.
				        </p>

				        <hr>

				        <p style="
				            color:#777;
				            font-size:13px;
				            text-align:center;
				        ">
				            If you did not request this OTP,
				            please ignore this email.
				        </p>

				    </div>

				</body>
				</html>
				""".formatted(otp);
	}

	public static class BrevoResponse {

		private String messageId;

		public BrevoResponse() {
		}

		public String getMessageId() {
			return messageId;
		}

		public void setMessageId(String messageId) {
			this.messageId = messageId;
		}
	}
}