package com.example.ecommerce_api.payment.controller;

import com.example.ecommerce_api.payment.service.AbaPayWayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;

/**
 * Public endpoints for ABA PayWay: the customer's browser opens the checkout page, and PayWay calls
 * the callback. Neither is trusted on its own; see AbaPayWayService.
 */
@RestController
@RequestMapping("api/v1/payments/aba-payway")
@RequiredArgsConstructor
@Tag(name = "payment")
@ConditionalOnExpression("!'${app.aba-payway.merchant-id:}'.isBlank() and !'${app.aba-payway.public-key:}'.isBlank()")
public class AbaPayWayController {

    private final AbaPayWayService abaPayWayService;

    @Operation(summary = "ABA PayWay checkout page",
            description = "The redirectUrl of an ABA PayWay payment: posts the signed purchase form to PayWay's hosted checkout.")
    @GetMapping(value = "/checkout/{paymentId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> checkout(@PathVariable Long paymentId, @RequestParam(required = false) String sig) {
        return abaPayWayService.checkoutPage(paymentId, sig)
                .map(ResponseEntity::ok)
                .orElseGet(() -> messagePage(404, "This payment link is invalid or has expired. Please go back to your order and try again."));
    }

    @Operation(summary = "ABA PayWay callback (return_url)",
            description = "PayWay posts here after a payment; the result is re-checked with PayWay before it is applied.")
    @PostMapping("/callback")
    public ResponseEntity<Void> callback(@RequestBody(required = false) Map<String, Object> body,
                                         @RequestParam(name = "tran_id", required = false) String tranIdParam) {
        Object tranId = body != null && body.get("tran_id") != null ? body.get("tran_id") : tranIdParam;
        if (tranId != null) {
            abaPayWayService.handleCallback(String.valueOf(tranId));
        }
        return ResponseEntity.ok().build();
    }

    private static ResponseEntity<String> messagePage(int status, String message) {
        return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML).body("""
                <!doctype html><html lang="en"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1"><title>ABA PayWay</title></head>
                <body style="font-family: system-ui, sans-serif; text-align: center; padding: 48px 16px;"><p>%s</p></body></html>
                """.formatted(HtmlUtils.htmlEscape(message)));
    }
}
