package com.jingdianjichi.subject.infra.rpc;

import com.jingdianjichi.auth.api.UserFeignService;
import com.jingdianjichi.auth.entity.AuthUserDTO;
import com.jingdianjichi.auth.entity.Result;
import com.jingdianjichi.subject.infra.entity.UserInfo;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class UserRpc {

    @Resource
    private UserFeignService userFeignService;

    /**
     * 根据用户名获取用户信息
     * @param userName 用户名
     * @return UserInfo 用户信息对象
     */
    public UserInfo getUserInfo(String userName) {
        // 创建认证用户DTO对象
        AuthUserDTO authUserDTO = new AuthUserDTO();
        // 设置用户名
        authUserDTO.setUserName(userName);
        // 通过Feign服务调用获取用户信息
        Result<AuthUserDTO> result = userFeignService.getUserInfo(authUserDTO);
        // 创建用户信息对象
        UserInfo userInfo = new UserInfo();
        // 判断调用是否成功
        if (!result.getSuccess()) {
            // 如果调用失败，返回空用户信息对象
            return userInfo;
        }
        // 获取返回数据
        AuthUserDTO data = result.getData();
        // 设置用户名
        userInfo.setUserName(data.getUserName());
        // 设置昵称
        userInfo.setNickName(data.getNickName());
        // 设置头像
        userInfo.setAvatar(data.getAvatar());
        // 返回用户信息
        return userInfo;
    }

}
