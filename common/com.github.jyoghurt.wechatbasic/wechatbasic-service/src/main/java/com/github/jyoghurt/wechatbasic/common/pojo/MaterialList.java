package com.github.jyoghurt.wechatbasic.common.pojo;

import java.util.List;

/**
 * Created by zhangjl on 2015/11/11.
 */
public class MaterialList {
    private String total_count;
    private String item_count;
    private List<Material> item;

    public String getTotal_count() {
        return total_count;
    }

    public void setTotal_count(String total_count) {
        this.total_count = total_count;
    }

    public String getItem_count() {
        return item_count;
    }

    public void setItem_count(String item_count) {
        this.item_count = item_count;
    }

    public List<Material> getItem() {
        return item;
    }

    public void setItem(List<Material> item) {
        this.item = item;
    }
}
