package org.jsoup.helper;

import org.jspecify.annotations.Nullable;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 Handles per-request Authenticator-based authentication. The authenticator is set on the
 {@link java.net.HttpURLConnection} or {@link java.net.http.HttpClient} that executes the request.
 */
class AuthenticationHandler extends Authenticator {
    static final int MaxAttempts = 3; // max authentication attempts per request. allows for multiple auths (e.g. proxy and server) in one request, but saves otherwise 20 requests if credentials are incorrect.
    static final AuthShim handler = new RequestAuthHandler();

    @Nullable RequestAuthenticator auth;
    int attemptCount = 0;

    AuthenticationHandler() {}

    AuthenticationHandler(RequestAuthenticator auth) {
        this.auth = auth;
    }

    /**
     Authentication callback, called by the connection executing this request.
     * @return credentials, or null if not attempting to auth.
     */
    @Nullable @Override public final PasswordAuthentication getPasswordAuthentication() {
        AuthenticationHandler delegate = handler.get(this);
        if (delegate == null) return null; // this request has no auth handler
        delegate.attemptCount++;
        // if the password returned fails, Java will repeatedly retry the request with a new password auth hit (because
        // it may be an interactive prompt, and the user could eventually get it right). But in Jsoup's context, the
        // auth will either be correct or not, so just abandon
        if (delegate.attemptCount > MaxAttempts)
            return null; // When using HttpClient, this will manifest as "No credentials provided" IOException; not ideal; would be clearer if we could then detach the authenticator which would bubble the 401, but there's no path for that
        if (delegate.auth == null)
            return null; // detached

        RequestAuthenticator.Context ctx = new RequestAuthenticator.Context(
            this.getRequestingURL(), this.getRequestorType(), this.getRequestingPrompt());
        return delegate.auth.authenticate(ctx);
    }

    interface AuthShim {
        void enable(RequestAuthenticator auth, Object connOrHttp);

        void remove();

        @Nullable AuthenticationHandler get(AuthenticationHandler helper);
    }
}
