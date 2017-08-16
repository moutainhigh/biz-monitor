/**
 * Project Name:security-center-provider
 * File Name:HbaseClient.java
 * Package Name:com.huntkey.rx.sceo.security.center.provider.controller.client
 * Date:2017年6月30日下午5:38:01
 * Copyright (c) 2017 嘉源锐信 All Rights Reserved.
 */

package com.huntkey.rx.sceo.monitor.provider.controller.client;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.huntkey.rx.commons.utils.rest.Result;
import com.huntkey.rx.sceo.monitor.commom.model.TargetNodeTo;

/**
 * ClassName:HbaseClient
 * Function: 调用Hbase接口
 * Reason:	 TODO ADD REASON
 * Date:     2017年6月30日 下午5:38:01
 * @author lijie
 * @version
 * @see
 */
@FeignClient(value = "serviceCenter-provider", fallback = HbaseClientFallback.class)
public interface HbaseClient {

    @RequestMapping(value = "/servicecenter/find", method = RequestMethod.POST)
    Result find(@RequestBody String datas);


    @RequestMapping(value = "/servicecenter/add", method = RequestMethod.POST)
    Result add(@RequestBody String datas);
    
    @RequestMapping(value= "/servicecenter/delete", method = RequestMethod.POST)
    Result delete(@RequestBody String datas);
    
    @RequestMapping(value= "/servicecenter/update", method = RequestMethod.POST)
    Result update(@RequestBody String datas);

    @RequestMapping(value= "/monitor/queryTreeNodeResource", method = RequestMethod.GET)
    Result queryTreeNodeResource(@RequestParam(value="orderId") String orderId,@RequestParam(value="startDate",defaultValue="") String startDate,
                                 @RequestParam(value="endDate",defaultValue="") String endDate, @RequestParam(value="excNodeId",defaultValue="") String excNodeId);
    
    @RequestMapping(value= "/monitor/updateTargetNode", method = RequestMethod.POST)
    Result updateTargetNode(@RequestParam(value="edmName") String edmName, @RequestBody TargetNodeTo node);
    
    @RequestMapping(value= "/monitor/getTargetAllChildNode", method = RequestMethod.GET)
    Result getTargetAllChildNode(@RequestParam(value="edmName") String edmName, 
                                 @RequestParam(value="nodeId") String nodeId,@RequestParam(value="endDate",defaultValue = "") String endDate);
    
    @RequestMapping(value= "/servicecenter/business/monitors/trees/nodes", method = RequestMethod.GET)
    Result getMonitorTreeNodes(@RequestParam(value = "edmcNameEn") String edmcNameEn,
            @RequestParam(value = "searchDate") String searchDate,
            @RequestParam(value = "rootNodeId") String rootNodeId);
    
    @RequestMapping(value= "/servicecenter/load", method = RequestMethod.POST)
    Result load(@RequestBody String datas);
}

