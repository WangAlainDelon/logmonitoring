package com.wx.logmonitor.domain;

/**
 * Describe: 用户信息
   用来保存用户的信息，包括账号、手机号码、邮箱、是否有效等信息
   一个应用交给一个负责人来管，用户编号(用户id)就是应用id
 */
public class User {
    private int id;//用户编号
    private String name;//用户名称
    private String mobile;//用户手机
    private String email;//用户邮箱
    private int isValid;//用户是否可用

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getIsValid() {
        return isValid;
    }

    public void setIsValid(int isValid) {
        this.isValid = isValid;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", mobile='" + mobile + '\'' +
                ", email='" + email + '\'' +
                ", isValid=" + isValid +
                '}';
    }
}
