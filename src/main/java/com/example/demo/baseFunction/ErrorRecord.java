package com.example.demo.baseFunction;

import com.example.demo.CommonFunction;
import lombok.extern.log4j.Log4j2;

@Log4j2
//错误记录
public class ErrorRecord {
    static private final PgSQL sql = new PgSQL();

    static public boolean setMemberError(String data) {
        return setMemberError(data, "");
    }

    static public boolean setMemberError(String data, String comment) {
        return setError("member", data, comment);
    }

    static public boolean setError(String errorType, String data, String comment) {
        log.error(errorType, data);
        int result = sql.update("insert into error.error " +
                "(error_type, error, error_comment, error_create_time, error_status) values " +
                "(?, ?, ?, ?, ?);", new Object[]{errorType, data, comment, CommonFunction.getTimestamp(), 0}, ".error ", false);
        return result > 0;
    }
}
