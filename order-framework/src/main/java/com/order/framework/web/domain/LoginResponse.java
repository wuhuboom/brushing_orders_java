package com.order.framework.web.domain;

public class LoginResponse {
    private String token;
    private boolean firstTimeGoogleSetup;
    private String qrCodeBase64;
    private String otpAuthUrl;
    private String secret;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isFirstTimeGoogleSetup() {
        return firstTimeGoogleSetup;
    }

    public void setFirstTimeGoogleSetup(boolean firstTimeGoogleSetup) {
        this.firstTimeGoogleSetup = firstTimeGoogleSetup;
    }

    public String getQrCodeBase64() {
        return qrCodeBase64;
    }

    public void setQrCodeBase64(String qrCodeBase64) {
        this.qrCodeBase64 = qrCodeBase64;
    }

    public String getOtpAuthUrl() {
        return otpAuthUrl;
    }

    public void setOtpAuthUrl(String otpAuthUrl) {
        this.otpAuthUrl = otpAuthUrl;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}

