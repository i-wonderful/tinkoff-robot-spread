package com.byby.trobot.controller.dto.mapper;

import com.byby.trobot.controller.dto.PortfolioDto;
import com.byby.trobot.controller.dto.MoneyDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.PortfolioResponse;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.core.models.Money;
import ru.tinkoff.piapi.core.models.Portfolio;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import static ru.tinkoff.piapi.core.utils.MapperUtils.moneyValueToBigDecimal;
import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

@RequestScoped
public class PortfolioMapper {
    private static final Logger log = LoggerFactory.getLogger(PortfolioMapper.class);

    @Inject
    PositionMapper positionMapper;

    /**
     * Sandbox mapping
     *
     * @param portfolio
     * @param accountId
     * @return
     */
    public PortfolioDto toDto(PortfolioResponse portfolio, String accountId) {

        PortfolioDto dto = new PortfolioDto(true, accountId);

        var totalAmountCurrencies = portfolio.getTotalAmountCurrencies();
        dto.setTotalAmountCurrencies(moneyValueToBigDecimal(totalAmountCurrencies));
        log.info("общая стоимость валют в портфеле {}", totalAmountCurrencies);

        var totalAmountShares = portfolio.getTotalAmountShares();
        dto.setTotalAmountShares(toDto(totalAmountShares));
        log.info("общая стоимость акций в портфеле {}", totalAmountShares);

        dto.setExpectedYeld(quotationToBigDecimal(portfolio.getExpectedYield()));
        log.info("текущая доходность портфеля {}", portfolio.getExpectedYield());

        var positions = portfolio.getPositionsList();
        log.info("в портфолио {} позиций", positions.size());
        positionMapper.updateDto(portfolio, dto);

        return dto;
    }


    /**
     * Real mapping
     *
     * @param portfolio
     * @param accountId
     * @return
     */
    public PortfolioDto toDto(Portfolio portfolio, String accountId) {
        PortfolioDto dto = new PortfolioDto(false, accountId);

        var totalAmountCurrencies = portfolio.getTotalAmountCurrencies();
        dto.setTotalAmountCurrencies(totalAmountCurrencies.getValue());
        log.info("общая стоимость валют в портфеле {}", totalAmountCurrencies);

        var totalAmountShares = portfolio.getTotalAmountShares();
        dto.setTotalAmountShares(toDto(totalAmountShares));
        log.info("общая стоимость акций в портфеле {}", totalAmountShares);

        dto.setExpectedYeld(portfolio.getExpectedYield());
        log.info("текущая доходность портфеля {}", portfolio.getExpectedYield());

        var positions = portfolio.getPositions();
        log.info("в портфолио {} позиций", positions.size());
        positionMapper.updateDto(portfolio, dto);

        return dto;
    }


    private MoneyDto toDto(MoneyValue value) {
        MoneyDto dto = new MoneyDto();
        dto.setCurrency(value.getCurrency());
        dto.setValue(moneyValueToBigDecimal(value));
        return dto;
    }

    private MoneyDto toDto(Money money) {
        MoneyDto dto = new MoneyDto();
        dto.setCurrency(money.getCurrency() != null ? money.getCurrency().getDisplayName() : "");
        dto.setValue(money.getValue());
        return dto;
    }

    private MoneyDto toDto(Quotation value, String currency) {
        MoneyDto dto = new MoneyDto();
        dto.setCurrency(currency);
        dto.setValue(quotationToBigDecimal(value));
        return dto;
    }
}
