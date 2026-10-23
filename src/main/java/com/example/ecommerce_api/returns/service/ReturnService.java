package com.example.ecommerce_api.returns.service;

import com.example.ecommerce_api.Enum.ReturnStatus;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.returns.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ReturnService {

    // ----- customer -----

    ReturnEligibilityResponse getMyEligibility(String orderNumber);

    /** My orders a return can be requested for right now (same rules as getMyEligibility). */
    List<ReturnableOrderResponse> getMyReturnableOrders();

    ReturnResponse requestReturn(CreateReturnRequest request);

    PageResponse<ReturnResponse> getMyReturns(Pageable pageable);

    List<ReturnResponse> getMyReturnsForOrder(String orderNumber);

    ReturnResponse getMyReturn(String returnNumber);

    ReturnResponse cancelMyReturn(String returnNumber);

    // ----- admin -----

    PageResponse<ReturnResponse> getAll(ReturnStatus status, String search, Pageable pageable);

    Map<ReturnStatus, Long> countByStatus();

    ReturnResponse getByReturnNumber(String returnNumber);

    List<ReturnResponse> getForOrder(String orderNumber);

    ReturnResponse approve(String returnNumber, String note);

    ReturnResponse reject(String returnNumber, String note);

    /** Restocks sellable items, takes all returned units off the sold count and records the refund. */
    ReturnResponse receive(String returnNumber, ReceiveReturnRequest request);
}
