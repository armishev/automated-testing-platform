package ru.brk.stockmarketapi.repo;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import ru.brk.stockmarketapi.models.AuditTransaction;

import java.util.List;

public interface AuditRepository extends ReactiveCrudRepository<AuditTransaction, Long> {
    @Query("""
        SELECT * FROM trans_audit where trans_audit.currency_id = :currencyId
        AND chat_id = :user_id order by time desc limit 5
       """)
    Flux<AuditTransaction> getAuditTransactionsByCurrency(@Param("currencyId") Integer currencyId,
                                                      @Param("user_id") Long userId
    );

    @Query("""
        SELECT * FROM trans_audit where trans_audit.currency_id = :currencyId
        AND trans_audit.chat_id = :userId order by time desc limit 5
       """)
    Flux<AuditTransaction> getAuditTransactionsById(@Param("userId") Long userId);

}
