package br.dev.andrestamatto.identityhub.identity.adapter.out.jdbc;

import br.dev.andrestamatto.identityhub.identity.application.IdentityTransaction;
import java.util.function.Supplier;
import java.util.Objects;
import org.springframework.transaction.support.TransactionOperations;

public final class SpringVerificationTransaction implements IdentityTransaction {

    private final TransactionOperations transactions;

    public SpringVerificationTransaction(TransactionOperations transactions) {
        this.transactions = Objects.requireNonNull(transactions);
    }

    @Override
    public void execute(Runnable work) {
        Objects.requireNonNull(work);
        transactions.executeWithoutResult(status -> work.run());
    }

    @Override
    public <T> T execute(Supplier<T> work) {
        Objects.requireNonNull(work);
        return transactions.execute(status -> work.get());
    }
}
