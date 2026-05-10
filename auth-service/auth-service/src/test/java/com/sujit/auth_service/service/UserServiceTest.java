package com.sujit.auth_service.service;

import com.sujit.auth_service.dto.RegisterRequest;
import com.sujit.auth_service.entity.User;
import com.sujit.auth_service.enums.Role;
import com.sujit.auth_service.exception.AlreadyExistsException;
import com.sujit.auth_service.exception.ResourceNotFoundException;
import com.sujit.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private RegisterRequest registerRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("testuser", "test@example.com", "password123");
        
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.FREE)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should register a new user successfully")
    void testRegisterUserSuccess() {
        // Arrange
        when(userRepository.existsByUsername(registerRequest.username())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User registeredUser = userService.register(registerRequest);

        // Assert
        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getUsername()).isEqualTo("testuser");
        assertThat(registeredUser.getEmail()).isEqualTo("test@example.com");
        assertThat(registeredUser.getRole()).isEqualTo(Role.FREE);

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(registerRequest.password());
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void testRegisterUserWithExistingUsername() {
        // Arrange
        when(userRepository.existsByUsername(registerRequest.username())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessage("username already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void testRegisterUserWithExistingEmail() {
        // Arrange
        when(userRepository.existsByUsername(registerRequest.username())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessage("email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should find user by username successfully")
    void testFindByUsernameSuccess() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> foundUser = userService.findByUsername("testuser");

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should return empty optional when user not found by username")
    void testFindByUsernameNotFound() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act
        Optional<User> foundUser = userService.findByUsername("nonexistent");

        // Assert
        assertThat(foundUser).isEmpty();

        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should find user by email successfully")
    void testFindByEmailSuccess() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> foundUser = userService.findByEmail("test@example.com");

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");

        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should handle null email when finding by email")
    void testFindByEmailWithNullEmail() {
        // Act
        Optional<User> foundUser = userService.findByEmail(null);

        // Asset
        assertThat(foundUser).isEmpty();

        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Should normalize email when finding by email")
    void testFindByEmailNormalization() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        userService.findByEmail("  TEST@EXAMPLE.COM  ");

        // Assert
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should get user by username successfully")
    void testGetByUsernameSuccess() {
        // Assert
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        User foundUser = userService.getByUsername("testuser");

        // Assert
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should throw exception when getting user by username that doesn't exist")
    void testGetByUsernameNotFound() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getByUsername("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("user not found");
    }

    @Test
    @DisplayName("Should trim and lowercase email during registration")
    void testEmailTrimmingAndLowercasing() {
        // Arrange
        RegisterRequest requestWithSpaces = new RegisterRequest("testuser", "  TEST@EXAMPLE.COM  ", "password123");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.register(requestWithSpaces);

        // Asset
        verify(userRepository).save(argThat(user -> 
                user.getEmail().equals("test@example.com")
        ));
    }
}
