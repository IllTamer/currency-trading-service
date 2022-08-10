# currency-trading-service
基于 spring-cloud 的证券交易所。

## Install

### docker

本应用使用 docker-compose 创建服务，其 [[配置文件]](docker-compose.yml) 中配置的服务列表如下(开放端口均为标准端口)：

- zookeeper
- kafka
- redis
- mysql

    mysql 服务将使用 [[schema.sql]](sql/schema.sql) 中的语句对数据库进行初始化
    
    其初始账号密码均为 `root`

## 项目结构

- common - 公共代码；
- config - 配置服务器；
- push - 推送服务；
- quotation - 行情服务；
- trading-api - 交易API服务；
- trading-engine - 交易引擎；
- trading-sequencer - 定序服务；
- ui - 用户Web界面。
