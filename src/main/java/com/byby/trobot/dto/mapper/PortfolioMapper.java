package com.byby.trobot.dto.mapper;

import com.byby.trobot.dto.MoneyDto;
import com.byby.trobot.dto.PortfolioDto;
import com.byby.trobot.dto.PortfolioPositionDto;
import com.byby.trobot.service.impl.SharesService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.PortfolioPosition;
import ru.tinkoff.piapi.contract.v1.PortfolioResponse;
import ru.tinkoff.piapi.contract.v1.Quotation;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import static java.util.Optional.ofNullable;
import static ru.tinkoff.piapi.core.utils.MapperUtils.moneyValueToBigDecimal;
import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

@RequestScoped
public class PortfolioMapper {
    private static final Logger log = LoggerFactory.getLogger(PortfolioMapper.class);

    @Inject
    SharesService sharesService;

    @ConfigProperty(name = "tinkoff.figi.usd")
    String tinkoffFigiUsd;

    @ConfigProperty(name = "tinkoff.figi.rub")
    String tinkoffFigiRub;

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
        for (int i = 0; i < positions.size(); i++) {
            PortfolioPosition position = positions.get(i);
            if (tinkoffFigiUsd.equals(position.getFigi())) {
                dto.setBalanceUsd(quotationToBigDecimal(position.getQuantity()));
            } else if (tinkoffFigiRub.equals(position.getFigi())){
                dto.setBalanceRub(quotationToBigDecimal(position.getQuantity()));
            } else {
                dto.addPosition(toDto(position));
            }
        }

        return dto;
    }

    private  PortfolioPositionDto toDto(PortfolioPosition value) {
        if (value == null) {
            return null;
        }

        PortfolioPositionDto dto = new PortfolioPositionDto();
        dto.setFigi(value.getFigi());
        dto.setTicker(sharesService.findTickerByFigiSync(value.getFigi()));
        dto.setName(sharesService.findNameByFigi(value.getFigi()));
        dto.setAveragePrice(moneyValueToBigDecimal(value.getAveragePositionPrice()));
        dto.setCurrency(value.getAveragePositionPrice().getCurrency());
        dto.setQuantity(value.getQuantity().getUnits());
        dto.setExpectedYield(quotationToBigDecimal(value.getExpectedYield()));

        return dto;
    }

    private List<PortfolioPositionDto> toDto(List<PortfolioPosition> values) {
        return ofNullable(values).orElseGet(Collections::emptyList)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private MoneyDto toDto(MoneyValue value) {
        MoneyDto dto = new MoneyDto();
        dto.setCurrency(value.getCurrency());
        dto.setValue(moneyValueToBigDecimal(value));
        return dto;
    }

    private MoneyDto toDto(Quotation value, String currency) {
        MoneyDto dto = new MoneyDto();
        dto.setCurrency(currency);
        dto.setValue(quotationToBigDecimal(value));
        return dto;
    }
}
