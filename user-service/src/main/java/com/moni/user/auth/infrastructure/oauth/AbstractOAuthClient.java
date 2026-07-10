package com.moni.user.auth.infrastructure.oauth;

import com.moni.user.auth.application.oauth.OAuthClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

public abstract class AbstractOAuthClient implements OAuthClient {

    protected final String clientId;
    protected final String clientSecret;
    protected final String redirectUri;
    protected final RestClient restClient;

    protected AbstractOAuthClient(
            String clientId, String clientSecret, String redirectUri, RestClient restClient) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.restClient = restClient;
    }

    protected MultiValueMap<String, String> buildTokenExchangeBody(String code, String codeVerifier) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code_verifier", codeVerifier);
        body.add("grant_type", "authorization_code");
        return body;
    }
}
