package com.usts.ob.controller;


import com.usts.ob.enums.OperatorFriendRequestTypeEnum;
import com.usts.ob.enums.SearchFriendsStatusEnum;
import com.usts.ob.pojo.ChatMsg;
import com.usts.ob.pojo.Users;
import com.usts.ob.pojo.bo.UsersBO;
import com.usts.ob.pojo.vo.MyFriendsVO;
import com.usts.ob.pojo.vo.UsersVO;
import com.usts.ob.service.UserService;
import com.usts.ob.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("u")
//u作为命名空间
public class UserController {
//    @RequestMapping("/hello")

    @Autowired
    private UserService userService;

    @Autowired
    private FastDFSClient fastDFSClient;


    @PostMapping("/registerOrLogin")
    public ChatJSONResult registerOrLogin(@RequestBody Users user) throws Exception{

        //判断用户名和密码 不能为空
        if (StringUtils.isBlank(user.getUsername()) || StringUtils.isBlank(user.getPassword())) {
            return ChatJSONResult.errorMsg("用户名和密码不能为空...");
        }

        //判断用户是否存在 存在就登陆 否则注册
        boolean usernameIsExist = userService.queryUsernameIsExist(user.getUsername());
        Users userResult = null;
        if (usernameIsExist) {
//            login in
            userResult = userService.queryUserForLogin(user.getUsername(), MD5Utils.getMD5Str(user.getPassword()));
            if (userResult == null) {
                return ChatJSONResult.errorMsg("用户名或者密码不正确...请重新输入！");
            }

        } else {
//            sign up
//            TODO faceimage faceimagebig
            user.setNickname(user.getUsername());
            user.setFaceImage("");
            user.setFaceImageBig("");
            //使用md5加密注册密码
            user.setPassword(MD5Utils.getMD5Str(user.getPassword()));

//          相比较登录增加了一个sid
            userResult = userService.saveUser(user);
        }

        //usersVO对原始返回数据处理 格式化
        UsersVO usersVO = new UsersVO();
        BeanUtils.copyProperties(userResult, usersVO);

        return ChatJSONResult.ok(usersVO);
    }


    @PostMapping("/uploadFaceBase64")
    public ChatJSONResult uploadFaceBase64(@RequestBody UsersBO usersBO) throws Exception {
        //获取前端传过来的base64字符串 然后转换为文件对象上传
        String base64Data = usersBO.getFaceData();
//        将image标识成唯一
        String userFacePath = "/fdfs/baseset/" +  usersBO.getUserId() + "userface64.png";
//        String userFacePath = "/Users/upmcy/Desktop/basetest" +  usersBO.getUserId() + "userface64.png";
//        写入原始图片
        FileUtils.base64ToFile(userFacePath, base64Data);

        //转换城multipartfile上传到fastdfs
        MultipartFile faceFile = FileUtils.fileToMultipart(userFacePath);

//        返回的是缩略图
        String url = fastDFSClient.uploadBase64(faceFile);
        System.out.println(url);

        //获取缩略图的url
        String thump = "_80x80.";
        String arr[] = url.split("\\.");
        String thumpImgUrl = arr[0] + thump + arr[1];
        System.out.println(thumpImgUrl);
        //更新用户头像
        Users users = new Users();
        users.setId(usersBO.getUserId());
        users.setFaceImage(thumpImgUrl);
        users.setFaceImageBig(url);
        //返回给前端的user是从数据库中查询后得到
        Users result = userService.updateUserInfo(users);

        return ChatJSONResult.ok(result);
    }

    /**
     * 设置昵称接口
     * @param usersBO
     * @return
     * @throws Exception
     */
    @PostMapping("/setNickName")
    public ChatJSONResult setNickName(@RequestBody UsersBO usersBO) throws Exception{
        Users users = new Users();
        users.setId(usersBO.getUserId());
        users.setNickname(usersBO.getNickname());

        //将前端数据传入后端service处理
        Users result = userService.updateUserInfo(users);


        return ChatJSONResult.ok(result);
    }

