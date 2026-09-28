package com.otp.repository;

import com.otp.entity.OtpVerification;
import com.otp.entity.OtpVerification.OtpStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

	Optional<OtpVerification> findTopByEmailOrderByCreatedAtDesc(String email);

	Optional<OtpVerification> findTopByEmailAndStatusOrderByCreatedAtDesc(String email, OtpStatus status);

	List<OtpVerification> findByEmailAndStatus(String email, OtpStatus status);
}