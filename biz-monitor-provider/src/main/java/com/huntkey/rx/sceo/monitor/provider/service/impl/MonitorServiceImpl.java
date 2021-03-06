package com.huntkey.rx.sceo.monitor.provider.service.impl;

import com.huntkey.rx.sceo.monitor.provider.controller.client.ServiceCenterClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.huntkey.rx.commons.utils.rest.Result;
import com.huntkey.rx.commons.utils.string.StringUtil;
import static com.huntkey.rx.sceo.monitor.commom.constant.Constant.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.huntkey.rx.sceo.monitor.commom.enums.ChangeType;
import com.huntkey.rx.sceo.monitor.commom.enums.ErrorMessage;
import com.huntkey.rx.sceo.monitor.commom.exception.ApplicationException;
import com.huntkey.rx.sceo.monitor.commom.exception.ServiceException;
import com.huntkey.rx.sceo.monitor.commom.model.AddMonitorTreeTo;
import com.huntkey.rx.sceo.monitor.commom.model.Condition;
import com.huntkey.rx.sceo.monitor.commom.model.JoinTO;
import com.huntkey.rx.sceo.monitor.commom.model.LoopTO;
import com.huntkey.rx.sceo.monitor.commom.model.NodeTo;
import com.huntkey.rx.sceo.monitor.commom.utils.DataUtil;
import com.huntkey.rx.sceo.monitor.commom.utils.JsonUtil;
import com.huntkey.rx.sceo.monitor.commom.utils.ToolUtil;
import com.huntkey.rx.sceo.monitor.provider.service.MonitorService;
import com.huntkey.rx.sceo.monitor.provider.service.OrderNumberService;
import com.huntkey.rx.sceo.monitor.provider.utils.DBUtils;
@Service
public class MonitorServiceImpl implements MonitorService {
	@Autowired
	DBUtils DBUtils;
	@Autowired
	ServiceCenterClient serviceCenterClient;
	@Autowired
	OrderNumberService orderNumberService;
	private static final Logger logger = LoggerFactory.getLogger(MonitorServiceImpl.class);
	
