package com.byby.trobot.db.service;

import com.byby.trobot.db.entity.OrderDone;
import com.byby.trobot.db.entity.OrderDoneDirection;
import com.byby.trobot.db.mapper.OrderEntityMapper;
import com.byby.trobot.db.repository.OrderDoneRepository;
import com.google.protobuf.Timestamp;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import ru.tinkoff.piapi.contract.v1.OrderTrades;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.ZonedDateTime;

@Deprecated
@ApplicationScoped
public class DbService {

    @Inject
    OrderDoneRepository orderDoneRepository;

    @Inject
    OrderEntityMapper orderEntityMapper;

    // for test
    public Uni save2(){
        OrderDone o2 = new OrderDone();
        o2.setFigi("Bla bla");
        o2.setDateTimeDone(ZonedDateTime.now());
        o2.setDirection(OrderDoneDirection.BUY);
        return orderDoneRepository.persistAndFlush(o2);
    }



    public Multi<OrderDone> getOrderDones(){
        return OrderDone.findAll().stream();
    }

}
