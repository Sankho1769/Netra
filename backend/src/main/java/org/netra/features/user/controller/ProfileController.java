package org.netra.features.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.netra.core.security.ClientIpResolver;
import org.netra.features.user.dto.UpdateUserProfileRequest;
import org.netra.features.user.dto.UserProfileDto;
import org.netra.features.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final UserService userService;
    private final ClientIpResolver clientIpResolver;

    public ProfileController(UserService userService, ClientIpResolver clientIpResolver) {
        this.userService = userService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMyProfile() {
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> updateMyProfile(
            @Valid @RequestBody UpdateUserProfileRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(userService.updateCurrentUserProfile(request, clientIp, userAgent));
    }
}
