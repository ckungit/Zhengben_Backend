package com.zhangben.backend.config;

import cn.dev33.satoken.stp.StpInterface;
import com.zhangben.backend.model.User;
import com.zhangben.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private UserService userService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Integer userId = Integer.parseInt(loginId.toString());
        User user = userService.findById(userId);
        if (user == null || user.getRole() == null) {
            return List.of();
        }
        return List.of(user.getRole());
    }
}