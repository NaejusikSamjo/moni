package com.moni.user.auth.application.oauth;

public interface OAuthClient {

    OAuthUserInfo exchangeCodeForUser(String code, String codeVerifier);

    String buildLoginUrl(String codeChallenge, String state);
}
