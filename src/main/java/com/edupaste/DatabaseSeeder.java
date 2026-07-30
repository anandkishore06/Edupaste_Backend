package com.edupaste;

import com.edupaste.models.Role;
import com.edupaste.models.User;
import com.edupaste.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Checking test accounts...");

        seedUserIfNotFound("Super Admin", "superadmin@edupaste.com", "password123", Role.SUPER_ADMIN);
        seedUserIfNotFound("School Admin", "schooladmin@edupaste.com", "password123", Role.SCHOOL_ADMIN);
        seedUserIfNotFound("Test Teacher", "teacher@edupaste.com", "password123", Role.TEACHER);
        seedUserIfNotFound("Test Student", "student@edupaste.com", "password123", Role.STUDENT);
        seedUserIfNotFound("Test Parent", "parent@edupaste.com", "password123", Role.PARENT);
        
        System.out.println("Test account check completed.");
    }

    private void seedUserIfNotFound(String fullName, String email, String password, Role role) {
        if (!userRepository.existsByEmail(email)) {
            userRepository.save(createUser(fullName, email, password, role));
            System.out.println("Seeded missing account: " + email);
        }
    }

    private User createUser(String fullName, String email, String password, Role role) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        
        // Ensure test users have a default schoolId to test Academic endpoints
        if (role != Role.SUPER_ADMIN) {
            user.setSchoolId(1L);
        }
        
        return user;
    }
}
