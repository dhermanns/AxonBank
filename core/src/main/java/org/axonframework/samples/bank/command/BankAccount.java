/*
 * Copyright (c) 2016. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.samples.bank.command;

import org.axonframework.commandhandling.model.AggregateIdentifier;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.samples.bank.api.bankaccount.*;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.ArrayList;
import java.util.List;

import static org.axonframework.commandhandling.model.AggregateLifecycle.apply;

@Aggregate
public class BankAccount {

    @AggregateIdentifier
    private String id;
    private long overdraftLimit;
    private long balanceInCents;
    private List<SubBankAccount> subAccounts = new ArrayList<>();

    @SuppressWarnings("unused")
    private BankAccount() {
    }

    public BankAccount(String bankAccountId, long overdraftLimit) {
        apply(new BankAccountCreatedEvent(bankAccountId, overdraftLimit));
    }

    public void addSubBankAccount(String name, long balanceInCents) {
        apply(new SubBankAccountCreatedEvent(this.id, name, balanceInCents));
    }

    public void deposit(long amount) {
        apply(new MoneyDepositedEvent(id, amount));
    }

    public void withdraw(long amount) {
        if (amount <= balanceInCents + overdraftLimit) {
            apply(new MoneyWithdrawnEvent(id, amount));
        }
    }

    public void debit(long amount, String bankTransferId) {
        if (amount <= balanceInCents + overdraftLimit) {
            apply(new SourceBankAccountDebitedEvent(id, amount, bankTransferId));
        }
        else {
            apply(new SourceBankAccountDebitRejectedEvent(bankTransferId));
        }
    }

    public void credit(long amount, String bankTransferId) {
        apply(new DestinationBankAccountCreditedEvent(id, amount, bankTransferId));
    }

    public void modifySubAccountBalanceInCents(int subAccountNr, long newBalanceInCents) {
        if (subAccountNr >= subAccounts.size()) {
            throw new IllegalStateException(
                String.format("The Subaccount %s to modify does not exist. The maximum number of subacounts of bankaccount %s is %s",
                    subAccountNr, id, subAccounts.size()));
        }
        // simulate time to compute the outcome event
        try {
            Thread.sleep(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        apply(new SubAccountBalanceInCentsAdjustedEvent(id, subAccountNr, newBalanceInCents));
    }

    public void batchModifySubAccountBalanceInCents(List<Integer> subAccountNrList, List<Long> newBalanceInCentsList) {

        for (int i = 0; i < subAccountNrList.size(); i++) {
            if (subAccountNrList.get(i) >= subAccounts.size()) {
                throw new IllegalStateException(
                    String.format("The Subaccount %s to modify does not exist. The maximum number of subacounts of bankaccount %s is %s",
                        subAccountNrList.get(i), id, subAccounts.size()));
            }
            // simulate time to compute the outcome event
            try {
                Thread.sleep(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            apply(new SubAccountBalanceInCentsAdjustedEvent(id, subAccountNrList.get(i), newBalanceInCentsList.get(i)));
        }
    }

    public void returnMoney(long amount) {
        apply(new MoneyOfFailedBankTransferReturnedEvent(id, amount));
    }

    @EventHandler
    public void on(BankAccountCreatedEvent event) {
        this.id = event.getId();
        this.overdraftLimit = event.getOverdraftLimit();
        this.balanceInCents = 0;
    }

    @EventHandler
    public void on(SubBankAccountCreatedEvent event) {
        this.subAccounts.add(new SubBankAccount(this.id, event.getName(), event.getBalanceInCents()));
    }

    @EventHandler
    public void on(MoneyAddedEvent event) {
        balanceInCents += event.getAmount();
    }

    @EventHandler
    public void on(MoneySubtractedEvent event) {
        balanceInCents -= event.getAmount();
    }

    @EventHandler
    public void on(SubAccountBalanceInCentsAdjustedEvent event) {
        subAccounts.get(event.getSubAccountNr()).setBalanceInCents(event.getBalanceInCents());
    }

    public List<SubBankAccount> getSubAccounts() {
        return subAccounts;
    }

    public void setSubAccounts(List<SubBankAccount> subAccounts) {
        this.subAccounts = subAccounts;
    }
}
