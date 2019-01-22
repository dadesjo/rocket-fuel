package impl;

import api.Auth;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.jaxrs.WebException;

import java.util.Objects;
import java.util.Optional;

import static rx.Observable.error;
import static se.fortnox.reactivewizard.util.rx.FirstThen.first;

/**
 * When a resource method has a {@link Auth} in the signature, this parameter resolver will try
 * to bind the parameter to an auth instance, that can be used inside of the method for getting valuable information
 * about the signed in user.
 *
 */
@Singleton
public class AuthResolverImpl implements AuthResolver {
    private static final Logger LOG = LoggerFactory.getLogger(AuthResolverImpl.class);
    private static final String FAILURE_TO_AUTHENTICATE_REQUEST = "failure to authenticate request";
    private static final Observable<Auth> UNAUTHORIZED = error(new WebException(HttpResponseStatus.UNAUTHORIZED, FAILURE_TO_AUTHENTICATE_REQUEST));
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private final JwtAuthenticator jwtAuthenticator;
    private final UserDao userResource;

    @Inject
    public AuthResolverImpl(JwtAuthenticator jwtAuthenticator, UserDao userResource) {
        this.userResource = userResource;
        Objects.requireNonNull(jwtAuthenticator);
        this.jwtAuthenticator = jwtAuthenticator;
    }

    /**
     * Resolves a auth for a resource method as part of the authentication process. If the authentication
     * fails, a unauthorized will be returned instead.
     *
     * @param request the request bound to a resource method with an Auth in the signature.
     * @return Auth or an exception, if failure to authorize.
     */
    @Override
    public Observable<Auth> resolve(JaxRsRequest request) {
        try {
            return resolveAuth(request);
        } catch (Throwable e) {
            LOG.warn("Unexpected error loading auth", e);
            return UNAUTHORIZED;
        }
    }

    /**
     * Resolves an auth through a JWT instance.
     * @param request the current request
     * @return Auth for the user
     */
    private Observable<Auth> resolveAuth(JaxRsRequest request) {
        Optional<String> possibleAuthorization = Optional.ofNullable(request.getHeader(AUTHORIZATION_HEADER));
        if(!possibleAuthorization.isPresent()) {
            return UNAUTHORIZED;
        }

        return jwtAuthenticator.getAuth(possibleAuthorization.get())
                .flatMap(auth -> first(userResource.upsertUser(auth.getEmail())).thenReturn(auth));
    }

}
