package org.kruskopf.backend.user.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.kruskopf.backend.character.Character;
import org.kruskopf.backend.message.Message;
import org.kruskopf.backend.user.UserRole;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", unique = true, nullable = false)
    //@jakarta.persistence.Column(unique = true, name = "provider_id")
    private String providerId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProviderType providerType;

    @Column(nullable = false, unique = true)
    //@jakarta.persistence.Column(unique = true, name = "email")
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(nullable = false, unique = true)
    //@jakarta.persistence.Column(unique = true, name = "user_name")
    private String userName;

    @Enumerated(EnumType.STRING)
    private UserRole role; // Enum: USER/ADMIN

    private String password;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Character> characters = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();


    public User() {
    }

    // parameterized constructor
    public User(String providerId, ProviderType providerType, String email, String fullName, String username, UserRole role, String password) {
        this.providerId = providerId;
        this.providerType = providerType;
        this.email = email;
        this.fullName = fullName;
        this.userName = username;
        this.role = role;
        setPassword(password);
    }
    // getters and setters

    public Long getId() {
        return id;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public ProviderType getProviderType() {
        return providerType;
    }

    public void setProviderType(ProviderType providerType) {
        this.providerType = providerType;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String username) {
        this.userName = username;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        this.password = passwordEncoder.encode(password);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Character> getCharacters() {
        return characters;
    }

    public void setCharacters(List<Character> characters) {
        this.characters = characters;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
//@Entity
//public class User {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//
//    @jakarta.persistence.Column(unique = true, name = "google_id")
//    private String googleId;
//
//    @jakarta.persistence.Column(unique = true, name = "user_name")
//    private String userName;
//
//    @Column(name = "full_name")
//    private String fullName;
//
//    @jakarta.persistence.Column(unique = true, name = "email")
//    private String email;
//
//    @Column(name = "role")
//    private String role;
//
//    @Column(name = "password")
//    private String password;
//
//    public User() {
//    }
//
//    // Parameterized constructor
//    public User(String googleId, String email, String fullName) {
//        this.googleId = googleId;
//        this.email = email;
//        this.fullName = fullName;
//        this.userName = email;
//        this.password = new BCryptPasswordEncoder().encode("password"); // Set a default password
//        this.role = "USER"; // Set a default role
//    }
//
//    public String getGoogleId() {
//        return googleId;
//    }
//
//    public void setGoogleId(String googleId) {
//        this.googleId = googleId;
//    }
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getUserName() {
//        return userName;
//    }
//
//    public void setUserName(String userName) {
//        this.userName = userName;
//    }
//
//    public String getFullName() {
//        return fullName;
//    }
//
//    public void setFullName(String fullName) {
//        this.fullName = fullName;
//    }
//
//    public String getEmail() {
//        return email;
//    }
//
//    public void setEmail(String email) {
//        this.email = email;
//    }
//
//    public String getRole() {
//        return role;
//    }
//
//    public void setRole(String role) {
//        this.role = role;
//    }
//
//    public String getPassword() {
//        return password;
//    }
//
//    public void setPassword(String password) {
//        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
//        this.password = passwordEncoder.encode(password);
//    }
//}
