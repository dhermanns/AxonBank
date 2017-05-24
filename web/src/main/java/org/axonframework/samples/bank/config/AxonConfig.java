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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.ehcache.CacheManager;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.gateway.DefaultCommandGateway;
import org.axonframework.commandhandling.gateway.RetryScheduler;
import org.axonframework.commandhandling.model.Repository;
import org.axonframework.common.caching.Cache;
import org.axonframework.common.caching.EhCacheAdapter;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.jpa.SimpleEntityManagerProvider;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventsourcing.*;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.eventsourcing.eventstore.jdbc.EventSchema;
import org.axonframework.eventsourcing.eventstore.jdbc.JdbcEventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jpa.JpaEventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jpa.SQLErrorCodesResolver;
import org.axonframework.samples.bank.util.NonStrictJdbcEventStorageEngine;
import org.axonframework.samples.bank.command.BankAccount;
import org.axonframework.samples.bank.command.BankAccountCommandHandler;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.axonframework.serialization.upcasting.event.NoOpEventUpcaster;
import org.axonframework.spring.config.AxonConfiguration;
import org.axonframework.spring.eventsourcing.SpringAggregateSnapshotterFactoryBean;
import org.axonframework.spring.messaging.unitofwork.SpringTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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
    @Autowired
    private Cache cache;

    @Bean
    public Serializer jacksonSerializer() {

        ObjectMapper mapper  = new ObjectMapper();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        return new JacksonSerializer(mapper);
    }

    // An own resolver is needed because Axon doesn't recognize DB2 on linux correctly
    @Bean
    public SQLErrorCodesResolver sqlErrorCodesResolver() {
        return new SQLErrorCodesResolver("DB2");
    }

    @Bean
    public EventStorageEngine eventStorageEngine(PlatformTransactionManager platformTransactionManager, DataSource dataSource) throws SQLException {

        EntityManagerProvider entityManagerProvider = new SimpleEntityManagerProvider(entityManager);

//        return new JpaEventStorageEngine(
//                jacksonSerializer(), NoOpEventUpcaster.INSTANCE, sqlErrorCodesResolver(),
//                null, entityManagerProvider, new SpringTransactionManager(platformTransactionManager),
//                null, null, true);

        return new NonStrictJdbcEventStorageEngine(
            jacksonSerializer(), NoOpEventUpcaster.INSTANCE, sqlErrorCodesResolver(), 50,
            dataSource::getConnection, new SpringTransactionManager(platformTransactionManager), byte[].class, eventSchema(),
            null, null);
    }

    @Bean
    public CommandGateway commandGateway(CommandBus commandBus) {

//        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
//        RetryScheduler retryScheduler = new OwnRetryScheduler(scheduledExecutorService, 1, 10);
//        return new DefaultCommandGateway(commandBus, retryScheduler);

        return new DefaultCommandGateway(commandBus);
    }

    @Bean
    public BankAccountCommandHandler bankAccountCommandHandler(EventStore eventStore, Snapshotter snapshotter) {

        Repository<BankAccount> bankAccountRepository = bankAccountRepository2(eventStore, snapshotter);

        return new BankAccountCommandHandler(bankAccountRepository, eventBus);
    }

    @Bean
    public Repository<BankAccount> bankAccountRepository2(EventStore eventStore, Snapshotter snapshotter) {

        axonConfiguration.repository(BankAccount.class);
        Repository<BankAccount> bankAccount =
            new CachingEventSourcingRepository<BankAccount>(
                new GenericAggregateFactory<BankAccount>(BankAccount.class), eventStore, cache,
                new EventCountSnapshotTriggerDefinition(snapshotter, 50));

//        Repository<BankAccount> bankAccount =
//            new EventSourcingRepository<BankAccount>(
//                new GenericAggregateFactory<BankAccount>(BankAccount.class), eventStore,
//                new EventCountSnapshotTriggerDefinition(snapshotter, 50));


        return bankAccount;
    }

    @Bean
    public EhCacheAdapter ehCache(CacheManager cacheManager) {
        return new EhCacheAdapter(cacheManager.getCache("testCache"));
    }

    @Bean
    public EhCacheManagerFactoryBean ehCacheManagerFactoryBean() {
        EhCacheManagerFactoryBean ehCacheManagerFactoryBean = new EhCacheManagerFactoryBean();
        ehCacheManagerFactoryBean.setShared(true);

        return ehCacheManagerFactoryBean;
    }

    @Bean
    public SpringAggregateSnapshotterFactoryBean snapshotter() {
        return new SpringAggregateSnapshotterFactoryBean();
    }

    private EventSchema eventSchema() {
        return EventSchema.builder()
            .withEventTable("DOMAIN_EVENT_ENTRY")
            .withAggregateIdentifierColumn("AGGREGATE_IDENTIFIER")
            .withEventIdentifierColumn("EVENT_IDENTIFIER")
            .withMetaDataColumn("META_DATA")
            .withGlobalIndexColumn("GLOBAL_INDEX")
            .withPayloadColumn("PAYLOAD")
            .withPayloadRevisionColumn("PAYLOAD_REVISION")
            .withPayloadTypeColumn("PAYLOAD_TYPE")
            .withSequenceNumberColumn("SEQUENCE_NUMBER")
            .withSnapshotTable("SNAPSHOT_EVENT_ENTRY")
            .withTimestampColumn("TIME_STAMP")
            .withTypeColumn("TYPE")
            .build();
    }
}
