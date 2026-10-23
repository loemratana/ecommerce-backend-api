package com.example.ecommerce_api.invoice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/orders")
@Tag(name = "invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Operation(summary = "Download the invoice PDF for one of my orders", description = "Only for paid orders")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @GetMapping("/me/{orderNumber}/invoice")
    public ResponseEntity<byte[]> getMyInvoice(@PathVariable String orderNumber) {
        return pdf(orderNumber, invoiceService.getMyInvoice(orderNumber));
    }

    @Operation(summary = "Download the invoice PDF for any order", description = "Only for paid orders")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{orderNumber}/invoice")
    public ResponseEntity<byte[]> getInvoice(@PathVariable String orderNumber) {
        return pdf(orderNumber, invoiceService.getInvoice(orderNumber));
    }

    private static ResponseEntity<byte[]> pdf(String orderNumber, byte[] body) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("invoice-" + orderNumber + ".pdf").build().toString())
                .body(body);
    }
}
