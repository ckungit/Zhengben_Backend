package com.zhangben.backend.service;

import com.zhangben.backend.dto.RegisterRequest;
import com.zhangben.backend.model.User;

public interface UserService {

    User findByEmail(String email);

    User findById(Integer id);

    User createUser(RegisterRequest req);

    boolean checkPassword(String raw, String encoded);
}