	/***
	 * 查询监管树临时结构
	 * @param tempId 监管树临时单id
	 * @param validDate 日期   
	 * @return
	 */
	@Override
	public JSONArray tempTree(String tempId, String validDate) {
		// TODO Auto-generated method stub
		//初始化查询参数器
		Condition condition=new Condition();
		//组装查询条件
		condition.addCondition(PID, EQUAL, tempId, true);
		condition.addCondition(MTOR021, LT, ChangeType.INVALID.toString(), false);
		if(StringUtil.isNullOrEmpty(validDate)){
			validDate=ToolUtil.getNowDateStr(YYYY_MM_DD);
			condition.addCondition(MTOR012, GT, validDate, false);
		}else{
			validDate+=" 00:00:00";
			condition.addCondition(MTOR011, LTE, validDate, false);
			condition.addCondition(MTOR012, GT, validDate, false);
		}
		
		//查询节点集合表
		JSONArray nodeArray=DBUtils.getArrayResult(MTOR005,null,condition);
		return nodeArray;
	}
	/**
	 * 监管树临时单预览 是否需要包含资源
	 * @param nodes
	 * @return
	 */
	@Override
	public JSONArray resource(String[] nodes,String classId) {
		// TODO Auto-generated method stub
		Result result=new Result();
		result.setRetCode(Result.RECODE_SUCCESS);
		
		//根据类ID查询出资源表
		JSONObject resourceObj=getCharacter(classId);
		
		//循环查询资源表
		JSONArray resourceArr=new JSONArray();
		JSONArray resourceArrNew=new JSONArray();
		for(String node:nodes){
			if(!StringUtil.isNullOrEmpty(node)){
				resourceArr=getNodeResource(node, resourceObj.getString("resourceTab"),
						resourceObj.getJSONObject("jsonCharacter"));//查询单个节点关联资源
				resourceArrNew=JsonUtil.mergeJsonArray(resourceArrNew,resourceArr);
			}
		}
		return resourceArrNew;
	}
	/**
	 * 查询节点详情
	 * @param nodeId 节点ID
	 * @return
	 */
	@Override
	public JSONObject nodeDetail(String nodeId) {
		// TODO Auto-generated method stub
		//组装查询条件
		Condition condition=new Condition();
		condition.addCondition(ID, EQUAL, nodeId, true);
		//查询节点详情
		JSONObject nodeJson=queryNode(condition);
		if(nodeJson!=null && !nodeJson.isEmpty()){
			//查询员工表并且做左连
			//根据类ID查询出资源表
			JSONObject jsonCharacter=DBUtils.getCharacterAndFormat(STAFFCLASSID);
			JSONObject staffObj=null;
			if(nodeJson.containsKey(MTOR009) && !StringUtil.isNullOrEmpty(nodeJson.getString(MTOR009))){
				condition.addCondition(ID, EQUAL, nodeJson.getString(MTOR009), true);//主管人
				staffObj=DBUtils.getObjectResult(STAFF, null, condition);
				if(staffObj!=null){
					staffObj=convert(jsonCharacter, staffObj);
					nodeJson.put("majorStaff", staffObj.getString("text"));
				}
			}
			if(nodeJson.containsKey(MTOR010) && !StringUtil.isNullOrEmpty(nodeJson.getString(MTOR010))){
				condition.addCondition(ID, EQUAL, nodeJson.getString(MTOR010), true);//协管人
				staffObj=DBUtils.getObjectResult(STAFF, null, condition);
				if(staffObj!=null){
					staffObj=convert(jsonCharacter, staffObj);
					nodeJson.put("assistStaff", staffObj.getString("text"));
				}
			}
			//日期转换
			nodeJson.put(MTOR011, ToolUtil.formatDateStr(nodeJson.getString(MTOR011),YYYY_MM_DD));
			nodeJson.put(MTOR012, ToolUtil.formatDateStr(nodeJson.getString(MTOR012),YYYY_MM_DD));
			
		}else{
			ApplicationException.throwCodeMesg(ErrorMessage._60003.getCode(), 
					ErrorMessage._60003.getMsg());
			logger.info("MonitorServiceImpl类的nodeDetail方法：==》"+ErrorMessage._60003.getMsg());
		}
		return nodeJson;
	}
	/**  
	 * 查询节点关联资源
	 * @param nodeId 节点ID
	 * @return
	 */
	@Override
	public JSONArray nodeResource(String nodeId,String classId) {
		// TODO Auto-generated method stub
		//根据类ID查询出资源表
		JSONObject resourceObj=getCharacter(classId);
		JSONArray resourceArr=getNodeResource(nodeId,resourceObj.getString("resourceTab"),
				resourceObj.getJSONObject("jsonCharacter"));
		return resourceArr;
	}
	/***
	 * 获取资源表特征值
	 * @return
	 */
	private JSONObject getCharacter(String classId){
		JSONObject resourceObj=DBUtils.getEdmcNameEn(classId, "moni012");
		if(resourceObj==null){
			logger.info("未设置资源特征值！");
			throw new ServiceException("未设置资源特征值！");
		}
		String resourceTab=resourceObj.getString("edmcNameEn").toLowerCase();
		String resourceClassId=resourceObj.getString(ID);
		//查询moderler特征表
		JSONObject jsonCharacter=DBUtils.getCharacterAndFormat(resourceClassId);
		
		JSONObject jsonCharacterObj=new JSONObject();
		jsonCharacterObj.put("resourceTab", resourceTab);
		jsonCharacterObj.put("jsonCharacter", jsonCharacter);
		return jsonCharacterObj;
	}
	private JSONArray getNodeResource(String nodeId,String resourceTab,JSONObject jsonCharacter){
		JSONArray resources=null;
		//根据节点ID查询出关联资源结果集
		Condition condition=new Condition();
		condition.addCondition(PID, EQUAL, nodeId, true);
		JSONArray resourceArr=DBUtils.getArrayResult(MTOR019, null, condition);
		if(!JsonUtil.isNullOrEmpty(resourceArr)){
			LoopTO loop=new LoopTO(resourceTab,ID,MTOR020,null,null);  
			//循环查询资源表
			resources=DBUtils.loopQuery(loop, resourceArr);
			//结果集中字段转换
			resources=convert(jsonCharacter,resources);
			//数据集做交集
			JoinTO join=new JoinTO(MTOR020,ID,new String[]{"text"});
			resourceArr=DataUtil.mergeJsonArray(resourceArr, resources, join);
		}
		return resourceArr;
	}
	//转换单个对象
	private JSONObject convert(JSONObject characterObj,JSONObject resourcesObj) {
		// TODO Auto-generated method stub
		if(characterObj==null)
			return resourcesObj;
		JSONArray characterArray = characterObj.getJSONArray("character");
		if(characterArray.size()==0){
			logger.info("未设置资源表特征值");
			return resourcesObj;
		}
        String format = characterObj.getString("format");
        String[] resourceFields = new String[characterArray.size()];
        characterArray.toArray(resourceFields);
        String edmObjName = format.toLowerCase();
        for (String fieldName : resourceFields){
            edmObjName = edmObjName.replace(fieldName, resourcesObj.getString(fieldName));
            resourcesObj.put("text",edmObjName);
        }
        return resourcesObj;
	}
	//转化资源数组对象
	private JSONArray convert(JSONObject characterObj,JSONArray resourcesObjs) {
		// TODO Auto-generated method stub
		if(characterObj==null)
			return resourcesObjs;
		
		JSONArray characterArray = characterObj.getJSONArray("character");
		if(characterArray.size()==0){
			logger.info("未设置资源表特征值");
			return resourcesObjs;
		}
		JSONArray resources = new JSONArray();
        String format = characterObj.getString("format");
        String[] resourceFields = new String[characterArray.size()];
        characterArray.toArray(resourceFields);
        for (int j = 0; j < resourcesObjs.size(); j++) {
            JSONObject resourcesObj = resourcesObjs.getJSONObject(j);
            String edmObjName = format.toLowerCase();
            for (String fieldName : resourceFields){
                edmObjName = edmObjName.replace(fieldName, resourcesObj.getString(fieldName));
                resourcesObj.put("text",edmObjName);
            }
            resources.add(resourcesObj);
        }
        return resources;
	}
	@Override
	public String saveNodeDetail(NodeTo nodeDetail) {
		// TODO Auto-generated method stub
  		String nodeId=nodeDetail.getId();
		String endDate=nodeDetail.getMtor012();
		String beginDate=nodeDetail.getMtor011();
		if(StringUtil.isNullOrEmpty(nodeId)){
			logger.info("不存在当前信息！");
			throw new ServiceException("不存在当前信息！");
		}
		DBUtils.update(MTOR005, JsonUtil.getJson(nodeDetail),"");
		//修改下级节点失效日期
		//1.根据根节点ID 临时单第一级子节点信息
		Condition condition=new Condition();
		condition.addCondition(MTOR013, "=", nodeId, true);
		JSONArray childrenNodes=DBUtils.getArrayResult(MTOR005, null, condition);
		childrenNodes.add((JSONObject)JSONObject.toJSON(nodeDetail));
		childDateUpdate(beginDate,endDate,childrenNodes);
		return nodeId;
	}
	//判断节点时间修改对子节点影响
	private void childDateUpdate(String beginDate,String endDate,JSONArray childrenNodes){
		childrenNodes=updateNodesByDate(beginDate,endDate,childrenNodes);//一级子节点的时间修改影响
		//查询一级子节点所有子节点  
		JSONArray arr=new JSONArray();
		if(!JsonUtil.isNullOrEmpty(childrenNodes)){
			for(Object o:childrenNodes){
				JSONObject json=JsonUtil.getJson(o);
				if(!StringUtil.isNullOrEmpty(json.getString(MTOR013))){
					arr=JsonUtil.mergeJsonArray(arr, getChildNode(json.getString(ID)));
				}
			}
		}
		updateNodesByDate(beginDate,endDate,arr);
	}
	
