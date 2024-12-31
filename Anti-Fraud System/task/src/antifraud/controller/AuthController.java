package antifraud.controller;

import antifraud.DTO.UserDTO;
import antifraud.model.User;
import antifraud.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/user")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            return new ResponseEntity<>("Username is required", HttpStatus.BAD_REQUEST);
        }

        if (user.getPassword() == null || user.getPassword().length() < 6) {
            return new ResponseEntity<>("Password must be at least 6 characters", HttpStatus.BAD_REQUEST);
        }

        if (user.getName() == null || user.getName().isEmpty()) {
            return new ResponseEntity<>("Name is required", HttpStatus.BAD_REQUEST);
        }

        if (userService.isUsernameTaken(user.getUsername())) {
            return new ResponseEntity<>("Username already exists", HttpStatus.CONFLICT);
        }

        User savedUser = userService.saveUser(user);
        UserDTO userDTO = new UserDTO(savedUser.getId(), savedUser.getName(), savedUser.getUsername());

        return new ResponseEntity<>(userDTO, HttpStatus.CREATED);
    }

    @GetMapping("/list")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            List<UserDTO> userDTOs = users.stream()
                    .map(user -> new UserDTO(user.getId(), user.getName(), user.getUsername()))
                    .toList();
            return ResponseEntity.ok(userDTOs);
        } catch (Exception e) {
            log.error("Error fetching users", e);
            return new ResponseEntity<>("Error fetching users", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/user/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
        userService.deleteUser(user);

        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("username", username);
        responseMap.put("status", "Deleted successfully!");

        return ResponseEntity.ok().body(responseMap);
    }
}
