package com.example.demo.baseFunction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;
import org.springframework.lang.Nullable;

import java.sql.*;
import java.util.*;

@Log4j2
public class PgSQL {
    //因不喜欢myBatis风格，手写一款
    Gson gson = new Gson();
    ObjectMapper json = new ObjectMapper();

    static Connection conn;

    static boolean testMode = Environment.getTestMode();
    static String className = Config.getConfig("pgSQL.className");
    static String
            urlOuter = Config.getConfig(testMode ? "pgSQL.url.test.outer" : "pgSQL.url.formal.outer"),
            urlInner = Config.getConfig(testMode ? "pgSQL.url.test.inner" : "pgSQL.url.formal.inner");
    static String user = Config.getConfig("pgSQL.user");
    static String password = Config.getConfig("pgSQL.password");

    static int cacheTime_s = 3600;//缓存保存时，秒

    public PgSQL() {
        cacheTime_s = Integer.parseInt(Objects.requireNonNull(Config.getConfig("pgSQL.cacheTime_s")));
        connect();
    }

    //连接数据库
    void connect() {
        try {
            Class.forName(className);
            if (Environment.getInnerMode())
                try {
                    conn = DriverManager.getConnection(urlInner, user, password);
                    return;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            conn = DriverManager.getConnection(urlOuter, user, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //连接数据库
    void reconnect() {
        try {
            if (!conn.isClosed())
                return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        connect();
    }

    //为PreparedStatement安全插入参数，防止注入
    void insertParamSafety(PreparedStatement preparedStatement, @Nullable Object[] list) {
        if (list == null)
            return;
        try {
            for (int i = 0; i < list.length; i++) {
                Object param = list[i];
                int index = i + 1;
                String paramString = param.toString();
                if (String.class.equals(param.getClass())) {
                    preparedStatement.setString(index, paramString);
                } else if (Integer.class.equals(param.getClass())) {
                    preparedStatement.setInt(index, Integer.parseInt(paramString));
                } else if (Long.class.equals(param.getClass())) {
                    preparedStatement.setLong(index, Long.parseLong(paramString));
                } else if (Double.class.equals(param.getClass())) {
                    preparedStatement.setDouble(index, Double.parseDouble(paramString));
                } else if (Boolean.class.equals(param.getClass())) {
                    preparedStatement.setBoolean(index, Boolean.parseBoolean(paramString));
                } else {
                    preparedStatement.setObject(index, param);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //为PreparedStatement安全插入参数，防止注入 batch模式
    void insertParamSafetyBatch(PreparedStatement preparedStatement, @Nullable List<Object[]> list) {
        if (list == null)
            return;
        try {
            for (Object[] paramArray : list) {
                insertParamSafety(preparedStatement, paramArray);
                preparedStatement.addBatch();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void transDatasetToList(List<Map<String, Object>> result, ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (resultSet.next()) {
            Map<String, Object> rowData = new HashMap<>();
            for (int i = 1; i <= columnCount; i++)
                rowData.put(metaData.getColumnName(i), resultSet.getObject(i));
            result.add(rowData);
        }
    }

    /**
     * 安全模式 通过PreparedStatement带参数的select，安全模式，参数通过paramList，防止注入
     *
     * @param sql       参数用?代替的PreparedStatement模式SQL
     * @param paramList 替代?的参数列表
     * @param withCache 是否加入缓存机制
     * @return 返回结果
     */
    public List<Map<String, Object>> select(String sql, Object[] paramList, boolean withCache) {
        return select(sql, paramList, withCache, Redis.DatabaseType.WECHAT_SQL, sql + gson.toJson(paramList));
    }

    public List<Map<String, Object>> selectActivitySeat(String sql, Object[] paramList, boolean withCache, String keyWord) {
        return select(sql, paramList, withCache, Redis.DatabaseType.ACTIVITY_SEAT_MAP, keyWord);
    }

    public List<Map<String, Object>> select(String sql, Object[] paramList, boolean withCache, int type, String keyWord) {
        //判断缓存中是否存在
        Redis sqlRedis = new Redis(type, true);
        try {
            if (withCache && sqlRedis.containKey(keyWord))
                return json.readValue(sqlRedis.get(keyWord), new TypeReference<>() {
                });
        } catch (Exception e) {
            log.warn(e);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        reconnect();
        try (PreparedStatement stat = conn.prepareStatement(sql)) {
            insertParamSafety(stat, paramList);
            ResultSet rs = stat.executeQuery();
            transDatasetToList(result, rs);
            //不管是否使用redis，均刷新
            if (withCache) {
                Thread thread = new Thread(() -> {
                    sqlRedis.setAsync(keyWord, result, cacheTime_s);
                    sqlRedis.close();
                });
                thread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    /**
     * 非安全模式 通过Statement方式select，无法防止注入，只能用于内部select
     *
     * @param sql       sql
     * @param withCache 是否加入缓存机制
     * @return 返回结果
     */
    public List<Map<String, Object>> selectUnsafe(String sql, boolean withCache) {
        Redis redis = new Redis(Redis.DatabaseType.WECHAT_SQL, true);
        //判断缓存中是否存在
        try {
            if (withCache && redis.containKey(sql))
                return json.readValue(redis.get(sql), new TypeReference<>() {
                });
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        reconnect();
        try (Statement stat = conn.createStatement()) {
            ResultSet rs = stat.executeQuery(sql);
            transDatasetToList(result, rs);
            if (withCache)
                redis.setAsync(sql, result, cacheTime_s);
        } catch (Exception e) {
            log.error("sql selectUnsafe", e);
            e.printStackTrace();
        }
        return result;
    }

    static public void updateCache(String tableName, boolean hardAsyncCache, int type) {
        updateCache(tableName, hardAsyncCache, new Redis(type, true));
    }

    static public void updateCache(String tableName, boolean hardAsyncCache, Redis redis) {
        if (hardAsyncCache) {
            redis.cleanAllData();
            return;
        }
        Thread t = new Thread(() -> {
            Set<String> keys = redis.getAllKeys();
            for (String key : keys)
                if (key.contains(tableName))
                    redis.remove(key);
        });
        t.start();
    }

    /**
     * 安全模式 通过PreparedStatement带参数的update, insert, remove等，安全模式，参数通过paramList，防止注入
     *
     * @param sql             参数用?代替的PreparedStatement模式SQL
     * @param paramList       替代?的参数列表
     * @param tableName       更新涉及的表名，用于清理缓存，可以不包括shame
     * @param refreshAllCache 是否立即同步(清除)所有缓存，默认选false
     * @return 返回结果
     */
    public int update(String sql, Object[] paramList, String tableName, boolean refreshAllCache) {
        return update(sql, paramList, tableName, refreshAllCache, Redis.DatabaseType.WECHAT_SQL);
    }

    public int updateActivitySeat(String sql, Object[] paramList, String tableName, boolean refreshAllCache) {
        return update(sql, paramList, tableName, refreshAllCache, Redis.DatabaseType.ACTIVITY_SEAT_MAP);
    }

    public int update(String sql, Object[] paramList, String tableName, boolean refreshAllCache, int type) {
        int result = -1;

        reconnect();
        try (PreparedStatement pStmt = conn.prepareStatement(sql)) {
            insertParamSafety(pStmt, paramList);
            result = pStmt.executeUpdate();
            updateCache(tableName, refreshAllCache, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 安全模式 通过PreparedStatement带参数的update, insert, remove等，安全模式，参数通过paramList，防止注入
     *
     * @param sql            参数用?代替的PreparedStatement模式SQL
     * @param paramList      替代?的参数列表
     * @param tableName      更新涉及的表名，用于清理缓存，可以不包括shame
     * @param hardAsyncCache 是否立即同步(清除)所有缓存，默认选false
     * @return 返回结果
     */
    public int[] updateBatch(String sql, List<Object[]> paramList, String tableName, boolean hardAsyncCache) {
        return updateBatch(sql, paramList, tableName, hardAsyncCache, Redis.DatabaseType.WECHAT_SQL);
    }

    public int[] updateBatch(String sql, List<Object[]> paramList, String tableName, boolean hardAsyncCache, int type) {
        int[] result = null;

        reconnect();
        try (PreparedStatement pStmt = conn.prepareStatement(sql)) {
            insertParamSafetyBatch(pStmt, paramList);
            result = pStmt.executeBatch();
            updateCache(tableName, hardAsyncCache, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    protected void finalize() throws SQLException {
        conn.close();
    }
}