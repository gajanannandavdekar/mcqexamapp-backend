package com.freelance.mcq.controller;

import com.freelance.mcq.dto.CreateOrderRequest;
import com.freelance.mcq.dto.VerifyPaymentRequest;
import com.freelance.mcq.entity.PaymentOrder;
import com.freelance.mcq.entity.PremiumPlan;
import com.freelance.mcq.entity.User;
import com.freelance.mcq.repository.PaymentOrderRepository;
import com.freelance.mcq.repository.PremiumPlanRepository;
import com.freelance.mcq.repository.UserRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PremiumPlanRepository premiumPlanRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final UserRepository userRepository;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    public PaymentController(PremiumPlanRepository premiumPlanRepository, PaymentOrderRepository paymentOrderRepository, UserRepository userRepository) {
        this.premiumPlanRepository = premiumPlanRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/premium-plans")
    public List<Map<String, Object>> listPlans() {
        return premiumPlanRepository.findByIsActiveTrue().stream()
                .map(p -> Map.<String, Object>of(
                        "planKey", p.getPlanKey(),
                        "title", p.getTitle(),
                        "durationDays", p.getDurationDays(),
                        "priceInRupees", p.getPriceInPaise() / 100.0
                ))
                .toList();
    }

    @PostMapping("/payments/create-order")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createOrder(Authentication auth, @RequestBody CreateOrderRequest req) {
        User user = (User) auth.getPrincipal();
        PremiumPlan plan = premiumPlanRepository.findByIsActiveTrue().stream()
                .filter(p -> p.getPlanKey().equals(req.planKey()))
                .findFirst().orElse(null);

        if (plan == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Plan not found"));
        }

        try {
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", plan.getPriceInPaise());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_" + System.currentTimeMillis());

            com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);

            PaymentOrder order = new PaymentOrder();
            order.setUser(userRepository.findById(user.getId()).orElseThrow());
            order.setPlan(plan);
            order.setRazorpayOrderId(razorpayOrder.get("id"));
            order.setAmountInPaise(plan.getPriceInPaise());
            paymentOrderRepository.save(order);

            return ResponseEntity.ok(Map.of(
                    "razorpayOrderId", razorpayOrder.get("id"),
                    "amountInPaise", plan.getPriceInPaise(),
                    "keyId", keyId,
                    "currency", "INR"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Could not create order: " + e.getMessage()));
        }
    }

    @PostMapping("/payments/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> verifyPayment(Authentication auth, @RequestBody VerifyPaymentRequest req) {
        User authUser = (User) auth.getPrincipal();

        PaymentOrder order = paymentOrderRepository.findByRazorpayOrderId(req.razorpayOrderId()).orElse(null);
        if (order == null || !order.getUser().getId().equals(authUser.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Order not found"));
        }

        if ("PAID".equals(order.getStatus())) {
            return ResponseEntity.ok(Map.of("message", "Payment already verified"));
        }

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", req.razorpayOrderId());
            options.put("razorpay_payment_id", req.razorpayPaymentId());
            options.put("razorpay_signature", req.razorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);
            if (!isValid) {
                order.setStatus("SIGNATURE_INVALID");
                paymentOrderRepository.save(order);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Payment verification failed"));
            }

            order.setStatus("PAID");
            order.setRazorpayPaymentId(req.razorpayPaymentId());
            order.setCompletedAt(OffsetDateTime.now());
            paymentOrderRepository.save(order);

            User user = userRepository.findById(order.getUser().getId()).orElseThrow();
            OffsetDateTime base = (user.getPremiumExpiresAt() != null && user.getPremiumExpiresAt().isAfter(OffsetDateTime.now()))
                    ? user.getPremiumExpiresAt() : OffsetDateTime.now();
            user.setPremium(true);
            user.setPremiumExpiresAt(base.plusDays(order.getPlan().getDurationDays()));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "message", "Payment verified, premium activated",
                    "premiumExpiresAt", user.getPremiumExpiresAt().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Verification error: " + e.getMessage()));
        }
    }
}