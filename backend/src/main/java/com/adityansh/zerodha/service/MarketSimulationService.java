package com.adityansh.zerodha.service;

import com.adityansh.zerodha.entity.Stock;
import com.adityansh.zerodha.repository.StockRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MarketSimulationService {
    private final StockRepository stocks;

    public MarketSimulationService(StockRepository stocks) {
        this.stocks = stocks;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void moveMarket() {
        List<Stock> all = stocks.findAll();
        for (Stock stock : all) {
            double movement = ThreadLocalRandom.current().nextDouble(-0.025, 0.025);
            double oldPrice = stock.getPrice();
            double newPrice = Math.max(1.0, oldPrice * (1 + movement));
            stock.setPrice(Math.round(newPrice * 100.0) / 100.0);
            stock.setChangePercent(Math.round(movement * 10000.0) / 100.0);
        }
        stocks.saveAll(all);
    }
}
