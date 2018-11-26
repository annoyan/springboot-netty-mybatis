package com.usts.ob.service.impl;


import com.usts.ob.mapper.UsersMapper;
import com.usts.ob.pojo.ChatMsg;
import com.usts.ob.pojo.Users;
import com.usts.ob.pojo.vo.FriendRequestVO;
import com.usts.ob.pojo.vo.MyFriendsVO;
import com.usts.ob.service.UserService;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * 实现service的方法
 */
@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UsersMapper usersMapper;

	@Autowired
	private Sid sid;


	@Transactional(propagation = Propagation.SUPPORTS)

	@Override
    /**
     * 接收前端的数据 判断是否为空
     */
	public boolean queryUsernameIsExist(String username) {

	Users user = new Users();
		user.setUsername(username);

		Users result = usersMapper.selectOne(user);

		return result != null ? true : false;
	}

    /**
     * 由于使用了查询 就要添加事物 事物级别
     * @param username
     * @param pwd
     * @return
     */
	@Transactional(propagation = Propagation.SUPPORTS)
	@Override
	public Users queryUserForLogin(String username, String pwd) {
//      通过example使用条件查询
		Example userExample = new Example(Users.class);
//		创建查询条件
		Example.Criteria criteria = userExample.createCriteria();

		//前端数据是否与数据库中的数据相同 后者为前端数据
		criteria.andEqualTo("username", username);
		criteria.andEqualTo("password", pwd);

		//返回查询结果
		Users result = usersMapper.selectOneByExample(userExample);

		return result;
	}

    @Override
    public Users saveUser(Users user) {

//      返回固定16位的字母数字混编的字符串
	    String userId = sid.nextShort();
//	    TODO 为每个用户生成唯一的二维码
	    user.setQrcode("");

	    user.setId(userId);
//	    使用mapper操纵数据库
	    usersMapper.insert(user);
        return user;
    }

    @Override
    public Users updateUserInfo(Users user) {
        return null;
    }

    @Override
    public Integer preconditionSearchFriends(String myUserId, String friendUsername) {
        return null;
    }

    @Override
    public Users queryUserInfoByUsername(String username) {
        return null;
    }

    @Override
    public void sendFriendRequest(String myUserId, String friendUsername) {

    }

    @Override
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId) {
        return null;
    }

    @Override
    public void deleteFriendRequest(String sendUserId, String acceptUserId) {

    }

    @Override
    public void passFriendRequest(String sendUserId, String acceptUserId) {

    }

    @Override
    public List<MyFriendsVO> queryMyFriends(String userId) {
        return null;
    }

    @Override
    public String saveMsg(ChatMsg chatMsg) {
        return null;
    }

    @Override
    public void updateMsgSigned(List<String> msgIdList) {

    }

    @Override
    public List<ChatMsg> getUnReadMsgList(String acceptUserId) {
        return null;
    }
}
