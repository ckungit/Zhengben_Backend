package com.zhangben.backend.service;

import com.zhangben.backend.model.User;
import java.util.List;

public interface FavoredUserService {

    void addFavored(Integer userId, Integer favoredUserId);

    void deleteFavored(Integer userId, Integer favoredUserId);

    List<User> listFavored(Integer userId);

    /**
     * V25: 获取待处理的好友请求（谁加了我但我没加他们）
     */
    List<User> getPendingFriendRequests(Integer userId);
}