package com.byby.trobot.dto.mapper;

import com.byby.trobot.dto.MoneyDto;
import com.byby.trobot.dto.PortfolioDto;
import com.byby.trobot.dto.PortfolioPositionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.PortfolioPosition;
import ru.tinkoff.piapi.contract.v1.PortfolioResponse;
import ru.tinkoff.piapi.contract.v1.Quotation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import static java.util.Optional.ofNullable;
import static ru.tinkoff.piapi.core.utils.MapperUtils.moneyValueToBigDecimal;
import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

public class PortfolioMapper {
    private static final Logger log = LoggerFactory.getLogger(PortfolioMapper.class);

    public static PortfolioDto toDto(PortfolioResponse portfolio, String accountId) {
        PortfolioDto dto = new PortfolioDto(true, accountId);

        var totalAmountCurrencies = portfolio.getTotalAmountCurrencies();
        dto.setBalance(toDto(totalAmountCurrencies));
        log.info("общая стоимость валют в портфеле {}", totalAmountCurrencies);

        var totalAmountShares = portfolio.getTotalAmountShares();
        dto.setTotalAmountShares(toDto(totalAmountShares));
        log.info("общая стоимость акций в портфеле {}", totalAmountShares);

        dto.setExpectedYeld(quotationToBigDecimal(portfolio.getExpectedYield()));
        log.info("текущая доходность портфеля {}", portfolio.getExpectedYield());

        var positions = portfolio.getPositionsList();
        log.info("в портфолио {} позиций", positions.size());
        for (int i = 0; i < Math.min(positions.size(), 5); i++) {
            var position = positions.get(i);
            var figi = position.getFigi();
            if ("BBG0013HGFT4".equals(figi)) { // todo
                dto.setBalanceUsd(toDto(position.getQuantity(), "USD"));
            } else {
                dto.addPosition(toDto(position));
            }
        }

        return dto;
    }

    private static PortfolioPositionDto toDto(PortfolioPosition value) {
        if (value == null) {
            return null;
        }
        PortfolioPositionDto dto = new PortfolioPositionDto();
        dto.setFigi(value.getFigi());
        dto.setQuantity(value.getQuantity().getUnits());
        dto.setExpectedYield(quotationToBigDecimal(value.getExpectedYield()));
        return dto;
    }

    private static List<PortfolioPositionDto> toDto(List<PortfolioPosition> values) {
        return ofNullable(values).orElseGet(Collections::emptyList)
                .stream()
                .map(PortfolioMapper::toDto)
                .collect(Collectors.toList());
    }

    private static MoneyDto toDto(MoneyValue value) {
        MoneyDto dto = new MoneyDto();
        dto.setCurrency(value.getCurrency());
        dto.setValue(moneyValueToBigDecimal(value));
        return dto;
    }

    private static MoneyDto toDto(Quotation value, String currency) {
        MoneyDto dto = new MoneyDto();
        dto.setCurrency(currency);
        dto.setValue(quotationToBigDecimal(value));
        return dto;
    }
}
