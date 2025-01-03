package antifraud.controller;

import antifraud.DTO.FeedbackRequestDTO;
import antifraud.DTO.IPDTO;
import antifraud.DTO.StolenCardDTO;
import antifraud.constants.Region;
import antifraud.model.IP;
import antifraud.model.StolenCard;
import antifraud.model.Transaction;
import antifraud.service.GlobalLimitsService;
import antifraud.service.IPService;
import antifraud.service.StolenCardService;
import antifraud.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/antifraud")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);
    private final IPService ipService;
    private final StolenCardService stolenCardService;
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\.){3}" +
                    "(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)$");
    private final TransactionService transactionService;
    private final GlobalLimitsService globalLimitsService;

    @Autowired
    public TransactionController(IPService ipService, StolenCardService stolenCardService, TransactionService transactionService, GlobalLimitsService globalLimitsService) {
        this.ipService = ipService;
        this.stolenCardService = stolenCardService;
        this.transactionService = transactionService;
        this.globalLimitsService = globalLimitsService;
    }

    @PostMapping("/transaction")
    public ResponseEntity<?> checkTransaction(@RequestBody Transaction transaction) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Map<String, String> response = new HashMap<>();

        if (!userDetails.isAccountNonLocked()) {
            log.debug("User account is locked for: {}", userDetails.getUsername());
            response.put("error", "User account is locked");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // Basic validation of amount, region, and date:
        if (transaction.getAmount() == null || transaction.getAmount() <= 0 ||
                transaction.getRegion() == null || transaction.getRegion().isEmpty() ||
                transaction.getDate() == null) {
            log.debug("Invalid transaction data: {}", transaction);
            response.put("error", "Invalid transaction data");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Check region is a valid enum:
        boolean regionValid = false;
        for (Region r : Region.values()) {
            if (r.name().equals(transaction.getRegion())) {
                regionValid = true;
                break;
            }
        }
        if (!regionValid) {
            log.debug("Invalid region: {}", transaction.getRegion());
            response.put("error", "Invalid region code");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        log.debug("Checking transaction: {}", transaction);

        String result = "ALLOWED";
        Set<String> reasons = new TreeSet<>();
        Date now = transaction.getDate();
        Date oneHourAgo = new Date(transaction.getDate().getTime() - 3600000);

        // Get the transaction history within the last hour
        List<Transaction> recentTransactions = transactionService.findByNumberAndDateBetween(
                transaction.getNumber(), oneHourAgo, now);

        // Check how many unique regions (besides this transaction's region) we have
        long uniqueRegions = recentTransactions.stream()
                .map(Transaction::getRegion)
                .filter(r -> !r.equals(transaction.getRegion()))
                .distinct()
                .count();

        // Check how many unique IPs (besides this transaction’s IP) we have
        long uniqueIps = recentTransactions.stream()
                .map(Transaction::getIp)
                .filter(ip -> !ip.equals(transaction.getIp()))
                .distinct()
                .count();

        // region-correlation
        if (uniqueRegions > 2) {
            result = "PROHIBITED";
            reasons.add("region-correlation");
        } else if (uniqueRegions == 2 && !"PROHIBITED".equals(result)) {
            result = "MANUAL_PROCESSING";
            reasons.add("region-correlation");
        }

        // ip-correlation
        if (uniqueIps > 2) {
            result = "PROHIBITED";
            reasons.add("ip-correlation");
        } else if (uniqueIps == 2 && !"PROHIBITED".equals(result)) {
            result = "MANUAL_PROCESSING";
            reasons.add("ip-correlation");
        }

        // stolen card
        if (stolenCardService.stolenCardExist(Long.valueOf(transaction.getNumber()))) {
            result = "PROHIBITED";
            reasons.add("card-number");
        }

        // suspicious ip
        if (ipService.isIpTaken(transaction.getIp())) {
            result = "PROHIBITED";
            reasons.add("ip");
        }

        // amount
        long allowedLimit = globalLimitsService.getAllowedLimit();
        long manualLimit  = globalLimitsService.getManualLimit();

        if (transaction.getAmount() > manualLimit) {
            result = "PROHIBITED";
            reasons.add("amount");
        } else if (transaction.getAmount() > allowedLimit && reasons.isEmpty()) {
            result = "MANUAL_PROCESSING";
            reasons.add("amount");
        }

        String info = reasons.isEmpty() ? "none" : String.join(", ", reasons);

        transaction.setResult(result);
        transactionService.saveTransaction(transaction);

        response.put("result", result);
        response.put("info", info);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/transaction")
    public ResponseEntity<?> updateTransactionFeedback(@RequestBody FeedbackRequestDTO request) {
        if (request.getTransactionId() == null || request.getFeedback() == null) {
            return ResponseEntity.badRequest().build();
        }

        String fb = request.getFeedback().trim().toUpperCase();
        if (!List.of("ALLOWED","MANUAL_PROCESSING","PROHIBITED").contains(fb)) {
            return ResponseEntity.badRequest().build();
        }

        Transaction tx = transactionService.getTransactionById(request.getTransactionId());
        if (tx == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (tx.getFeedback() != null && !tx.getFeedback().isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        try {
            globalLimitsService.applyFeedback(tx.getResult(), fb, tx.getAmount());
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().build();
        }

        tx.setFeedback(fb);
        transactionService.saveTransaction(tx);

        Map<String,Object> body = new LinkedHashMap<>();
        body.put("transactionId", tx.getId());
        body.put("amount", tx.getAmount());
        body.put("ip", tx.getIp());
        body.put("number", tx.getNumber());
        body.put("region", tx.getRegion());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateString = sdf.format(tx.getDate());
        body.put("date", dateString);
        body.put("result", tx.getResult());
        body.put("feedback", tx.getFeedback());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getAllTransactionHistory() {
        List<Transaction> allTx = transactionService.getAllTransactionsSorted();

        List<Map<String,Object>> list = new ArrayList<>();
        for (Transaction tx : allTx) {
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("transactionId", tx.getId());
            row.put("amount", tx.getAmount());
            row.put("ip", tx.getIp());
            row.put("number", tx.getNumber());
            row.put("region", tx.getRegion());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String dateString = sdf.format(tx.getDate());
            row.put("date", dateString);
            row.put("result", tx.getResult());
            row.put("feedback", tx.getFeedback() == null ? "" : tx.getFeedback());
            list.add(row);
        }

        return ResponseEntity.ok(list);
    }

    @GetMapping("/history/{number}")
    public ResponseEntity<?> getTransactionHistoryByCard(@PathVariable String number) {

        if (!isValidCardNumber(Long.valueOf(number))) {
            return ResponseEntity.badRequest().build();
        }

        List<Transaction> txList = transactionService.getTransactionsByNumberSorted(number);
        if (txList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<Map<String,Object>> list = new ArrayList<>();
        for (Transaction tx : txList) {
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("transactionId", tx.getId());
            row.put("amount", tx.getAmount());
            row.put("ip", tx.getIp());
            row.put("number", tx.getNumber());
            row.put("region", tx.getRegion());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String dateString = sdf.format(tx.getDate());
            row.put("date", dateString);
            row.put("date", dateString);
            row.put("result", tx.getResult());
            row.put("feedback", tx.getFeedback() == null ? "" : tx.getFeedback());
            list.add(row);
        }

        return ResponseEntity.ok(list);
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
