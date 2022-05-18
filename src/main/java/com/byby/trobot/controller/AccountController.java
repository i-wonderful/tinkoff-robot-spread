package com.byby.trobot.controller;

import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.dto.ExchangeOpenDto;
import com.byby.trobot.dto.OrderStateDto;
import com.byby.trobot.dto.PortfolioDto;
import com.byby.trobot.dto.SettingsRobotDto;
import com.byby.trobot.dto.mapper.OrderMapper;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.SandboxAccountService;
import com.byby.trobot.service.impl.ExchangeService;
import io.smallrye.mutiny.Uni;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.List;

import static com.byby.trobot.dto.mapper.SettingsMapper.toDto;

@Path("/account")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class AccountController {
    @Inject
    Instance<Executor> executor;

    @Inject
    ApplicationProperties properties;

    @Inject
    ExchangeService exchangeService;

    @Inject
    SandboxAccountService sandboxAccountService;

    @Inject
    OrderMapper orderMapper;

    /**
     * Портфолио
     */
    @GET
    @Path("/portfolio")
    public Uni<PortfolioDto> getPortfolio() {
        return executor.get().getPortfolio();
    }

    /**
     * Настройки из properties
     */
    @GET
    @Path("/settings")
    public Uni<SettingsRobotDto> getSettings() {
        return Uni.createFrom().item(toDto(properties));
    }

    /**
     * Открытые биржи в данный момент
     */
    @GET
    @Path("/exchanges")
    public Uni<List<ExchangeOpenDto>> openExchanges() {
        List<ExchangeOpenDto> openExchanges = exchangeService.getExchangesInfoNow();
        return Uni.createFrom().item(openExchanges);
    }

    /**
     * Текущие заявки со статусами
     */
    @GET
    @Path("/orders")
    public Uni<List<OrderStateDto>> getOrders() {
        return executor.get().getMyOrders()
                .onItem()
                .transformToUni(orderStates -> orderMapper.toDtoUni(orderStates));
    }

    /**
     * Отменить все заявки
     */
    @GET
    @Path("/cancel-all-orders")
    public Uni<Void> cancelAllOrders() {
        return executor.get().cancelAllOrders();
    }

    /**
     * Пересоздать аккаунт песочницы
     */
    @GET
    @Path("/recreate-sandbox")
    public Uni<String> recreateSandbox() {
        return sandboxAccountService.recreateSandbox();
    }
}
