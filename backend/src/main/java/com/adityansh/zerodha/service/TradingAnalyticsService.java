package com.adityansh.zerodha.service;

import com.adityansh.zerodha.entity.Order;
import com.adityansh.zerodha.entity.User;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TradingAnalyticsService {
    private final TradingService tradingService;

    public TradingAnalyticsService(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    public Map<String, Object> analytics(Long userId) {
        User user = tradingService.user(userId);
        List<Order> orders = tradingService.orders(userId)
                .stream()
                .sorted(Comparator.comparing(Order::getCreatedAt))
                .toList();

        int buys = 0;
        int sells = 0;
        int totalShares = 0;
        double buyVolume = 0;
        double sellVolume = 0;
        double realizedPnl = 0;

        Map<String, Integer> tradeCountBySymbol = new LinkedHashMap<>();
        Map<String, Integer> runningQty = new LinkedHashMap<>();
        Map<String, Double> runningAvg = new LinkedHashMap<>();

        for (Order order : orders) {
            String symbol = order.getStock().getSymbol();
            int qty = order.getQuantity();
            double value = qty * order.getPrice();

            tradeCountBySymbol.merge(symbol, qty, Integer::sum);
            totalShares += order.getType() == Order.Type.BUY ? qty : -qty;

            if (order.getType() == Order.Type.BUY) {
                buys++;
                buyVolume += value;

                int oldQty = runningQty.getOrDefault(symbol, 0);
                double oldAvg = runningAvg.getOrDefault(symbol, 0.0);
                double newAvg = oldQty + qty == 0
                        ? 0
                        : ((oldAvg * oldQty) + value) / (oldQty + qty);
                runningQty.put(symbol, oldQty + qty);
                runningAvg.put(symbol, newAvg);
            } else {
                sells++;
                sellVolume += value;

                double avg = runningAvg.getOrDefault(symbol, 0.0);
                realizedPnl += (order.getPrice() - avg) * qty;
                int newQty = runningQty.getOrDefault(symbol, 0) - qty;
                runningQty.put(symbol, Math.max(0, newQty));
            }
        }

        String mostTraded = tradeCountBySymbol.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("—");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalOrders", orders.size());
        result.put("buyOrders", buys);
        result.put("sellOrders", sells);
        result.put("buyVolume", round(buyVolume));
        result.put("sellVolume", round(sellVolume));
        result.put("netCashFlow", round(sellVolume - buyVolume));
        result.put("realizedPnl", round(realizedPnl));
        result.put("openShareCount", totalShares);
        result.put("mostTraded", mostTraded);
        result.put("currentCash", round(user.getCashBalance()));
        return result;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
