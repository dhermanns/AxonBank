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

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.conflictresolution.ConflictResolution;
import org.axonframework.commandhandling.conflictresolution.ConflictResolver;
import org.axonframework.commandhandling.conflictresolution.NoConflictResolver;
import org.axonframework.commandhandling.model.Aggregate;
import org.axonframework.commandhandling.model.AggregateNotFoundException;
import org.axonframework.commandhandling.model.ConcurrencyException;
import org.axonframework.commandhandling.model.Repository;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.samples.bank.api.bankaccount.*;

import static org.axonframework.eventhandling.GenericEventMessage.asEventMessage;

public class BankAccountCommandHandler {

    private Repository<BankAccount> repository;
    private EventBus eventBus;

    public BankAccountCommandHandler(Repository<BankAccount> repository, EventBus eventBus) {
        this.repository = repository;
        this.eventBus = eventBus;
    }

    @CommandHandler
    public void handle(CreateBankAccountCommand command) throws RuntimeException {
        try {
            repository.newInstance(() -> new BankAccount(command.getBankAccountId(), command.getOverdraftLimit()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @CommandHandler
    public void handle(DepositMoneyCommand command) {
        Aggregate<BankAccount> bankAccountAggregate = repository.load(command.getBankAccountId());
        bankAccountAggregate.execute(bankAccount -> bankAccount.deposit(command.getAmountOfMoney()));
    }

    @CommandHandler
    public void handle(WithdrawMoneyCommand command) {
        Aggregate<BankAccount> bankAccountAggregate = repository.load(command.getBankAccountId());
        bankAccountAggregate.execute(bankAccount -> bankAccount.withdraw(command.getAmountOfMoney()));
    }

    @CommandHandler
    public void handle(DebitSourceBankAccountCommand command) {
        try {
            Aggregate<BankAccount> bankAccountAggregate = repository.load(command.getBankAccountId());
            bankAccountAggregate.execute(bankAccount -> bankAccount
                    .debit(command.getAmount(), command.getBankTransferId()));
        } catch (AggregateNotFoundException exception) {
            eventBus.publish(asEventMessage(new SourceBankAccountNotFoundEvent(command.getBankTransferId())));
        }
    }

    @CommandHandler
    public void handle(CreditDestinationBankAccountCommand command) {
        try {
            Aggregate<BankAccount> bankAccountAggregate = repository.load(command.getBankAccountId());
            bankAccountAggregate.execute(bankAccount -> bankAccount
                    .credit(command.getAmount(), command.getBankTransferId()));

        }
        catch (AggregateNotFoundException exception) {
            eventBus.publish(asEventMessage(new DestinationBankAccountNotFoundEvent(command.getBankTransferId())));
        }
    }

    @CommandHandler
    public void handle(ReturnMoneyOfFailedBankTransferCommand command) {
        Aggregate<BankAccount> bankAccountAggregate = repository.load(command.getBankAccountId());
        bankAccountAggregate.execute(bankAccount -> bankAccount.returnMoney(command.getAmount()));
    }

    @CommandHandler
    public void handle(CreateSubBankAccountCommand command) {
        Aggregate<BankAccount> bankAccountAggregate = repository.load(command.getBankAccountId());
        bankAccountAggregate.execute(bankAccount -> bankAccount.addSubBankAccount(command.getName(), command.getBalanceInCents()));
    }

    @CommandHandler
    public void handle(AdjustSubAccountBalanceInCentsCommand command) {

        Aggregate<BankAccount> bankAccountAggregate = repository.load(command.getBankAccountId());
        bankAccountAggregate.execute(bankAccount -> bankAccount.modifySubAccountBalanceInCents(command.getSubAccountNr(), command.getBalanceInCents()));
    }
}
