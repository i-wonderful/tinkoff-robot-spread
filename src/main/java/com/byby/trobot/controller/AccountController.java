package com.byby.trobot.controller;

import com.byby.trobot.config.RobotProperties;
import com.byby.trobot.config.RobotSandboxProperties;
import com.byby.trobot.config.StrategySharesProperties;
import com.byby.trobot.controller.dto.ExchangeDto;
import com.byby.trobot.controller.dto.OrderStateDto;
import com.byby.trobot.controller.dto.PortfolioDto;
import com.byby.trobot.controller.dto.SettingsRobotDto;
import com.byby.trobot.controller.dto.mapper.OrderMapper;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.ExchangeService;
import com.byby.trobot.service.SandboxAccountService;
import io.smallrye.mutiny.Uni;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static com.byby.trobot.controller.dto.mapper.SettingsMapper.toDto;

@Path("/account")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AccountController {
    @Inject
    Instance<Executor> executor;
    @Inject
    ExchangeService exchangeService;
    @Inject
    SandboxAccountService sandboxAccountService;
    @Inject
    OrderMapper orderMapper;
    @Inject
    RobotSandboxProperties robotSandboxProperties;
    @Inject
    StrategySharesProperties strategySharesProperties;
    @Inject
    RobotProperties robotProperties;

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
        return Uni.createFrom().item(toDto(robotSandboxProperties, strategySharesProperties, robotProperties));
    }

    /**
     * Открытые биржи в данный момент
     */
    @GET
    @Path("/exchanges")
    public Uni<List<ExchangeDto>> openExchanges() {
        List<ExchangeDto> openExchanges = exchangeService.getExchangesInfo();
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
