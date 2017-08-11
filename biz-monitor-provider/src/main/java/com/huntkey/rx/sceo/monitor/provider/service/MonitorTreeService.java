package com.huntkey.rx.sceo.monitor.provider.service;


import com.alibaba.fastjson.JSONArray;

/**
 * Created by zhaomj on 2017/8/9.
 */
public interface MonitorTreeService {

    /**
     * 查询某个时间的指定监管树所有节点
     * 若有根节点ID则根据根节点ID查询
     * 无根节点ID则先根据时间查询出根节点ID
     *
     * @param edmcNameEn
     * @param searchDate
     * @return
     */
    JSONArray getMonitorTreeNodes(String edmcNameEn, String searchDate, String rootNodeId);

    /**
     * 查询监管树类列表，并根据查询条件统计监管类下监管树的数量
     * @param treeName
     * @param beginTime
     * @param endTime
     * @return
     */
    JSONArray getEntityByVersionAndEnglishName(String treeName,String beginTime,String endTime);
}