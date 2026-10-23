package com.example.ecommerce_api.export;

import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.common.exception.BadRequestException;
import com.example.ecommerce_api.order.entity.Order;
import com.example.ecommerce_api.order.entity.OrderItem;
import com.example.ecommerce_api.order.repository.OrderRepository;
import com.example.ecommerce_api.settings.service.SettingsService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Writes orders placed between two dates to an .xlsx with two sheets: one row per order
 * ("Orders") and one row per ordered item ("Items").
 */
@Service
@RequiredArgsConstructor
public class OrderExcelExporter {

    private static final int BATCH_SIZE = 500;
    private static final int MAX_RANGE_DAYS = 366;

    private static final String[] ORDER_HEADERS = {"Order #", "Date", "Customer", "Email", "Status",
            "Items", "Subtotal", "Discount", "Tax", "Shipping", "Total", "Refunded", "Ship To"};
    private static final String[] ITEM_HEADERS = {"Order #", "Date", "Status", "Product", "SKU", "Color",
            "Size", "Quantity", "Unit Price", "Price Paid", "Line Total"};

    private final OrderRepository orderRepository;
    private final EntityManager entityManager;
    private final SettingsService settingsService;

    /**
     * {@code from} and {@code to} are both inclusive days on the store's clock (Settings / Localization),
     * and dates in the file are shown in that timezone too; a null status exports every status.
     */
    @Transactional(readOnly = true)
    public void export(LocalDate from, LocalDate to, OrderStatus status, OutputStream out) throws IOException {
        validateRange(from, to);

        // SXSSF keeps only 100 rows in memory and flushes the rest to a temp file
        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            Styles styles = new Styles(wb);
            Sheet orders = createSheet(wb, "Orders", ORDER_HEADERS, styles);
            Sheet items = createSheet(wb, "Items", ITEM_HEADERS, styles);

            int orderRow = 1, itemRow = 1, page = 0;
            Slice<Order> batch;
            do {
                batch = orderRepository.findForExport(settingsService.toServerTime(from.atStartOfDay()),
                        settingsService.toServerTime(to.plusDays(1).atStartOfDay()), status,
                        PageRequest.of(page++, BATCH_SIZE, Sort.by("orderDate", "id")));
                for (Order o : batch) {
                    writeOrder(orders.createRow(orderRow++), o, styles);
                    for (OrderItem item : o.getOrderItems()) {
                        writeItem(items.createRow(itemRow++), o, item, styles);
                    }
                }
                // Rows are already written; drop the entities so memory doesn't grow with each batch
                entityManager.clear();
            } while (batch.hasNext());

            wb.write(out);
        } finally {
            out.flush();
        }
    }

    /** Call before writing response headers, so a bad range still gets a normal JSON error. */
    public static void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("'from' must be on or before 'to'");
        }
        if (from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw new BadRequestException("Export range cannot be longer than " + MAX_RANGE_DAYS + " days");
        }
    }

    private void writeOrder(Row row, Order o, Styles styles) {
        int c = 0;
        text(row, c++, o.getOrderNumber());
        date(row, c++, o.getOrderDate(), styles);
        text(row, c++, o.getUser().getFirstName() + " " + o.getUser().getLastName());
        text(row, c++, o.getUser().getEmail());
        text(row, c++, o.getStatus().name());
        row.createCell(c++).setCellValue(o.getOrderItems().stream().mapToInt(OrderItem::getQuantity).sum());
        money(row, c++, o.getSubtotal(), styles);
        money(row, c++, o.getDiscountAmount(), styles);
        money(row, c++, o.getTaxPrice(), styles);
        money(row, c++, o.getShippingPrice(), styles);
        money(row, c++, o.getTotalOrderPrice(), styles);
        money(row, c++, o.getRefundedAmount(), styles);
        var a = o.getShippingAddress();
        text(row, c, a == null ? null
                : String.join(", ", a.getStreet(), a.getCity(), a.getState(), a.getZipCode(), a.getCountry()));
    }

    private void writeItem(Row row, Order o, OrderItem item, Styles styles) {
        int c = 0;
        text(row, c++, o.getOrderNumber());
        date(row, c++, o.getOrderDate(), styles);
        text(row, c++, o.getStatus().name());
        text(row, c++, item.getProductName());
        text(row, c++, item.getSku());
        text(row, c++, item.getColor());
        text(row, c++, item.getSizeName());
        row.createCell(c++).setCellValue(item.getQuantity());
        money(row, c++, item.getPrice(), styles);
        money(row, c++, item.getPriceAfterDiscount(), styles);
        money(row, c, item.getLineTotal(), styles);
    }

    private static Sheet createSheet(Workbook wb, String name, String[] headers, Styles styles) {
        Sheet sheet = wb.createSheet(name);
        Row head = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = head.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.header);
            // autoSizeColumn is slow with SXSSF, so use a fixed width
            sheet.setColumnWidth(i, 18 * 256);
        }
        sheet.createFreezePane(0, 1);
        return sheet;
    }

    private static void text(Row row, int col, String value) {
        if (value != null) {
            row.createCell(col).setCellValue(value);
        }
    }

    private static void money(Row row, int col, BigDecimal value, Styles styles) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? 0 : value.doubleValue());
        cell.setCellStyle(styles.money);
    }

    private void date(Row row, int col, LocalDateTime value, Styles styles) {
        if (value != null) {
            Cell cell = row.createCell(col);
            cell.setCellValue(settingsService.toStoreTime(value));
            cell.setCellStyle(styles.date);
        }
    }

    private static final class Styles {
        final CellStyle header;
        final CellStyle money;
        final CellStyle date;

        Styles(Workbook wb) {
            Font bold = wb.createFont();
            bold.setBold(true);
            header = wb.createCellStyle();
            header.setFont(bold);

            money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            date = wb.createCellStyle();
            date.setDataFormat(wb.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));
        }
    }
}
