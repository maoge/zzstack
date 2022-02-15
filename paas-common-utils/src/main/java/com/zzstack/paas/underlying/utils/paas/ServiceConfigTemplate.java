package com.zzstack.paas.underlying.utils.paas;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.utils.YamlParser;
import com.zzstack.paas.underlying.utils.config.IPaasConfig;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class ServiceConfigTemplate {
    
    private static Logger logger = LoggerFactory.getLogger(ServiceConfigTemplate.class);

    private static Map<String, ConfigTemplateEnum> CONFIG_TEMPLATE_MAP = null;
    
    static {
        CONFIG_TEMPLATE_MAP = new HashMap<String, ConfigTemplateEnum>();
        ConfigTemplateEnum[] templates = ConfigTemplateEnum.values();
        for (ConfigTemplateEnum temp : templates) {
            CONFIG_TEMPLATE_MAP.put(temp.getServType(), temp);
        }
    }
    
    public static IPaasConfig getConfigTemplate(final String servType) {
        ConfigTemplateEnum tempEnum = CONFIG_TEMPLATE_MAP.get(servType);
        if (tempEnum == null) {
            String errInfo = String.format("paas config template for: {} not found ......", servType);
            logger.error("{}", errInfo);
            return null;
        }
        
        Class<? extends IPaasConfig> configMeta = tempEnum.getConfigMeta();
        String tempFile = tempEnum.getTemplateFile();
        
        YamlParser parser = new YamlParser(tempFile);
        return (IPaasConfig) parser.parseObject(configMeta);
    }
    
    public static void main(String[] args) {
        IPaasConfig config = ServiceConfigTemplate.getConfigTemplate(CONSTS.SERV_TYPE_DB_ORACLE_DG);
        System.out.println(config.getClass().getName());
    }

}
