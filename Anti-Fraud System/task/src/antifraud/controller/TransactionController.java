package antifraud.controller;

import antifraud.DTO.IPDTO;
import antifraud.DTO.StolenCardDTO;
import antifraud.model.IP;
import antifraud.model.StolenCard;
import antifraud.model.Transaction;
import antifraud.service.IPService;
import antifraud.service.StolenCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/antifraud")
public class TransactionController {

    private final IPService ipService;
    private final StolenCardService stolenCardService;
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\.){3}" +
                    "(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)$");

    @Autowired
    public TransactionController(IPService ipService, StolenCardService stolenCardService) {
        this.ipService = ipService;
        this.stolenCardService = stolenCardService;
    }

    @PostMapping("/transaction")
    public ResponseEntity<?> checkTransaction(@RequestBody Transaction transaction) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Map<String, String> response = new HashMap<>();

        if (!userDetails.isAccountNonLocked()) {
            response.put("error", "User account is locked");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        if (transaction.getAmount() == null || transaction.getAmount() <= 0) {
            response.put("error", "Amount must be greater than 0");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        String result = "ALLOWED";
        Set<String> reasons = new TreeSet<>();

        if (stolenCardService.stolenCardExist(Long.valueOf(transaction.getNumber()))) {
            result = "PROHIBITED";
            reasons.add("card-number");
        }

        if (ipService.isIpTaken(transaction.getIp())) {
            result = "PROHIBITED";
            reasons.add("ip");
        }

        if (transaction.getAmount() > 1500) {
            result = "PROHIBITED";
            reasons.add("amount");
        } else if (transaction.getAmount() > 200 && reasons.isEmpty()) {
            result = "MANUAL_PROCESSING";
            reasons.add("amount");
        }

        String info = reasons.isEmpty() ? "none" : String.join(", ", reasons);

        response.put("result", result);
        response.put("info", info);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/suspicious-ip")
    public ResponseEntity<?> checkSuspiciousIp(@RequestBody IP ip) {
        Map<String, String> response = new HashMap<>();
        if (ip == null || ip.getIp() == null || ip.getIp().isEmpty()) {
            response.put("error", "IP is null");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (!isValidIpv4(ip.getIp())) {
            response.put("error", "Invalid IP format");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (ipService.isIpTaken(ip.getIp())) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        IP savedIP = ipService.saveIP(ip);
        IPDTO ipDTO = new IPDTO(savedIP.getId(), savedIP.getIp());

        return new ResponseEntity<>(ipDTO, HttpStatus.OK);
    }

    @PostMapping("stolencard")
    public ResponseEntity<?> checkStolencard(@RequestBody StolenCard stolenCard) {
        Map<String, String> response = new HashMap<>();
        if (stolenCard == null || stolenCard.getNumber() == null) {
            response.put("error", "stolen card is null");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (!isValidCardNumber(stolenCard.getNumber())) {
            response.put("error", "Invalid card number format");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (stolenCardService.stolenCardExist(stolenCard.getNumber())) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        StolenCard savedCard = stolenCardService.saveStolenCard(stolenCard);
        StolenCardDTO stolenCardDTO = new StolenCardDTO(savedCard.getId(), savedCard.getNumber());

        return new ResponseEntity<>(stolenCardDTO, HttpStatus.OK);
    }

    @GetMapping("/suspicious-ip")
    public ResponseEntity<?> getSuspiciousIps() {
        List<IP> ips = ipService.getAllIPs();
        List<IPDTO> ipDTOs = ips.stream()
                .map(ip -> new IPDTO(ip.getId(), ip.getIp()))
                .toList();
        return ResponseEntity.ok(ipDTOs);
    }

    @GetMapping("/stolencard")
    public ResponseEntity<?> getStolencards() {
        List<StolenCard> cards = stolenCardService.getAllStolenCards();
        List<StolenCardDTO> stolenCardDTOs = cards.stream()
                .map(card -> new StolenCardDTO(card.getId(), card.getNumber()))
                .toList();
        return ResponseEntity.ok(stolenCardDTOs);
    }

    @DeleteMapping("/suspicious-ip/{ip}")
    public ResponseEntity<?> deleteSuspiciousIps(@PathVariable String ip) {
        Map<String, String> responseMap = new HashMap<>();

        if (!isValidIpv4(ip)) {
            responseMap.put("error", "Invalid IP format");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseMap);
        }

        IP ipEntry = ipService.findByIp(ip);
        if (ipEntry == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        ipService.deleteIP(ipEntry);

        responseMap.put("status", "IP " + ipEntry.getIp() + " successfully removed!");

        return ResponseEntity.ok().body(responseMap);
    }

    @DeleteMapping("/stolencard/{number}")
    public ResponseEntity<?> deleteStolencard(@PathVariable String number) {
        Map<String, String> responseMap = new HashMap<>();

        if (!isValidCardNumber(Long.parseLong(number))) {
            responseMap.put("error", "Invalid card number format");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseMap);
        }

        StolenCard stolenCard = stolenCardService.findStolenCardById(Long.parseLong(number));
        if (stolenCard == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        stolenCardService.deleteStolenCard(stolenCard);

        responseMap.put("status", "Card " + stolenCard.getNumber() + " successfully removed!");

        return ResponseEntity.ok().body(responseMap);
    }

    private boolean isValidIpv4(String ip) {
        return IPV4_PATTERN.matcher(ip).matches();
    }

    private boolean isValidCardNumber(Long cardNumber) {
        String cardNumberStr = Long.toString(cardNumber);

        if (cardNumberStr.length() != 16) {
            return false;
        }

        int totalSum = 0;

        for (int i = 0; i < cardNumberStr.length(); i++) {
            int digit = Character.getNumericValue(cardNumberStr.charAt(cardNumberStr.length() - 1 - i));

            if (i % 2 == 1) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            totalSum += digit;
        }

        return totalSum % 10 == 0;
    }
}
