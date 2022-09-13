package com.illtamer.service.currencytrading.common.bean;

import com.illtamer.service.currencytrading.common.enums.DirectionEnum;
import com.illtamer.service.currencytrading.common.exception.APIError;
import com.illtamer.service.currencytrading.common.exception.APIException;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
public class OrderRequestBean implements Validatable {

    private DirectionEnum direction;

    public BigDecimal price;

    public BigDecimal quantity;

    @Override
    public void validate() {
        if (this.direction == null) {
            throw new APIException(APIError.PARAMETER_INVALID, "direction", "direction is required.");
        }
        // price:
        if (this.price == null) {
            throw new APIException(APIError.PARAMETER_INVALID, "price", "price is required.");
        }
        this.price = this.price.setScale(2, RoundingMode.DOWN);
        if (this.price.signum() <= 0) {
            throw new APIException(APIError.PARAMETER_INVALID, "price", "price must be positive.");
        }
        // quantity:
        if (this.quantity == null) {
            throw new APIException(APIError.PARAMETER_INVALID, "quantity", "quantity is required.");
        }
        this.quantity = this.quantity.setScale(2, RoundingMode.DOWN);
        if (this.quantity.signum() <= 0) {
            throw new APIException(APIError.PARAMETER_INVALID, "quantity", "quantity must be positive.");
        }
    }

}