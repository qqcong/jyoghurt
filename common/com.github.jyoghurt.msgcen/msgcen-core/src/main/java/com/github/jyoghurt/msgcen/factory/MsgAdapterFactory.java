package com.github.jyoghurt.msgcen.factory;

import com.github.jyoghurt.msgcen.adapter.AppletTmplAdapter;
import com.github.jyoghurt.msgcen.adapter.EmailAdapter;
import com.github.jyoghurt.msgcen.adapter.SMSAdapter;
import com.github.jyoghurt.msgcen.adapter.WechatTmplAdapter;
import com.github.jyoghurt.msgcen.common.enums.MsgTypeEnum;
import com.github.jyoghurt.core.exception.BaseErrorException;

/**
 * user:zjl
 * date: 2016/11/17.
 */
public class MsgAdapterFactory {
    /**
     * 根据消息类型生成消息适配器
     *
     * @param msgType 消息类型
     * @return 消息适配器
     */
    public static MsgAdapter produce(MsgTypeEnum msgType) {
        switch (msgType) {
            case EMAIL:
                return new EmailAdapter();
            case WECHAT_TMPL:
                return new WechatTmplAdapter();
            case SMS:
                return new SMSAdapter();
            case APPLET_TMPL:
                return new AppletTmplAdapter();
            default:
                throw new BaseErrorException("无法获得消息类型为:{0}的适配器", msgType.name());
        }
    }
}
