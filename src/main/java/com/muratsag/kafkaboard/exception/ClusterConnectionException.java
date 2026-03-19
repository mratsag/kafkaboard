package com.muratsag.kafkaboard.exception;

public class ClusterConnectionException extends RuntimeException {
    public ClusterConnectionException(String message) {
        super(message);
    }
}