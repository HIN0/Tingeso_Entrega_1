package com.ProyectoTGS.BackCore.Services;

import entities.UserEntity;
import entities.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repositories.UserRepository;
import services.UserService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // =======================================================================
    // MÉTODO: createUser
    // =======================================================================

    @Test
    void createUser_Success() {
        // ARRANGE
        UserEntity userToCreate = UserEntity.builder()
                .username("testuser")
                .password("pass123")
                .role(UserRole.EMPLOYEE)
                .build();
        
        UserEntity savedUser = UserEntity.builder()
                .id(1L) // Simular que la BD asigna un ID
                .username("testuser")
                .password("pass123")
                .role(UserRole.EMPLOYEE)
                .build();

        // Mockeo: Cuando se guarde el usuario, devolver el usuario con ID
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        // ACT
        UserEntity result = userService.createUser(userToCreate);

        // ASSERT
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        
        // Verificar que save fue llamado una vez con el objeto correcto
        verify(userRepository, times(1)).save(userToCreate);
    }

    // =======================================================================
    // MÉTODO: findByUsername
    // =======================================================================

    @Test
    void findByUsername_Success_WhenUserExists() {
        // ARRANGE
        String username = "diego";
        UserEntity existingUser = UserEntity.builder()
                .id(2L)
                .username(username)
                .role(UserRole.ADMIN)
                .build();
        
        // Mockeo: El repositorio encuentra al usuario
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));

        // ACT
        Optional<UserEntity> resultOptional = userService.findByUsername(username);

        // ASSERT
        assertTrue(resultOptional.isPresent()); // Verificar que el Optional contiene algo
        assertEquals(existingUser, resultOptional.get()); // Verificar que es el usuario correcto
        
        // Verificar que el repositorio fue llamado
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    void findByUsername_ReturnsEmpty_WhenUserNotFound() {
        // ARRANGE
        String username = "usuario_inexistente";
        
        // Mockeo: El repositorio NO encuentra al usuario
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // ACT
        Optional<UserEntity> resultOptional = userService.findByUsername(username);

        // ASSERT
        assertTrue(resultOptional.isEmpty()); // Verificar que el Optional está vacío
        
        // Verificar que el repositorio fue llamado
        verify(userRepository, times(1)).findByUsername(username);
    }

    // =======================================================================
    // MÉTODO: findOrCreate (user provisioning from the Keycloak JWT)
    // =======================================================================

    @Test
    void findOrCreate_CreatesUserWhenMissing() {
        when(userRepository.findByUsername("maria")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserEntity result = userService.findOrCreate("maria", UserRole.EMPLOYEE);

        assertEquals("maria", result.getUsername());
        assertEquals(UserRole.EMPLOYEE, result.getRole());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void findOrCreate_ReturnsExistingUserWithoutSaving() {
        UserEntity existing = UserEntity.builder().id(1L).username("diego").role(UserRole.ADMIN).build();
        when(userRepository.findByUsername("diego")).thenReturn(Optional.of(existing));

        assertSame(existing, userService.findOrCreate("diego", UserRole.ADMIN));
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void findOrCreate_SyncsRoleWhenItChangedInKeycloak() {
        UserEntity existing = UserEntity.builder().id(2L).username("juan").role(UserRole.EMPLOYEE).build();
        when(userRepository.findByUsername("juan")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        UserEntity result = userService.findOrCreate("juan", UserRole.ADMIN);

        assertEquals(UserRole.ADMIN, result.getRole());
        verify(userRepository).save(existing);
    }

    @Test
    void findOrCreate_FailsOnBlankUsername() {
        assertThrows(IllegalArgumentException.class, () -> userService.findOrCreate(" ", UserRole.EMPLOYEE));
        assertThrows(IllegalArgumentException.class, () -> userService.findOrCreate(null, UserRole.EMPLOYEE));
    }
}