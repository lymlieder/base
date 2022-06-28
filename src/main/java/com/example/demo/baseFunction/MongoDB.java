package com.example.demo.baseFunction;

import com.example.demo.CommonFunction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Log4j2
@RestController
public class MongoDB {
    Gson gson = new Gson();
    ObjectMapper json = new ObjectMapper();

    static boolean innerMode = Environment.getInnerMode();

    static String outerPath = Config.getConfig("mongoDB.ServerAddress.outer"),
            innerPath1 = Config.getConfig("mongoDB.ServerAddress.Primary.inner"),
            innerPath2 = Config.getConfig("mongoDB.ServerAddress.Secondary.inner");
    static int outerPort1 = Integer.parseInt(Objects.requireNonNull(Config.getConfig("mongoDB.port.Primary.outer"))),
            outerPort2 = Integer.parseInt(Objects.requireNonNull(Config.getConfig("mongoDB.port.Secondary.outer"))),
            innerPort = Integer.parseInt(Objects.requireNonNull(Config.getConfig("mongoDB.port.inner")));

    static int port;
    public ServerAddress seed1;
    public ServerAddress seed2;
    public String username;
    public String password;
    public String ReplSetName;
    public String DEFAULT_DB;

    public MongoClient mongoClient;
    public MongoDatabase mongoDatabase;
    public MongoCollection mongoCollection;

    String databaseName = "test";
    String collectionName = "test";
    int cacheDataBaseIndex;

    int cacheTime_s = 60 * 60;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    LinkedList<Document> queue = new LinkedList<>();
    AtomicBoolean working = new AtomicBoolean(false);

    public MongoDB() {
        init(databaseName, collectionName);
    }

    public MongoDB(String collectionName) {
        init(databaseName, collectionName);
    }

    public MongoDB(String databaseName, String collectionName) {
        init(databaseName, collectionName);
    }

    void init(String databaseName, String collectionName) {
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        timeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        if (innerMode) {
            try {
                seed1 = new ServerAddress(innerPath1, innerPort);
                seed2 = new ServerAddress(innerPath2, innerPort);
            } catch (Exception e) {
                e.printStackTrace();
                seed1 = new ServerAddress(outerPath, outerPort1);
                seed2 = new ServerAddress(outerPath, outerPort2);
            }
        } else {
            seed1 = new ServerAddress(outerPath, outerPort1);
            seed2 = new ServerAddress(outerPath, outerPort2);
        }
        username = Config.getConfig("mongoDB.username");
        password = Config.getConfig("mongoDB.password");
        ReplSetName = Config.getConfig("mongoDB.ReplSetName");
        DEFAULT_DB = Config.getConfig("mongoDB.DEFAULT_DB");
        // 构建Seed列表
        List<ServerAddress> seedList = new ArrayList<>();
        seedList.add(seed1);
        seedList.add(seed2);
        // 构建鉴权信息
        List<MongoCredential> credentials = new ArrayList<>();
        credentials.add(MongoCredential.createScramSha1Credential(username, DEFAULT_DB,
                password.toCharArray()));
        // 构建操作选项，requiredReplicaSetName属性外的选项根据自己的实际需求配置，默认参数满足大多数场景
        MongoClientOptions options = MongoClientOptions.builder().requiredReplicaSetName(ReplSetName).threadsAllowedToBlockForConnectionMultiplier(5000)
                .socketTimeout(30000).maxConnectionIdleTime(6000).socketKeepAlive(true).connectionsPerHost(50).build();
        mongoClient = new MongoClient(seedList, credentials, options);
        this.databaseName = databaseName;
        this.collectionName = collectionName;
        mongoDatabase = mongoClient.getDatabase(databaseName);
        mongoCollection = mongoDatabase.getCollection(collectionName);
        cacheDataBaseIndex = Redis.DatabaseType.MONGO_DB;
    }

    public void setValue(Document dataMap) {
        if (dataMap == null || dataMap.size() < 1)
            return;
        long timestamp;
        if (dataMap.containsKey("create_time"))
            timestamp = CommonFunction.getLongFromObject(dataMap.get("create_time"));
        else
            timestamp = CommonFunction.getTimestamp();
        if (!dataMap.containsKey("_id"))
            dataMap.put("_id", (new ObjectId()).toString());
        dataMap.put("create_date", dateFormat.format(new Date(timestamp)));
        dataMap.put("create_clock", timeFormat.format(new Date(timestamp)));
        dataMap.put("create_time", CommonFunction.getTimestamp());
        mongoCollection.insertOne(dataMap);
        updateCache();
    }

