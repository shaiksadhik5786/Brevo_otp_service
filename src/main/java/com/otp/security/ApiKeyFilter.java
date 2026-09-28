package com.otp.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

	@Value("${app.api-key}")
	private String apiKey;

	private static final String API_KEY_HEADER = "API-KEY";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String requestUri = request.getRequestURI();

		// Allow health check without API key
		if (requestUri.equals("/api/otp/health")) {
			filterChain.doFilter(request, response);
			return;
		}

		String requestApiKey = request.getHeader(API_KEY_HEADER);

		if (requestApiKey == null || requestApiKey.isBlank()) {
			sendUnauthorized(response, "API key is required");
			return;
		}

		boolean validKey = MessageDigest.isEqual(requestApiKey.getBytes(StandardCharsets.UTF_8),
				apiKey.getBytes(StandardCharsets.UTF_8));

		if (!validKey) {
			sendUnauthorized(response, "Invalid API key");
			return;
		}

		filterChain.doFilter(request, response);
	}

	private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {

		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType("application/json");

		response.getWriter().write("""
				{
				    "success": false,
				    "message": "%s"
				}
				""".formatted(message));
	}
}