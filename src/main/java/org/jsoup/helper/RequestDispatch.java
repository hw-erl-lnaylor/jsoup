package org.jsoup.helper;

import org.jsoup.internal.SharedConstants;
import org.jspecify.annotations.Nullable;

import static org.jsoup.helper.HttpConnection.Request;
import static org.jsoup.helper.HttpConnection.Response;

import java.net.Proxy;

/**
 Handles requests using {@link java.net.http.HttpClient}, unless the system property
 {@link SharedConstants#UseHttpClient} is set to {@code false}, or the request needs a transport HttpClient does not
 support. Those requests use {@link java.net.HttpURLConnection}.
 */
class RequestDispatch {

    static RequestExecutor get(Request request, @Nullable Response previousResponse) {
        if (useHttpClient(request))
            return new HttpClientExecutor(request, previousResponse);
        return new UrlConnectionExecutor(request, previousResponse);
    }

    private static boolean useHttpClient(Request request) {
        if (!Boolean.parseBoolean(System.getProperty(SharedConstants.UseHttpClient, "true")))
            return false;
        if (request.sslSocketFactory() != null) // cannot be supplied to HttpClient
            return false;
        Proxy proxy = request.proxy();
        // HttpClient does not support SOCKS proxies
        return proxy == null || proxy.type() != Proxy.Type.SOCKS;
    }
}
