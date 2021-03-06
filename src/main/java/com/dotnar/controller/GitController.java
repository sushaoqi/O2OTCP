package com.dotnar.controller;

import com.dotnar.bean.BaseResult;
import com.dotnar.bean.git.GitConfigRequest;
import com.dotnar.bean.git.GitInfoResponse;
import com.dotnar.bean.git.GitProject;
import com.dotnar.bean.git.GitUpdateRequest;
import com.dotnar.git.service.GitService;
import com.dotnar.util.JsonUtil;
import jdk.nashorn.internal.ir.RuntimeNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author chovans on 15/8/11.
 */
@Controller
public class GitController {

    @Autowired
    private GitService gitService;

    /**
     * 发送Clone仓库请求
     * @param gitConfigRequest
     * @return
     */
    @RequestMapping(value = "/configGit.do", method = RequestMethod.POST)
    @ResponseBody
    public String configGit(@RequestBody GitConfigRequest gitConfigRequest) {
        BaseResult baseResult = new BaseResult();
        if (StringUtils.isEmpty(gitConfigRequest.getUrl())) {
            baseResult.setErrmsg("url is empty");
        } else {
            baseResult.setErrmsg(gitService.configGit(gitConfigRequest.getUrl()));
        }
        return JsonUtil.toJSONString(baseResult);
    }

    /**
     * 获取列表
     * @return
     */
    @RequestMapping(value = "/getGitProjects.do", method = RequestMethod.GET)
    @ResponseBody
    public String getGitProjects() {
        return JsonUtil.toJSONString(gitService.findAll());
    }

    /**
     * 更新
     * @param gitUpdateRequest
     * @return
     */
    @RequestMapping("/updateGit.do")
    @ResponseBody
    public String updateGit(@RequestBody GitUpdateRequest gitUpdateRequest) {
        BaseResult baseResult = new BaseResult();
        baseResult.setErrmsg(gitService.updateGitProject(gitUpdateRequest.getId()));
        return JsonUtil.toJSONString(baseResult);
    }


    /**
     * delete git repository
     * @param gitUpdateRequest
     * @return
     */
    @RequestMapping("/deleteGit.do")
    @ResponseBody
    public String deleteGit(@RequestBody GitUpdateRequest gitUpdateRequest){
        BaseResult baseResult = new BaseResult();
        baseResult.setErrmsg(gitService.deleteGitProject(gitUpdateRequest.getId(), null, null));
        return JsonUtil.toJSONString(baseResult);
    }

    /**
     * hook 更新
     * e.g: http://localhost/projectName/O2OTCP4
     * @param projectName
     */
    @RequestMapping(value = "/projectName/{projectName}")
    @ResponseBody
    public void hookGit(@PathVariable String projectName){
        BaseResult baseResult = new BaseResult();
        baseResult.setErrmsg(gitService.hookGit(projectName));
    }

    /**
     * hook 更新(新版本)
     * e.g: http://localhost/projectName/O2OTCP4/userName/Chovans
     * @param projectName
     */
    @RequestMapping(value = "/projectName/{projectName}/userName/{userName}",produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public String hookGit(@PathVariable String projectName,@PathVariable String userName){
        BaseResult baseResult = new BaseResult();
        baseResult.setErrmsg(gitService.hookGitV2(projectName,userName));
        return JsonUtil.toJSONString(baseResult);
    }

    /**
     * 通过提供RESTful查询API，可以通过模板名获取到指定的package信息以及仓库所在的文件夹目录
     Package里面有会有一个dotnar.parent_template_name字段来实现继承功能，所以需要提供API获取文件夹目录数组（继承链）
     * @param projectName
     * @return
     */
    @RequestMapping(value = "/getInfoByTemplateName/{projectName}",produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public String getInfoByName(@PathVariable String projectName){
        GitInfoResponse gitInfoResponse = gitService.getInfoByName(null,projectName);
        return JsonUtil.toJSONString(gitInfoResponse);
    }

    /**
     * 新版本查询API
     * @param userName
     * @param projectName
     * @return
     */
    @RequestMapping(value = "/getInfoByTemplateName/{userName}/{projectName}",produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public String getInfoByUserAndTemplateName(@PathVariable String userName,@PathVariable String projectName){
        GitInfoResponse gitInfoResponse = gitService.getInfoByName(userName,projectName);
        return JsonUtil.toJSONString(gitInfoResponse);
    }
}
