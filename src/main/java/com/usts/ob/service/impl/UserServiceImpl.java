package com.usts.ob.service.impl;


import com.usts.ob.enums.MsgActionEnum;
import com.usts.ob.enums.MsgSignFlagEnum;
import com.usts.ob.enums.SearchFriendsStatusEnum;
import com.usts.ob.mapper.*;
import com.usts.ob.netty.ChatMsg;
import com.usts.ob.netty.DataContent;
import com.usts.ob.netty.UserChannelRel;
import com.usts.ob.pojo.FriendsRequest;
import com.usts.ob.pojo.MyFriends;
import com.usts.ob.pojo.Users;
import com.usts.ob.pojo.vo.FriendRequestVO;
import com.usts.ob.pojo.vo.MyFriendsVO;
import com.usts.ob.service.UserService;
import com.usts.ob.utils.FastDFSClient;
import com.usts.ob.utils.FileUtils;
import com.usts.ob.utils.JsonUtils;
import com.usts.ob.utils.QRCodeUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
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

    @Autowired
	private QRCodeUtils qrCodeUtils;

    @Autowired
    private FastDFSClient fastDFSClient;

    @Autowired
    private MyFriendsMapper myFriendsMapper;

    @Autowired
    private FriendsRequestMapper friendsRequestMapper;

    @Autowired
    private UsersMapperCustom usersMapperCustom;

    @Autowired
    private ChatMsgMapper chatMsgMapper;

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

    /**
     * 保存用户
     * @param user
     * @return
     */
	@Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users saveUser(Users user) {

//      返回固定16位的字母数字混编的字符串
	    String userId = sid.nextShort();
//	    TODO 为每个用户生成唯一的二维码

//        缓存二维码路径
//        String qrCodePath = "/Users/upmcy/Desktop/basetest/" + userId + "qrcode.png";
        String qrCodePath = "/fdfs/baseset/" + userId + "qrcode.png";
        //格式 mchat_qrcode:[username]

        qrCodeUtils.createQRCode(qrCodePath, "mchat_qrcode:" + user.getUsername());
//        上传二维码到fastdfs
        MultipartFile qrCodefile = FileUtils.fileToMultipart(qrCodePath);
        String qrCodeUrl = "";
        try {
            qrCodeUrl = fastDFSClient.uploadQRCode(qrCodefile);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        上传数据库
	    user.setQrcode(qrCodeUrl);
	    user.setId(userId);
//	    使用mapper操纵数据库
	    usersMapper.insert(user);
        return user;
    }

    /**
     * 更新完读取返回
     * @param user
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users updateUserInfo(Users user) {

	    usersMapper.updateByPrimaryKeySelective(user);
	    return queryUserById(user.getId());
    }

    /**
     * 执行updateUserInfo()后 在执行读取返回给前端
     * @param userId
     * @return
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    protected Users queryUserById(String userId) {
        return usersMapper.selectByPrimaryKey(userId);
    }

    /**
     *
     *          * 前置条件：
     *          * 1 搜索的用户不存在 返回--无此用户
     *          * 2 搜索的是自己 返回--不能添加自己
     *          * 3 搜索的朋友已经是你的好友 返回--该用户已经是你的好友
     *
     * @param myUserId
     * @param friendUsername
     * @return
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Integer preconditionSearchFriends(String myUserId, String friendUsername) {

        Users users = queryUserInfoByUsername(friendUsername);
        if (users == null) {
            return SearchFriendsStatusEnum.USER_NOT_EXIST.status;
        }

        if (users.getId().equals(myUserId)) {
            return SearchFriendsStatusEnum.NOT_YOURSELF.status;
        }

        Example example = new Example(MyFriends.class);
        Example.Criteria criteria = example.createCriteria();
        //TODO 查一下用法
        /**
         * 通过example类可以构造你想到的任何筛选条件
         * example由表生成
         */
        criteria.andEqualTo("myUserId", myUserId);
        criteria.andEqualTo("myFriendUserId", users.getId());


        MyFriends myFriends = myFriendsMapper.selectOneByExample(example);
        if (myFriends != null) {
            return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;
        }
        return SearchFriendsStatusEnum.SUCCESS.status;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserInfoByUsername(String username) {
        Example example = new Example(Users.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("username", username);

        return usersMapper.selectOneByExample(example);
    }

    /**
     * 发送好友请求
     * @param myUserId
     * @param friendUsername
     */
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void sendFriendRequest(String myUserId, String friendUsername) {

//        根据用户名把朋友信息查出
        Users friend = queryUserInfoByUsername(friendUsername);

//        查询发送好友请求的记录表
        Example example = new Example(FriendsRequest.class);
        Example.Criteria criteria = example.createCriteria();

        criteria.andEqualTo("sendUserId", myUserId);
        criteria.andEqualTo("acceptUserId", friendUsername);

//        查询返回
        FriendsRequest friendsRequest = friendsRequestMapper.selectOneByExample(example);
        if (friendsRequest == null) {
//            如果不是你的好友 并且好友记录没有添加 就新增
            String requestId = sid.nextShort();

            FriendsRequest request = new FriendsRequest();

//            同一请求在数据库中只有一次 根据date鉴别

            request.setId(requestId);
            request.setSendUserId(myUserId);
            request.setAcceptUserId(friend.getId());
            request.setRequestDateTime(new Date());

            friendsRequestMapper.insert(request);
        }
    }

    /**
     * 查询好友请求
     * @param acceptUserId
     * @return
     */


    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId) {
//        返回好友请求列表
        return usersMapperCustom.queryFriendRequestList(acceptUserId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void deleteFriendRequest(String sendUserId, String acceptUserId) {
//               查询发送好友请求的记录表
        Example example = new Example(FriendsRequest.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("sendUserId", sendUserId);
        criteria.andEqualTo("acceptUserId", acceptUserId);

        friendsRequestMapper.deleteByExample(example);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void passFriendRequest(String sendUserId, String acceptUserId) {
//        互相为朋友
        saveFriends(sendUserId, acceptUserId);
        saveFriends(acceptUserId, sendUserId);
//        添加完成删除请求记录
        deleteFriendRequest(sendUserId, acceptUserId);

        /**
         * 完成请求 使用websocket主动推送消息更新请求发送者 更新其通讯录
         *
         */
        Channel sendChannel = UserChannelRel.get(sendUserId);
        if (sendChannel != null) {
            // 使用websocket主动推送消息到请求发起者，更新他的通讯录列表为最新
            DataContent dataContent = new DataContent();
            dataContent.setAction(MsgActionEnum.PULL_FRIEND.type);

            sendChannel.writeAndFlush(
                    new TextWebSocketFrame(
                            JsonUtils.objectToJson(dataContent)));
        }


    }

    @Transactional(propagation = Propagation.REQUIRED)
    protected void saveFriends(String sendUserId, String acceptUserId) {
        MyFriends myFriends = new MyFriends();
        String recordId = sid.nextShort();
        myFriends.setId(recordId);
        myFriends.setMyFriendUserId(acceptUserId);
        myFriends.setMyUserId(sendUserId);
        myFriendsMapper.insert(myFriends);


    }
    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<MyFriendsVO> queryMyFriends(String userId) {
        List<MyFriendsVO> myFriendsVOS = usersMapperCustom.queryMyFriends(userId);

        return myFriendsVOS;
    }

    /**
     * 传入的netty chatmsg 将数据持久化到 pojo chatmsg
     * 返回信息的id
     * @param chatMsg
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public String saveMsg(ChatMsg chatMsg) {
        com.usts.ob.pojo.ChatMsg msgDB = new com.usts.ob.pojo.ChatMsg();
        String msgId = sid.nextShort();
        msgDB.setId(msgId);
        msgDB.setAcceptUserId(chatMsg.getReceiverId());
        msgDB.setSendUserId(chatMsg.getSenderId());
        msgDB.setCreateTime(new Date());
        /**
         * 信息未签收
         */
        msgDB.setSignFlag(MsgSignFlagEnum.unsign.type);
        msgDB.setMsg(chatMsg.getMsg());

        chatMsgMapper.insert(msgDB);

        return msgId;
    }


    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void updateMsgSigned(List<String> msgIdList) {
        usersMapperCustom.batchUpdateMsgSigned(msgIdList);
    }


    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<com.usts.ob.pojo.ChatMsg> getUnReadMsgList(String acceptUserId) {

        Example chatExample = new Example(com.usts.ob.pojo.ChatMsg.class);
        Example.Criteria chatCriteria = chatExample.createCriteria();
        /**
         *
         */
        chatCriteria.andEqualTo("signFlag", 0);
        chatCriteria.andEqualTo("acceptUserId", acceptUserId);
        /**
         * 根据条件查找未签收的消息
         */
        List<com.usts.ob.pojo.ChatMsg> result = chatMsgMapper.selectByExample(chatExample);


        return result;
    }
}
