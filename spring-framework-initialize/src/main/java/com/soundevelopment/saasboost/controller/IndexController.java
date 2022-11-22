package com.soundevelopment.saasboost.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@PropertySource("classpath:application.properties")
public class IndexController {

    @Value("${tenant.id}")
    private String tenantId;

    @Value("${global.license}")
    private String licenseKey;

    @RequestMapping("/")
    public ModelAndView index() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("index");
        mv.addObject("tenantId", getTenantId());
        mv.addObject("licenseKey", getLicenseKey());
        return mv;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getLicenseKey() {
        return licenseKey;
    }
}
