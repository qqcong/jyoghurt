package com.github.jyoghurt.wechatbasic.common.pojo;


import com.github.jyoghurt.wechatbasic.enums.WeChatMenusTypeEnum;

/**
 * 普通按钮（子按钮） 
 *  
 * @author chihang
 * @date 2015-10-14
 */  
public class CommonButton extends ComplexButton {
    private WeChatMenusTypeEnum type;
    private String key;
    private String url;
    private String media_id;
    private String appid;
    private String pagepath;

    public WeChatMenusTypeEnum getType() {
        return type;
    }

    public void setType(WeChatMenusTypeEnum type) {
        this.type = type;
    }

    public String getKey() {
        return key;  
    }  
  
    public void setKey(String key) {  
        this.key = key;  
    }

    public String getMedia_id() {
        return media_id;
    }

    public void setMedia_id(String media_id) {
        this.media_id = media_id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getPagepath() {
        return pagepath;
    }

    public void setPagepath(String pagepath) {
        this.pagepath = pagepath;
    }
}