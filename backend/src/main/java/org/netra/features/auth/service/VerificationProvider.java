package org.netra.features.auth.service;

public interface VerificationProvider {

    void sendVerificationCode(String email, String code);
}
