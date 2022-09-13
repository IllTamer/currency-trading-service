import com.illtamer.service.currencytrading.common.enums.DirectionEnum;
import com.illtamer.service.currencytrading.common.enums.OrderStatus;
import com.illtamer.service.currencytrading.common.model.trade.OrderEntity;
import com.illtamer.service.currencytrading.enging.match.MatchDetailRecord;
import com.illtamer.service.currencytrading.enging.match.MatchService;
import com.illtamer.service.currencytrading.enging.match.MatchResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class MarketEngineTest {

    static Long USER_A = 12345L;
    long sequenceId = 0;
    MatchService engine;

    @BeforeEach
    void init() {
        this.engine = new MatchService();
    }

    @Test
    void processOrders() {
        List<OrderEntity> orders = List.of( //
                createOrder(DirectionEnum.BUY, "12300.21", "1.02"), // 0
                createOrder(DirectionEnum.BUY, "12305.39", "0.33"), // 1
                createOrder(DirectionEnum.SELL, "12305.39", "0.11"), // 2
                createOrder(DirectionEnum.SELL, "12300.01", "0.33"), // 3
                createOrder(DirectionEnum.SELL, "12400.00", "0.10"), // 4
                createOrder(DirectionEnum.SELL, "12400.00", "0.20"), // 5
                createOrder(DirectionEnum.SELL, "12390.00", "0.15"), // 6
                createOrder(DirectionEnum.BUY, "12400.01", "0.55"), // 7
                createOrder(DirectionEnum.BUY, "12300.00", "0.77")); // 8
        List<MatchDetailRecord> matches = new ArrayList<>();
        for (OrderEntity order : orders) {
            MatchResult mr = this.engine.processOrder(order.getSequenceId(), order);
            matches.addAll(mr.getMatchDetails());
        }
        assertArrayEquals(new MatchDetailRecord[] { //
                new MatchDetailRecord(parseBD("12305.39"), parseBD("0.11"), orders.get(2), orders.get(1)), //
                new MatchDetailRecord(parseBD("12305.39"), parseBD("0.22"), orders.get(3), orders.get(1)), //
                new MatchDetailRecord(parseBD("12300.21"), parseBD("0.11"), orders.get(3), orders.get(0)), //
                new MatchDetailRecord(parseBD("12390.00"), parseBD("0.15"), orders.get(7), orders.get(6)), //
                new MatchDetailRecord(parseBD("12400.00"), parseBD("0.10"), orders.get(7), orders.get(4)), //
                new MatchDetailRecord(parseBD("12400.00"), parseBD("0.20"), orders.get(7), orders.get(5)), //
        }, matches.toArray(MatchDetailRecord[]::new));
        Assertions.assertEquals(0, parseBD("12400.00").compareTo(engine.getMarketPrice()));
    }

    OrderEntity createOrder(DirectionEnum direction, String price, String quantity) {
        this.sequenceId++;
        return OrderEntity.builder()
                .id(this.sequenceId << 4)
                .sequenceId(this.sequenceId)
                .direction(direction)
                .price(parseBD(price))
                .unfilledQuantity(parseBD(quantity))
                .quantity(parseBD(quantity))
                .status(OrderStatus.PENDING)
                .userId(USER_A)
                .createdAt(1234567890000L + this.sequenceId)
                .updatedAt(1234567890000L + this.sequenceId)
                .build();
    }

    BigDecimal parseBD(String s) {
        return new BigDecimal(s);
    }

}
