package br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc;

import br.dev.andrestamatto.identityhub.identity.application.OnboardingProofTransaction;
import java.util.Objects;
import org.springframework.transaction.support.TransactionOperations;

public final class SpringOnboardingProofTransaction implements OnboardingProofTransaction {

    private final TransactionOperations transactions;

    public SpringOnboardingProofTransaction(TransactionOperations transactions) {
        this.transactions = Objects.requireNonNull(transactions);
    }

    @Override
    public void execute(Runnable work) {
        Objects.requireNonNull(work);
        transactions.executeWithoutResult(status -> work.run());
    }
}
