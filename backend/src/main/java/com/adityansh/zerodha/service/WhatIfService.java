package com.adityansh.zerodha.service;

import com.adityansh.zerodha.entity.Holding;
import com.adityansh.zerodha.entity.Stock;
import com.adityansh.zerodha.entity.User;
import com.adityansh.zerodha.repository.StockRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WhatIfService {
    private final TradingService tradingService;
    private final StockRepository stocks;

    public WhatIfService(TradingService tradingService, StockRepository stocks) {
        this.tradingService = tradingService;
        this.stocks = stocks;
    }

    public Map<String, Object> simulate(Long userId, String symbol, double percentChange) {
        if (percentChange < -100) {
            throw new RuntimeException("Price change cannot be below -100%");
        }

        User user = tradingService.user(userId);
        Stock stock = stocks.findBySymbol(symbol.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Stock not found"));

        List<Holding> holdings = tradingService.holdings(userId);

        double currentPortfolioValue = 0;
        double projectedPortfolioValue = 0;
        double currentPnl = 0;
        double projectedPnl = 0;
        int affectedQuantity = 0;

        for (Holding holding : holdings) {
            double currentPrice = holding.getStock().getPrice();
            double projectedPrice = currentPrice;

            if (holding.getStock().getSymbol().equalsIgnoreCase(stock.getSymbol())) {
                projectedPrice = currentPrice * (1 + percentChange / 100.0);
                affectedQuantity = holding.getQuantity();
            }

            double invested = holding.getAveragePrice() * holding.getQuantity();
            currentPortfolioValue += currentPrice * holding.getQuantity();
            projectedPortfolioValue += projectedPrice * holding.getQuantity();
            currentPnl += currentPrice * holding.getQuantity() - invested;
            projectedPnl += projectedPrice * holding.getQuantity() - invested;
        }

        double currentTotalWealth = user.getCashBalance() + currentPortfolioValue;
        double projectedTotalWealth = user.getCashBalance() + projectedPortfolioValue;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("symbol", stock.getSymbol());
        result.put("company", stock.getCompany());
        result.put("percentChange", round(percentChange));
        result.put("currentPrice", round(stock.getPrice()));
        result.put("projectedPrice", round(stock.getPrice() * (1 + percentChange / 100.0)));
        result.put("affectedQuantity", affectedQuantity);
        result.put("currentPortfolioValue", round(currentPortfolioValue));
        result.put("projectedPortfolioValue", round(projectedPortfolioValue));
        result.put("portfolioImpact", round(projectedPortfolioValue - currentPortfolioValue));
        result.put("currentPnl", round(currentPnl));
        result.put("projectedPnl", round(projectedPnl));
        result.put("wealthImpact", round(projectedTotalWealth - currentTotalWealth));
        return result;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
