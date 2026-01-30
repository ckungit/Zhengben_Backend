package com.zhangben.backend.mapper;

import com.zhangben.backend.model.InviteLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InviteLinkMapper {
    
    int insert(InviteLink inviteLink);
    
    InviteLink selectByCode(@Param("code") String code);
    
    List<InviteLink> selectByCreatorId(@Param("creatorId") Integer creatorId);
    
    int incrementUsedCount(@Param("id") Integer id);
    
    int updateStatus(@Param("id") Integer id, @Param("status") Byte status);
    
    int deleteById(@Param("id") Integer id);
    
    int insertUsage(@Param("inviteId") Integer inviteId, @Param("userId") Integer userId);
    
    int checkUsage(@Param("inviteId") Integer inviteId, @Param("userId") Integer userId);
}
