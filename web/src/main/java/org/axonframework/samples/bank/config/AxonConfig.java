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

package org.axonframework.samples.bank.config;

import org.axonframework.commandhandling.model.Repository;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.jpa.SimpleEntityManagerProvider;
import org.axonframework.common.transaction.NoTransactionManager;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition;
import org.axonframework.eventsourcing.EventSourcingRepository;
import org.axonframework.eventsourcing.GenericAggregateFactory;
import org.axonframework.eventsourcing.Snapshotter;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.eventsourcing.eventstore.jpa.JpaEventStorageEngine;
import org.axonframework.samples.bank.command.BankAccount;
import org.axonframework.samples.bank.command.BankAccountCommandHandler;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.axonframework.serialization.upcasting.event.NoOpEventUpcaster;
import org.axonframework.spring.config.AxonConfiguration;
import org.axonframework.spring.eventsourcing.SpringAggregateSnapshotterFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.sql.SQLException;

@SuppressWarnings("SpringJavaAutowiringInspection")
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass=true)
public class AxonConfig {

    private Logger logger = LoggerFactory.getLogger(AxonConfig.class);

    @Autowired
    private AxonConfiguration axonConfiguration;
    @Autowired
    private EventBus eventBus;
    @PersistenceContext
    private EntityManager entityManager;

//    @Bean
//    public EventStorageEngine eventStorageEngine(DataSource dataSource) throws SQLException {
//
//        EntityManagerProvider entityManagerProvider = new SimpleEntityManagerProvider(entityManager);
//        return new JpaEventStorageEngine(serializer(), NoOpEventUpcaster.INSTANCE, dataSource, entityManagerProvider, NoTransactionManager.INSTANCE);
//    }

    @Bean
    public BankAccountCommandHandler bankAccountCommandHandler(EventStore eventStore, Snapshotter snapshotter) {

        Repository<BankAccount> bankAccountRepository = bankAccountRepository2(eventStore, snapshotter);

        return new BankAccountCommandHandler(bankAccountRepository, eventBus);
    }

    @Bean
    public Repository<BankAccount> bankAccountRepository2(EventStore eventStore, Snapshotter snapshotter) {

        axonConfiguration.repository(BankAccount.class);
        Repository<BankAccount> bankAccount =
            new EventSourcingRepository<>(
                new GenericAggregateFactory<>(BankAccount.class), eventStore,
                new EventCountSnapshotTriggerDefinition(snapshotter, 50));

        return bankAccount;
    }

    @Bean
    public SpringAggregateSnapshotterFactoryBean snapshotter() {
        return new SpringAggregateSnapshotterFactoryBean();
    }

    @Bean
    public Serializer serializer() {
        return new JacksonSerializer();
    }
}