	private JSONArray updateNodesByDate(String beginDate,String endDate,JSONArray childrenNodes){
		//遍历子节点，判断父节点修改时间对子节点的影响
		JSONObject json=null;
		String childBeginDate=null;
		String childEndDate=null;
		JSONArray tempList=new JSONArray();
		if(!JsonUtil.isNullOrEmpty(childrenNodes)){
  			for(Object obj:childrenNodes){
				json=JsonUtil.getJson(obj); 
				if(json!=null){
					childBeginDate=json.getString(MTOR011);
					childEndDate=json.getString(MTOR012);
					//-->修改父节点的失效日期
					//2.修改的子节点失效日期小于等于子节点的生效日期==>子节点失效 
					if(!ToolUtil.dateCompare(childBeginDate, endDate)){
						//选择性失效节点
						invalidNodeSelected(json,childrenNodes);
						tempList.add(json);
					}
					else if(ToolUtil.dateCompare(endDate, childEndDate)){//1.子节点失效日期大于父节点修改的失效日期  ==>子节点失效日期=父节点失效日期
						json.put(MTOR012, endDate);
						DBUtils.update(MTOR005, json, "");
					}
					//-->修改父节点的生效日期
					//2.如果父节点的生效日期大于等于子节点失效日期==>子节点失效
					if(!ToolUtil.dateCompare(beginDate,childEndDate)){
						invalidNodeSelected(json,childrenNodes);
						tempList.add(json);
					}
					else if(ToolUtil.dateCompare(childBeginDate,beginDate)){//1.父节点生效日期>子节点生效日期时==>子节点生效日期=父节点生效日期
						json.put(MTOR011, beginDate);
						DBUtils.update(MTOR005, json, "");
					}
				}
			}
		}
		childrenNodes.removeAll(tempList);
		return childrenNodes;
	}
	
	//失效节点 
	private void invalidNodeSelected(JSONObject node,JSONArray nodes){
		if(node!=null){
			//变更节点信息
			nodes=updateNodesCache(node,nodes);
			DBUtils.update(MTOR005, nodes, "");
			deleteChildrenNodes(Integer.parseInt(node.getString(MTOR021))==ChangeType.ADD.getValue()?1:0,node);
		}
	}
	
