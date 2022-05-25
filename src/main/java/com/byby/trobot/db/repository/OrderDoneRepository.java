package com.byby.trobot.db.repository;

import com.byby.trobot.db.entity.OrderDone;
import com.byby.trobot.db.entity.OrderDoneDirection;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Multi;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderDoneRepository implements PanacheRepository<OrderDone> {

      public Multi<OrderDone> findByName(OrderDoneDirection direction){
        return find("direction", direction).stream();
    }

}
