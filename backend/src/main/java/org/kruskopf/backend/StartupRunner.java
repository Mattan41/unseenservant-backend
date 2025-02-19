package org.kruskopf.backend;

import org.kruskopf.backend.user.UserRole;
import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
public class StartupRunner implements CommandLineRunner {


    private final UserService userService;

    public StartupRunner(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userService.findByUserName("Mattan") == null) {
            User user = new User();
            user.setUserName("Mattan");
            user.setPassword("password");
            user.setFullName("Mats Kruskopf");
            user.setEmail("krishopf@gmail.com");
            user.setRole(UserRole.ROLE_USER);
            user.setProviderType(ProviderType.GOOGLE);
            user.setProviderId("123456");
            userService.save(user);
        }

        if (userService.findByUserName("Mats") == null) {
            User user2 = new User();
            user2.setUserName("Mats");
            user2.setPassword("password");
            user2.setFullName("Mats Kruskopf Eriksson");
            user2.setEmail("mats.fpoksurk@gmail.com");
            user2.setRole(UserRole.ROLE_USER);
            user2.setProviderType(ProviderType.GOOGLE);
            user2.setProviderId("654321");
            userService.save(user2);
        }

        if (userService.findByUserName("admin") == null) {
            userService.createAdminUser("admin", "admin@admin.se", "admin", "000000", ProviderType.GOOGLE);
        }
    }
}
