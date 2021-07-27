/**
 * abssqr.com Inc. Copyright (c) 2017-2021 All Rights Reserved.
 */
package cn.dalgen.mybatis.gen.enums;

import org.apache.commons.lang.StringUtils;

/**
 * @author bangis.wangdf
 * @version cn.dalgen.mybatis.gen.enums: ResultSetTypeEnum.java, v 0.1 2021-05-19 下午2:21 bangis.wangdf Exp $
 */
public enum ResultSetTypeEnum {
    FORWARD_ONLY("FORWARD_ONLY"),
    SCROLL_INSENSITIVE("SCROLL_INSENSITIVE"),
    SCROLL_SENSITIVE("SCROLL_SENSITIVE"),
    DEFAULT("DEFAULT");

    private String code;

    ResultSetTypeEnum(String code) {
        this.code = code;
    }

    /**
     * Gets by code.
     *
     * @param code the code
     * @return the by code
     */
    public static ResultSetTypeEnum getByCode(String code) {
        for (ResultSetTypeEnum typeEnum : ResultSetTypeEnum.values()) {
            if (StringUtils.equals(code, typeEnum.code)) {
                return typeEnum;
            }
        }
        return ResultSetTypeEnum.DEFAULT;
    }

    /**
     * Gets code.
     *
     * @return the code
     */
    public String getCode() {
        return code;
    }
}
