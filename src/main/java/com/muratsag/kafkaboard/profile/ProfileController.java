package com.muratsag.kafkaboard.profile;

import com.muratsag.kafkaboard.auth.dto.AuthResponse;
import com.muratsag.kafkaboard.profile.dto.DeleteAccountRequest;
import com.muratsag.kafkaboard.profile.dto.ProfileResponse;
import com.muratsag.kafkaboard.profile.dto.UpdateDisplayNameRequest;
import com.muratsag.kafkaboard.profile.dto.UpdateEmailRequest;
import com.muratsag.kafkaboard.profile.dto.UpdatePasswordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(profileService.getProfile(authentication));
    }

    @PutMapping("/email")
    public ResponseEntity<AuthResponse> updateEmail(
            Authentication authentication,
            @RequestBody UpdateEmailRequest request
    ) {
        return ResponseEntity.ok(profileService.updateEmail(authentication, request));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> updatePassword(
            Authentication authentication,
            @RequestBody UpdatePasswordRequest request
    ) {
        profileService.updatePassword(authentication, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/display-name")
    public ResponseEntity<ProfileResponse> updateDisplayName(
            Authentication authentication,
            @RequestBody UpdateDisplayNameRequest request
    ) {
        return ResponseEntity.ok(profileService.updateDisplayName(authentication, request));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(
            Authentication authentication,
            @RequestBody DeleteAccountRequest request
    ) {
        profileService.deleteAccount(authentication, request.getPassword());
        return ResponseEntity.noContent().build();
    }
}