	/**
	 * 删除节点资源
	 * @param nodeId 节点ID
	 * @param resourceId 临时单ID
	 * @return
	 */
	@Override
	public Result deleteNodeResource(String nodeId,String resourceId) {  
		// TODO Auto-generated method stub
		Result result=new Result();
		result.setRetCode(Result.RECODE_SUCCESS);
		Condition condition=new Condition();
		condition.addCondition(PID, EQUAL, nodeId, true);
		condition.addCondition(MTOR020, EQUAL, resourceId, false);
		JSONObject retObj=DBUtils.getObjectResult(MTOR019, null, condition);
		if(retObj==null){
			return result;
		}else{
			DBUtils.delete(MTOR019, retObj);
		}
		return result;
	}
	@Override
	public Result changeFormula(String nodeId,String formularId) {
		// TODO Auto-generated method stub   
		
		return null;
	}
	/**
	 * 新增资源
	 * @param nodeId 节点ID
	 * @param resourceIds 资源id集合
	 * @return
	 */
	@Override
	public List<String> addResource(String nodeId,String[] resourceIds) {
		// TODO Auto-generated method stub
		Result result=new Result();
		result.setRetCode(Result.RECODE_SUCCESS);
		JSONArray params=new JSONArray();
		for(String resourceId:resourceIds){
		    JSONObject param=new JSONObject();
			if(!StringUtil.isNullOrEmpty(resourceId)){
				param.put(MTOR020, resourceId);
				param.put(PID, nodeId);
				params.add(param);
			}
		}
		@SuppressWarnings("unchecked")
		List<String> list=(List<String>) DBUtils.add(MTOR019, params,"");
		return list;
	}
	@Override
	public Result saveTemp(String datas) {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * 新增节点
	 * @param nodeId 节点ID
	 * @param nodeType 创建节点的类型 0 创建子节点 1创建上节点 2创建下节点
	 * @return
	 */
	@Override
	public String addNode(String nodeId,int nodeType,String nodeName) {
		// TODO Auto-generated method stub
		//根据nodeId查询当前节点信息
		String newNodeId="";
		Condition condition=new Condition();
		condition.addCondition(ID, EQUAL, nodeId, true);
		//查询节点详情
		JSONObject node=queryNode(condition);
		NodeTo nodeDetail=null;
		JSONObject nodeRight=null;
		String beginDate="";
		String endDate="";
		JSONObject nodeParent=null;
		String nowDate=ToolUtil.getNowDateStr(YYYY_MM_DD);
		if(node!=null){
			switch (nodeType){
				case 0://创建子节点
					beginDate=node.getString(MTOR011);
					//如果当前节点生效时间小于当天则 将生成的字节点的日期设置为当天
					beginDate=ToolUtil.dateCompare(beginDate, null)?nowDate:beginDate;
					endDate=node.getString(MTOR012);
					condition.addCondition(MTOR013, EQUAL, node.getString(ID), true);//当前节点的子节点
					condition.addCondition(MTOR016, EQUAL, NULL, false);//最右侧节点
					condition.addCondition(MTOR021, LT, ChangeType.INVALID.toString(), false);//过滤失效节点
					nodeRight=DBUtils.getObjectResult(MTOR005,null,condition);
					nodeDetail=setNodePosition(node.getString(ID), NULL, 
							nodeRight!=null?nodeRight.getString(ID):NULL, 
							NULL,node.getString(PID),beginDate,endDate,nodeName);
					newNodeId=(String) DBUtils.add(MTOR005, JsonUtil.getJson(nodeDetail),"");
					
					//如果存在最右侧节点  则变更最右侧节点的右节点信息
					if(nodeRight!=null){
						changeNodePosition(nodeRight.getString(ID), 4, newNodeId);
					}
					
					if(StringUtil.isNullOrEmpty(node.getString(MTOR014))) {
						//如果父节点以前没有子节点  变更父节点的子节点信息
						changeNodePosition(node.getString(ID), 2, newNodeId);
					}
				break;
				case 1://创建左节点
					//0.获取父节点信息
					condition.addCondition(ID, EQUAL, node.getString(MTOR013), true);
					nodeParent=DBUtils.getObjectResult(MTOR005, null, condition);
					if(nodeParent!=null){
						beginDate=nodeParent.getString(MTOR011);
						//如果当前节点生效时间小于当天则 将生成的字节点的日期设置为当天
						beginDate=ToolUtil.dateCompare(beginDate, null)?nowDate:beginDate;
						endDate=nodeParent.getString(MTOR012);
					}
					//1.创建新的左节点
					nodeDetail=setNodePosition(node.getString(MTOR013), NULL, 
							node.getString(MTOR015),node.getString(ID), 
							node.getString(PID),beginDate,endDate,nodeName);
					newNodeId=(String) DBUtils.add(MTOR005, JsonUtil.getJson(nodeDetail),"");
					//2.如果当前节点之前没有左节点 则变更父节点的子节点信息 
					if(StringUtil.isNullOrEmpty(node.getString(MTOR015))){
						changeNodePosition(node.getString(MTOR013), 2, newNodeId);
					}else{//4.如果有左节点 则变更之前左节点的有节点
						changeNodePosition(node.getString(MTOR015), 4, newNodeId);
					}
					//3.要变更当前节点的左节点信息
					changeNodePosition(node.getString(ID), 3, newNodeId);
					
				break;	
				case 2://创建右节点
					//0.获取父节点信息
					condition.addCondition(ID, EQUAL, node.getString(MTOR013), true);
					nodeParent=DBUtils.getObjectResult(MTOR005, null, condition);
					if(nodeParent!=null){
						beginDate=nodeParent.getString(MTOR011);
						//如果当前节点生效时间小于当天则 将生成的字节点的日期设置为当天
						beginDate=ToolUtil.dateCompare(beginDate, null)?nowDate:beginDate;
						endDate=nodeParent.getString(MTOR012);
					}
					//1.创建新的右节点
					nodeDetail=setNodePosition(node.getString(MTOR013), NULL, 
							node.getString(ID), node.getString(MTOR016),
							node.getString(PID),beginDate,endDate,nodeName);
					newNodeId=(String) DBUtils.add(MTOR005, JsonUtil.getJson(nodeDetail),"");
					//2.要变更当前节点的右节点信息
					changeNodePosition(node.getString(ID), 4, newNodeId);
					//3.变更之前右节点的左节点信息
					if(!StringUtil.isNullOrEmpty(node.getString(MTOR016))){
						changeNodePosition(node.getString(MTOR016), 3, newNodeId);
					}
				break;
			}
		}
		return newNodeId;
	}
	/**
	 * 删除节点
	 * @param nodeId 节点ID
	 * @param type 0失效 1删除
	 * @return
	 */
	@Override
	public String deleteNode(String nodeId,int type) {
		// TODO Auto-generated method stub
		long beginTime=System.currentTimeMillis();
		logger.info("删除开始时间==>"+beginTime);
		//查询出被删除节点信息
		Condition condition=new Condition();
		condition.addCondition(ID, EQUAL, nodeId, true);
		JSONObject delNode=queryNode(condition);
		if(delNode!=null){
			updateNodes(delNode);
			deleteChildrenNodes(type,delNode);
		}
		long endTime=System.currentTimeMillis();
		logger.info("删除结束时间==>"+endTime);
		logger.info("删除节点方法耗时==>"+(endTime-beginTime)/1000+"s");
		return nodeId;
	}
	private void deleteChildrenNodes(int type,JSONObject delNode){
		//1.递归查询删除的节点的子节点(包含子节点的子节点)
		JSONArray nodes=getChildNode(delNode.getString(ID));//结果集中只包含ID
		
		JSONArray addNodes=null;//新增节点
		JSONArray updateNodes=null;//修改节点
		if(!JsonUtil.isNullOrEmpty(nodes)){//存在子节点
			
			JSONObject nodesClassify=classifyNodes(nodes);
			if(nodesClassify!=null && nodesClassify.containsKey("addNodes")){//取出新增节点
				addNodes=JsonUtil.getJsonArrayByAttr(nodesClassify, "addNodes");
			}
			if(nodesClassify!=null && nodesClassify.containsKey("updateNodes")){//取出新增节点
				updateNodes=JsonUtil.getJsonArrayByAttr(nodesClassify, "updateNodes");
			}
		}
		
		//新增节点做删除
		if(!JsonUtil.isNullOrEmpty(addNodes)){
			if(type==1){
				addNodes.add(delNode);
			}
			DBUtils.delete(MTOR005, addNodes);
		}else{//没有子节点只删除当前一个节点
			if(type==1){
				addNodes=new JSONArray();
				addNodes.add(delNode);
				DBUtils.delete(MTOR005, addNodes);
			}
		}
		
		//修改节点失效
		if(!JsonUtil.isNullOrEmpty(updateNodes)){
			if(type==0){
				updateNodes.add(delNode);
			}
			Map<String, Object> map=new HashMap<String, Object>();
			map.put(MTOR021, ChangeType.INVALID.getValue());
			map.put(MTOR013, "");
			map.put(MTOR014, "");
			map.put(MTOR015, "");
			map.put(MTOR016, "");
			updateNodes=JsonUtil.addAttr(updateNodes, map);
			DBUtils.update(MTOR005, updateNodes,"");
			clearNodeResource(updateNodes);
		}else{//没有子节点只失效当前一个节点
			if(type==0){  
				updateNodes=new JSONArray();
				delNode.put(MTOR021, ChangeType.INVALID.getValue());
				delNode.put(MTOR013,"");
				delNode.put(MTOR014,"");
				delNode.put(MTOR015,"");
				delNode.put(MTOR016,"");
				updateNodes.add(delNode);
				DBUtils.update(MTOR005, updateNodes,"");
				clearNodeResource(updateNodes);
			}
		}
	}
	/**
	 * 删除或者失效节点变更节点关系
	 * @param delNode
	 */
	private void updateNodes(JSONObject delNode){
		String nodeParent=null;
		String nodeLeft=null;
		String nodeRight=null;
		//2.得到删除节点之前的父节点 左、右节点信息
		nodeParent=delNode.getString(MTOR013);
		nodeLeft=delNode.getString(MTOR015);
		nodeRight=delNode.getString(MTOR016);
		//3.变更各节点信息
		//a.如果删除的节点没有左右节点 
		if(StringUtil.isNullOrEmpty(nodeLeft) && StringUtil.isNullOrEmpty(nodeRight)){
			changeNodePosition(nodeParent, 2, NULL);//将父节点的子节点置空
		}
		//b.如果删除的节点没有左节点右有节点   
		else if(StringUtil.isNullOrEmpty(nodeLeft) && !StringUtil.isNullOrEmpty(nodeRight)){
			changeNodePosition(nodeParent, 2, nodeRight);//更改父节点的子节点为右节点
			changeNodePosition(nodeRight, 3, NULL);//右节点的左节点置空
		}
		//c.如果删除的节点有左节点没有右节点  
		else if(!StringUtil.isNullOrEmpty(nodeLeft) && StringUtil.isNullOrEmpty(nodeRight)){
			changeNodePosition(nodeLeft, 4, NULL);//将左节点的右节点置空
		}
		//d.如果存在左右节点
		else{
			changeNodePosition(nodeLeft, 4, nodeRight);//将左节点的右节点变更成右节点
			changeNodePosition(nodeRight, 3, nodeLeft);//将右节点的左节点变更成左节点
		}
	}
	/***
	 * 变更缓存节点关系
	 * @param delNode
	 */
	private JSONArray updateNodesCache(JSONObject delNode,JSONArray nodes){
		String nodeParent=null;
		String nodeLeft=null;
		String nodeRight=null;
		//2.得到删除节点之前的父节点 左、右节点信息
		nodeParent=delNode.getString(MTOR013);
		nodeLeft=delNode.getString(MTOR015);
		nodeRight=delNode.getString(MTOR016);
		
		JSONArray retArr=new JSONArray();
		//3.变更各节点信息
		//a.如果删除的节点没有左右节点 
		if(StringUtil.isNullOrEmpty(nodeLeft) && StringUtil.isNullOrEmpty(nodeRight)){
			retArr.add(changeNodePositionCache(nodeParent, 2, NULL,nodes));//将父节点的子节点置空
		}
		//b.如果删除的节点没有左节点右有节点   
		else if(StringUtil.isNullOrEmpty(nodeLeft) && !StringUtil.isNullOrEmpty(nodeRight)){
			retArr.add(changeNodePositionCache(nodeParent, 2, nodeRight,nodes));//更改父节点的子节点为右节点
			retArr.add(changeNodePositionCache(nodeRight, 3, NULL,nodes));//右节点的左节点置空
		}
		//c.如果删除的节点有左节点没有右节点  
		else if(!StringUtil.isNullOrEmpty(nodeLeft) && StringUtil.isNullOrEmpty(nodeRight)){
			retArr.add(changeNodePositionCache(nodeLeft, 4, NULL,nodes));//将左节点的右节点置空
		}
		//d.如果存在左右节点
		else{
			retArr.add(changeNodePositionCache(nodeLeft, 4, nodeRight,nodes));//将左节点的右节点变更成右节点
			retArr.add(changeNodePositionCache(nodeRight, 3, nodeLeft,nodes));//将右节点的左节点变更成左节点
		}
		return nodes;
	}
	
	/***
	 * 删除节点时  删除节点下资源
	 * fk
	 */
	private void clearNodeResource(JSONArray nodes){
		//循环查询资源
		LoopTO loop=new LoopTO(MTOR019, PID, ID, new String[]{ID}, null);
		JSONArray resourceArr=DBUtils.loopQuery(loop, nodes);
		if(!JsonUtil.isNullOrEmpty(resourceArr)){
			DBUtils.delete(MTOR019, resourceArr);
		}
	}
	//将节点分类为修改的和新增的
	private JSONObject classifyNodes(JSONArray nodes) {
		// TODO Auto-generated method stub
		JSONObject allNodes=new JSONObject();
		JSONArray updateNodes=new JSONArray();
		JSONArray addNodes=new JSONArray();
		JSONObject node=new JSONObject();
		for(Object obj:nodes){
			node=JsonUtil.getJson(obj);
			if(node!=null){
				if(StringUtil.isEqual(node.getString(MTOR021), ChangeType.UPDATE.toString())){
					updateNodes.add(node);
				}else{
					addNodes.add(node);
				}
			}
		}
		if(!JsonUtil.isNullOrEmpty(updateNodes)){
			allNodes.put("updateNodes", updateNodes);
		}
		if(!JsonUtil.isNullOrEmpty(addNodes)){
			allNodes.put("addNodes", addNodes);
		}
		return allNodes;
	}
	@Override
	public String moveNode(String nodeId,String nodeParentId,String nodeLeftId,String nodeRightId) {
		// TODO Auto-generated method stub
		if(!StringUtil.isNullOrEmpty(nodeParentId)){//如果存在父节点
			changeNodePosition(nodeId, 1, nodeParentId);//变更移动节点的父节点
		}
		
		if(!StringUtil.isNullOrEmpty(nodeLeftId)){//变更左节点
			changeNodePosition(nodeId, 3, nodeLeftId);//变更移动节点的左节点
			changeNodePosition(nodeLeftId, 4, nodeId);//左节点的右节点变更
			
		}else{
			//1.如果左节点为空  则将移动节点做为父节点的子节点
			changeNodePosition(nodeParentId, 2, nodeId);
			changeNodePosition(nodeId, 3, NULL);//变更移动节点的左节点为空
		}
		if(!StringUtil.isNullOrEmpty(nodeRightId)){//变更右节点
			changeNodePosition(nodeId, 4, nodeRightId);//变更移动节点的右节点
			changeNodePosition(nodeRightId, 3, nodeId);//变更右节点的左节点
		}else{//如果不存在右节点 将右节点置空
			changeNodePosition(nodeId, 4, NULL);
		}
		
		return nodeId;
	}
	
	//递归查询子节点
	public JSONArray getChildNode(String nodeId){
		JSONArray allNodes=new JSONArray();
		Condition condition=new Condition();
		condition.addCondition(MTOR013, EQUAL, nodeId, true);
		condition.addCondition(MTOR021, LT, ChangeType.INVALID.toString(), false);
		JSONArray nodes=queryNodes(condition);
		JSONArray childrenNodes=new JSONArray();
		for (int i = 0; i < nodes.size(); i++) {
            JSONObject levelNode = nodes.getJSONObject(i);
            String levelNodeId = levelNode.getString("id");
            allNodes.add(levelNode);
            if(!"".equals(levelNode.getString(MTOR014))){
            	childrenNodes=getChildNode(levelNodeId);
            	allNodes=JsonUtil.mergeJsonArray(allNodes,childrenNodes);
            }
        }
		return allNodes;
	}
	
	/***
	 * 查询节点信息
	 * @param condition 查询条件
	 * @return
	 */
	private JSONObject queryNode(Condition condition){
		JSONObject nodeJson=DBUtils.getObjectResult(MTOR005,null,condition);
		if(nodeJson==null){
			ApplicationException.throwCodeMesg(ErrorMessage._60003.getCode(),
					ErrorMessage._60003.getMsg()); 
		}
		return nodeJson;
	}
	/***
	 * 查询节点集信息
	 * @param condition 查询条件
	 * @return
	 */
	private JSONArray queryNodes(Condition condition){
		JSONArray nodes=DBUtils.getArrayResult(MTOR005,null,condition);
		if(nodes==null){
			ApplicationException.throwCodeMesg(ErrorMessage._60003.getCode(),
					ErrorMessage._60003.getMsg()); 
		}
		return nodes;
	}
	/**
	 * 新增节点设置节点的方位信息
	 * @param parentNode
	 * @param childNode
	 * @param leftNode
	 * @param rightNode
	 * @param treeId
	 * @return
	 */
	private NodeTo setNodePosition(String parentNode,String childNode,
			String leftNode,String rightNode,String treeId,
			String beginDate,String endDate,String nodeName){
		NodeTo node=new NodeTo();
		node.setMtor007(StringUtil.isNullOrEmpty(nodeName)?"未命名节点":nodeName);
		node.setMtor013(parentNode);
		node.setMtor014(childNode);
		node.setMtor015(leftNode);
		node.setMtor016(rightNode);
		node.setMtor021(1);  
		node.setMtor011(StringUtil.isNullOrEmpty(beginDate)?
				ToolUtil.getNowDateStr(YYYY_MM_DD)+STARTTIME:beginDate+STARTTIME);
		node.setMtor012(StringUtil.isNullOrEmpty(endDate)?MAXINVALIDDATE+ENDTIME:endDate+ENDTIME);
		node.setPid(treeId);
		node.setMtor006("NODE"+System.currentTimeMillis());
		return node;
	}
	
	/**
	 * 改变节点的位置
	 * @param nodeId 要改变节点的ID
	 * @param changeTpye 改变类型
	 * @param positionNodeId 给出位置节点ID
	 */
	private void changeNodePosition(String nodeId,int changeTpye,String positionNodeId){
		JSONObject json=new JSONObject();
		switch(changeTpye){
			case 1://改变父节点位置
				json.put(MTOR013,positionNodeId);
				break;
			case 2://改变子节点位置
				json.put(MTOR014,positionNodeId);
				break;
			case 3://改变左节点位置
				json.put(MTOR015,positionNodeId);
				break;
			case 4://改变右节点位置
				json.put(MTOR016,positionNodeId);
				break;	
		}
		json.put(ID,nodeId);
		DBUtils.update(MTOR005, json,"");
	}
	/****
	 * 变更缓存中节点关系
	 * @param nodeId
	 * @param changeTpye
	 * @param positionNodeId
	 */
	private JSONObject changeNodePositionCache(String nodeId,int changeTpye,
			String positionNodeId,JSONArray nodes){
		JSONObject json=null;
		//找到要定位的节点
		if(JsonUtil.isNullOrEmpty(nodes)){
			return null;
		}
		for(Object o:nodes){
			json=JsonUtil.getJson(o);
			if(json!=null){
				if(StringUtil.isEqual(json.getString(ID),nodeId)){
					switch(changeTpye){
						case 1://改变父节点位置
							json.put(MTOR013,positionNodeId);
							break;
						case 2://改变子节点位置  
							json.put(MTOR014,positionNodeId);
							break;
						case 3://改变左节点位置
							json.put(MTOR015,positionNodeId);
							break;
						case 4://改变右节点位置
							json.put(MTOR016,positionNodeId);
							break;	
					}
					break;
				}
			}
		}
		return json;
	}
	
	/**
	 * 监管树的操作
	 * type 1新增 2复制新增 3树维护
	 * @param addMonitorTreeTo
	 * @return
	 */
	@Override
	public String addMonitorTree(AddMonitorTreeTo addMonitorTreeTo) {
		// TODO Auto-generated method stub
		int type=addMonitorTreeTo.getType(); 
		String beginDate=addMonitorTreeTo.getBeginDate(); 
		String endDate=addMonitorTreeTo.getEndDate(); 
		String classId=addMonitorTreeTo.getClassId();
		String rootId=addMonitorTreeTo.getRootId();
		String edmcNameEn=addMonitorTreeTo.getEdmcNameEn().toLowerCase();
		
		String tempId=createTemp(classId,ChangeType.ADD.getValue(),"");
		switch(type){
			case 1://提示界面新增
				addRootNode(tempId,beginDate,endDate);
				break;
			case 2://提示界面复制
				copyTree(edmcNameEn,rootId,tempId,ChangeType.ADD.getValue(),beginDate,endDate);
				break;	
		}
		return tempId;
	}
	/**
	 * 生成临时单
	 * @param classId 监管类ID
	 * @param changeType 树的变更类型
	 * @param rootId 根节点ID
	 * @return 临时单ID
	 */
	private String createTemp(String classId,int changeType,String rootId){
		//1.生成监管类临时单
		JSONObject jsonTemp=new JSONObject();
		String tempNum=orderNumberService.generateOrderNumber("LS");
		jsonTemp.put(MTOR001,StringUtil.isNullOrEmpty(tempNum)?"LS"+System.currentTimeMillis():tempNum);
		jsonTemp.put(MTOR002, changeType);
		jsonTemp.put(MTOR003, classId);
		jsonTemp.put(MTOR004, rootId);
		return (String) DBUtils.add(MONITORTREEORDER, jsonTemp,"");
	}
	
	/**
	 * 复制监管树
	 * @param edmcNameEn edm类英文名  即监管树实体对象表
	 * @param rootId 根节点ID
	 * @param tempId 临时单ID
	 * @param changeType 变更类型
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void copyTree(String edmcNameEn,String rootId,String tempId,int changeType,String beginDate,String endDate) {
		// TODO Auto-generated method stub
		JSONArray resourceArr=new JSONArray();
		JSONArray treeFormal=new JSONArray();//正式树
		JSONArray treeTemp=new JSONArray();//临时树
		JSONArray treeArr=new JSONArray();//所有节点
		String rootBeginDate =null;//根节点生效时间
		String rootEndDate =null;//根节点失效时间
		int type =0;
		//1.查询出根节点信息
		Condition condition=new Condition();
		condition.addCondition(ID, EQUAL, rootId,true);
		JSONObject root=DBUtils.getObjectResult(edmcNameEn, null, condition);
		if(root!=null){
			treeFormal.add(root.clone());
		}else{
			ApplicationException.throwCodeMesg(ErrorMessage._60014.getCode(), 
					ErrorMessage._60014.getMsg());
			logger.info("MonitorServiceImpl类的copyTree方法："+ErrorMessage._60014.getMsg());
		}
		List<String> listNodeIds=new ArrayList<String>();
		
		//判断复制的是历史树还是未来树
		//1表示历史树与未来树的复制  
		//2表示再用树的复制
		//3表示在用树和未来树的维护
		rootBeginDate=root.getString("moni004");
		rootEndDate=root.getString("moni005");
		if(ToolUtil.dateCompare(null, rootBeginDate)){//根节点生效时间大于当前时间==》未来树
			type=changeType==1?1:3;//未来树的复制和维护逻辑不一致
		}else if(!ToolUtil.dateCompare(null, rootEndDate)){//如果根节点的失效时间小于等于当前日期==》历史树
			type=1;
		}else if(ToolUtil.dateCompare(rootBeginDate,null)){//如果根节点生效日期大于等于当前时间==>再用树
			type=changeType==1?2:3;
		}
		//2.根据根节点ID 查询正式树表的所有节点
		Result res= serviceCenterClient.getOrderMonitorTreeNodes(edmcNameEn,"" , rootId,type);
		if(res.getRetCode()==Result.RECODE_SUCCESS ){
			if(!JsonUtil.isEmpity(res.getData())){
				treeArr=JsonUtil.listToJsonArray((List)res.getData());
				treeFormal=JsonUtil.mergeJsonArray(treeFormal,treeArr);  
			}
		}else{
			logger.info("MonitorServiceImpl类的copyTree方法,查询根节点下所有子节点失败："+res.getErrMsg());
		}
		treeArr.add(root);
		treeArr=JsonUtil.removeAttr(treeArr, ID);//去除ID后进行新增
		treeArr=ToolUtil.formal2Temp(treeArr,ToolUtil.treeConvert());//转换成临时树的字段
		//添加PID项
		Map<String, Object> map=new HashMap<String, Object>();
		map.put(PID, tempId);
		map.put(MTOR021, changeType);
		if(changeType==ChangeType.ADD.getValue()){
			map.put(MTOR011, beginDate);
			map.put(MTOR012, endDate);
		}
		treeArr=JsonUtil.addAttr(treeArr, map);
		listNodeIds=(List<String>) DBUtils.add(MTOR005, treeArr,"");//新增节点信息
		//4.查询资源
		LoopTO loop=new LoopTO(edmcNameEn+".moni015", PID, ID, null, null);
		resourceArr=DBUtils.loopQuery(loop, treeFormal);
		//3.查询出所有新增的节点
		treeTemp=DBUtils.load(MTOR005, null, "base", listNodeIds);
		//5.正式树和临时树的匹对修改节点的上下左右节点
		JSONObject retobj=updateNodePosition(treeFormal,treeTemp,resourceArr);
		if(retobj!=null){
			treeTemp=JsonUtil.getJsonArrayByAttr(retobj,"treeTemp");
			if(!JsonUtil.isNullOrEmpty(treeTemp)){
				DBUtils.update(MTOR005, treeTemp,"");
			}
			
			resourceArr=JsonUtil.getJsonArrayByAttr(retobj,"resource");
			if(!JsonUtil.isNullOrEmpty(resourceArr)){
				//去除ID后新增
				resourceArr=JsonUtil.removeAttr(resourceArr, ID);
				resourceArr=ToolUtil.formal2Temp(resourceArr,ToolUtil.resourceConvert());
				DBUtils.add(MTOR019, resourceArr,"");
			}
		}
	}
	/**
	 * 正式表中节点 变更到临时单中会生成新的节点ID 此方法为节点关系维护
	 * @param treeFormal 正式树集合  
	 * @param treeTemp 临时树集合
	 * @param resourceArr 资源集合
	 * @return 新的临时树和资源集合
	 */
	private JSONObject updateNodePosition(JSONArray treeFormal,JSONArray treeTemp,JSONArray resourceArr) {
		// TODO Auto-generated method stub
		Map<String, String> map=new HashMap<String, String>();//新旧id关系维护  key==》是旧的节点ID  value==》新的节点ID
		JSONArray treeTempClone=null;
		if(!JsonUtil.isNullOrEmpty(treeFormal) && !JsonUtil.isNullOrEmpty(treeTemp)){
			treeTempClone=(JSONArray) treeTemp.clone();
			for(Object objFormal:treeFormal){//遍历正式树
				JSONObject nodeFormal=JsonUtil.getJson(objFormal);
				if(nodeFormal!=null){
					for(Object objTemp:treeTemp){//遍历临时树
						JSONObject nodeTemp=JsonUtil.getJson(objTemp);
						if(nodeTemp!=null){//匹配正式树和临时树的节点编号
							if(StringUtil.isEqual(nodeFormal.getString("moni001"),
									nodeTemp.getString("mtor006"))){//如果编号相等  则记录新旧ID
								map.put(nodeFormal.getString(ID), nodeTemp.getString(ID));
								map.put(nodeTemp.getString(ID), nodeFormal.getString(ID));
								treeTemp.remove(nodeTemp);
								break;
							}
						}
					}
				}
			}
		}
		//修改临时树的节点关系
		if(treeTempClone!=null){
			String nodeId,pNode,cNode,lNode,rNode;
			String nodeOldId,pNodeOld,cNodeOld,lNodeOld,rNodeOld;
			for(Object temp:treeTempClone){//遍历临时树  根据新旧ID关系变更
				JSONObject tempNode=JsonUtil.getJson(temp);
				if(tempNode!=null){
					nodeId=tempNode.getString(ID);
					nodeOldId=map.get(nodeId);
					pNodeOld=tempNode.getString(MTOR013);
					pNode=map.get(pNodeOld);
					cNodeOld=tempNode.getString(MTOR014);
					cNode=map.get(cNodeOld);
					lNodeOld=tempNode.getString(MTOR015);
					lNode=map.get(lNodeOld);
					rNodeOld=tempNode.getString(MTOR016);
					rNode=map.get(rNodeOld);
					tempNode.put(MTOR013, StringUtil.isNullOrEmpty(pNode)?pNodeOld:pNode);
					tempNode.put(MTOR014, StringUtil.isNullOrEmpty(cNode)?cNodeOld:cNode);
					tempNode.put(MTOR015, StringUtil.isNullOrEmpty(lNode)?lNodeOld:lNode);
					tempNode.put(MTOR016, StringUtil.isNullOrEmpty(rNode)?rNodeOld:rNode);
					tempNode.put(MTOR023, StringUtil.isNullOrEmpty(nodeOldId)?nodeId:nodeOldId);
				}
			}
		}
		
		//修改资源的pid
		if(resourceArr!=null){
			String pidOld,pidNew;
			for(Object resourceObj:resourceArr){//资源关系表  根据新旧ID关系变更PID
				JSONObject resource=JsonUtil.getJson(resourceObj);
				if(resource!=null){
					pidOld=resource.getString(PID);
					pidNew=map.get(pidOld);
					resource.put(PID, StringUtil.isNullOrEmpty(pidNew)?pidOld:pidNew);
				}
			}
		}
		
		JSONObject retObj=new JSONObject();
		retObj.put("treeTemp", treeTempClone);
		retObj.put("resource", resourceArr);
		return retObj;
	}
	/**
	 * 新增根节点
	 * @param pid 临时单ID
	 * @param beginDate 生效日期  
	 * @param endDate 失效日期
	 * @return
	 */
	private String addRootNode(String pid,String beginDate,String endDate) {
		// TODO Auto-generated method stub
		NodeTo node=new NodeTo();
		node.setMtor007(INITNODENAME);
		node.setMtor011(StringUtil.isNullOrEmpty(beginDate)?
				ToolUtil.getNowDateStr(YYYY_MM_DD):beginDate);
		endDate= StringUtil.isNullOrEmpty(endDate)?MAXINVALIDDATE:endDate;
		node.setMtor012(endDate);
		node.setMtor013(NULL);
		node.setMtor014(NULL);
		node.setMtor015(NULL);
		node.setMtor016(NULL);
		node.setMtor021(1);
		node.setPid(pid);
		node.setMtor006("NODE"+System.currentTimeMillis());
		return (String) DBUtils.add(MTOR005, JsonUtil.getJson(node),"");
	}
	@Override
	public String treeMaintaince(String classId, String rootId, String edmcNameEn) {
		// TODO Auto-generated method stub
		//先根据根节点查询是否存在临时树
		Condition condition=new Condition();
		condition.addCondition(MTOR004, EQUAL, rootId, true);
		JSONObject ret=DBUtils.getObjectResult(MONITORTREEORDER, null, condition);
		if(ret!=null){
			if(!StringUtil.isNullOrEmpty(ret.getString(ID))){
				return ret.getString(ID);//临时单ID
			}
		}
		//如果没有临时单
		String tempId=createTemp(classId,ChangeType.UPDATE.getValue(),rootId);
		copyTree(edmcNameEn.toLowerCase(),rootId,tempId,ChangeType.UPDATE.getValue(),null,null);
		
		return tempId;
	}
	
	
}
