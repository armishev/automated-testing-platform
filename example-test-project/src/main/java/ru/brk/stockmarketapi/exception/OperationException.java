package ru.brk.stockmarketapi.exception;

public class OperationException extends RuntimeException {
    public OperationException(String message) {
        super(message);
    }
}
