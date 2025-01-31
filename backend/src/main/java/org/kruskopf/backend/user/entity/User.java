package org.kruskopf.backend.user.entity;

import jakarta.persistence.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Entity
public class User {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @jakarta.persistence.Column(unique = true, name = "google_id")
    private String googleId;

    @jakarta.persistence.Column(unique = true, name = "user_name")
    private String userName;

    @Column(name = "full_name")
    private String fullName;

    @jakarta.persistence.Column(unique = true, name = "email")
    private String email;

    @Column(name = "role")
    private String role;

    @Column(name = "password")
    private String password;

    public User() {
    }

    // Parameterized constructor
    public User(String googleId, String email, String fullName) {
        this.googleId = googleId;
        this.email = email;
        this.fullName = fullName;
        this.userName = email;
        this.password = new BCryptPasswordEncoder().encode("password"); // Set a default password
        this.role = "USER"; // Set a default role
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        this.password = passwordEncoder.encode(password);
    }
}
