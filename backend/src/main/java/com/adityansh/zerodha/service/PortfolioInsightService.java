package com.adityansh.zerodha.service;

import com.adityansh.zerodha.entity.Holding;
import com.adityansh.zerodha.entity.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PortfolioInsightService {

    private final TradingService tradingService;

    public PortfolioInsightService(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    public Map<String, Object> generateInsights(Long userId) {
        User user = tradingService.user(userId);
        List<Holding> holdings = tradingService.holdings(userId);

        double portfolioValue = 0.0;
        Map<String, Double> sectorValues = new LinkedHashMap<>();

        Map<String, Object> bestHolding = null;
        Map<String, Object> worstHolding = null;
        double bestPnl = Double.NEGATIVE_INFINITY;
        double worstPnl = Double.POSITIVE_INFINITY;

        for (Holding holding : holdings) {
            double currentValue =
                    holding.getStock().getPrice() * holding.getQuantity();

            double investedValue =
                    holding.getAveragePrice() * holding.getQuantity();

            double pnl = currentValue - investedValue;

            portfolioValue += currentValue;

            String sector = holding.getStock().getSector();
            sectorValues.put(
                    sector,
                    sectorValues.getOrDefault(sector, 0.0) + currentValue
            );

            if (pnl > bestPnl) {
                bestPnl = pnl;
                bestHolding = holdingData(holding, pnl);
            }

            if (pnl < worstPnl) {
                worstPnl = pnl;
                worstHolding = holdingData(holding, pnl);
            }
        }

        double totalCapital = user.getCashBalance() + portfolioValue;

        double cashPercentage = totalCapital == 0
                ? 0
                : (user.getCashBalance() / totalCapital) * 100;

        double diversificationScore =
                calculateDiversificationScore(sectorValues, portfolioValue);

        String riskMessage =
                generateRiskMessage(sectorValues, portfolioValue);

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("diversificationScore", round(diversificationScore));
        result.put("cashPercentage", round(cashPercentage));
        result.put("portfolioValue", round(portfolioValue));
        result.put("totalCapital", round(totalCapital));
        result.put("sectorAllocation",
                buildSectorAllocation(sectorValues, portfolioValue));
        result.put("bestHolding", bestHolding);
        result.put("worstHolding", worstHolding);
        result.put("riskMessage", riskMessage);

        return result;
    }

    private double calculateDiversificationScore(
            Map<String, Double> sectors,
            double totalValue
    ) {
        if (totalValue == 0 || sectors.isEmpty()) {
            return 0;
        }

        double concentration = 0;

        for (double value : sectors.values()) {
            double weight = value / totalValue;
            concentration += weight * weight;
        }

        // HHI-style concentration translated to a simple 0-100 score.
        return Math.max(0, Math.min(100, (1 - concentration) * 100));
    }

    private List<Map<String, Object>> buildSectorAllocation(
            Map<String, Double> sectors,
            double totalValue
    ) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (totalValue == 0) {
            return result;
        }

        for (Map.Entry<String, Double> entry : sectors.entrySet()) {
            Map<String, Object> sector = new LinkedHashMap<>();
            sector.put("sector", entry.getKey());
            sector.put("percentage",
                    round((entry.getValue() / totalValue) * 100));
            result.add(sector);
        }

        result.sort((a, b) ->
                Double.compare(
                        ((Number) b.get("percentage")).doubleValue(),
                        ((Number) a.get("percentage")).doubleValue()
                )
        );

        return result;
    }

    private Map<String, Object> holdingData(
            Holding holding,
            double pnl
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("symbol", holding.getStock().getSymbol());
        data.put("company", holding.getStock().getCompany());
        data.put("pnl", round(pnl));
        return data;
    }

    private String generateRiskMessage(
            Map<String, Double> sectors,
            double totalValue
    ) {
        if (totalValue == 0 || sectors.isEmpty()) {
            return "Build your portfolio to receive diversification insights.";
        }

        String largestSector = null;
        double largestPercentage = 0;

        for (Map.Entry<String, Double> entry : sectors.entrySet()) {
            double percentage =
                    (entry.getValue() / totalValue) * 100;

            if (percentage > largestPercentage) {
                largestPercentage = percentage;
                largestSector = entry.getKey();
            }
        }

        if (largestPercentage >= 50) {
            return "High concentration: "
                    + largestSector
                    + " represents "
                    + Math.round(largestPercentage)
                    + "% of your portfolio.";
        }

        if (largestPercentage >= 35) {
            return "Moderate concentration: "
                    + largestSector
                    + " represents "
                    + Math.round(largestPercentage)
                    + "% of your portfolio.";
        }

        return "Your portfolio has a relatively balanced sector allocation.";
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
