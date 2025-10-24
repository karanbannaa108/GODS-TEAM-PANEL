package com.example.godsteam.controller;

import com.example.godsteam.model.SessionInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final ConcurrentHashMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${phonepe.salt:}")
    private String phonepeSalt;

    @Value("${merchant.id:}")
    private String merchantId;

    @PostConstruct
    public void init(){
        System.out.println("Merchant ID: " + (merchantId.isEmpty() ? "(not set)" : "present"));
        System.out.println("PHONEPE_SALT present: " + (!phonepeSalt.isEmpty()));
    }

    @PostMapping("/create-session")
    public Map<String, String> createSession() {
        String sid = "s_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        sessions.put(sid, new SessionInfo(sid));
        return Map.of("sessionId", sid);
    }

    @GetMapping("/check-status")
    public Map<String, Object> checkStatus(@RequestParam String sessionId) {
        SessionInfo s = sessions.get(sessionId);
        if (s == null) return Map.of("paid", false);
        return Map.of("paid", s.isPaid(), "tx", s.getTx());
    }

    @PostMapping(value = "/payment-callback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> paymentCallback(@RequestBody Map<String, Object> payload,
                                               @RequestHeader(value = "X-VERIFY", required = false) String xverify) {
        try {
            if (phonepeSalt != null && !phonepeSalt.isBlank()) {
                String computed = computeChecksum(payload, phonepeSalt);
                if (xverify == null || !computed.equals(xverify)) {
                    System.err.println("Checksum mismatch. computed=" + computed + " received=" + xverify);
                    return Map.of("success", false, "message", "Invalid checksum");
                }
            } else {
                System.out.println("PHONEPE_SALT not set; skipping checksum validation.");
            }

            boolean success = Boolean.TRUE.equals(payload.get("success"));
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            if (success && data != null) {
                String paymentState = (String) data.get("paymentState");
                Map<String, Object> txContext = (Map<String, Object>) data.get("transactionContext");
                String storeId = null;
                if (txContext != null) {
                    Object sid = txContext.get("storeId");
                    if (sid != null) storeId = sid.toString();
                }

                if ("COMPLETED".equalsIgnoreCase(paymentState)) {
                    if (storeId != null && sessions.containsKey(storeId)) {
                        SessionInfo si = sessions.get(storeId);
                        si.setPaid(true);
                        si.setTx(data);
                        System.out.println("Marked session paid: " + storeId);
                    } else {
                        String txid = (String) (data.get("transactionId") != null ? data.get("transactionId") : UUID.randomUUID().toString());
                        sessions.put(txid, new SessionInfo(txid));
                        sessions.get(txid).setPaid(true);
                        sessions.get(txid).setTx(data);
                        System.out.println("Stored unmatched payment under txid:" + txid);
                    }
                }
            }

            return Map.of("success", true, "code", "PAYMENT_SUCCESS", "message", "Recorded");
        } catch (Exception ex) {
            ex.printStackTrace();
            return Map.of("success", false, "message", "Server error");
        }
    }

    private String computeChecksum(Map<String, Object> payload, String salt) {
        try {
            String json = mapper.writeValueAsString(payload);
            String combined = json + salt;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
