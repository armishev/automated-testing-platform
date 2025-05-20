package ru.brk.stockmarketapi.repo;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import ru.brk.stockmarketapi.models.Currency;
import ru.brk.stockmarketapi.models.EverySecondPrices;

public interface SecondCurrencyRepo extends ReactiveCrudRepository<EverySecondPrices, Long> {
    Mono<EverySecondPrices> findByCurrencyId(Integer currencyId);
}
