package ru.brk.stockmarketapi.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.brk.stockmarketapi.dto.ExchangeRate;
import ru.brk.stockmarketapi.dto.OperationDto;
import ru.brk.stockmarketapi.dto.TradeDto;
import ru.brk.stockmarketapi.exception.OperationException;
import ru.brk.stockmarketapi.exception.TradeRendererException;
import ru.brk.stockmarketapi.models.*;
import ru.brk.stockmarketapi.repo.*;

import javax.xml.stream.util.EventReaderDelegate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;


@Slf4j
@Service
@RequiredArgsConstructor
public class MarketService {

    private  static Mono<Map<Integer, String>> currMap;
    private final AccountRepository accountRepository;
    private final AuditRepository auditRepository;
    private final CurrencyRepo currencyRepo;
    private final DailyPricesRepo dailyPricesRepo;
    private final SecondCurrencyRepo secondCurrencyRepo;
    private static final WebClient webClient = WebClient.builder().build();

    @PostConstruct
    public void initCurrency() {
        updateCurrencies();
    }

    private void updateCurrencies() {
        currMap = currencyRepo.findAll().collectMap(Currency::getCurrencyId, Currency::getName);
    }




    public Mono<TradeDto> tradeRender(Long userId, Integer currencyId, String operation) {
        log.info("tradeRender() called with args: {}, {}, {} ", userId, currencyId, operation);
        try {
            Mono<Account> accountMono = accountRepository.findByCurrencyIdAndChatId(operation.equals("buy") ? 1 : currencyId, userId);
            Mono<List<AuditTransaction>> trans = auditRepository.getAuditTransactionsByCurrency(currencyId, userId).collectList();
            Mono<EverySecondPrices> convert = getSecondPrice(currencyId);
            Mono<Double> diff = getExchangeRateMono(currencyId);


            return Mono.zip(trans, currMap, convert, diff, accountMono)
                    .flatMap(trade -> {
                        List<AuditTransaction> transactions = trade.getT1();
                        Map<Integer, String> currencies = trade.getT2();
                        EverySecondPrices price = trade.getT3();
                        Double diffr = trade.getT4();
                        Account account = trade.getT5();
                        return Mono.just(TradeDto.builder()
                                .auditDtoList(transactions)
                                .currency(currencies)
                                .available(account.getBalance())
                                .exchangeRate(ExchangeRate.builder()
                                        .conversionRate(price.getPrice())
                                        .diff(diffr)
                                        .build())
                                .build());
                    });
        }
        catch (RuntimeException e) {
            log.error(e.getMessage(), e);
            throw new TradeRendererException("Произошла ошибка, мы об этом уже знаем");
        }
    }

