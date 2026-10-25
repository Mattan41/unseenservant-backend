package org.kruskopf.backend.user.entity;

import jakarta.persistence.*;
import org.kruskopf.backend.entity.BaseEntity;
import org.kruskopf.backend.message.entity.Message;
import org.kruskopf.backend.playercharacter.entity.GameCharacter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_username", columnList = "userName"),
        @Index(name = "idx_user_provider_id", columnList = "provider_id")
})
public class User extends BaseEntity {

    @Column(name = "provider_id", unique = true, nullable = false)
    private String providerId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProviderType providerType;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(nullable = false, unique = true)
    private String userName;

    @Column(name = "display_name", nullable = true, length = 50, unique = true)
    private String displayName;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private String password;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameCharacter> playerCharacters = new ArrayList<>();

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
    // todo: remove setters that are unesesary and could be a security risk

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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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
        this.password = password;
    }

    public List<GameCharacter> getCharacters() {
        return playerCharacters;
    }

    public void setCharacters(List<GameCharacter> playerCharacters) {
        this.playerCharacters = playerCharacters;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}