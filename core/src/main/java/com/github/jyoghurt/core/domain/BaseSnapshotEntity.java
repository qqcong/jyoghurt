package com.github.jyoghurt.core.domain;

import java.io.Serializable;

/**
 * @Project: 所有需要纪录历史版本的实体，需要继承自该类
 * @Package: com.github.jyoghurt.core.domain
 * @Description:
 * @author: baoxiaobing@lvyushequ.com
 * @date: 2016-12-19 14:30
 */
public class BaseSnapshotEntity<T extends BaseSnapshotEntity<T>> extends BaseEntity<T> implements Serializable {

    private Integer version;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
