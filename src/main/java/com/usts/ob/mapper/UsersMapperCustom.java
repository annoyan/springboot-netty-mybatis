package com.usts.ob.mapper;

import com.usts.ob.pojo.Users;
import com.usts.ob.pojo.vo.FriendRequestVO;
import com.usts.ob.pojo.vo.MyFriendsVO;
import com.usts.ob.utils.MyMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
//解决Intellij Idea Spring Boot Mybatis @Autowired报错的问题
@Repository
public interface UsersMapperCustom extends MyMapper<Users> {
	
	List<FriendRequestVO> queryFriendRequestList(String acceptUserId);
	
	List<MyFriendsVO> queryMyFriends(String userId);
	
	void batchUpdateMsgSigned(List<String> msgIdList);
	
}