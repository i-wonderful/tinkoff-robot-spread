package com.byby.trobot.controller.dto.mapper;

import com.byby.trobot.controller.dto.PortfolioDto;
import com.byby.trobot.controller.dto.PortfolioPositionDto;
import com.byby.trobot.service.impl.SharesServiceImpl;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import ru.tinkoff.piapi.contract.v1.PortfolioPosition;
import ru.tinkoff.piapi.contract.v1.PortfolioResponse;
import ru.tinkoff.piapi.core.models.Portfolio;
import ru.tinkoff.piapi.core.models.Position;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static ru.tinkoff.piapi.core.utils.MapperUtils.moneyValueToBigDecimal;
import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

@RequestScoped
public class PositionMapper {

    @Inject
    SharesServiceImpl sharesService;

    @ConfigProperty(name = "tinkoff.figi.usd")
    String tinkoffFigiUsd;

    @ConfigProperty(name = "tinkoff.figi.rub")
    String tinkoffFigiRub;

    public void updateDto(PortfolioResponse portfolio, PortfolioDto dto) {
        List<PortfolioPosition> positions = portfolio.getPositionsList();

        for (int i = 0; i < positions.size(); i++) {
            PortfolioPosition position = positions.get(i);
            if (tinkoffFigiUsd.equals(position.getFigi())) {
                dto.setBalanceUsd(quotationToBigDecimal(position.getQuantity()));
            } else if (tinkoffFigiRub.equals(position.getFigi())) {
                dto.setBalanceRub(quotationToBigDecimal(position.getQuantity()));
            } else {
                dto.addPosition(toDto(position));
            }
        }
    }

    public void updateDto(Portfolio portfolio, PortfolioDto dto ){
        List<Position> positions = portfolio.getPositions();
        for (int i = 0; i < positions.size(); i++) {
            Position position = positions.get(i);
            if (tinkoffFigiUsd.equals(position.getFigi())) {
                dto.setBalanceUsd(position.getQuantity());
            } else if (tinkoffFigiRub.equals(position.getFigi())) {
                dto.setBalanceRub(position.getQuantity());
            } else {
                dto.addPosition(toDto(position));
            }
        }
    }

    private PortfolioPositionDto toDto(PortfolioPosition value) {
        if (value == null) {
            return null;
        }

        PortfolioPositionDto dto = new PortfolioPositionDto();
        dto.setFigi(value.getFigi());
        dto.setTicker(sharesService.findTickerByFigiSync(value.getFigi()));
        dto.setName(sharesService.findNameByFigiSync(value.getFigi()));
        dto.setAveragePrice(moneyValueToBigDecimal(value.getAveragePositionPrice()));
        dto.setCurrency(value.getAveragePositionPrice().getCurrency());
        dto.setQuantity(value.getQuantity().getUnits());
        dto.setExpectedYield(quotationToBigDecimal(value.getExpectedYield()));

        return dto;
    }

    private PortfolioPositionDto toDto(Position value) {
        if (value == null) {
            return null;
        }

        PortfolioPositionDto dto = new PortfolioPositionDto();
        dto.setFigi(value.getFigi());
        dto.setTicker(sharesService.findTickerByFigiSync(value.getFigi()));
        dto.setName(sharesService.findNameByFigiSync(value.getFigi()));
        dto.setAveragePrice(value.getAveragePositionPrice().getValue());
        dto.setCurrency(value.getAveragePositionPrice().getCurrency().getDisplayName());
        dto.setQuantity(value.getQuantity().longValue());
        dto.setExpectedYield(value.getExpectedYield());

        return dto;
    }

    private List<PortfolioPositionDto> toDto(List<PortfolioPosition> values) {
        return ofNullable(values).orElseGet(Collections::emptyList)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
