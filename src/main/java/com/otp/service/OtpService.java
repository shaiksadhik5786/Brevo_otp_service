package com.otp.service;

import com.otp.entity.OtpVerification;
import com.otp.entity.OtpVerification.OtpStatus;
import com.otp.repository.OtpVerificationRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OtpService {

	private final OtpVerificationRepository otpRepository;
	private final EmailService emailService;
	private final PasswordEncoder passwordEncoder;

	private final SecureRandom secureRandom = new SecureRandom();

	private static final int OTP_EXPIRY_MINUTES = 5;

	private static final int MAX_ATTEMPTS = 5;

	private static final int RESEND_COOLDOWN_SECONDS = 60;

	public OtpService(OtpVerificationRepository otpRepository, EmailService emailService,
			PasswordEncoder passwordEncoder) {

		this.otpRepository = otpRepository;
		this.emailService = emailService;
		this.passwordEncoder = passwordEncoder;

	}

	@Transactional
	public void sendOtp(String email) {

		email = normalizeEmail(email);

		LocalDateTime now = LocalDateTime.now();

		// Check last OTP
		var latestOtp = otpRepository.findTopByEmailOrderByCreatedAtDesc(email);

		if (latestOtp.isPresent()) {

			OtpVerification previous = latestOtp.get();

			LocalDateTime cooldownTime = previous.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS);

			if (now.isBefore(cooldownTime)) {

				long remainingSeconds = java.time.Duration.between(now, cooldownTime).getSeconds();

				throw new RuntimeException(
						"Please wait " + remainingSeconds + " seconds before requesting another OTP");
			}
		}

		// Invalidate previous pending OTPs
		List<OtpVerification> pendingOtps = otpRepository.findByEmailAndStatus(email, OtpStatus.PENDING);

		for (OtpVerification otp : pendingOtps) {

			otp.setStatus(OtpStatus.EXPIRED);

			otpRepository.save(otp);
		}

		// Generate OTP
		String otp = generateOtp();

		// Hash OTP
		String otpHash = passwordEncoder.encode(otp);

		OtpVerification otpVerification = new OtpVerification();

		otpVerification.setEmail(email);
		otpVerification.setOtpHash(otpHash);
		otpVerification.setCreatedAt(now);

		otpVerification.setExpiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES));

		otpVerification.setAttempts(0);
		otpVerification.setMaxAttempts(MAX_ATTEMPTS);
		otpVerification.setStatus(OtpStatus.PENDING);

		// Save before sending
		OtpVerification saved = otpRepository.save(otpVerification);

		try {

			String messageId = emailService.sendOtpEmail(email, otp);

			saved.setBrevoMessageId(messageId);

			otpRepository.save(saved);

		} catch (Exception exception) {

			saved.setStatus(OtpStatus.FAILED);

			otpRepository.save(saved);

			throw new RuntimeException("Unable to send OTP email through Brevo");
		}
	}

	@Transactional
	public void verifyOtp(String email, String enteredOtp) {

		email = normalizeEmail(email);
		System.out.println(email + " " + enteredOtp);

		OtpVerification otpVerification = otpRepository
				.findTopByEmailAndStatusOrderByCreatedAtDesc(email, OtpStatus.PENDING)
				.orElseThrow(() -> new RuntimeException("No active OTP found"));

		LocalDateTime now = LocalDateTime.now();

		// Check expiration
		if (now.isAfter(otpVerification.getExpiresAt())) {

			otpVerification.setStatus(OtpStatus.EXPIRED);

			otpRepository.save(otpVerification);

			throw new RuntimeException("OTP has expired. Please request a new OTP.");
		}

		// Check attempts
		if (otpVerification.getAttempts() >= otpVerification.getMaxAttempts()) {

			otpVerification.setStatus(OtpStatus.FAILED);

			otpRepository.save(otpVerification);

			throw new RuntimeException("Maximum OTP verification attempts exceeded");
		}

		// Verify BCrypt
		boolean matches = passwordEncoder.matches(enteredOtp, otpVerification.getOtpHash());

		if (!matches) {

			int attempts = otpVerification.getAttempts() + 1;

			otpVerification.setAttempts(attempts);

			if (attempts >= otpVerification.getMaxAttempts()) {

				otpVerification.setStatus(OtpStatus.FAILED);

				otpRepository.save(otpVerification);

				throw new RuntimeException("Maximum OTP verification attempts exceeded");
			}

			otpRepository.save(otpVerification);

			int remaining = otpVerification.getMaxAttempts() - attempts;

			throw new RuntimeException("Invalid OTP. Attempts remaining: " + remaining);
		}

		// OTP correct
		otpVerification.setStatus(OtpStatus.VERIFIED);

		otpVerification.setVerifiedAt(now);

		otpRepository.save(otpVerification);
	}

	private String generateOtp() {

		int number = secureRandom.nextInt(900000) + 100000;

		return String.valueOf(number);
	}

	private String normalizeEmail(String email) {

		return email.trim().toLowerCase();

	}
}