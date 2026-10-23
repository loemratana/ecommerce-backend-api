package com.example.ecommerce_api.export;

import com.example.ecommerce_api.Enum.OrderStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("api/v1/admin/exports")
@Tag(name = "export")
@RequiredArgsConstructor
public class ExportController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final OrderExcelExporter orderExcelExporter;

    @Operation(summary = "Export orders to Excel",
            description = "Orders placed from 'from' to 'to' (both inclusive, at most 366 days), optionally filtered by status. "
                    + "The file has an Orders sheet and an Items sheet.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/orders")
    public void exportOrders(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                             @RequestParam(required = false) OrderStatus status,
                             HttpServletResponse response) throws IOException {
        OrderExcelExporter.validateRange(from, to);
        String filename = "orders_" + from + "_" + to + (status != null ? "_" + status : "") + ".xlsx";
        response.setContentType(XLSX);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename).build().toString());
        orderExcelExporter.export(from, to, status, response.getOutputStream());
    }
}
