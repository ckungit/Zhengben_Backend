package com.zhangben.backend.service;

public interface UserProfileService {

    void updatePassword(Integer userId, String oldPwd, String newPwd);

    void updateName(Integer userId, String firstname, String secondname);

    void updateNickname(Integer userId, String nickname);

    void updatePayMethods(Integer userId,
                          Byte paypayFlag, String paypayAccount,
                          Byte bankFlag, String bankName, String bankBranch, String bankAccount);
}