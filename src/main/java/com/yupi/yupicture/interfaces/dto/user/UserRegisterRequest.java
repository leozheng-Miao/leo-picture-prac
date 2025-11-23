package com.yupi.yupicture.interfaces.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @program: yu-picture
 * @description:
 * @author: Miao Zheng
 * @date: 2025-10-22 12:25
 **/
@Data
public class UserRegisterRequest implements Serializable {


    private static final long serialVersionUID = 8735650154179439661L;

    private String userAccount;
    private String userPassword;
    private String checkPassword;

}
