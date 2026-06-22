package com.bankplanet9.milkyway.auth;

/** Supplies bearer access tokens for the Payments API. */
public interface AccessTokenProvider {

    /**
     * Returns a valid access token.
     *
     * @param forceRefresh when {@code true}, bypass the cache and acquire a fresh
     *                     token (used after a {@code 401} to recover from a revoked
     *                     or early-expired token).
     */
    String getAccessToken(boolean forceRefresh);
}
