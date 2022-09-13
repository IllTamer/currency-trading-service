package com.illtamer.service.currencytrading.common.support;

import com.illtamer.service.currencytrading.common.database.DBTemplate;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractDBService {

    @Autowired
    protected DBTemplate db;

}
