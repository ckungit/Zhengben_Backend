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
}