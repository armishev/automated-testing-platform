package ru.brk.stockmarketapi.repo;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.brk.stockmarketapi.models.DailyPrices;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface DailyPricesRepo extends ReactiveCrudRepository<DailyPrices, Integer> {


    Mono<DailyPrices> findByDailyAndCurrencyId(LocalDate daily, Integer CurrencyId);
}
