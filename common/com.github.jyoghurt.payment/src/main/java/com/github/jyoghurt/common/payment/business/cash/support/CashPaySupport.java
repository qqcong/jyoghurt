package com.github.jyoghurt.common.payment.business.cash.support;

import com.github.jyoghurt.common.payment.common.domain.PaymentRecordsT;
import com.github.jyoghurt.common.payment.common.service.PaymentRecordsService;
import com.github.jyoghurt.common.payment.common.support.BasePaymentSupport;
import com.github.jyoghurt.common.payment.common.enums.PaymentCloseEnum;
import com.github.jyoghurt.common.payment.common.enums.PaymentRefundEnum;
import com.github.jyoghurt.common.payment.common.exception.PaymentPreviousErrorException;
import com.github.jyoghurt.common.payment.common.module.RefundRequest;
import com.github.jyoghurt.core.exception.BaseErrorException;
import com.github.jyoghurt.common.payment.common.enums.PaymentGatewayEnum;
import com.github.jyoghurt.common.payment.common.enums.PaymentStateEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * user:dell
 * data:2016/4/27.
 */
@Component
public class CashPaySupport extends BasePaymentSupport {
    @Autowired
    private PaymentRecordsService paymentRecordsService;

    @Override
    public Boolean createPreviousOrder(PaymentRecordsT paymentRecordsT) throws PaymentPreviousErrorException {
        if (null == paymentRecordsT || null == paymentRecordsT.getPaymentState()) {
            throw new BaseErrorException("现金支付请求预支付异常,支付记录为空");
        }
        paymentRecordsT.setPaymentMethod(PaymentGatewayEnum.CASH_PAY);
        paymentRecordsService.previousPayment(PaymentGatewayEnum.CASH_PAY,paymentRecordsT.getPaymentId());
        return true;
    }

    @Override
    public PaymentStateEnum queryPaymentResult(PaymentRecordsT paymentRecordsT) {
        if (null == paymentRecordsT || null == paymentRecordsT.getPaymentState()) {
            throw new BaseErrorException("现金支付请求预支付异常,支付记录为空");
        }
        if (null == paymentRecordsT.getPaymentState()) {
            throw new BaseErrorException("现金支付请求预支付异常,支付状态为空，支付记录Id:{0}", paymentRecordsT.getPaymentId());
        }
        if (paymentRecordsT.getPaymentState()) {
            return PaymentStateEnum.SUCCESS;
        }
        return PaymentStateEnum.NOTPAY;
    }

    @Override
    public PaymentCloseEnum closePayment(PaymentRecordsT paymentRecordsT) {
        return null;
    }

    @Override
    public PaymentRefundEnum refundPayment(RefundRequest refundRequest, PaymentRecordsT paymentRecordsT) {
        return PaymentRefundEnum.SUCCESS;
    }
}
