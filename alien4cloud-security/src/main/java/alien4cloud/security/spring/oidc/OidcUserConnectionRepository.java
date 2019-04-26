package alien4cloud.security.spring.oidc;

import alien4cloud.exception.NotFoundException;
import alien4cloud.security.model.Role;
import alien4cloud.security.users.IAlienUserDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.mem.InMemoryUsersConnectionRepository;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Profile("oidc-auth")
public class OidcUserConnectionRepository extends InMemoryUsersConnectionRepository {
    private IAlienUserDao alienUserDao;

    @Value("${oidc.roles}")
    private String roles;

    private String[] roleList;

    @Autowired
    public OidcUserConnectionRepository(IAlienUserDao alienUserDao, ConnectionFactoryLocator locator) {
        super(locator);
        this.alienUserDao = alienUserDao;
    }

    @PostConstruct
    public void init() {
        if (roles == null) {
            roleList = new String[]{};
        } else {
            String[] roleSplit = roles.split(",");
            List<String> goodRoles = Arrays.asList(roleSplit).stream()
                    .map(role -> {
                        try {
                            return Role.getStringFormatedRole(role);
                        } catch (NotFoundException e) {
                            return null;
                        }
                    })
                    .filter(role -> role != null)
                    .collect(Collectors.toList());
            roleList = goodRoles.toArray(new String[goodRoles.size()]);
        }
        setConnectionSignUp(new OidcConnectionSignUp(alienUserDao, roleList));
    }
}