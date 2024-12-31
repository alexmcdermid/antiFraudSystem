package antifraud.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception to handle role conflicts when a role already assigned to a user.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class RoleConflictException extends RuntimeException {

    public RoleConflictException(String message) {
        super(message);
    }
}
