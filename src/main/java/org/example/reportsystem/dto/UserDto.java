// org.example.reportsystem.dto.UserDto
package org.example.reportsystem.dto;
import org.example.reportsystem.model.User;

public record UserDto(Long id, String name, String role, String username, String email) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getName(),
                u.getRole() == null ? null : u.getRole().name(),
                u.getUsername(), u.getEmail());
    }
}
