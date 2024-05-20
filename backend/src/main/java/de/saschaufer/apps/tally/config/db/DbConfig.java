package de.saschaufer.apps.tally.config.db;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.transaction.ReactiveTransactionManager;

@Configuration
@RequiredArgsConstructor
public class DbConfig {

    private final DbProperties dbProperties;

    @Bean
    @Nonnull
    public ConnectionFactory connectionFactory() {
        return ConnectionFactories.get(dbProperties.url());
    }

    @Bean
    public R2dbcEntityTemplate r2dbcEntityTemplate() {
        return new R2dbcEntityTemplate(connectionFactory());
    }

    @Bean
    public ReactiveTransactionManager transactionManager(final ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    public ConnectionFactoryInitializer initializer(final ConnectionFactory connectionFactory) {

        final Resource sql = new ByteArrayResource("""

                create table if not exists users (
                    id serial primary key,
                    username varchar not null unique,
                    password varchar not null,
                    roles varchar not null
                );

                create table if not exists products (
                    id serial primary key,
                    name varchar not null unique
                );

                create table if not exists product_prices (
                    id serial primary key,
                    product_id integer not null,
                    price decimal(12,2) not null,
                    valid_until timestamp,
                    constraint fk_product_prices_products
                        foreign key (product_id) references products(id)
                );

                create table if not exists purchases (
                    id serial primary key,
                    user_id integer not null,
                    product_price_id integer not null,
                    timestamp timestamp not null,
                    constraint fk_purchases_users
                        foreign key (user_id) references users(id),
                    constraint fk_purchases_product_prices
                        foreign key (product_price_id) references product_prices(id)
                );

                create table if not exists payments (
                    id serial primary key,
                    user_id integer not null,
                    amount decimal(12,2) not null,
                    timestamp timestamp not null,
                    constraint fk_payments_users
                        foreign key (user_id) references users(id)
                );
                """
                .getBytes()
        );

        final ResourceDatabasePopulator resource = new ResourceDatabasePopulator();
        resource.addScript(sql);

        final ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        initializer.setDatabasePopulator(resource);

        return initializer;
    }
}
