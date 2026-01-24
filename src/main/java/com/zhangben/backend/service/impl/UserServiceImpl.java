package com.zhangben.backend.service.impl;

import com.zhangben.backend.dto.RegisterRequest;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.model.User;
import com.zhangben.backend.model.UserExample;
import com.zhangben.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Override
    public User findByEmail(String email) {
        UserExample example = new UserExample();
        example.createCriteria().andEmailEqualTo(email);
        List<User> list = userMapper.selectByExample(example);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public User findById(Integer id) {
        return userMapper.selectByPrimaryKey(id);
    }

    @Override
    public User createUser(RegisterRequest req) {
        User user = new User();

        user.setEmail(req.getEmail());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        user.setRole(req.getRole());

        user.setSecondname(req.getSecondname());
        user.setFirstname(req.getFirstname());

        user.setPaypayFlag(req.getPaypayFlag());
        user.setPaypayAccount(req.getPaypayAccount());

        user.setBankFlag(req.getBankFlag());
        user.setBankName(req.getBankName());
        user.setBankBranch(req.getBankBranch());
        user.setBankAccount(req.getBankAccount());

        userMapper.insertSelective(user);
        return user;

    }

    @Override
    public boolean checkPassword(String raw, String encoded) {
        return encoder.matches(raw, encoded);
    }
}