package com.example.demo.baseFunction;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClientPool {
//    static public Redis authRedis;// = new Redis(Redis.DatabaseType.LOGIN);
//    static public Redis sqlRedis;// = new Redis(Redis.DatabaseType.SQL);
//    static public Redis seatMapRedis;
//    static public Redis MongoRedis;
//    static public Redis defaultRedis = new Redis(Redis.DatabaseType.DEFAULT);

    static public OSSClient ossClient = new OSSClient();

    public ClientPool() {
//        authRedis = new Redis(Redis.DatabaseType.WECHAT_LOGIN);
//        sqlRedis = new Redis(Redis.DatabaseType.WECHAT_SQL);
//        seatMapRedis = new Redis(Redis.DatabaseType.ACTIVITY_SEAT_MAP);
//        MongoRedis = new Redis(Redis.DatabaseType.MONGO_DB);
    }
}
