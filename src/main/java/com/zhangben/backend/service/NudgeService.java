package com.zhangben.backend.service;

/**
 * V41: 催促还账服务接口
 */
public interface NudgeService {

    /**
     * 发送催促提醒
     * @param creditorId 债权人ID（当前用户）
     * @param debtorId 债务人ID（被催促的人）
     * @return 催促结果
     */
    NudgeResult sendNudge(Integer creditorId, Integer debtorId);

    /**
     * 检查是否可以催促（24小时限制）
     * @param creditorId 债权人ID
     * @param debtorId 债务人ID
     * @return true 如果可以催促
     */
    boolean canNudge(Integer creditorId, Integer debtorId);

    /**
     * 获取下次可催促的时间（毫秒时间戳）
     * @param creditorId 债权人ID
     * @param debtorId 债务人ID
     * @return 下次可催促的时间戳，如果可以立即催促则返回0
     */
    long getNextNudgeTime(Integer creditorId, Integer debtorId);

    /**
     * 催促结果
     */
    class NudgeResult {
        private boolean success;
        private String message;
        private long nextNudgeTime; // 下次可催促的时间戳

        public static NudgeResult success() {
            NudgeResult result = new NudgeResult();
            result.success = true;
            result.message = "OK";
            result.nextNudgeTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000;
            return result;
        }

        public static NudgeResult rateLimited(long nextTime) {
            NudgeResult result = new NudgeResult();
            result.success = false;
            result.message = "RATE_LIMITED";
            result.nextNudgeTime = nextTime;
            return result;
        }

        public static NudgeResult error(String message) {
            NudgeResult result = new NudgeResult();
            result.success = false;
            result.message = message;
            return result;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public long getNextNudgeTime() {
            return nextNudgeTime;
        }

        public void setNextNudgeTime(long nextNudgeTime) {
            this.nextNudgeTime = nextNudgeTime;
        }
    }
}
