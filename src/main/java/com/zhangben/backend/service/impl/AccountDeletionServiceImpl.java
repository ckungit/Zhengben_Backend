package com.zhangben.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.mapper.*;
import com.zhangben.backend.model.*;
import com.zhangben.backend.service.AccountDeletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * V40: Account Deletion Service Implementation
 * GDPR compliant - implements "Right to be Forgotten"
 *
 * Uses soft delete + data anonymization approach:
 * - User record is kept for referential integrity (other users' bills/activities)
 * - All PII (Personal Identifiable Information) is cleared
 * - User is marked as deleted with is_deleted=1
 * - Sensitive data (payment methods, tokens) is physically deleted
 */
@Service
public class AccountDeletionServiceImpl implements AccountDeletionService {

    private static final Logger logger = LoggerFactory.getLogger(AccountDeletionServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FavoredUserMapper favoredUserMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ActivityMemberMapper activityMemberMapper;

    @Autowired
    private ActivityInvitationMapper activityInvitationMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserPaymentMethodMapper userPaymentMethodMapper;

    @Autowired
    private PasswordResetTokenMapper passwordResetTokenMapper;

    @Autowired
    private InviteLinkMapper inviteLinkMapper;

    @Override
    @Transactional
    public void deleteAccountPermanently(Integer userId) {
        logger.info("Starting GDPR account deletion (anonymization) for user: {}", userId);

        try {
            // 1. Delete sensitive data that should be physically removed

            // Delete user's payment methods (sensitive financial data)
            userPaymentMethodMapper.deleteAllByUserId(userId);
            logger.debug("Deleted payment methods for user: {}", userId);

            // Delete notifications (personal communications)
            notificationMapper.deleteByUserId(userId);
            logger.debug("Deleted notifications for user: {}", userId);

            // Delete password reset tokens (security data)
            passwordResetTokenMapper.deleteAllByUserId(userId);
            logger.debug("Deleted password reset tokens for user: {}", userId);

            // 2. Delete relationship data

            // Delete favored users (friend relationships - both directions)
            FavoredUserExample favExample1 = new FavoredUserExample();
            favExample1.createCriteria().andUserIdEqualTo(userId);
            favoredUserMapper.deleteByExample(favExample1);

            FavoredUserExample favExample2 = new FavoredUserExample();
            favExample2.createCriteria().andFavoredUserIdEqualTo(userId);
            favoredUserMapper.deleteByExample(favExample2);
            logger.debug("Deleted friend relationships for user: {}", userId);

            // Delete pending activity invitations (both sent and received)
            activityInvitationMapper.deleteByUser(userId);
            logger.debug("Deleted activity invitations for user: {}", userId);

            // Delete invite links created by user
            inviteLinkMapper.deleteByCreatorId(userId);
            logger.debug("Deleted invite links for user: {}", userId);

            // 3. Handle activity ownership transfer
            List<Activity> userActivities = activityMapper.selectByCreatorId(userId);

            for (Activity activity : userActivities) {
                // Check if there are other members
                Map<String, Object> otherMember = activityMemberMapper.selectFirstOtherMember(activity.getId(), userId);

                if (otherMember != null) {
                    // Transfer ownership to another member
                    Integer newOwnerId = (Integer) otherMember.get("user_id");
                    activityMapper.updateCreatorId(activity.getId(), newOwnerId);
                    logger.debug("Transferred activity {} ownership to user {}", activity.getId(), newOwnerId);
                }
                // If no other members, the activity stays with the deleted user
                // The UI will show "已注销用户" as the creator
            }

            // 4. Remove user from activity memberships
            // Note: We keep outcome records intact - they reference this user ID
            // The anonymized user record ensures JOIN queries still work
            activityMemberMapper.deleteByUserId(userId);
            logger.debug("Deleted activity memberships for user: {}", userId);

            // 5. Anonymize user data (soft delete)
            // This clears all PII but keeps the record for referential integrity
            userMapper.anonymizeUser(userId);
            logger.info("GDPR account anonymization completed for user: {}", userId);

            // 6. Logout and destroy session
            if (StpUtil.isLogin()) {
                StpUtil.logout();
            }

        } catch (Exception e) {
            logger.error("Error during account deletion for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Account deletion failed: " + e.getMessage(), e);
        }
    }
}
