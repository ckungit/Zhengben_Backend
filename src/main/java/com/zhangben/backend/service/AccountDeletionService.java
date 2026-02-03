package com.zhangben.backend.service;

/**
 * V40: Account Deletion Service Interface
 * GDPR compliant - implements "Right to be Forgotten"
 */
public interface AccountDeletionService {

    /**
     * Permanently delete user account and all associated data
     * This is a GDPR-compliant physical deletion
     *
     * @param userId The user ID to delete
     */
    void deleteAccountPermanently(Integer userId);
}
