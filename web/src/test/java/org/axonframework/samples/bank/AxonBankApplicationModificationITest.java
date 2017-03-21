package org.axonframework.samples.bank;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.model.Repository;
import org.axonframework.common.transaction.TransactionManager;
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
public class AxonBankApplicationModificationITest {

    private Logger logger = LoggerFactory.getLogger(AxonBankApplicationModificationITest.class);

    private int maxModifications = 100000;

    @Autowired
    private CommandBus commandBus;
    @Autowired
    private Repository<BankAccount> repository;
    @Autowired
    private TransactionManager transactionManager;

    @Test
    public void testMassiveModifySubAccount() {
        String bankAccountId = "MyBankAccountId";

        long startTime = System.currentTimeMillis();

        // modify this big Aggregate a lot of Times
        startTime = System.currentTimeMillis();
        Random random = new Random();
        for (int i = 0; i < maxModifications; i++) {
            commandBus.dispatch(GenericCommandMessage.asCommandMessage(
                new AdjustSubAccountBalanceInCentsCommand(
                    bankAccountId, random.nextInt(AxonBankApplicationCreationITest.MAX_SUB_ACCOUNT_TO_CREATE), random.nextInt(1000))));
        }

        long stopTime = System.currentTimeMillis();
        logger.info("Modifying {} Subaccounts took {}ms on average", maxModifications, (stopTime-startTime) / maxModifications);
    }

}
