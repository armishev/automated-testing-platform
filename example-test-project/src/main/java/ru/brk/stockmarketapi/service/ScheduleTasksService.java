package ru.brk.stockmarketapi.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.brk.stockmarketapi.dto.CurrencyDto;
import ru.brk.stockmarketapi.models.DailyPrices;
import ru.brk.stockmarketapi.models.EverySecondPrices;
import ru.brk.stockmarketapi.repo.CurrencyRepo;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleTasksService {
    private final CurrencyRepo currencyRepo;
    private static final WebClient webClient = WebClient.builder().build();

    @Scheduled(fixedRate = 5000)
    public void getCurrentPriceAndSave() {
        log.info("start getCurrentPriceAndSave()");

        currencyRepo.findAll()
                .filter(currency -> !"BRK".equalsIgnoreCase(currency.getName()))
                .flatMap(currency ->
                        webClient.get()
                                .uri(buildBinanceUri(currency.getName()))
                                .retrieve()
                                .bodyToMono(CurrencyDto.class)
                                .map(dto -> new EverySecondPrices(currency.getCurrencyId(), dto.getPrice()))
                                .onErrorResume(e -> {
                                    log.error("Error fetching price for {}: {}", currency.getName(), e.getMessage());
                                    return Mono.empty();
                                })
                )
                .collectList()
                .flatMap(pricesList -> {
                    if (pricesList.isEmpty()) {
                        return Mono.empty();
                    }
                    Integer[] currencyIds = pricesList.stream()
                            .map(EverySecondPrices::getCurrencyId)
                            .toArray(Integer[]::new);
                    Double[] priceValues = pricesList.stream()
                            .map(EverySecondPrices::getPrice)
                            .toArray(Double[]::new);
                    return currencyRepo.batchInsertOrUpdate(currencyIds, priceValues);
                })
                .subscribe(
                        unused -> log.info("Batch update completed"),
                        error -> log.error("Batch update error: {}", error.getMessage())
                );
    }


    @Scheduled(cron = "0 0 0 * * ?")
    public void getDailyPriceAndSave() {
        final LocalDate date = LocalDate.now();
        log.info("start getDailyPriceAndSave()");

        currencyRepo.findAll()
                .filter(currency -> !"BRK".equalsIgnoreCase(currency.getName()))
                .flatMap(currency ->
                        webClient.get()
                                .uri(buildBinanceUri(currency.getName()))
                                .retrieve()
                                .bodyToMono(CurrencyDto.class)
                                .map(currencyDto -> new DailyPrices(currency.getCurrencyId(), currencyDto.getPrice(), date))
                                .onErrorResume(e -> {
                                    log.error("Error fetching price for {}: {}", currency.getName(), e.getMessage());
                                    return Mono.empty();
                                })
                )
                .collectList()
                .flatMap(dailyPrices -> {
                    if (dailyPrices.isEmpty()) {
                        return Mono.empty();
                    }
                    Integer[] currencyIds = dailyPrices.stream()
                            .map(DailyPrices::getCurrencyId)
                            .toArray(Integer[]::new);
                    Double[] priceValues = dailyPrices.stream()
                            .map(DailyPrices::getPrice)
                            .toArray(Double[]::new);
                    LocalDate[] dates = dailyPrices.stream()
                            .map(DailyPrices::getDaily)
                            .toArray(LocalDate[]::new);

                    return currencyRepo.batchInsertOrUpdateDaily(currencyIds, priceValues, dates);
                })
                .subscribe(
                        unused -> log.info("Daily batch update completed"),
                        error -> log.error("Daily batch update error: {}", error.getMessage())
                );
    }


    @PostConstruct
    public void run() {
        getDailyPriceAndSave();
        getCurrentPriceAndSave();
    }





    private String buildBinanceUri(String currencyName) {
        return "https://api.binance.com/api/v3/ticker/price?symbol=" + currencyName + "USDT";
    }

}
