package com.huntkey.rx.sceo.monitor.provider.utils;

import com.huntkey.rx.sceo.monitor.provider.controller.client.ServiceCenterClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.huntkey.rx.commons.utils.rest.Result;
import static com.huntkey.rx.sceo.monitor.commom.constant.Constant.*;

import java.util.ArrayList;
import java.util.List;

import com.huntkey.rx.sceo.monitor.commom.enums.ErrorMessage;
import com.huntkey.rx.sceo.monitor.commom.exception.ApplicationException;
import com.huntkey.rx.sceo.monitor.commom.exception.ServiceException;
import com.huntkey.rx.sceo.monitor.commom.model.Condition;
import com.huntkey.rx.sceo.monitor.commom.model.InputArgument;
import com.huntkey.rx.sceo.monitor.commom.model.LoopTO;
import com.huntkey.rx.sceo.monitor.commom.utils.JsonUtil;
import com.huntkey.rx.sceo.monitor.provider.controller.client.ModelerClient;

@Component
public class DBUtils {
	@Autowired
	ServiceCenterClient hbase;
	@Autowired
	ModelerClient modelerClient;
	private Logger logger=LoggerFactory.getLogger(DBUtils.class);
	/***
	 * 查询结果集多条
	 * @param edmName 表名
	 * @param columns 查询字段名
	 * @param condition 查询条件
	 * @return
	 */
	public  JSONArray getArrayResult(String edmName,String[] columns, Condition condition) {
		//设置查询参数
		InputArgument inputArgument=new InputArgument();
		inputArgument.setEdmName(edmName);
		inputArgument.setColumns(columns);
		inputArgument.setConditions(condition.getConditions());
        Result result = hbase.find(inputArgument.toString());
        //进行查询
        if(result == null || result.getRetCode() != Result.RECODE_SUCCESS){
            String msg = result != null ? result.getErrMsg():null;
            logger.info(msg);
            ApplicationException.throwCodeMesg(ErrorMessage._60002.getCode(), 
            		ErrorMessage._60002.getMsg());
        }
        return JsonUtil.getJsonArrayByAttr(result.getData(), DATASET);
	} 
	/****
	 * 查询
	 * @param param 查询参数
	 * @return JSONObject
	 */
	public  JSONObject getObjectResult(String edmName,String[] columns, Condition condition) {
		//设置查询参数
		InputArgument inputArgument=new InputArgument();
		inputArgument.setEdmName(edmName);
		inputArgument.setColumns(columns);
		inputArgument.setConditions(condition.getConditions());
        Result result = hbase.find(inputArgument.toString());
        //进行查询
        if(result == null || result.getRetCode() != Result.RECODE_SUCCESS){
            String msg = result != null ? result.getErrMsg():null;
            logger.info(msg);
            ApplicationException.throwCodeMesg(ErrorMessage._60002.getCode(), 
            		ErrorMessage._60002.getMsg());
        }
        JSONArray arr=JsonUtil.getJsonArrayByAttr(result.getData(), DATASET);
        if(arr!=null && arr.size()>0){
        	return arr.getJSONObject(0);
        }else{
        	return null;
        }
	}
	/****
	 * 查询
	 * @param param 查询参数
	 * @return JSONObject
	 */
	public  JSONArray load(String edmName,String[] columns, String type,List<String> ids) {
		//设置查询参数
		InputArgument inputArgument=new InputArgument();
		inputArgument.setEdmName(edmName);
		inputArgument.setColumns(columns);
		inputArgument.setType(type);
		inputArgument.setIds(ids);
        Result result = hbase.load(inputArgument.toString());
        //进行查询
        if(result == null || result.getRetCode() != Result.RECODE_SUCCESS){
            String msg = result != null ? result.getErrMsg():null;
            logger.info(msg);
            ApplicationException.throwCodeMesg(ErrorMessage._60002.getCode(), 
            		ErrorMessage._60002.getMsg());
        }
        return JsonUtil.getJsonArrayByAttr(result.getData(), DATASET);
	}
	/****
	 * 单条新增
	 * @param edmName 表名
	 * @param params 提交数据
	 * @return JSONObject
	 */
	public  Object add(String edmName,Object params,String adduser) {
		int addType=0;
		//设置查询参数
		InputArgument inputArgument=new InputArgument();
		inputArgument.setEdmName(edmName);
		inputArgument.setAdduser(adduser);
		 if(params instanceof JSONObject){
			 addType=0;
        	((JSONObject) params).remove("moduser");
        	((JSONObject) params).remove("modtime");
        }else{
        	addType=1;
        	params=JsonUtil.removeAttr((JSONArray)params, new String[]{"moduser","modtime"});
        }
		inputArgument.addData(params);

        Result result = hbase.add(inputArgument.toString());
        //进行查询
        if(result == null || result.getRetCode() != Result.RECODE_SUCCESS){
            String msg = result != null ? result.getErrMsg():null;
            logger.info(msg);
            ApplicationException.throwCodeMesg(ErrorMessage._60002.getCode(), 
            		ErrorMessage._60002.getMsg());
        }
        @SuppressWarnings("unchecked")
		List<String> list=(List<String>) result.getData();
        if(addType==0){
        	if(list!=null && list.size()>0){
        		return list.get(0);
        	}else{
        		return "";
        	}
        }else{
        	return list;
        }
	}
	public  void update(String edmName,Object params,String moduser) {
		//设置查询参数
		InputArgument inputArgument=new InputArgument();
		inputArgument.setEdmName(edmName);
		inputArgument.setAddOrUpdate(1);;
		inputArgument.setModuser(moduser);
		if(params instanceof JSONObject){
        	((JSONObject) params).remove("adduser");
        	((JSONObject) params).remove("addtime");
        }else{
        	params=JsonUtil.removeAttr((JSONArray)params, new String[]{"adduser","addtime"});
        }
		inputArgument.addData(params);
        Result result = hbase.update(inputArgument.toString());
        //进行查询
        if(result == null || result.getRetCode() != Result.RECODE_SUCCESS){
            String msg = result != null ? result.getErrMsg():null;
            logger.info(msg);
            ApplicationException.throwCodeMesg(ErrorMessage._60002.getCode(), 
            		ErrorMessage._60002.getMsg());
        }
	}
	/****
	 * 删除(根据id删除)
	 * @param params 查询参数
	 * @return JSONObject
	 */
	public  String delete(String edmName,Object params) {
		//设置查询参数
		InputArgument inputArgument=new InputArgument();
		inputArgument.setEdmName(edmName);
		inputArgument.addData(params);
        Result result = hbase.delete(inputArgument.toString());
        //进行查询
        if(result == null || result.getRetCode() != Result.RECODE_SUCCESS){
            String msg = result != null ? result.getErrMsg():null;
            logger.info(msg);
            ApplicationException.throwCodeMesg(ErrorMessage._60002.getCode(), 
            		ErrorMessage._60002.getMsg());
        }
        return (String) result.getData();
	}
	/****
	 * 查询资源表信息
	 * @param classId 类ID
	 * @param edmpCode 
	 * @return JSONObject
	 */
	public  JSONObject getEdmcNameEn(String classId,String edmpCode) {
		//设置查询参数
        Result result = modelerClient.getEdmcNameEn(classId, edmpCode);
        //进行查询
        if(result == null || result.getRetCode() != Result.RECODE_SUCCESS){
            String msg = result != null ? result.getErrMsg():null;
            logger.info(msg);
            ApplicationException.throwCodeMesg(ErrorMessage._60007.getCode(), 
            		ErrorMessage._60007.getMsg());
        }
        JSONObject json=JsonUtil.getJson(result.getData());
        if(json!=null && !json.isEmpty()){
        	json=(JSONObject) json.get("value");
        }
        return json;
	}
	/****
	 * 根据类ID查询类信息
	 * @param classId 类ID
	 * @return JSONObject
	 */
	public  JSONObject queryEdmClassById(String classId) {
		//设置查询参数
        Result result = modelerClient.queryEdmClassById(classId);
        //进行查询
        if(result == null || result.getRetCode() != Result.RECODE_SUCCESS){
            String msg = result != null ? result.getErrMsg():null;
            logger.info(msg);
            ApplicationException.throwCodeMesg(ErrorMessage._60007.getCode(), 
            		ErrorMessage._60007.getMsg());
        }
        JSONObject json=JsonUtil.getJson(result.getData());
        return json;
	}
	/****
	 * 根据类ID查询类信息
	 * @param id 类ID
	 * @return JSONObject
	 */
	public  JSONObject getCharacterAndFormat(String classId) {
		//设置查询参数
        Result result = modelerClient.getCharacterAndFormat(classId);
        //进行查询
        if(result == null || result.getRetCode() != Result.RECODE_SUCCESS){
            String msg = result != null ? result.getErrMsg():null;
            logger.info(msg);
			throw new ServiceException("调用 Modeler 类特征值显示格式查询接口失败！");
        }
        JSONObject json=JsonUtil.getJson(result.getData());
        return json;
	}
	/**
	 * 根据所得数据集循环查询  (处理类型IN循环)
	 * @param loopJson 循环对象
	 * @param loopArray 循环数组
	 * @return
	 */
	public  JSONArray loopQuery(LoopTO loopJson,JSONArray loopArray){
		if(loopJson==null)
			return loopArray;
		if(JsonUtil.isNullOrEmpty(loopArray))
			return loopArray;
		String tableName=loopJson.getTableName();
		String sourceKey=loopJson.getSourceKey();
		String mergeKey=loopJson.getMergeKey();
		String[] column= loopJson.getColumns();
		Condition condition= loopJson.getCondition()==null?new Condition():loopJson.getCondition();
		JSONArray jsonArrNew=null;
		JSONArray jsonArrSub=null;
		//取出loopArr中的循环字段
		JSONObject json=null;
		List<String> list=new ArrayList<String>();
		String value=null;
		for(Object obj:loopArray){
			json=JsonUtil.getJson(obj);
			if(json!=null){
				value=json.getString(mergeKey);
				if(!list.contains(value))
					list.add(value);
			}
		}
		
 		//取得数据集id集合
 		if(list!=null && list.size()>0){ 			
	 		for(String listValue :list){
	 			condition.addCondition(sourceKey, "=", listValue, loopJson.getCondition()==null);
	 			jsonArrSub=getArrayResult(tableName, column,condition);
	 			jsonArrNew=JsonUtil.mergeJsonArray(jsonArrNew,jsonArrSub);
	 		}
 		}
 		return jsonArrNew;
	}
	
}
