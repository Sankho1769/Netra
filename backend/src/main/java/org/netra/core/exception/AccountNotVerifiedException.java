package org.netra.core.exception;

public class AccountNotVerifiedException extends RuntimeException {

    private final String email;

    public AccountNotVerifiedException(String email) {
        super("Account is not verified. Please verify your email to continue.");
        this.email = email;
    }

    public AccountNotVerifiedException(String email, String message) {
        super(message);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
