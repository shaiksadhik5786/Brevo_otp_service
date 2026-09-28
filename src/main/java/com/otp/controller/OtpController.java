package com.otp.controller;

import com.otp.dto.ApiResponse;
import com.otp.dto.SendOtpRequest;
import com.otp.dto.VerifyOtpRequest;
import com.otp.service.OtpService;
import com.otp.*;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

	private final OtpService otpService;

	public OtpController(OtpService otpService) {

		this.otpService = otpService;

	}

	@GetMapping("/health")
	public ResponseEntity<ApiResponse> health() {

		return ResponseEntity.ok(new ApiResponse(true, "OTP service is running"));
	}

	@PostMapping("/send")
	public ResponseEntity<ApiResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {

		otpService.sendOtp(request.getEmail());

		return ResponseEntity.ok(new ApiResponse(true, "OTP sent successfully to your email"));
	}

	@PostMapping("/verify")
	public ResponseEntity<ApiResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {

		otpService.verifyOtp(request.getEmail(), request.getOtp());

		return ResponseEntity.ok(new ApiResponse(true, "OTP verified successfully"));
	}
}