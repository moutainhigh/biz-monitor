package com.huntkey.rx.sceo.monitor.provider.controller.client;

import com.huntkey.rx.commons.utils.rest.Result;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(value = "modeler-provider", fallback = ModelerClientFallback.class)
public interface ModelerClient {

	/**
	 * 根据id查询EDM类
	 * @author 方坤
	 * @version
	 * @see
	 */
    @RequestMapping(value = "/classes/{id}")
    Result queryEdmClassById(@PathVariable(value = "id") String id);


    /**
     * 查询属性
     * @param id
     * @return
     */
    @RequestMapping(value = "/classes/{id}/properties")
    Result queryEdmClassProperties(@PathVariable(value = "id") String id);


    /**
     * 根据模型id + 类英文名称数组查询类树
     * @param modelerId
     * @param edmcNameEns
     * @return
     */
    @RequestMapping(value="/classes/classTree", method= RequestMethod.GET)
    Result queryClassTree(@RequestParam(value = "modelerId") String modelerId,
                          @RequestParam(value = "edmcNameEns") String[] edmcNameEns);

    /**
     * 根据类id 查询特征值字段集合和格式化样式
     * @param classId
     * @return
     */
    @RequestMapping(value={"/classFormats/getCharacterAndFormat"}, method={RequestMethod.GET})
    Result getCharacterAndFormat(@RequestParam(value = "classId") String classId);
    
}
