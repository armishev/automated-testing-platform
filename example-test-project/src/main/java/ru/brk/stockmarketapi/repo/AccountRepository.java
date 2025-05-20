package ru.brk.stockmarketapi.repo;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.brk.stockmarketapi.dto.AccountPriceDto;
import ru.brk.stockmarketapi.models.Account;

import java.math.BigDecimal;

public interface AccountRepository extends ReactiveCrudRepository<Account, Long> {

    Mono<Account> findByCurrencyIdAndChatId(Integer currencyId, Long userId);

    @Modifying
    @Query("""
            update account set balance = :value where currency_id = :currencyId and chat_id = :userId
            """)
    Mono<Void> updateUserValue(BigDecimal value, Integer currencyId, Long userId);

@Query("""
        SELECT a.currency_id AS currencyId,  e.price AS price
        FROM account a
        Left JOIN every_second_prices e ON a.currency_id = e.currency_id
        WHERE a.currency_id IN (1, :currencyId) AND a.chat_id = :userId order by a.currency_id
        """)
Flux<AccountPriceDto> findByCurrencyIdAndChatIdJoin(Integer currencyId, Long userId);
}
