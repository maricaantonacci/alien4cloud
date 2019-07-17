package alien4cloud.security.spring.oidc;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.Environment;
import org.springframework.social.UserIdSource;
import org.springframework.social.config.annotation.ConnectionFactoryConfigurer;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.config.annotation.SocialConfigurer;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.oidc.deep.api.DeepOrchestrator;
import org.springframework.social.oidc.deep.connect.OidcConnectionFactory;
import org.springframework.social.security.AuthenticationNameUserIdSource;

import alien4cloud.security.users.IAlienUserDao;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@Slf4j
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
        String orchestratorKeystorePath = environment.getProperty("deep.orchestrator.keystore.location");
        String orchestratorKeystorePassword = environment.getProperty("deep.orchestrator.keystore.password");
        KeyStore orchestratorKeystore = null;
        if (orchestratorKeystorePath != null) {
            try {
                orchestratorKeystore = KeyStore.getInstance("JKS");
                char[] keystorePassword = null;
                if (orchestratorKeystorePassword != null) {
                    keystorePassword = orchestratorKeystorePassword.toCharArray();
                }
                orchestratorKeystore.load(new FileInputStream(orchestratorKeystorePath), keystorePassword);
            } catch (KeyStoreException e) {
                log.error("Error creating keystore for the orchestrator certificate", e);
            } catch (IOException e) {
                log.error("Error loading orchestrator certificate keystore", e);
            } catch (NoSuchAlgorithmException e) {
                log.error("Invalid algorithm for orchestrator certificate keystore", e);
            } catch (CertificateException e) {
                log.error("Invalid orchestrator certificate", e);
            }
        }
        connectionFactoryConfigurer.addConnectionFactory(
                    new OidcConnectionFactory(
                            environment.getProperty("deep.orchestrator.url"),
                            orchestratorKeystore,
                            environment.getProperty("oidc.iam.issuer"),
                            environment.getProperty("oidc.iam.client-id"),
                            environment.getProperty("oidc.iam.client-secret")));
    }

    @Bean
    @Scope(value="request", proxyMode= ScopedProxyMode.INTERFACES)
    public DeepOrchestrator orchestrator(ConnectionRepository repository) {
        Connection<DeepOrchestrator> connection = repository.findPrimaryConnection(DeepOrchestrator.class);
        return connection != null ? connection.getApi() : null;
    }

    @Override
    public UsersConnectionRepository getUsersConnectionRepository(ConnectionFactoryLocator connectionFactoryLocator) {
        return new OidcUserConnectionRepository(alienUserDao, connectionFactoryLocator);
    }
}