package com.byby.trobot.db.repository;

import com.byby.trobot.db.entity.OrderDone;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderDoneRepository implements PanacheRepository<OrderDone> {
    // todo
    /*
      public RimEntity findByName(String name){
        return find("name", name).firstResult();
    }

    public List<RimEntity> findByType(String type) {
        return find("type", type).list();
    }
    * */
}