    public void setValueList(List<Document> dataMap) {
        if (dataMap == null || dataMap.size() < 1)
            return;
        long timestamp = CommonFunction.getTimestamp();
        for (Document data : dataMap) {
            long time = data.containsKey("create_time") ? CommonFunction.getLongFromObject(data.get("create_time")) : timestamp;
            String _id = data.containsKey("_id") ? data.get("_id").toString() : (new ObjectId()).toString();
//            Document document = new Document(data);
            data.put("_id", _id);
            data.put("create_date", dateFormat.format(new Date(time)));
            data.put("create_clock", timeFormat.format(new Date(time)));
            data.put("create_time", time);
        }
        mongoCollection.insertMany(dataMap);
        updateCache();
    }

    public void setValue(List<Map<String, Object>> dataMap) {
        if (dataMap == null || dataMap.size() < 1)
            return;
        long timestamp = CommonFunction.getTimestamp();
        List<Document> documentList = new ArrayList<>();
        for (Map<String, Object> data : dataMap) {
            long time = data.containsKey("create_time") ? CommonFunction.getLongFromObject(data.get("create_time")) : timestamp;
            String _id = data.containsKey("_id") ? data.get("_id").toString() : (new ObjectId()).toString();
            Document document = new Document(data);
            document.put("_id", _id);
            document.put("create_date", dateFormat.format(new Date(time)));
            document.put("create_clock", timeFormat.format(new Date(time)));
            document.put("create_time", time);
            documentList.add(document);
        }
        mongoCollection.insertMany(documentList);
        updateCache();
    }

    boolean flush() {
        if (working.get())
            return false;
        try {
            if (queue.isEmpty())
                return true;
            working.set(true);
            List<Document> documentList = new LinkedList<>();
            while (true) {
                Document document = queue.poll();
                if (document == null)
                    break;
                documentList.add(document);
            }
            setValueList(documentList);
            log.info("mongo录入 " + documentList.size());
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            working.set(false);
        }
        return true;
    }

