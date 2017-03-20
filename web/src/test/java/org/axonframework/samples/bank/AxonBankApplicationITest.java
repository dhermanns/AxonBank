package org.axonframework.samples.bank;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.model.Aggregate;
import org.axonframework.commandhandling.model.Repository;
import org.axonframework.common.transaction.Transaction;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.messaging.unitofwork.DefaultUnitOfWork;
import org.axonframework.samples.bank.api.bankaccount.AdjustSubAccountBalanceInCentsCommand;
import org.axonframework.samples.bank.api.bankaccount.CreateBankAccountCommand;
import org.axonframework.samples.bank.api.bankaccount.CreateSubBankAccountCommand;
import org.axonframework.samples.bank.command.BankAccount;
import org.jgroups.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Random;

/**
 * Created by m500516 on 17.03.17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ComponentScan(basePackages = "org.axonframework.samples.bank")
public class AxonBankApplicationITest {

    private Logger logger = LoggerFactory.getLogger(AxonBankApplicationITest.class);

    private int maxSubAccountToCreate = 1;
    private int maxModifications = 1000;

    @Autowired
    private CommandBus commandBus;
    @Autowired
    private Repository<BankAccount> repository;
    @Autowired
    private TransactionManager transactionManager;

    @Test
    public void testCreateBigBankAccount() {
        String bankAccountId = UUID.randomUUID().toString();

        long startTime = System.currentTimeMillis();

        // create a Big Aggregate with 1000 Events
        commandBus.dispatch(GenericCommandMessage.asCommandMessage(
            new CreateBankAccountCommand(bankAccountId, 0)));

        Random random = new Random();
        for (int i = 0; i < maxSubAccountToCreate; i++) {
            commandBus.dispatch(GenericCommandMessage.asCommandMessage(
                new CreateSubBankAccountCommand(
                    bankAccountId, UUID.randomUUID().toString(), random.nextInt(1000))));
        }

        long stopTime = System.currentTimeMillis();
        long creationTime = stopTime-startTime;

        // No modify this big Aggregate 1000 Times
        startTime = System.currentTimeMillis();

        for (int i = 0; i < maxModifications; i++) {
            commandBus.dispatch(GenericCommandMessage.asCommandMessage(
                new AdjustSubAccountBalanceInCentsCommand(
                    bankAccountId, random.nextInt(maxSubAccountToCreate), random.nextInt(1000))));
        }

        stopTime = System.currentTimeMillis();
        logger.info("The creation and the query of {} Events took {}ms", maxSubAccountToCreate, creationTime);
        logger.info("Modifying {} Subaccounts took {}ms on average", maxModifications, (stopTime-startTime) / maxModifications);
    }

}
