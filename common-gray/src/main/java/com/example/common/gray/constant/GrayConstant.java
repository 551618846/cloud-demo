package com.example.common.gray.constant;

/**
 * 灰度相关常量
 */
public final class GrayConstant {

    private GrayConstant() {
    }

    /** 灰度版本请求头 */
    public static final String GRAY_VERSION_HEADER = "X-Gray-Version";

    /** 灰度用户ID请求头 */
    public static final String GRAY_USER_ID_HEADER = "X-Gray-UserId";

    /** 灰度标签请求头 */
    public static final String GRAY_TAG_HEADER = "X-Gray-Tag";

    /** 灰度规则请求头 */
    public static final String GRAY_RULE_HEADER = "X-Gray-Rule";

    /** Nacos元数据中的版本key */
    public static final String GRAY_VERSION_METADATA_KEY = "version";
}
