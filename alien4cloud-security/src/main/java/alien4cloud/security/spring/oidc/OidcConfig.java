package alien4cloud.security.spring.oidc;

import javax.annotation.Resource;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.social.UserIdSource;
import org.springframework.social.config.annotation.ConnectionFactoryConfigurer;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.config.annotation.SocialConfigurer;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.oidc.connect.OidcConnectionFactory;
import org.springframework.social.security.AuthenticationNameUserIdSource;

import alien4cloud.security.users.IAlienUserDao;

@Configuration
@EnableSocial
@Profile("oidc-auth")
public class OidcConfig implements SocialConfigurer {
    @Resource
    private IAlienUserDao alienUserDao;

    @Override
    public UserIdSource getUserIdSource() {
        return new AuthenticationNameUserIdSource();
    }

    @Override
    public void addConnectionFactories(ConnectionFactoryConfigurer connectionFactoryConfigurer, Environment environment) {
            connectionFactoryConfigurer.addConnectionFactory(
                    new OidcConnectionFactory(
                            environment.getProperty("oidc.iam.issuer"),
                            environment.getProperty("oidc.iam.client-id"),
                            environment.getProperty("oidc.iam.client-secret")));
    }

    @Override
    public UsersConnectionRepository getUsersConnectionRepository(ConnectionFactoryLocator connectionFactoryLocator) {
        return new OidcUserConnectionRepository(alienUserDao, connectionFactoryLocator);
    }
}