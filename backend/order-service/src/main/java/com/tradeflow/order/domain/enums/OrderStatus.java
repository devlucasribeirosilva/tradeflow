package com.tradeflow.order.domain.enums;

public enum OrderStatus {

    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    PROCESSING,
    SETTLED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case DRAFT -> next == PENDING_APPROVAL;
            case PENDING_APPROVAL -> next == APPROVED || next == REJECTED;
            case APPROVED -> next == PROCESSING || next == CANCELLED;
            case PROCESSING -> next == SETTLED || next == CANCELLED;
            default -> false;
        };
    }
}