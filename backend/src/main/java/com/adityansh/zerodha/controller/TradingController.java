package com.adityansh.zerodha.controller;

import com.adityansh.zerodha.dto.HoldingResponse;
import com.adityansh.zerodha.dto.OrderResponse;
import com.adityansh.zerodha.dto.TradeRequest;
import com.adityansh.zerodha.dto.WhatIfRequest;
import com.adityansh.zerodha.dto.BacktestRequest;
import com.adityansh.zerodha.entity.Order;
import com.adityansh.zerodha.entity.User;
import com.adityansh.zerodha.service.PortfolioInsightService;
import com.adityansh.zerodha.service.TradingAnalyticsService;
import com.adityansh.zerodha.service.TradingService;
import com.adityansh.zerodha.service.WhatIfService;
import com.adityansh.zerodha.service.QuantLabService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TradingController {
    private final TradingService tradingService;
    private final PortfolioInsightService insightService;
    private final WhatIfService whatIfService;
    private final TradingAnalyticsService analyticsService;
    private final QuantLabService quantLabService;

    public TradingController(
            TradingService tradingService,
            PortfolioInsightService insightService,
            WhatIfService whatIfService,
            TradingAnalyticsService analyticsService,
            QuantLabService quantLabService
    ) {
        this.tradingService = tradingService;
        this.insightService = insightService;
        this.whatIfService = whatIfService;
        this.analyticsService = analyticsService;
        this.quantLabService = quantLabService;
    }

    @GetMapping("/users/{id}/portfolio")
    public Map<String, Object> portfolio(@PathVariable Long id, Authentication auth) {
        authorize(id, auth);
        User user = tradingService.user(id);
        List<HoldingResponse> holdings = tradingService.holdings(id).stream()
                .map(h -> new HoldingResponse(
                        h.getId(), h.getStock().getSymbol(), h.getStock().getCompany(),
                        h.getQuantity(), h.getAveragePrice(), h.getStock().getPrice()))
                .toList();
        return Map.of("cashBalance", user.getCashBalance(), "holdings", holdings);
    }

    @GetMapping("/users/{id}/orders")
    public List<OrderResponse> orders(@PathVariable Long id, Authentication auth) {
        authorize(id, auth);
        return tradingService.orders(id).stream().map(this::toOrderResponse).toList();
    }

    @GetMapping("/users/{id}/insights")
    public Map<String, Object> insights(@PathVariable Long id, Authentication auth) {
        authorize(id, auth);
        return insightService.generateInsights(id);
    }

    @GetMapping("/users/{id}/analytics")
    public Map<String, Object> analytics(@PathVariable Long id, Authentication auth) {
        authorize(id, auth);
        return analyticsService.analytics(id);
    }

    @PostMapping("/users/{id}/what-if")
    public Map<String, Object> whatIf(
            @PathVariable Long id,
            @Valid @RequestBody WhatIfRequest request,
            Authentication auth
    ) {
        authorize(id, auth);
        return whatIfService.simulate(id, request.symbol(), request.percentChange());
    }

    @PostMapping("/users/{id}/quant/backtest")
    public Map<String, Object> backtest(
            @PathVariable Long id,
            @Valid @RequestBody BacktestRequest request,
            Authentication auth
    ) {
        authorize(id, auth);
        return quantLabService.backtest(
                request.symbol(), request.strategy(),
                request.initialCapital(), request.lookbackDays());
    }

    @PostMapping("/users/{id}/buy")
    public OrderResponse buy(@PathVariable Long id, @Valid @RequestBody TradeRequest request, Authentication auth) {
        authorize(id, auth);
        return toOrderResponse(tradingService.trade(id, request, Order.Type.BUY));
    }

    @PostMapping("/users/{id}/sell")
    public OrderResponse sell(@PathVariable Long id, @Valid @RequestBody TradeRequest request, Authentication auth) {
        authorize(id, auth);
        return toOrderResponse(tradingService.trade(id, request, Order.Type.SELL));
    }

    private void authorize(Long id, Authentication auth) {
        User user = tradingService.user(id);
        if (auth == null || !user.getUsername().equals(auth.getName())) {
            throw new RuntimeException("You are not authorized to access this account");
        }
    }

    private OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(
                order.getId(), order.getStock().getSymbol(), order.getStock().getCompany(),
                order.getType().name(), order.getQuantity(), order.getPrice(),
                order.getQuantity() * order.getPrice(), order.getCreatedAt());
    }
}
