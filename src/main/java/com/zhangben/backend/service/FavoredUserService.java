package com.zhangben.backend.service;

import com.zhangben.backend.model.User;
import java.util.List;

public interface FavoredUserService {

    void addFavored(Integer userId, Integer favoredUserId);

    void deleteFavored(Integer userId, Integer favoredUserId);

    List<User> listFavored(Integer userId);
}