    @Transactional
    public Mono<Map<String, BigDecimal>> executeTrade(OperationDto operationDto, boolean isBuyOperation) {
        log.info("executeTrade() called with args: {}, ", operationDto);
        try {
            Mono<Account> brkAccount = accountRepository.findByCurrencyIdAndChatId(1, operationDto.getUserId());
            Mono<Account> currencyAccount = accountRepository.findByCurrencyIdAndChatId(operationDto.getCurrencyId(), operationDto.getUserId());
            Mono<EverySecondPrices> currencyMono = secondCurrencyRepo.findByCurrencyId(operationDto.getCurrencyId());

            Map<String, BigDecimal> resultMap = new LinkedHashMap<>();

            return Mono.zip(brkAccount, currencyAccount, currencyMono, currMap)
                    .filter(tuple -> tuple.getT1() != null && tuple.getT2() != null && tuple.getT3() != null)
                    .switchIfEmpty(Mono.error(new OperationException("Один из счетов недоступен.")))
                    .flatMap(accounts -> {
                        Account brk = accounts.getT1();
                        Account currency = accounts.getT2();
                        EverySecondPrices currPrice = accounts.getT3();
                        Map<Integer, String> currencyMap = accounts.getT4();

                        BigDecimal quantity = isBuyOperation
                                ? operationDto.getSymbolQuantity().negate()
                                : operationDto.getSymbolQuantity();

                        BigDecimal transactionValue = quantity.multiply(BigDecimal.valueOf(currPrice.getPrice()));

                        BigDecimal newBrkBalance = brk.getBalance().add(transactionValue);

                        brk.setBalance(newBrkBalance);

                        BigDecimal newCurrencyBalance = currency.getBalance().add(
                                isBuyOperation
                                        ? operationDto.getSymbolQuantity()
                                        : operationDto.getSymbolQuantity().negate()
                        );

                        currency.setBalance(newCurrencyBalance);

                        return accountRepository.updateUserValue(brk.getBalance(), brk.getCurrencyId(), operationDto.getUserId())
                                .then(accountRepository.updateUserValue(currency.getBalance(), currency.getCurrencyId(), operationDto.getUserId()))
                                .then(makeAudit(
                                        operationDto.getUserId(),
                                        operationDto.getCurrencyId(),
                                        isBuyOperation ? "buy" : "sell",
                                        operationDto.getSymbolQuantity()
                                ))
                                .then(Mono.fromSupplier(() -> {
                                    resultMap.put(currencyMap.get(brk.getCurrencyId()), brk.getBalance());
                                    resultMap.put(currencyMap.get(currency.getCurrencyId()), currency.getBalance());
                                    return resultMap;
                                }));
                    });
        } catch (RuntimeException e) {
            throw new OperationException("Ошибка во время операции, повторите попытку");
        }

    }


    @Transactional
    public Mono<Map<String, BigDecimal>> buy(OperationDto operationDto) {
        log.info("buy() called with args: {}, ", operationDto);
        return executeTrade(operationDto, true);
    }

    @Transactional
    public Mono<Map<String, BigDecimal>> sell(OperationDto operationDto) {
        log.info("sell() called with args: {}, ", operationDto);
        return executeTrade(operationDto, false);
    }

    @Transactional
    public Mono<AuditTransaction> makeAudit(Long userId, Integer currencyId, String typeOfOperation, BigDecimal count) {
        return auditRepository.save(new AuditTransaction(
                userId,
                currencyId,
                typeOfOperation,
                count
        ));
    }

    private Mono<EverySecondPrices> getSecondPrice(Integer currencyId) {
        return secondCurrencyRepo.findByCurrencyId(currencyId);
    }




    @Transactional
    public Mono<Account> reward(OperationDto operationDto) {
        log.info("reward() called with args: {}, ", operationDto);
        try {
            Mono<Account> accountMono = accountRepository.findByCurrencyIdAndChatId(operationDto.getCurrencyId(), operationDto.getUserId());
            return Mono.zip(accountMono, makeAudit(
                            operationDto.getUserId(),
                            operationDto.getCurrencyId(),
                            "reward",
                            operationDto.getSymbolQuantity()))
                    .flatMap(flat -> {
                        Account account = flat.getT1();
                        account.setBalance(account.getBalance().add(operationDto.getSymbolQuantity()));
                        return accountRepository.save(account);
                    });
        }
        catch (RuntimeException e) {
            throw  new OperationException("Ошибка в операции");
        }
    }


    public Mono<Double> getExchangeRateMono(Integer currency) {
        Mono<DailyPrices> oldPriceMono = dailyPricesRepo.findByDailyAndCurrencyId(LocalDate.now().minusDays(1), currency);
        Mono<DailyPrices> actualPriceMono = dailyPricesRepo.findByDailyAndCurrencyId(LocalDate.now(), currency);

        return actualPriceMono.flatMap(actualPrice ->
                oldPriceMono.map(oldPrice -> priceDiff(actualPrice.getPrice(), oldPrice.getPrice()))
                        .switchIfEmpty(Mono.just(priceDiff(actualPrice.getPrice(), actualPrice.getPrice())))
        );
    }


    private double priceDiff(double newPrice, double oldPrice) {
        return Math.floor(((newPrice - oldPrice) / oldPrice * 100) * 100) / 100;
    }







}
