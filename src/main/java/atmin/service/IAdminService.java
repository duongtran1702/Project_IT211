package atmin.service;

import atmin.controller.admin.dto.request.UserCreateRequest;
import atmin.controller.admin.dto.request.UserUpdateRequest;
import atmin.controller.admin.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IAdminService {
    Page<UserResponse> getUsers(String keyword, Pageable pageable);
    UserResponse getUserById(Long id);
    UserResponse createUser(UserCreateRequest request);
    UserResponse updateUser(Long id, UserUpdateRequest request);
    void deleteUser(Long id);
}
