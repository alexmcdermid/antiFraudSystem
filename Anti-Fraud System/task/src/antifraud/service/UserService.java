package antifraud.service;

import antifraud.constants.Role;
import antifraud.exceptions.RoleConflictException;
import antifraud.model.User;
import antifraud.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User changeRole(User user) {
        User existingUser = userRepository.findByUsernameIgnoreCase(user.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (Role.ADMINISTRATOR.equals(user.getRole())) {
            throw new IllegalArgumentException("Cannot change role of a user with ADMINISTRATOR role");
        }

        if (!Role.MERCHANT.equals(existingUser.getRole()) && !Role.SUPPORT.equals(existingUser.getRole())) {
            throw new IllegalArgumentException("Invalid role specified");
        }

        if (user.getRole().equals(existingUser.getRole())) {
            throw new RoleConflictException("The role is already assigned to the user");
        }

        existingUser.setRole(user.getRole());
        return userRepository.save(existingUser);
    }

    public String lockUnlockUser(String username, String operation) {
        User existingUser = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (Role.ADMINISTRATOR.equals(existingUser.getRole())) {
            throw new IllegalArgumentException("Administrator cannot be locked");
        }

        if ("LOCK".equalsIgnoreCase(operation)) {
            existingUser.setLocked("LOCK");
            userRepository.save(existingUser);
            return "User " + username + " locked!";
        } else if ("UNLOCK".equalsIgnoreCase(operation)) {
            existingUser.setLocked("UNLOCK");
            userRepository.save(existingUser);
            return "User " + username + " unlocked!";
        }

        return "Invalid operation";
    }

    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsernameIgnoreCase(username);
    }

    public User saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username).orElse(null);
    }

    public List<User> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll();
    }

    public void deleteUser(User user) {
        userRepository.delete(user);
    }

    public void deleteAll() {
        userRepository.deleteAll();
    }
}
