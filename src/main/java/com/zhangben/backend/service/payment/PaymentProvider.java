package com.zhangben.backend.service.payment;

import java.math.BigDecimal;
import java.util.Map;

/**
 * V42: 支付提供商接口
 * 预留接口，支持未来接入多种支付方式
 * 当前 ENABLE_BILLING=false，暂不启用
 */
public interface PaymentProvider {

    /**
     * 获取提供商名称
     */
    String getProviderName();

    /**
     * 检查是否可用
     */
    boolean isAvailable();

    /**
     * 创建支付订单
     *
     * @param userId       用户 ID
     * @param amount       金额
     * @param currency     货币代码 (JPY, USD, CNY 等)
     * @param description  订单描述
     * @param metadata     额外元数据
     * @return 支付信息（包含支付链接或二维码等）
     */
    PaymentResult createPayment(
        Integer userId,
        BigDecimal amount,
        String currency,
        String description,
        Map<String, String> metadata
    );

    /**
     * 查询支付状态
     *
     * @param paymentId 支付 ID
     * @return 支付结果
     */
    PaymentResult queryPayment(String paymentId);

    /**
     * 取消支付
     *
     * @param paymentId 支付 ID
     * @return 是否成功
     */
    boolean cancelPayment(String paymentId);

    /**
     * 处理支付回调
     *
     * @param payload 回调数据
     * @return 处理结果
     */
    PaymentResult handleCallback(String payload);

    /**
     * 支付结果
     */
    class PaymentResult {
        private boolean success;
        private String paymentId;
        private String status;       // PENDING, PAID, FAILED, CANCELLED
        private String paymentUrl;   // 支付链接
        private String qrCodeUrl;    // 二维码链接（如有）
        private String errorMessage;
        private Map<String, Object> extra;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(String paymentId) {
            this.paymentId = paymentId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getPaymentUrl() {
            return paymentUrl;
        }

        public void setPaymentUrl(String paymentUrl) {
            this.paymentUrl = paymentUrl;
        }

        public String getQrCodeUrl() {
            return qrCodeUrl;
        }

        public void setQrCodeUrl(String qrCodeUrl) {
            this.qrCodeUrl = qrCodeUrl;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public Map<String, Object> getExtra() {
            return extra;
        }

        public void setExtra(Map<String, Object> extra) {
            this.extra = extra;
        }

        public static PaymentResult success(String paymentId, String paymentUrl) {
            PaymentResult result = new PaymentResult();
            result.setSuccess(true);
            result.setPaymentId(paymentId);
            result.setPaymentUrl(paymentUrl);
            result.setStatus("PENDING");
            return result;
        }

        public static PaymentResult failure(String errorMessage) {
            PaymentResult result = new PaymentResult();
            result.setSuccess(false);
            result.setErrorMessage(errorMessage);
            result.setStatus("FAILED");
            return result;
        }
    }
}
