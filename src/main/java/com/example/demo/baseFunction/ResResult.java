package com.example.demo.baseFunction;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.*;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.service.ResponseMessage;

import java.util.ArrayList;
import java.util.List;

//public record ResResult(Integer code, boolean status, Object data, String message) { }

@Data
@Builder
@NoArgsConstructor
//@AllArgsConstructor
@ApiModel("返回结果")
public class ResResult {
    @Builder.Default
    @ApiModelProperty(value = "状态码", example = "0")
    private Integer code = 0;
    @Builder.Default
    @ApiModelProperty(value = "状态信息", example = "成功")
    private String message = "成功";
    @Builder.Default
    @ApiModelProperty(value = "数据")
    private Object data = "";
    @Builder.Default
    @ApiModelProperty(value = "注释")
    private String comment = "";

    public ResResult(Integer code, String message, Object data, String comment) {
        this.code = code;
        this.message = message;
        this.data = data == null ? "" : data;
        this.comment = comment;
    }

    public ResResult(ResultCode resultCode) {
        code = resultCode.code;
        message = resultCode.message;
    }

    public void setSuccess(Object data) {
        setSuccess(data, "");
    }

    public void setSuccess(Object data, String comment) {
        code = ResultCode.SUCCESS.code;
        message = ResultCode.SUCCESS.message;
        this.data = data;
        this.comment = comment;
    }

    public void setFail(ResultCode resultCode) {
        code = resultCode.code;
        message = resultCode.message;
        data = "";
        comment = "";
    }

    public void setFail(ResultCode resultCode, String reason) {
        code = resultCode.code;
        message = resultCode.message;
        data = "";
        comment = reason;
    }

    public void setFail(Object result, ResultCode resultCode) {
        code = resultCode.code;
        message = resultCode.message;
        data = result;
        comment = "";
    }

    public void flush(ResResult resResult) {
        code = resResult.code;
        message = resResult.message;
        data = resResult.data;
        comment = resResult.comment;
    }

    static public ResResult success() {
        return success(null);
    }

    static public ResResult success(Object data) {
        return success(data, "");
    }

    static public ResResult success(Object data, String comment) {
        return new ResResult(ResultCode.SUCCESS.code, ResultCode.SUCCESS.message, data, comment);
    }

    static public ResResult fail(ResultCode resultCode) {
        return fail(resultCode, "");
    }

    static public ResResult fail(ResultCode resultCode, String comment) {
        return fail(resultCode, comment, "");
    }

    static public ResResult fail(ResultCode resultCode, String comment, Object data) {
        return new ResResult(resultCode.code, resultCode.message, data, comment);
    }

    public enum ResultCode {


        SUCCESS(0, "成功"),
        PARAM_INVALID(1001, "参数无效"),
        PARAM_TYPE_ERROR(1003, "参数类型错误"),
        PARAM_UNCOMPLETED(1004, "参数缺失"),
        USER_NOT_EXISTED(2001, "账号不存在或密码错误"),
        USER_NOT_LOGIN(2002, "账号不存在或未登录"),
        USER_NOT_AUTH(2003, "账号无权限"),
        USER_FORBIDDEN(2004, "账号禁用"),
        USER_OFFSITE(2005, "异地登陆"),
        USER_CROSS(2006, "账号交叉"),
        TOKEN_NOT_EXISTED(3001, "token不存在"),
        OPERATE_FAIL(4001, "操作失败"),
        THIRD_FAIL(5001, "第三方报错"),
        INNER_FAIL(6001, "服务报错"),
        UNDEFINED(10001, "未定义");

        private final int code;
        private final String message;

        ResultCode(Integer code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}

