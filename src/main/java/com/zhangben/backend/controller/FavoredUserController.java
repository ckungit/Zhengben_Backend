package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.model.User;
import com.zhangben.backend.service.FavoredUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favored")
public class FavoredUserController {

    @Autowired
    private FavoredUserService favoredUserService;

    @PostMapping("/add")
    public String addFavored(@RequestParam Integer favoredUserId) {
        Integer userId = StpUtil.getLoginIdAsInt();
        favoredUserService.addFavored(userId, favoredUserId);
        return "OK";
    }

    @PostMapping("/delete")
    public String deleteFavored(@RequestParam Integer favoredUserId) {
        Integer userId = StpUtil.getLoginIdAsInt();
        favoredUserService.deleteFavored(userId, favoredUserId);
        return "OK";
    }

    @GetMapping("/list")
    public List<User> listFavored() {
        Integer userId = StpUtil.getLoginIdAsInt();
        return favoredUserService.listFavored(userId);
    }
}