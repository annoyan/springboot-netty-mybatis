package com.usts.ob.controller;


import com.usts.ob.pojo.Users;
import com.usts.ob.pojo.vo.UsersVO;
import com.usts.ob.service.UserService;
import com.usts.ob.utils.ChatJSONResult;
import com.usts.ob.utils.MD5Utils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("u")
//u作为命名空间
public class UserController {
//    @RequestMapping("/hello")

    @Autowired
    private UserService userService;

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
}
