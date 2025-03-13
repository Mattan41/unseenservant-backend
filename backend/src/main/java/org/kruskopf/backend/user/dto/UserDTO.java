package org.kruskopf.backend.user.dto;

import org.kruskopf.backend.user.entity.User;

public record UserDTO(Long id, String username, String email, String displayName) {
    public static UserDTO fromUser(User user) {
        String effectiveDisplayName = user.getDisplayName() != null && !user.getDisplayName().isBlank()
                ? user.getDisplayName()
                : user.getFullName();
        return new UserDTO(user.getId(), user.getUserName(), user.getEmail(), effectiveDisplayName);
    }
}