    /**
     * 搜索好友接口 根据账号匹配查询 不是模糊查询
     * 通过enumsbao处理
     * @param
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/searchUser")
    public ChatJSONResult searchUser(String myUserId, String friendUsername) throws Exception{

//        判断id是否为空
        if (StringUtils.isBlank(myUserId) || StringUtils.isBlank(friendUsername)) {
            return ChatJSONResult.errorMsg("");
        }
        /**
         * 前置条件：
         * 1 搜索的用户不存在 返回--无此用户
         * 2 搜索的是自己 返回--不能添加自己
         * 3 搜索的朋友已经是你的好友 返回--该用户已经是你的好友
         */

        Integer status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {

            Users users = userService.queryUserInfoByUsername(friendUsername);
            UsersVO usersVO = new UsersVO();
            BeanUtils.copyProperties(users, usersVO);

            return ChatJSONResult.ok(usersVO);

        } else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return ChatJSONResult.errorMsg(errorMsg);
        }

    }

    /**
     * 发送好友请求
     * @param myUserId
     * @param friendUsername
     * @return
     * @throws Exception
     */
    @PostMapping("/addFriendRequest")
    public ChatJSONResult addFriendRequest(String myUserId, String friendUsername) throws Exception{

//        判断id是否为空
        if (StringUtils.isBlank(myUserId) || StringUtils.isBlank(friendUsername)) {

            return ChatJSONResult.errorMsg("");
        }

        Integer status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            userService.sendFriendRequest(myUserId, friendUsername);
        } else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return ChatJSONResult.errorMsg(errorMsg);
        }
        System.out.println("addRequestOk!");
        return ChatJSONResult.ok();
    }

    /**
     * 返回请求方的info
     * @param userId
     * @return
     */
    @PostMapping("/queryFriendRequest")
    public ChatJSONResult queryFriendRequest(String userId) {
//        判断id是否为空
        if (StringUtils.isBlank(userId)){
            return ChatJSONResult.errorMsg("");
        }

//        查询用户接受的朋友信息
        System.out.println("queryRequestOk!");
        return ChatJSONResult.ok(userService.queryFriendRequestList(userId));
    }

    /**
     * 接收请求或拒绝请求
     *
     * 按照用户的id进行增减
     * @param acceptUserId
     * @param sendUserId
     * @param operType
     * @return
     */
    @PostMapping("/operFriendRequest")
    public ChatJSONResult operFriendRequest(String acceptUserId, String sendUserId, Integer operType) {
//        判断id是否为空
        if (StringUtils.isBlank(acceptUserId) || StringUtils.isBlank(sendUserId) || operType == null){
            return ChatJSONResult.errorMsg("");
        }


//        如果operType没有对应的枚举值 直接抛出空错误
        if (StringUtils.isBlank(OperatorFriendRequestTypeEnum.getMsgByType(operType)))
            return ChatJSONResult.errorMsg("");

        if (operType == OperatorFriendRequestTypeEnum.IGNORE.type) {
//            判断如果忽略好友请求 则删除数据库中的记录 发送者对应接受者的id
            userService.deleteFriendRequest(sendUserId, acceptUserId);
        }
        else if (operType == OperatorFriendRequestTypeEnum.PASS.type) {
            userService.passFriendRequest(sendUserId, acceptUserId);
        }

//        刷新显示好友列表
        List<MyFriendsVO> myFriendsVOS = userService.queryMyFriends(acceptUserId);
        return ChatJSONResult.ok(myFriendsVOS);
    }

    @PostMapping("/myFriends")
    public ChatJSONResult myFriends(String userId) {

        //        判断id是否为空
        if (StringUtils.isBlank(userId)){
            return ChatJSONResult.errorMsg("");
        }

        List<MyFriendsVO> myFriendsVOS = userService.queryMyFriends(userId);
        return ChatJSONResult.ok(myFriendsVOS);
    }

    /**
     * 用户手机端获取为签收状态信息
     * 返回未签收信息
     * @param acceptUserId
     * @return
     */

    @PostMapping("/getUnReaderMsgList")
    public ChatJSONResult getUnReaderMsgList(String acceptUserId) {

        //        判断id是否为空
        if (StringUtils.isBlank(acceptUserId)){
            return ChatJSONResult.errorMsg("");
        }

        List<ChatMsg> unReadMsgList = userService.getUnReadMsgList(acceptUserId);
        return ChatJSONResult.ok(unReadMsgList);
    }

}
