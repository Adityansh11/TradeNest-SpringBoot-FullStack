package com.adityansh.zerodha.service;

import com.adityansh.zerodha.entity.Stock;
import com.adityansh.zerodha.repository.StockRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class QuantLabService {
    private final StockRepository stockRepository;

    public QuantLabService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public Map<String, Object> backtest(String symbol, String strategy, double initialCapital, int lookbackDays) {
        Stock stock = stockRepository.findBySymbol(symbol.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Stock not found"));

        String normalized = strategy.toUpperCase();
        if (!List.of("BUY_HOLD", "SMA_CROSSOVER", "MOMENTUM").contains(normalized)) {
            throw new RuntimeException("Unknown strategy");
        }
        if (initialCapital < 1000 || lookbackDays < 30 || lookbackDays > 252) {
            throw new RuntimeException("Invalid backtest parameters");
        }

        List<Double> prices = generateHistory(stock.getPrice(), stock.getSymbol(), lookbackDays);
        List<Double> equity = new ArrayList<>();
        double cash = initialCapital;
        double shares = 0;
        int trades = 0;
        int winningTrades = 0;
        double lastEntry = 0;

        for (int i = 0; i < prices.size(); i++) {
            double price = prices.get(i);
            boolean invested = shouldHold(normalized, prices, i);

            if (invested && shares == 0) {
                shares = cash / price;
                cash = 0;
                trades++;
                lastEntry = price;
            } else if (!invested && shares > 0) {
                cash += shares * price;
                if (price > lastEntry) winningTrades++;
                shares = 0;
                trades++;
            }

            equity.add(cash + shares * price);
        }

        double finalPrice = prices.get(prices.size() - 1);
        double finalValue = cash + shares * finalPrice;
        if (shares > 0 && finalPrice > lastEntry) winningTrades++;

        double totalReturn = ((finalValue / initialCapital) - 1) * 100;
        double maxDrawdown = maxDrawdown(equity);
        double volatility = annualizedVolatility(equity);
        double sharpe = sharpeRatio(equity);
        double benchmark = ((prices.get(prices.size() - 1) / prices.get(0)) - 1) * 100;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("symbol", stock.getSymbol());
        result.put("company", stock.getCompany());
        result.put("strategy", normalized);
        result.put("lookbackDays", lookbackDays);
        result.put("initialCapital", round(initialCapital));
        result.put("finalValue", round(finalValue));
        result.put("totalReturn", round(totalReturn));
        result.put("benchmarkReturn", round(benchmark));
        result.put("maxDrawdown", round(maxDrawdown));
        result.put("annualizedVolatility", round(volatility));
        result.put("sharpeRatio", round(sharpe));
        result.put("trades", trades);
        result.put("winRate", round(trades == 0 ? 0 : (winningTrades * 100.0 / Math.max(1, trades / 2))));
        result.put("equityCurve", downsample(equity, 48));
        return result;
    }

    private boolean shouldHold(String strategy, List<Double> prices, int i) {
        return switch (strategy) {
            case "BUY_HOLD" -> true;
            case "MOMENTUM" -> i >= 10 && prices.get(i) > prices.get(i - 10);
            case "SMA_CROSSOVER" -> {
                if (i < 20) yield false;
                double shortSma = average(prices, i - 4, i);
                double longSma = average(prices, i - 19, i);
                yield shortSma > longSma;
            }
            default -> false;
        };
    }

    private List<Double> generateHistory(double currentPrice, String symbol, int days) {
        Random random = new Random(0x9E3779B9L ^ symbol.hashCode());
        List<Double> prices = new ArrayList<>();
        double price = currentPrice * (0.92 + random.nextDouble() * 0.08);

        for (int i = 0; i < days; i++) {
            double regimeDrift = (i < days / 3 ? 0.0005 : i < 2 * days / 3 ? -0.0002 : 0.0007);
            double shock = random.nextGaussian() * 0.012;
            price = Math.max(1, price * (1 + regimeDrift + shock));
            prices.add(price);
        }
        return prices;
    }

    private double average(List<Double> values, int from, int to) {
        double sum = 0;
        for (int i = from; i <= to; i++) sum += values.get(i);
        return sum / (to - from + 1);
    }

    private double maxDrawdown(List<Double> equity) {
        double peak = equity.get(0);
        double max = 0;
        for (double value : equity) {
            peak = Math.max(peak, value);
            if (peak > 0) max = Math.max(max, (peak - value) / peak * 100);
        }
        return max;
    }

    private double annualizedVolatility(List<Double> equity) {
        if (equity.size() < 2) return 0;
        double mean = 0;
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < equity.size(); i++) {
            double r = equity.get(i - 1) == 0 ? 0 : equity.get(i) / equity.get(i - 1) - 1;
            returns.add(r);
            mean += r;
        }
        mean /= returns.size();
        double variance = 0;
        for (double r : returns) variance += Math.pow(r - mean, 2);
        variance /= returns.size();
        return Math.sqrt(variance) * Math.sqrt(252) * 100;
    }

    private double sharpeRatio(List<Double> equity) {
        if (equity.size() < 2) return 0;
        double mean = 0;
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < equity.size(); i++) {
            double r = equity.get(i - 1) == 0 ? 0 : equity.get(i) / equity.get(i - 1) - 1;
            returns.add(r);
            mean += r;
        }
        mean /= returns.size();
        double variance = 0;
        for (double r : returns) variance += Math.pow(r - mean, 2);
        double std = Math.sqrt(variance / returns.size());
        return std == 0 ? 0 : (mean / std) * Math.sqrt(252);
    }

    private List<Double> downsample(List<Double> values, int maxPoints) {
        if (values.size() <= maxPoints) return values;
        List<Double> result = new ArrayList<>();
        double step = (values.size() - 1.0) / (maxPoints - 1);
        for (int i = 0; i < maxPoints; i++) result.add(round(values.get((int) Math.round(i * step))));
        return result;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
