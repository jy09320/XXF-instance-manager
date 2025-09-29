package com.jinyue.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class InvalidInstanceStateException extends RuntimeException {
    private final List<InvalidStateInfo> invalidStates;

    public InvalidInstanceStateException(String message, List<InvalidStateInfo> invalidStates) {
        super(message);
        this.invalidStates = invalidStates;
    }

    @Getter
    public static class InvalidStateInfo {
        private String instanceId;
        private String currentState;
        private String requiredState;
        private String reason;

        public InvalidStateInfo(String instanceId, String currentState, String requiredState, String reason) {
            this.instanceId = instanceId;
            this.currentState = currentState;
            this.requiredState = requiredState;
            this.reason = reason;
        }

    }
}