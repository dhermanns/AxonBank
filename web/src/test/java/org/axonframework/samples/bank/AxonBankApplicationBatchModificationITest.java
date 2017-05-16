package org.axonframework.samples.bank;

import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.samples.bank.api.bankaccount.AdjustSubAccountBalanceInCentsCommand;
import org.axonframework.samples.bank.api.bankaccount.BatchAdjustSubAccountBalanceInCentsCommand;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by m500516 on 17.03.17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ComponentScan(basePackages = "org.axonframework.samples.bank")
public class AxonBankApplicationBatchModificationITest {

    private Logger logger = LoggerFactory.getLogger(AxonBankApplicationBatchModificationITest.class);

    private int maxModifications = 2;
    private int maxModificationsInBatch = 1_000;

    @Autowired
    private CommandGateway commandGateway;

    @Test
    public void testMassiveModifySubAccount() {
        String bankAccountId = "MyBankAccountId2";

        long startTime = System.currentTimeMillis();

        // modify this big Aggregate a lot of Times
        startTime = System.currentTimeMillis();
        Random random = new Random();
        for (int i = 0; i < maxModifications; i++) {

            List<Integer> subAccountNrList = new ArrayList<>();
            List<Long> balanceInCentsList = new ArrayList<>();
            for (int j = 0; j < maxModificationsInBatch; j++) {

                subAccountNrList.add(random.nextInt(AxonBankApplicationCreationITest.MAX_SUB_ACCOUNT_TO_CREATE));
                balanceInCentsList.add(Long.valueOf(random.nextInt(1000)));
            }
            commandGateway.send(GenericCommandMessage.asCommandMessage(
                new BatchAdjustSubAccountBalanceInCentsCommand(
                    bankAccountId, subAccountNrList, balanceInCentsList)));

        }

        long stopTime = System.currentTimeMillis();
        logger.info("Modifying {} Subaccounts took {}ms on average", maxModifications * maxModificationsInBatch, (stopTime-startTime) / (maxModifications * maxModificationsInBatch));
    }

}
