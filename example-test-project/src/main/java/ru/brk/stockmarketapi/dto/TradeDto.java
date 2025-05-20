package ru.brk.stockmarketapi.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import reactor.core.publisher.Flux;
import ru.brk.stockmarketapi.models.AuditTransaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TradeDto {
    Map<Integer, String> currency;
    List<AuditTransaction> auditDtoList;
    BigDecimal available;
    ExchangeRate exchangeRate;


}
