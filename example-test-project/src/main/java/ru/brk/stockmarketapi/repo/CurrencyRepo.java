package ru.brk.stockmarketapi.repo;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import ru.brk.stockmarketapi.models.Currency;
import ru.brk.stockmarketapi.models.DailyPrices;
import ru.brk.stockmarketapi.models.EverySecondPrices;

import java.time.LocalDate;
import java.util.List;

public interface CurrencyRepo extends ReactiveCrudRepository<Currency, Long> {

    @Query("""
INSERT INTO every_second_prices (currency_id, price)
SELECT * FROM unnest(:currencyIds::int[], :prices::float8[])
ON CONFLICT (currency_id)
DO UPDATE SET price = EXCLUDED.price
""")
    Mono<Void> batchInsertOrUpdate(@Param("currencyIds") Integer[] currencyIds, @Param("prices") Double[] prices);




    @Modifying
    @Query("""
INSERT INTO daily_prices (currency_id, price, daily)
SELECT * FROM unnest(:currencyIds::integer[], :prices::double precision[], :dates::date[])
ON CONFLICT (currency_id, daily)
DO UPDATE SET price = EXCLUDED.price
""")
    Mono<Void> batchInsertOrUpdateDaily(@Param("currencyIds") Integer[] currencyIds,
                                        @Param("prices") Double[] prices,
                                        @Param("dates") LocalDate[] dates);

}