    public void setValueAsync_highFreq(Document document) {
        queue.offer(document);
        if (!flush()) {
            //500ms后收尾
            Runnable runnable = () -> {
                if (queue.size() < 1)
                    return;
                log.info("mongo尾刷开始 " + queue.size());
                log.info(queue);
                flush();
            };
            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
            service.schedule(runnable, 500, TimeUnit.MILLISECONDS);
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    static public class FilterCell {
        public String key;
        public Object value;
        @Builder.Default
        public boolean accurate = true;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    static public class SorterCell {
        public String key;
        @Builder.Default
        public boolean ascending = true;
    }

    static BasicDBObject getFilter(Set<FilterCell> searchList) {
        BasicDBObject result = new BasicDBObject();
        if (searchList != null)
            for (FilterCell filterCell : searchList)
                result.append(filterCell.key, filterCell.accurate ? filterCell.value : new BasicDBObject("$regex", filterCell.value));
        return result;
    }

    static BasicDBObject getSorter(List<SorterCell> sorterList) {
        BasicDBObject result = new BasicDBObject();
        if (sorterList != null)
            for (SorterCell sorter : sorterList)
                result.append(sorter.key, sorter.ascending ? 1 : -1);
        return result;
    }

    public List<Map<String, Object>> getValue(String key, Object value, boolean accurate, boolean withCache) {
        return getValue(key, value, accurate, null, withCache);
    }

    public List<Map<String, Object>> getValue(String key, Object value, boolean accurate, List<SorterCell> sorterList, boolean withCache) {
        return getValue(Collections.singleton(FilterCell.builder().key(key).value(value).accurate(accurate).build()), sorterList, withCache);
    }

    public List<Map<String, Object>> getValue(FilterCell filterCell, boolean withCache) {
        return getValue(filterCell, null, withCache);
    }

    public List<Map<String, Object>> getValue(FilterCell filterCell, List<SorterCell> sorterList, boolean withCache) {
        return getValue(Collections.singleton(filterCell), sorterList, withCache);
    }

    public List<Map<String, Object>> getValue(Set<FilterCell> filterSet, boolean withCache) {
        return getValue(filterSet, null, withCache);
    }

    public List<Map<String, Object>> getValue(boolean withCache) {
        return getValue(new HashSet<>(), null, withCache);
    }

    public List<Map<String, Object>> getValue(Set<FilterCell> filterSet, List<SorterCell> sorterList, boolean withCache) {
        Redis redis = new Redis(cacheDataBaseIndex, true);
        String key = getCacheHead() + "_|_filter_" + gson.toJson(filterSet) + "_|_sorter_" + gson.toJson(sorterList);
        try {
            if (withCache && redis.containKey(key))
                return json.readValue(redis.get(key), new TypeReference<>() {
                });
        } catch (Exception e) {
            e.printStackTrace();
        }

        FindIterable<Map<String, Object>> findIterable = mongoCollection.find(getFilter(filterSet));
        if (sorterList != null)//排序
            findIterable = findIterable.sort(getSorter(sorterList));
        List<Map<String, Object>> result = new ArrayList<>();
        findIterable.forEach((Consumer<? super Map<String, Object>>) result::add);
//        findIterable.forEach((Block<? super Map<String, Object>>) result::add);
        if (withCache) {
            Thread thread = new Thread(() -> {
                try {
                    redis.setAsync(key, result, cacheTime_s);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    redis.close();
                }
            });
            thread.start();
        }
        return result;
    }

    public void deleteValue(String key, Object value, boolean accurate) {
        deleteValue(FilterCell.builder().key(key).value(value).accurate(accurate).build());
    }

    public void deleteValue(FilterCell filterCell) {
        deleteValue(Collections.singleton(filterCell));
    }

    //删除符合过滤的相关记录
    public void deleteValue(Set<FilterCell> filterSet) {
        mongoCollection.deleteMany(getFilter(filterSet));
        updateCache();
    }

    //删除符合过滤的相关记录
    public boolean deleteAllValue(Set<FilterCell> filterSet) {
        DeleteResult result = mongoCollection.deleteMany(getFilter(filterSet));
        updateCache();
        return result.getDeletedCount() > 0;
    }

    public boolean updateValue(String key, Object value, boolean accurate, Map<String, Object> newData) {
        return updateValue(Collections.singleton(FilterCell.builder().key(key).value(value).accurate(accurate).build()), newData);
    }

    public boolean updateValue(FilterCell filterCell, Map<String, Object> newData) {
        return updateValue(Collections.singleton(filterCell), newData);
    }

    //更新符合筛选条件的指定项数据
    public boolean updateValue(Set<FilterCell> filterSet, Map<String, Object> newData) {
        UpdateResult result = mongoCollection.updateMany(getFilter(filterSet), getUpdateDocument(newData));
        updateCache();
        return result.getModifiedCount() > 0;
    }

    @AllArgsConstructor
    @Builder
    static public class UpdateCell {
        public Set<FilterCell> filterSet;
        public Map<String, Object> newData;
    }

    public Document getUpdateDocument(Map<String, Object> dataMap) {
        return new Document("$set", new Document(dataMap));
    }

    //更新符合筛选条件的指定项数据
    public void updateManyValues(List<UpdateCell> updateList) throws Exception {
        for (UpdateCell updateCell : updateList)
            if (mongoCollection.updateMany(getFilter(updateCell.filterSet), getUpdateDocument(updateCell.newData)).getModifiedCount() < 1)
                throw new Exception(json.writeValueAsString(updateCell.filterSet) + "更新错误");
        updateCache();
    }

    //更新所有记录指定项数据
    public boolean updateAllValue(Map<String, Object> newData) {
        UpdateResult result = mongoCollection.updateMany(getFilter(null), getUpdateDocument(newData));
        updateCache();
        return result.getModifiedCount() > 0;
    }

    public String getCacheHead() {
        return databaseName + "_|_" + collectionName;
    }

    void updateCache() {
        //清除相关redis数据
        PgSQL.updateCache(getCacheHead(), false, cacheDataBaseIndex);
    }
}