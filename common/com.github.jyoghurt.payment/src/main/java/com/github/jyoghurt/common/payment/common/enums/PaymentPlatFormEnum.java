package com.github.jyoghurt.common.payment.common.enums;

/**
 * user:dell
 * date: 2016/8/5.
 */
public enum PaymentPlatFormEnum {
    UNION_PAY("银联支付"),
    TENCENT_PAY("微信支付"),
    ALI_PAY("支付宝"),
    CASH_PAY("现金支付"),
    CARD_PAY("刷卡支付"),
    ONLY_LUCK_OR_BALANCE_PAY("仅红包或余额支付");

    private String platFormName;

    PaymentPlatFormEnum(String platFormName) {
        this.platFormName = platFormName;
    }

    public String getPlatFormName() {
        return platFormName;
    }

    public void setPlatFormName(String platFormName) {
        this.platFormName = platFormName;
    }
}
