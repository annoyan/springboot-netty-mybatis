package com.usts.ob.mapper;

import com.usts.ob.pojo.FriendsRequest;
import com.usts.ob.utils.MyMapper;
import org.springframework.stereotype.Repository;

//解决Intellij Idea Spring Boot Mybatis @Autowired报错的问题
@Repository
public interface FriendsRequestMapper extends MyMapper<FriendsRequest> {

}