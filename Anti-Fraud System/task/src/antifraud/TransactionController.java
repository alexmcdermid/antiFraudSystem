package antifraud;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/antifraud")
public class TransactionController {

    @PostMapping("/transaction")
    public ResponseEntity<?> checkTransaction(@RequestBody Transaction transaction) {
        Map<String, String> response = new HashMap<>();

        if (transaction.getAmount() == null || transaction.getAmount() <= 0) {
            response.put("error", "Amount must be greater than 0");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        String result;
        if (transaction.getAmount() <= 200) {
            result = "ALLOWED";
        } else if (transaction.getAmount() <= 1500) {
            result = "MANUAL_PROCESSING";
        } else {
            result = "PROHIBITED";
        }

        response.put("result", result);
        return ResponseEntity.ok(response);
    }
}