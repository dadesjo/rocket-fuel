package impl;

import api.User;
import api.UserResource;
import api.auth.ApplicationToken;
import api.auth.Auth;
import auth.openid.OpenIdValidator;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dao.UserDao;
import rx.Observable;

import javax.validation.constraints.NotNull;
import java.util.HashMap;

import static rx.Observable.just;

@Singleton
public class UserResourceImpl implements UserResource {

    private final ResponseHeaderHolder responseHeaderHolder;

    private final UserDao userDao;

    private final OpenIdValidator openIdValidator;

    private final ApplicationTokenCreator applicationTokenCreator;

    @Inject
    public UserResourceImpl(UserDao userDao, ResponseHeaderHolder responseHeaderHolder, OpenIdValidator openIdValidator, ApplicationTokenCreator applicationTokenCreator) {
        this.userDao = userDao;
        this.responseHeaderHolder = responseHeaderHolder;
        this.openIdValidator = openIdValidator;
        this.applicationTokenCreator = applicationTokenCreator;
    }

    @Override
    public Observable<String> getCurrent(Auth auth) {
        return just(Long.valueOf(auth.getUserId()).toString());
    }

    @Override
    public Observable<Integer> createUser(User user) {
        return this.userDao.insertUser(user);
    }

    @Override
    public Observable<User> getUserByEmail(String email) {
        return this.userDao.getUserByEmail(email);
    }

    @Override
    public Observable<User> getUserById(long userId) {
        return this.userDao.getUserById(userId);
    }

    @Override
    public Observable<ApplicationToken> generateToken(@NotNull String openIdToken) {
        final OpenIdValidator.ImmutableOpenIdToken validOpenId = openIdValidator.validate(openIdToken);
        return userDao.getUserByEmail(validOpenId.email).map((user)-> {
            ApplicationToken applicationToken = applicationTokenCreator.createApplicationToken(validOpenId, user.getId());
            addAsCookie(applicationToken);
            return applicationToken;
        });

    }
    private void addAsCookie(final ApplicationToken applicationToken) {
        responseHeaderHolder.addHeaders(applicationToken, new HashMap<String, Object>() {{
            put("Set-Cookie", "application=" + applicationToken.getApplicationToken() + "; path=/; domain=" + "localhost" + ";");
        }});
    }

}
