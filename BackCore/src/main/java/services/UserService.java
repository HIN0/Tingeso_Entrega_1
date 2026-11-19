package services;

import entities.UserEntity;
import entities.enums.UserRole;
import repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity createUser(UserEntity user) {
        return userRepository.save(user);
    }

    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Keycloak is the source of truth for users: the local record (used for auditing in
    // Kardex and loans) is created on first access and its role kept in sync with the token.
    @Transactional
    public UserEntity findOrCreate(String username, UserRole role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank.");
        }
        Optional<UserEntity> existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            UserEntity user = existing.get();
            if (role != null && user.getRole() != role) {
                user.setRole(role);
                return userRepository.save(user);
            }
            return user;
        }
        return userRepository.save(UserEntity.builder().username(username).role(role).build());
    }
}
