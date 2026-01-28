package com.zhangben.backend.service.impl;

import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.model.User;
import com.zhangben.backend.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Override
    public void updatePassword(Integer userId, String oldPwd, String newPwd) {

        User u = userMapper.selectByPrimaryKey(userId);

        if (!encoder.matches(oldPwd, u.getPassword())) {
            throw new IllegalArgumentException("旧密码错误");
        }

        if (newPwd.length() < 8) {
            throw new IllegalArgumentException("新密码至少8位");
        }

        u.setPassword(encoder.encode(newPwd));
        userMapper.updateByPrimaryKeySelective(u);
    }

    @Override
    public void updateName(Integer userId, String firstname, String secondname) {

        User u = new User();
        u.setId(userId);
        u.setFirstname(firstname);
        u.setSecondname(secondname);

        userMapper.updateByPrimaryKeySelective(u);
    }

    @Override
    public void updateNickname(Integer userId, String nickname) {

        User u = new User();
        u.setId(userId);
        u.setNickname(nickname);

        userMapper.updateByPrimaryKeySelective(u);
    }

    @Override
    public void updatePayMethods(Integer userId,
                                 Byte paypayFlag, String paypayAccount,
                                 Byte bankFlag, String bankName, String bankBranch, String bankAccount) {

        User u = new User();
        u.setId(userId);
        u.setPaypayFlag(paypayFlag);
        u.setPaypayAccount(paypayAccount);
        u.setBankFlag(bankFlag);
        u.setBankName(bankName);
        u.setBankBranch(bankBranch);
        u.setBankAccount(bankAccount);

        userMapper.updateByPrimaryKeySelective(u);
    }
}