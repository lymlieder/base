package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CommonGet {
    static Gson gson = new Gson();

    enum SearchType {Accurate, Vague, Range}

    /**
     * 为get语句添加权限，搜索后缀
     *
     * @param token         用户token
     * @param search_data   搜索数据
     * @param allowSet      过滤key
     * @param regionKeyword region key
     * @param paramList     param list 引用
     * @return sql后缀
     */
    static public String sqlAdditionForGet(String token, String search_data, Set<String> allowSet, String regionKeyword, List<Object> paramList, boolean withWhere, boolean withAnd) {
        String sql_string = "";

        List<String> middleSql = new ArrayList<>();

        //search data 处理
        if (search_data != null) {
            Map<SearchType, Map<String, Object>> searchMap = gson.fromJson(search_data, new TypeToken<Map<SearchType, Map<String, Object>>>() {
            }.getType());
            if (searchMap.containsKey(SearchType.Accurate)) {//精确查找部分
                Map<String, Object> subSearchMap = searchMap.get(SearchType.Accurate).entrySet().stream().filter(s -> allowSet.contains(s.getKey()) && s.getValue() != null && s.getValue().toString().length() > 0).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                if (subSearchMap.containsKey("region") && regionKeyword != null && regionKeyword.length() > 0) {
                    middleSql.add(" " + regionKeyword + " @> ?::jsonb");
                    paramList.add(subSearchMap.get("region"));
                }
                subSearchMap.remove("region");
                for (Map.Entry<String, Object> search : subSearchMap.entrySet())
                    if (search.getKey().length() > 0) {
                        middleSql.add(" " + search.getKey() + " = ?");
                        paramList.add(search.getValue());
                    }
            }
            if (searchMap.containsKey(SearchType.Vague)) {//模糊查找部分
                Map<String, String> subSearchMap = searchMap.get(SearchType.Vague).entrySet().stream().filter(s -> allowSet.contains(s.getKey()) && s.getValue() != null && s.getValue().toString().length() > 0).collect(Collectors.toMap(Map.Entry::getKey, s -> s.getValue().toString()));
                subSearchMap.remove("region");
                List<String> vagueList = new ArrayList<>();
                for (Map.Entry<String, String> search : subSearchMap.entrySet())
                    if (search.getKey().length() > 0) {
                        vagueList.add(" " + search.getKey() + " ilike ? ");
                        paramList.add("%" + search.getValue() + "%");
                    }
                if (vagueList.size() > 0)
                    middleSql.add("(" + String.join(" or ", vagueList) + ")");
            }
            if (searchMap.containsKey(SearchType.Range)) {//模糊查找部分
                Map<String, Map<String, Long>> subMap = gson.fromJson(gson.toJson(searchMap.get(SearchType.Range)), new TypeToken<Map<String, Map<String, Long>>>() {
                }.getType());
                Map<String, Map<String, Long>> subSearchMap = subMap.entrySet().stream().filter(s -> allowSet.contains(s.getKey()) && s.getValue() != null && s.getValue().size() > 0 && (s.getValue().get("start") != null || s.getValue().get("end") != null)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                subSearchMap.remove("region");
                for (Map.Entry<String, Map<String, Long>> search : subSearchMap.entrySet()) {
                    List<String> vagueList = new ArrayList<>();
                    if (search.getValue().containsKey("start") && search.getValue().get("start") >= 0) {
                        vagueList.add(search.getKey() + " >= ?");
                        paramList.add(search.getValue().get("start"));
                    }
                    if (search.getValue().containsKey("end") && search.getValue().get("end") >= 0) {
                        vagueList.add(search.getKey() + " <= ?");
                        paramList.add(search.getValue().get("end"));
                    }
                    if (vagueList.size() > 0)
                        middleSql.add("(" + String.join(" and ", vagueList) + ")");
                }
            }
        }
        if (middleSql.size() < 1)
            return "";
        sql_string += String.join(" and ", middleSql);
        if (withWhere)
            sql_string = "where " + sql_string;
        else if (withAnd)
            sql_string = "and " + sql_string;
        return " " + sql_string + " ";
    }
}
