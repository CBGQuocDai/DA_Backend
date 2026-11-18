package com.backend.application.service.impl;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

final class TransactionHooks {

    private TransactionHooks() {
    }

    /**
     * Chạy {@code action} sau khi giao dịch hiện tại commit; giao dịch rollback thì bỏ qua.
     * Không có giao dịch thì chạy ngay.
     */
    static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
