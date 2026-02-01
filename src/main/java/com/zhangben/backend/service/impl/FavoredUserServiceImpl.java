package com.zhangben.backend.service.impl;

import com.zhangben.backend.mapper.FavoredUserMapper;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.model.FavoredUser;
import com.zhangben.backend.model.FavoredUserExample;
import com.zhangben.backend.model.User;
import com.zhangben.backend.service.FavoredUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FavoredUserServiceImpl implements FavoredUserService {

    @Autowired
    private FavoredUserMapper favoredUserMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public void addFavored(Integer userId, Integer favoredUserId) {

        FavoredUserExample example = new FavoredUserExample();
        example.createCriteria()
                .andUserIdEqualTo(userId)
                .andFavoredUserIdEqualTo(favoredUserId);

        // 已收藏则忽略
        if (!favoredUserMapper.selectByExample(example).isEmpty()) {
            return;
        }

        FavoredUser f = new FavoredUser();
        f.setUserId(userId);
        f.setFavoredUserId(favoredUserId);

        favoredUserMapper.insertSelective(f);
    }

    @Override
    public void deleteFavored(Integer userId, Integer favoredUserId) {

        FavoredUserExample example = new FavoredUserExample();
        example.createCriteria()
                .andUserIdEqualTo(userId)
                .andFavoredUserIdEqualTo(favoredUserId);

        favoredUserMapper.deleteByExample(example);
    }

    @Override
    public List<User> listFavored(Integer userId) {

        FavoredUserExample example = new FavoredUserExample();
        example.createCriteria().andUserIdEqualTo(userId);

        List<FavoredUser> list = favoredUserMapper.selectByExample(example);

        List<User> result = new ArrayList<>();

        for (FavoredUser f : list) {
            User u = userMapper.selectByPrimaryKey(f.getFavoredUserId());
            if (u != null) {
                result.add(u);
            }
        }

        return result;
    }

    @Override
    public List<User> getPendingFriendRequests(Integer userId) {
        // 找出：谁把我加为好友，但我没有把他们加为好友

        // 1. 找出所有把我加为好友的人
        FavoredUserExample addedMeExample = new FavoredUserExample();
        addedMeExample.createCriteria().andFavoredUserIdEqualTo(userId);
        List<FavoredUser> addedMeList = favoredUserMapper.selectByExample(addedMeExample);

        // 2. 找出我加为好友的人
        FavoredUserExample iAddedExample = new FavoredUserExample();
        iAddedExample.createCriteria().andUserIdEqualTo(userId);
        List<FavoredUser> iAddedList = favoredUserMapper.selectByExample(iAddedExample);

        // 转换为 Set 方便查找
        java.util.Set<Integer> iAddedSet = new java.util.HashSet<>();
        for (FavoredUser f : iAddedList) {
            iAddedSet.add(f.getFavoredUserId());
        }

        // 3. 过滤出：加了我但我没加他们的人
        List<User> result = new ArrayList<>();
        for (FavoredUser f : addedMeList) {
            Integer theirUserId = f.getUserId();
            if (!iAddedSet.contains(theirUserId)) {
                User u = userMapper.selectByPrimaryKey(theirUserId);
                if (u != null) {
                    result.add(u);
                }
            }
        }

        return result;
    }
}