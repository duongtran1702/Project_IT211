package atmin.controller.admin.dto.request;

import atmin.entity.Role;
import atmin.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Pattern(regexp = "^\\d{10}$", message = "Phone number must be exactly 10 numeric digits")
    private String phoneNumber;

    private Set<Long> roleIds;

    public User toEntity(String encodedPassword, Set<Role> roles) {
        return User.builder()
                .username(this.username)
                .password(encodedPassword)
                .fullName(this.fullName)
                .email(this.email)
                .phoneNumber(this.phoneNumber)
                .roles(roles)
                .isEnabled(true)
                .build();
    }
}
