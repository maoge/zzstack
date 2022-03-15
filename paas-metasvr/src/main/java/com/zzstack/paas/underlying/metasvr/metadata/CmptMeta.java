package com.zzstack.paas.underlying.metasvr.metadata;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.zzstack.paas.underlying.metasvr.bean.AccountBean;
import com.zzstack.paas.underlying.metasvr.bean.AccountSessionBean;
import com.zzstack.paas.underlying.metasvr.bean.PaasCmptVer;
import com.zzstack.paas.underlying.metasvr.bean.PaasDeployFile;
import com.zzstack.paas.underlying.metasvr.bean.PaasDeployHost;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstAttr;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaAttr;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.bean.PaasServer;
import com.zzstack.paas.underlying.metasvr.bean.PaasService;
import com.zzstack.paas.underlying.metasvr.bean.PaasSsh;
import com.zzstack.paas.underlying.metasvr.bean.PaasTopology;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.metasvr.eventbus.EventBean;
import com.zzstack.paas.underlying.metasvr.eventbus.EventBusMsg;
import com.zzstack.paas.underlying.metasvr.eventbus.EventType;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class CmptMeta {

    private static Logger logger = LoggerFactory.getLogger(CmptMeta.class);

    private Map<String,  AccountBean>      accountMap;
    private Map<String,  AccountSessionBean> accSessionMap;
    private Map<String,  AccountSessionBean> magicKeyMap;
    
    private Map<String,  String>           metaServRootMap;
    private Map<Integer, PaasMetaAttr>     metaAttrIdMap;
    private Map<String,  PaasMetaAttr>     metaAttrNameMap;
    private Map<Integer, PaasMetaCmpt>     metaCmptIdMap;
    private Map<String,  PaasMetaCmpt>     metaCmptNameMap;
    private Multimap<Integer, Integer>     metaCmptAttrMMap;
    private Map<String,  PaasInstance>     metaInstMap;
    private Multimap<String, PaasInstAttr> metaInstAttrMMap;
    private Map<String,  PaasService>      metaServiceMap;
    private Multimap<String, PaasTopology> metaTopoMMap;
    private Map<Integer, PaasDeployHost>   metaDeployHostMap;
    private Map<Integer, PaasDeployFile>   metaDeployFileMap;
    private Map<String,  PaasServer>       metaServerMap;
    private Multimap<String, PaasSsh>      metaSshMMap;
    private Map<String,  PaasCmptVer>      metaCmptVerMap;

    private ReentrantLock lock = null;

    public CmptMeta() {
        super();

        lock = new ReentrantLock();

        accountMap        = new ConcurrentHashMap<String, AccountBean>();
        accSessionMap     = new ConcurrentHashMap<String, AccountSessionBean>();
        magicKeyMap       = new ConcurrentHashMap<String, AccountSessionBean>();
        
        metaServRootMap   = new ConcurrentHashMap<String, String>();
        metaAttrIdMap     = new ConcurrentHashMap<Integer, PaasMetaAttr>();
        metaAttrNameMap   = new ConcurrentHashMap<String, PaasMetaAttr>();
        metaCmptIdMap     = new ConcurrentHashMap<Integer, PaasMetaCmpt>();
        metaCmptNameMap   = new ConcurrentHashMap<String, PaasMetaCmpt>();
        metaCmptAttrMMap  = ArrayListMultimap.create();
        metaInstMap       = new ConcurrentHashMap<String, PaasInstance>();
        metaInstAttrMMap  = ArrayListMultimap.create();
        metaServiceMap    = new ConcurrentHashMap<String, PaasService>();
        metaTopoMMap      = ArrayListMultimap.create();
        metaDeployHostMap = new ConcurrentHashMap<Integer, PaasDeployHost>();
        metaDeployFileMap = new ConcurrentHashMap<Integer, PaasDeployFile>();
        metaServerMap     = new ConcurrentHashMap<String, PaasServer>();
        metaSshMMap       = ArrayListMultimap.create();
        metaCmptVerMap    = new ConcurrentHashMap<String, PaasCmptVer>();
    }

    public boolean init() {
        try {
            release();
            loadAccount();
            loadMetaServRoot(metaServRootMap);
            loadMetaAttr(metaAttrIdMap, metaAttrNameMap);
            loadMetaCmpt(metaCmptIdMap, metaCmptNameMap);
            loadMetaCmptAttr(metaCmptAttrMMap);
            loadMetaInst(metaInstMap);
            loadMetaInstAttr(metaInstAttrMMap);
            loadMetaService(metaServiceMap);
            loadMetaTopo(metaTopoMMap);
            loadDeployHost(metaDeployHostMap);
            loadDeployFile(metaDeployFileMap);
            loadMetaServer(metaServerMap);
            loadMetaSsh(metaSshMMap);
            loadMetaCmptVersion(metaCmptVerMap);
        } catch (Exception e) {
            logger.error("load cmpt data caught:{}", e.getMessage());
            return false;
        }

        return true;
    }

    public void release() {
        lock.lock();
        try {
            metaServRootMap.clear();
            metaAttrIdMap.clear();
            metaAttrNameMap.clear();
            metaCmptIdMap.clear();
            metaCmptNameMap.clear();
            metaCmptAttrMMap.clear();
            metaInstMap.clear();
            metaInstAttrMMap.clear();
            metaServiceMap.clear();
            metaTopoMMap.clear();
            metaDeployHostMap.clear();
            metaDeployFileMap.clear();
            metaServerMap.clear();
            metaSshMMap.clear();
            metaCmptVerMap.clear();
        } finally {
            lock.unlock();
        }
    }
    
    public void reloadMetaData(String type) {
        switch (type) {
        case FixHeader.HEADER_ALL:
            reloadAll();
            break;
        case FixHeader.HEADER_META_SERVICE:
            reloadMetaService();
            break;
        case FixHeader.HEADER_META_ATTR:
            reloadMetaAttr();
            break;
        case FixHeader.HEADER_META_CMPT:
            reloadMetaCmpt();
            break;
        case FixHeader.HEADER_META_CMPT_ATTR:
            reloadMetaCmptAttr();
            break;
        case FixHeader.HEADER_META_META_INST:
            reloadInstAndAttr();
            break;
        case FixHeader.HEADER_META_TOPO:
            reloadMetaTopo();
            break;
        case FixHeader.HEADER_META_DEPLOY:
            reloadMetaDeploy();
            break;
        case FixHeader.HEADER_META_SERVER_SSH:
            reloadMetaServerAndSsh();
            break;
        case FixHeader.HEADER_META_CMPT_VERSION:
            reloadMetaCmptVersion();
            break;
        }
    }
    
    // 重载全部元数据
    public void reloadAll() {
        reloadMetaService();
        reloadMetaAttr();
        reloadMetaCmpt();
        reloadMetaCmptAttr();
        reloadInstAndAttr();
        reloadMetaTopo();
        reloadMetaDeploy();
        reloadMetaServerAndSsh();
        reloadMetaCmptVersion();
    }

    // 重载服务列表元数据
    public void reloadMetaService() {
        Map<String, PaasService> tmepMetaServiceMap = new ConcurrentHashMap<String, PaasService>();
        loadMetaService(tmepMetaServiceMap);
        
        lock.lock();
        try {
            if (!tmepMetaServiceMap.isEmpty()) {
                Map<String, PaasService> oldMetaServiceMap = metaServiceMap;
                metaServiceMap = tmepMetaServiceMap;
                oldMetaServiceMap.clear();
                oldMetaServiceMap = null;
                logger.info("reloadMetaService ......");
            }
        } finally {
            lock.unlock();
        }
    }
    
    // 重载属性元数据
    public void reloadMetaAttr() {
        Map<Integer, PaasMetaAttr> tempMetaAttrIdMap = new ConcurrentHashMap<Integer, PaasMetaAttr>();
        Map<String,  PaasMetaAttr> tempMetaAttrNameMap = new ConcurrentHashMap<String, PaasMetaAttr>();
        loadMetaAttr(tempMetaAttrIdMap, tempMetaAttrNameMap);
        
        lock.lock();
        try {
            if (!tempMetaAttrIdMap.isEmpty() && !tempMetaAttrNameMap.isEmpty()) {
                Map<Integer, PaasMetaAttr> oldMetaAttrIdMap = metaAttrIdMap;
                Map<String, PaasMetaAttr> oldMetaAttrNameMap = metaAttrNameMap;
                metaAttrIdMap = tempMetaAttrIdMap;
                metaAttrNameMap = tempMetaAttrNameMap;
                oldMetaAttrIdMap.clear();
                oldMetaAttrNameMap.clear();
                oldMetaAttrIdMap = null;
                oldMetaAttrNameMap = null;
            }
        } finally {
            lock.unlock();
        }
    }
    
    // 重载组件元数据
    public void reloadMetaCmpt() {
        Map<Integer, PaasMetaCmpt> tempMetaCmptIdMap = new ConcurrentHashMap<Integer, PaasMetaCmpt>();
        Map<String, PaasMetaCmpt> tempMetaCmptNameMap = new ConcurrentHashMap<String, PaasMetaCmpt>();
        loadMetaCmpt(tempMetaCmptIdMap, tempMetaCmptNameMap);
        
        lock.lock();
        try {
            if (!tempMetaCmptIdMap.isEmpty() && !tempMetaCmptNameMap.isEmpty()) {
                Map<Integer, PaasMetaCmpt> oldMetaCmptIdMap = metaCmptIdMap;
                Map<String, PaasMetaCmpt> oldMetaCmptNameMap = metaCmptNameMap;
                metaCmptIdMap = tempMetaCmptIdMap;
                metaCmptNameMap = tempMetaCmptNameMap;
                oldMetaCmptIdMap.clear();
                oldMetaCmptNameMap.clear();
                oldMetaCmptIdMap = null;
                oldMetaCmptNameMap = null;
            }
        } finally {
            lock.unlock();
        }
    }
    
    // 重载组件属性绑定元数据
    public void reloadMetaCmptAttr() {
        Multimap<Integer, Integer> tempMetaCmptAttrMMap = ArrayListMultimap.create();
        loadMetaCmptAttr(tempMetaCmptAttrMMap);
        
        lock.lock();
        try {
            if (!tempMetaCmptAttrMMap.isEmpty()) {
                Multimap<Integer, Integer> oldMetaCmptAttrMMap = metaCmptAttrMMap;
                metaCmptAttrMMap = tempMetaCmptAttrMMap;
                oldMetaCmptAttrMMap.clear();
                oldMetaCmptAttrMMap = null;
            }
        } finally {
            lock.unlock();
        }
    }
    
    // 重载实例元数据
    public void reloadInstAndAttr() {
        Map<String,  PaasInstance> tempMetaInstMap = new ConcurrentHashMap<String, PaasInstance>();
        Multimap<String, PaasInstAttr> tempMetaInstAttrMMap = ArrayListMultimap.create();
        loadMetaInst(tempMetaInstMap);
        loadMetaInstAttr(tempMetaInstAttrMMap);
        
        lock.lock();
        try {
            if (!tempMetaInstMap.isEmpty() && !tempMetaInstAttrMMap.isEmpty()) {
                Map<String, PaasInstance> oldMetaInstMap = metaInstMap;
                Multimap<String, PaasInstAttr> oldMetaInstAttrMMap = metaInstAttrMMap;
                metaInstMap = tempMetaInstMap;
                metaInstAttrMMap = tempMetaInstAttrMMap;
                oldMetaInstMap.clear();
                oldMetaInstMap = null;
                oldMetaInstAttrMMap.clear();
                oldMetaInstAttrMMap = null;
            }
        } finally {
            lock.unlock();
        }
    }
    
    // 重载拓扑元数据
    public void reloadMetaTopo() {
        Multimap<String, PaasTopology> tempMetaTopoMMap = ArrayListMultimap.create();
        loadMetaTopo(tempMetaTopoMMap);
        
        lock.lock();
        try {
            if (!tempMetaTopoMMap.isEmpty()) {
                Multimap<String, PaasTopology> oldMetaTopoMMap = metaTopoMMap;
                metaTopoMMap = tempMetaTopoMMap;
                oldMetaTopoMMap.clear();
                oldMetaTopoMMap = null;
            }
        } finally {
            lock.unlock();
        }
    }

    // 重载部署物料元数据
    public void reloadMetaDeploy() {
        Map<Integer, PaasDeployHost> tempMetaDeployHostMap = new ConcurrentHashMap<Integer, PaasDeployHost>();
        Map<Integer, PaasDeployFile> tempMetaDeployFileMap = new ConcurrentHashMap<Integer, PaasDeployFile>();
        loadDeployHost(tempMetaDeployHostMap);
        loadDeployFile(tempMetaDeployFileMap);
        
        lock.lock();
        try {
            if (!tempMetaDeployHostMap.isEmpty() && !tempMetaDeployFileMap.isEmpty()) {
                Map<Integer, PaasDeployHost> oldMetaDeployHostMap = metaDeployHostMap;
                Map<Integer, PaasDeployFile> oldMetaDeployFileMap = metaDeployFileMap;
                metaDeployHostMap = tempMetaDeployHostMap;
                metaDeployFileMap = tempMetaDeployFileMap;
                oldMetaDeployHostMap.clear();
                oldMetaDeployFileMap.clear();
                oldMetaDeployHostMap = null;
                oldMetaDeployFileMap = null;
            }
        } finally {
            lock.unlock();
        }
    }
    
    // 重载部署发布服务器元数据
    public void reloadMetaServerAndSsh() {
        Map<String,  PaasServer> tempMetaServerMap = new ConcurrentHashMap<String, PaasServer>();
        Multimap<String, PaasSsh> tempMetaSshMMap = ArrayListMultimap.create();
        loadMetaServer(tempMetaServerMap);
        loadMetaSsh(tempMetaSshMMap);
        
        lock.lock();
        try {
            if (!tempMetaServerMap.isEmpty() && !tempMetaSshMMap.isEmpty()) {
                Map<String,  PaasServer> oldMetaServerMap = metaServerMap;
                Multimap<String, PaasSsh> oldMetaSshMMap = metaSshMMap;
                metaServerMap = tempMetaServerMap;
                metaSshMMap = tempMetaSshMMap;
                oldMetaServerMap.clear();
                oldMetaSshMMap.clear();
                oldMetaServerMap = null;
                oldMetaSshMMap = null;
            }
        } finally {
            lock.unlock();
        }
    }
    
    // 重载组件版本元数据
    public void reloadMetaCmptVersion() {
        Map<String, PaasCmptVer> tempMetaCmptVerMap = new ConcurrentHashMap<String, PaasCmptVer>();
        loadMetaCmptVersion(tempMetaCmptVerMap);
        
        if (!tempMetaCmptVerMap.isEmpty()) {
            Map<String, PaasCmptVer> oldMetaCmptVerMap = metaCmptVerMap;
            metaCmptVerMap = tempMetaCmptVerMap;
            oldMetaCmptVerMap.clear();
            oldMetaCmptVerMap = null;
        }
    }
    
    public JsonObject getMetaData2Json() {
        JsonObject root = new JsonObject();

        JsonArray servArr = new JsonArray();
        for (Entry<String, String> entry : metaServRootMap.entrySet()) {
            JsonObject item = new JsonObject();
            item.put(entry.getKey(), entry.getValue());
            servArr.add(item);
        }
        root.put("metaServRootMap", servArr);

        JsonArray metaAttrIdArr = new JsonArray();
        for (Entry<Integer, PaasMetaAttr> entry : metaAttrIdMap.entrySet()) {
            JsonObject item = new JsonObject();
            item.put(String.valueOf(entry.getKey()), entry.getValue().toJsonObject());
            metaAttrIdArr.add(item);
        }
        root.put("metaAttrIdMap", metaAttrIdArr);

        JsonArray metaCmptIdArr = new JsonArray();
        for (Entry<Integer, PaasMetaCmpt> entry : metaCmptIdMap.entrySet()) {
            JsonObject item = new JsonObject();
            item.put(String.valueOf(entry.getKey()), entry.getValue().toJsonObject());
            metaCmptIdArr.add(item);
        }
        root.put("metaCmptIdMap", metaCmptIdArr);

        JsonArray metaCmptAttrArr = new JsonArray();
        Map<Integer, Collection<Integer>> cmptAttrMMap = metaCmptAttrMMap.asMap();
        for (Entry<Integer, Collection<Integer>> entry : cmptAttrMMap.entrySet()) {
            JsonObject item = new JsonObject();

            Integer cmptId = entry.getKey();
            Collection<Integer> attrIds = entry.getValue();
            JsonArray attrIdArr = new JsonArray();
            for (Integer attrId : attrIds)
                attrIdArr.add(attrId);

            item.put(String.valueOf(cmptId), attrIdArr);
            metaCmptAttrArr.add(item);
        }
        root.put("metaCmptAttrMMap", metaCmptAttrArr);

        JsonArray metaInstArr = new JsonArray();
        for (Entry<String, PaasInstance> entry : metaInstMap.entrySet()) {
            JsonObject item = new JsonObject();
            item.put(entry.getKey(), entry.getValue().toJsonObject());
            metaInstArr.add(item);
        }
        root.put("metaInstMap", metaInstArr);

        JsonArray metaInstAttrArr = new JsonArray();
        Map<String, Collection<PaasInstAttr>> instAttrMMap = metaInstAttrMMap.asMap();
        for (Entry<String, Collection<PaasInstAttr>> entry : instAttrMMap.entrySet()) {
            JsonObject item = new JsonObject();
            JsonArray attrArr = new JsonArray();
            for (PaasInstAttr instAttr : entry.getValue()) {
                attrArr.add(instAttr.toJsonObject());
            }

            item.put(entry.getKey(), attrArr);
            metaInstAttrArr.add(item);
        }
        root.put("metaInstAttrMMap", metaInstAttrArr);

        JsonArray metaServiceArr = new JsonArray();
        for (Entry<String, PaasService> entry : metaServiceMap.entrySet()) {
            JsonObject item = new JsonObject();
            item.put(entry.getKey(), entry.getValue().toJsonObject());
            metaServiceArr.add(item);
        }
        root.put("metaServiceMap", metaServiceArr);

        JsonArray metaTopoArr = new JsonArray();
        Map<String, Collection<PaasTopology>> topoMMap = metaTopoMMap.asMap();
        for (Entry<String, Collection<PaasTopology>> entry : topoMMap.entrySet()) {
            Collection<PaasTopology> topos = entry.getValue();
            JsonArray topoArr = new JsonArray();
            for (PaasTopology topo : topos)
                topoArr.add(topo.toJsonObject());

            JsonObject item = new JsonObject();
            item.put(entry.getKey(), topoArr);

            metaTopoArr.add(item);
        }
        root.put("metaTopoMMap", metaTopoArr);

        JsonArray deployHostArr = new JsonArray();
        for (Entry<Integer, PaasDeployHost> entry : metaDeployHostMap.entrySet()) {
            JsonObject item = new JsonObject();
            item.put(entry.getKey().toString(), entry.getValue().toJsonObject());

            deployHostArr.add(item);
        }
        root.put("metaDeployHostMap", deployHostArr);

        JsonArray deployFileArr = new JsonArray();
        for (Entry<Integer, PaasDeployFile> entry : metaDeployFileMap.entrySet()) {
            JsonObject item = new JsonObject();
            item.put(entry.getKey().toString(), entry.getValue().toJsonObject());

            deployFileArr.add(item);
        }
        root.put("metaDeployFileMap", deployFileArr);

        JsonArray serverArr = new JsonArray();
        for (Entry<String, PaasServer> entry : metaServerMap.entrySet()) {
            JsonObject item = new JsonObject();
            item.put(entry.getKey(), entry.getValue().toJsonObject());

            serverArr.add(item);
        }
        root.put("metaServerMap", serverArr);

        JsonArray sshArr = new JsonArray();
        Map<String, Collection<PaasSsh>> sshMMap = metaSshMMap.asMap();
        for (Entry<String, Collection<PaasSsh>> entry : sshMMap.entrySet()) {
            JsonObject item = new JsonObject();
            JsonArray subItems = new JsonArray();
            for (PaasSsh ssh : entry.getValue()) {
                subItems.add(ssh.toJsonObject());
            }
            item.put(entry.getKey(), subItems);
            
            sshArr.add(item);
        }
        root.put("metaSshMMap", sshArr);
        
        JsonArray cmptVer = new JsonArray();
        for (Entry<String, PaasCmptVer> entry : metaCmptVerMap.entrySet()) {
            JsonObject item = new JsonObject();
            item.put(entry.getKey(), entry.getValue().toJsonObject());
            
            cmptVer.add(item);
        }
        root.put("metaCmptVerMap", cmptVer);

        return root;
    }

    public AccountBean getAccount(String accName) {
        return accountMap.get(accName);
    }
    
    public void modPasswd(String accName, String passwd) {
        AccountBean account = accountMap.get(accName);
        if (account != null) {
            account.setPasswd(passwd);
        }
    }
    
    public AccountSessionBean getAccSession(String accName) {
        return accSessionMap.get(accName);
    }
    
    public AccountSessionBean getSessionByMagicKey(String magicKey) {
        return magicKeyMap.get(magicKey);
    }

    public PaasMetaAttr getAttr(int attrId) {
        return metaAttrIdMap.get(attrId);
    }

    public PaasMetaCmpt getCmptById(int cmptId) {
        return metaCmptIdMap.get(cmptId);
    }

    public PaasMetaCmpt getCmptByName(String cmptName) {
        return metaCmptNameMap.get(cmptName);
    }
    
    public Vector<PaasMetaAttr> getCmptAttrs(int cmptId) {
        Vector<PaasMetaAttr> attrVec = null;
        
        lock.lock();
        try {
            Collection<Integer> attrIdList = metaCmptAttrMMap.get(cmptId);
            if (attrIdList == null)
                return null;
    
            attrVec = new Vector<PaasMetaAttr>();
            for (Integer attrId : attrIdList) {
                PaasMetaAttr attr = metaAttrIdMap.get(attrId);
                if (attr != null)
                    attrVec.add(attr);
            }
        } catch (Exception e) {
            logger.error("getCmptAttrs, cmptId:{} caught exception, {}", cmptId, e.getMessage(), e);
        } finally {
            lock.unlock();
        }

        return attrVec;
    }

    public String getServRootCmpt(String servType) {
        String servRootName = metaServRootMap.get(servType);
        return servRootName != null ? servRootName : "";
    }
    
    public boolean isInstServRootCmpt(String instID) {
        PaasInstance inst = metaInstMap.get(instID);
        if (inst == null)
            return false;
        
        int cmptID = inst.getCmptId();
        PaasMetaCmpt cmpt = metaCmptIdMap.get(cmptID);
        return isServRootCmpt(cmpt);
    }
    
    public boolean isServRootCmpt(PaasMetaCmpt cmpt) {
        if (cmpt == null)
            return false;
        
        String servRootCmptName = metaServRootMap.get(cmpt.getServType());
        if (servRootCmptName == null)
            return false;
        
        return servRootCmptName.equals(cmpt.getCmptName());
    }
    
    public boolean isServRootCmpt(String servType, String cmptName) {
        String servRootCmptName = metaServRootMap.get(servType);
        if (servRootCmptName == null)
            return false;
        
        return servRootCmptName.equals(cmptName);
    }

    public void addService(PaasService service) {
        metaServiceMap.put(service.getInstId(), service);
    }

    public void delService(String instId) {
        metaServiceMap.remove(instId);
    }

    public void reloadService(String instId) {
        PaasService service = new PaasService();
        if (MetaDataDao.getServiceById(instId, service)) {
            metaServiceMap.put(instId, service);
        }
    }

    public PaasService getService(String instId) {
        return metaServiceMap.get(instId);
    }
    
    public Map<String, PaasService> getMetaServiceMap() {
        return metaServiceMap;
    }

    public void addInstance(PaasInstance instance) {
        metaInstMap.put(instance.getInstId(), instance);
    }

    public boolean delInstance(String instId) {
        return metaInstMap.remove(instId) != null;
    }

    public void updInstPos(PaasInstance inst) {
        PaasInstance instRef = metaInstMap.get(inst.getInstId());
        if (instRef != null) {
            instRef.setPosX(inst.getPosX());
            instRef.setPosY(inst.getPosY());
            instRef.setWidth(inst.getWidth());
            instRef.setHeight(inst.getHeight());
            instRef.setRow(inst.getRow());
            instRef.setCol(inst.getCol());
        }
    }

    public PaasInstance getInstance(String instId) {
        return metaInstMap.get(instId);
    }

    public void addInstAttr(PaasInstAttr instAttr) {
        lock.lock();
        try {
            Collection<PaasInstAttr> attrCollection = metaInstAttrMMap.get(instAttr.getInstId());
            int attrId = instAttr.getAttrId();
            PaasInstAttr attrOld = null;
            
            Iterator<PaasInstAttr> it = attrCollection.iterator();
            while (it.hasNext()) {
                PaasInstAttr attr = it.next();
                if (attr.getAttrId() == attrId) {
                    attrOld = attr;
                    break;
                }
            }
            
            if (attrOld == null) {
                attrCollection.add(instAttr);
            } else {
                attrOld.setAttrValue(instAttr.getAttrValue());
            }
        } catch(Exception e) {
            logger.error("addInstAttr caught excetpion:{}", e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }
    
    public void updInstAttr(PaasInstAttr instAttr) {
        lock.lock();
        try {
            int attrId = instAttr.getAttrId();
            Collection<PaasInstAttr> attrs = metaInstAttrMMap.get(instAttr.getInstId());
            for (PaasInstAttr attr : attrs) {
                if (attr.getAttrId() == attrId) {
                    attr.setAttrValue(instAttr.getAttrValue());
                    break;
                }
            }
        
        } catch(Exception e) {
            logger.error("updInstAttr caught excetpion:{}", e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    public boolean delInstAttr(String instId) {
        boolean ret = false;
        lock.lock();
        try {
            ret = metaInstAttrMMap.removeAll(instId) != null;
        } catch(Exception e) {
            logger.error("delInstAttr caught excetpion:{}", e.getMessage(), e);
        } finally {
            lock.unlock();
        }
        return ret;
    }

    public Collection<PaasInstAttr> getInstAttrs(String instId) {
        Collection<PaasInstAttr> attrs = null;
        lock.lock();
        try {
            attrs = metaInstAttrMMap.get(instId);
        } catch(Exception e) {
            logger.error("getInstAttrs caught excetpion:{}", e.getMessage(), e);
        } finally {
            lock.unlock();
        }
        return attrs;
    }
    
    public final PaasInstAttr getInstAttr(String instId, int attrId) {
        PaasInstAttr ret = null;
        lock.lock();
        try {
            Collection<PaasInstAttr> attrs = metaInstAttrMMap.get(instId);
            for (PaasInstAttr attr : attrs) {
                if (attr.getAttrId() == attrId) {
                    ret = attr;
                    break;
                }
            }
        } catch(Exception e) {
            logger.error("getInstAttr caught excetpion:{}", e.getMessage(), e);
        } finally {
            lock.unlock();
        }
        
        return ret;
    }

    public void addTopo(PaasTopology topo) {
        lock.lock();
        try {
            metaTopoMMap.put(topo.getInstId1(), topo);
        } catch(Exception e) {
            logger.error("addTopo caught excetpion:{}", e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }
    
    public void modTopo(PaasTopology topo) {
        lock.lock();
        try {
            String parentID = topo.getInstId1();
            PaasInstance inst = metaInstMap.get(parentID);
            if (inst.getCmptId() == 801) {  // 'HA_CONTAINER'
                metaTopoMMap.removeAll(parentID);
            }
            
            metaTopoMMap.put(topo.getInstId1(), topo);
        } catch(Exception e) {
            logger.error("addTopo caught excetpion:{}", e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    public void delTopo(String parentId) {
        lock.lock();
        try {
            metaTopoMMap.removeAll(parentId);
        } catch(Exception e) {
            logger.error("delTopo parentId:{} caught excetpion:{}", parentId, e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }
    
    public void delTopo(String parentId, String instId) {
        lock.lock();
        try {
            metaTopoMMap.removeAll(instId);
    
            Collection<PaasTopology> parentSubs = metaTopoMMap.get(parentId);
            if (parentSubs == null || parentSubs.isEmpty())
                return;
            for (PaasTopology topo : parentSubs) {
                String toeId = topo.getToe(parentId);
                if (toeId.equals(instId)) {
                    parentSubs.remove(topo);
                    break;
                }
            }
        } catch(Exception e) {
            logger.error("delTopo with parentId:{}, instId:{}, caught excetpion:{}", parentId, instId, e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }
    
    public void delAllSubTopo(String parentId) {
        lock.lock();
        try {
            metaTopoMMap.removeAll(parentId);
        } catch(Exception e) {
            logger.error("delAllSubTopo parentId:{}, caught excetpion:{}", parentId, e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    public Collection<PaasTopology> getInstRelations(String instId) {
        return metaTopoMMap.get(instId);
    }

    public Collection<PaasTopology> getSameLevelInstList(String servInstId, String instId) {
        Collection<PaasTopology> containerList = metaTopoMMap.get(servInstId);
        if (containerList == null)
            return null;
        
        for (PaasTopology topo : containerList) {
            String containerId = topo.getInstId2();
            Collection<PaasTopology> instList = metaTopoMMap.get(containerId);
            if (instList == null || instList.isEmpty())
                continue;
            
            for (PaasTopology subTopo : instList) {
                if (subTopo.getInstId2().equals(instId)) {
                    return instList;
                }
            }
        }
        
        return null;
    }
    
    public void getInstRelations(String instId, Vector<PaasTopology> relations) {
        lock.lock();
        try {
            Collection<PaasTopology> topos = metaTopoMMap.get(instId);
            if (topos == null)
                return;

            for (PaasTopology topo : topos) {
                relations.add(topo);
            }
        } catch (Exception e) {
            logger.error("getInstRelations instId:{}, caught excetpion:{}", instId, e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }
    
    public boolean isTopoRelationExists(String parentId) {
        boolean res = false;
        lock.lock();
        try {
            Collection<PaasTopology> topos = metaTopoMMap.get(parentId);
            if (topos != null && topos.size() > 0) {
                res = true;
            }
        } catch (Exception e) {
            logger.error("isTopoRelationExists parentId:{}, caught excetpion:{}", parentId, e.getMessage(), e);
        } finally {
            lock.unlock();
        }
        return res;
    }
    
    public boolean isTopoRelationExists(String parentId, String subId) {
        boolean res = false;
        lock.lock();
        try {
            Collection<PaasTopology> topos = metaTopoMMap.get(parentId);
            for (PaasTopology topo : topos) {
                res = subId.equals(topo.getInstId2());
                if (res) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("isTopoRelationExists parentId:{}, caught excetpion:{}", parentId, e.getMessage(), e);
        } finally {
            lock.unlock();
        }
        return res;
    }

    public void addServer(PaasServer server) {
        metaServerMap.put(server.getServerIp(), server);
    }

    public void delServer(String serverIp) {
        metaServerMap.remove(serverIp);
    }

    public boolean isServerNull(String serverIp) {
        boolean result = false;
        lock.lock();
        try {
            Collection<PaasSsh> sshList = metaSshMMap.get(serverIp);
            if (sshList == null)
                return true;
            
            result = sshList.size() > 0 ? false : true;
        } finally {
            lock.unlock();
        }
        
        return result;
    }

    public void addSsh(PaasSsh ssh) {
        lock.lock();
        try {
            metaSshMMap.put(ssh.getServerIp(), ssh);
        } finally {
            lock.unlock();
        }
    }

    public void delSsh(String serverIp, String sshId) {
        lock.lock();
        try {
            Collection<PaasSsh> sshList = metaSshMMap.get(serverIp);
            if (sshList == null)
                return;
    
            Iterator<PaasSsh> it = sshList.iterator();
            while (it.hasNext()) {
                PaasSsh ref = it.next();
                if (ref.getSshId().equals(sshId)) {
                    it.remove();
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("delSsh serverIp:{}, sshId:{}, caught exception:{}", serverIp, sshId, e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    public void modSsh(String serverIp, String sshId, String sshName, String sshPwd, int sshPort) {
        lock.lock();
        try {
            Collection<PaasSsh> sshList = metaSshMMap.get(serverIp);
            if (sshList == null)
                return;
    
            Iterator<PaasSsh> it = sshList.iterator();
            while (it.hasNext()) {
                PaasSsh ref = it.next();
                if (ref.getSshId().equals(sshId)) {
                    ref.setSshName(sshName);
                    ref.setSshPwd(sshPwd);
                    ref.setSshPort(sshPort);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("modSsh serverIp:{}, sshId:{}, caught exception:{}", serverIp, sshId, e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    public PaasSsh getSshById(String sshId) {
        PaasSsh ssh = null;
        lock.lock();
        try {
            Collection<Entry<String, PaasSsh>> entrys = metaSshMMap.entries();
            for (Entry<String, PaasSsh> entry : entrys) {
                PaasSsh ref = entry.getValue();
                if (ref.getSshId().equals(sshId)) {
                    ssh = ref;
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("getSshById sshId:{}, caught exception:{}", sshId, e.getMessage());
        } finally {
            lock.unlock();
        }
        
        return ssh;
    }

    public PaasDeployFile getDeployFile(int fileId) {
        return metaDeployFileMap.get(fileId);
    }

    public PaasDeployHost getDeployHost(int hostId) {
        return metaDeployHostMap.get(hostId);
    }
    
    public void updInstPreEmbadded(String instId, String preEmbadded) {
        PaasInstAttr ref = getInstAttr(instId, 320);  // 320 -> 'PRE_EMBEDDED'
        if (ref == null)
            return;
        
        ref.setAttrValue(preEmbadded);
    }

    public void updInstDeploy(String instId, String deployFlag) {
        PaasInstance ref = metaInstMap.get(instId);
        if (ref == null)
            return;
        
        boolean flag = deployFlag.equals(CONSTS.STR_SAVED) ? false : true;
        ref.setDeployed(flag);
        ref.setStatus(deployFlag);
    }

    public void updServDeploy(String instId, String deployFlag) {
        PaasService ref = metaServiceMap.get(instId);
        if (ref == null)
            return;
        boolean flag = deployFlag.equals(CONSTS.STR_TRUE);
        ref.setDeployed(flag);
    }

    public String getInstCmptName(String instId) {
        PaasInstance instRef = metaInstMap.get(instId);
        if (instRef == null)
            return null;

        PaasMetaCmpt cmptRef = metaCmptIdMap.get(instRef.getCmptId());
        if (cmptRef == null)
            return null;

        return cmptRef.getCmptName();
    }
    
    public boolean isInstAttrExists(String instID, int attrID) {
        boolean res = false;
        lock.lock();
        try {
            Collection<PaasInstAttr> attrs = metaInstAttrMMap.get(instID);
            
            for (PaasInstAttr attr : attrs) {
                if (attr.getAttrId() == attrID) {
                    res = true;
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("isInstAttrExists instID:{}, attrID:{}, caught exception:{}", instID, attrID, e.getMessage());
        } finally {
            lock.unlock();
        }
        
        return res;
    }
    
    public JsonArray getSurpportSSHList(String servClazz) {
        JsonArray res = new JsonArray();
        lock.lock();
        try {
            Set<Entry<String, PaasServer>> entrySet = metaServerMap.entrySet();
            for (Entry<String, PaasServer> entry : entrySet) {
                String serverIP = entry.getKey();
                
                Collection<PaasSsh> sshs = metaSshMMap.get(serverIP);
                if (sshs == null || sshs.isEmpty())
                    continue;
                
                JsonArray subSSH = null;
                for (PaasSsh ssh : sshs) {
                    if (!ssh.getServClazz().equals(servClazz))
                        continue;
                    
                    if (subSSH == null)
                        subSSH = new JsonArray();
                    
                    JsonObject item = new JsonObject();
                    item.put(FixHeader.HEADER_SSH_NAME, ssh.getSshName());
                    item.put(FixHeader.HEADER_SSH_ID,   ssh.getSshId());
                    subSSH.add(item);
                }
                
                if (subSSH != null) {
                    JsonObject o = new JsonObject();
                    o.put(FixHeader.HEADER_SERVER_IP, serverIP);
                    o.put(FixHeader.HEADER_SSH_LIST, subSSH);
                    
                    res.add(o);
                }
            }
        } catch (Exception e) {
            logger.error("getSurpportSSHList servClazz:{}, caught exception:{}", servClazz, e.getMessage());
        } finally {
            lock.unlock();
        }
        
        return res;
    }
    
    public JsonArray getServListFromCache(String servType) {
        JsonArray res = new JsonArray();
        lock.lock();
        try {
            Set<Entry<String, PaasService>> entrySet = metaServiceMap.entrySet();
            for (Entry<String, PaasService> entry : entrySet) {
                PaasService serv = entry.getValue();
                String instID = serv.getInstId();
                
                // 未部署服务不加入可用服务列表
                if (!serv.isDeployed())
                    continue;
                
                // 只录了服务还未做初始化
                if (!metaInstMap.containsKey(instID) || !metaTopoMMap.containsKey(instID))
                    continue;
                
                if (serv.getServType().equals(servType)) {
                    JsonObject item = new JsonObject();
                    item.put(FixHeader.HEADER_SERV_NAME, serv.getServName());
                    item.put(FixHeader.HEADER_INST_ID,   instID);
                    
                    res.add(item);
                }
            }
        } catch (Exception e) {
            logger.error("getServListFromCache servType:{}, caught exception:{}", servType);
        } finally {
            lock.unlock();
        }
        
        return res;
    }

    private void loadMetaServRoot(Map<String,  String> metaServRootMapRef) {
        metaServRootMapRef.put(CONSTS.SERV_TYPE_CACHE_REDIS_CLUSTER,      "REDIS_SERV_CLUSTER_CONTAINER");
        metaServRootMapRef.put(CONSTS.SERV_TYPE_CACHE_REDIS_MASTER_SLAVE, "REDIS_SERV_MS_CONTAINER");
        metaServRootMapRef.put(CONSTS.SERV_TYPE_CACHE_REDIS_HA_CLUSTER,   "REDIS_HA_CLUSTER_CONTAINER");

        metaServRootMapRef.put(CONSTS.SERV_TYPE_DB_TDENGINE,              "TDENGINE_SERV_CONTAINER");
        metaServRootMapRef.put(CONSTS.SERV_TYPE_DB_ORACLE_DG,             "ORACLE_DG_SERV_CONTAINER");
        metaServRootMapRef.put(CONSTS.SERV_TYPE_DB_TIDB,                  "TIDB_SERV_CONTAINER");
        metaServRootMapRef.put(CONSTS.SERV_TYPE_DB_CLICKHOUSE,            "CLICKHOUSE_SERV_CONTAINER");
        metaServRootMapRef.put(CONSTS.SERV_TYPE_DB_VOLTDB,                "VOLTDB_SERV_CONTAINER");
        metaServRootMapRef.put(CONSTS.SERV_TYPE_DB_YUGABYTEDB,            "YUGABYTEDB_SERV_CONTAINER");

        metaServRootMapRef.put(CONSTS.SERV_TYPE_MQ_ROCKETMQ,              "ROCKETMQ_SERV_CONTAINER");
        metaServRootMapRef.put(CONSTS.SERV_TYPE_MQ_PULSAR,                "PULSAR_SERV_CONTAINER");
        
        metaServRootMapRef.put(CONSTS.SERV_TYPE_SERVERLESS_APISIX,        "APISIX_SERV_CONTAINER");
        metaServRootMapRef.put(CONSTS.SERV_TYPE_SMS_GATEWAY,              "SMS_GATEWAY_SERV_CONTAINER");
        metaServRootMapRef.put(CONSTS.SERV_TYPE_SMS_QUERY,                "SMS_QUERY_SERV_CONTAINER");
    }

    private void loadAccount() {
        MetaDataDao.loadAccount(accountMap);
    }

    private void loadMetaAttr(Map<Integer, PaasMetaAttr> metaAttrIdMapRef,
            Map<String, PaasMetaAttr> metaAttrNameMapRef) {
        MetaDataDao.loadMetaAttr(metaAttrIdMapRef, metaAttrNameMapRef);
    }

    private void loadMetaCmpt(Map<Integer, PaasMetaCmpt> metaCmptIdMapRef,
            Map<String, PaasMetaCmpt> metaCmptNameMapRef) {
        MetaDataDao.loadMetaCmpt(metaCmptIdMapRef, metaCmptNameMapRef);
    }

    private void loadMetaCmptAttr(Multimap<Integer, Integer> metaCmptAttrMMapRef) {
        MetaDataDao.loadMetaCmptAttr(metaCmptAttrMMapRef);
    }

    private void loadMetaInst(Map<String,  PaasInstance> metaInstMapRef) {
        MetaDataDao.loadMetaInst(metaInstMapRef);
    }

    private void loadMetaInstAttr(Multimap<String, PaasInstAttr> metaInstAttrMMapRef) {
        MetaDataDao.loadMetaInstAttr(metaInstAttrMMapRef);
    }

    private void loadMetaService(Map<String,  PaasService> metaServiceMapRef) {
        MetaDataDao.loadMetaService(metaServiceMapRef);
    }

    private void loadMetaTopo(Multimap<String, PaasTopology> metaTopoMMapRef) {
        MetaDataDao.loadMetaTopo(metaTopoMMapRef);
    }

    private void loadDeployHost(Map<Integer, PaasDeployHost> metaDeployHostMapRef) {
        MetaDataDao.loadDeployHost(metaDeployHostMapRef);
    }

    private void loadDeployFile(Map<Integer, PaasDeployFile> metaDeployFileMapRef) {
        MetaDataDao.loadDeployFile(metaDeployFileMapRef);
    }

    private void loadMetaServer(Map<String,  PaasServer> metaServerMapRef) {
        MetaDataDao.loadMetaServer(metaServerMapRef);
    }

    private void loadMetaSsh(Multimap<String, PaasSsh> metaSshMMapRef) {
        MetaDataDao.loadMetaSsh(metaSshMMapRef);
    }

    private void loadMetaCmptVersion(Map<String,  PaasCmptVer> metaCmptVerMapRef) {
        MetaDataDao.loadMetaCmptVersion(metaCmptVerMapRef);
    }

    public void addAccSession(AccountSessionBean accSession, boolean isLocalOnly) {
        accSessionMap.put(accSession.getAccName(), accSession);
        magicKeyMap.put(accSession.getMagicKey(), accSession);
        
        if (!isLocalOnly) {
            JsonObject msgBody = new JsonObject();
            msgBody.put(FixHeader.HEADER_ACC_NAME,  accSession.getAccName());
            msgBody.put(FixHeader.HEADER_MAGIC_KEY, accSession.getMagicKey());
            msgBody.put(FixHeader.HEADER_SESSION_TIMEOUT, accSession.getSessionTimeOut());
    
            EventBean ev = new EventBean(EventType.EVENT_ADD_SESSON, msgBody.toString(), "");
            EventBusMsg.publishEvent(ev);
            
            MetaDataDao.putSessionToRedis(accSession);
        }
    }
    
    public void removeTtlSession(String accName, String magicKey, boolean isLocalOnly) {
        accSessionMap.remove(accName);
        magicKeyMap.remove(magicKey);
        
        if (!isLocalOnly) {
            JsonObject msgBody = new JsonObject();
            msgBody.put(FixHeader.HEADER_ACC_NAME,  accName);
            msgBody.put(FixHeader.HEADER_MAGIC_KEY, magicKey);
            EventBean ev = new EventBean(EventType.EVENT_REMOVE_SESSON, msgBody.toString(), "");
            EventBusMsg.publishEvent(ev);
        }
    }

    public String getAccNameByMagicKey(String magicKey) {
        AccountSessionBean accSession = magicKeyMap.get(magicKey);
        return accSession == null ? "" : accSession.getAccName();
    }

    public void adjustSmsABQueueWeightInfo(String instAId, String weightA, String instBId, String weightB) {
        // 141 -> 'WEIGHT'
        PaasInstAttr attrA = getInstAttr(instAId, 141);
        PaasInstAttr attrB = getInstAttr(instBId, 141);
        
        if (attrA == null || attrB == null)
            return;
        
        attrA.setAttrValue(weightA);
        attrB.setAttrValue(weightB);
    }

    public void switchSmsDBType(String dgContainerID, String dbType) {
        // 225 -> 'ACTIVE_DB_TYPE'
        PaasInstAttr attr = getInstAttr(dgContainerID, 225);
        attr.setAttrValue(dbType);
    }

    public void getServTypeVerList(JsonObject retval) {
        try {
            JsonObject cmptVer = new JsonObject();
            for (Entry<String, PaasCmptVer> entry : metaCmptVerMap.entrySet()) {
                cmptVer.put(entry.getKey(), entry.getValue().toJsonObject());
            }
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put("metaCmptVerMap", cmptVer);
        } catch (Exception e) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_METADATA_NOT_FOUND);
        }
    }

    public void getServTypeList(JsonObject retval) {
        try {
            JsonArray servTypeList = new JsonArray();
            Set<String> servTypeSet = getServTypeListFromLocalCache();
            for (String s : servTypeSet) {
                servTypeList.add(s);
            }
            
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_SERV_TYPE, servTypeList);
        } catch (Exception e) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_METADATA_NOT_FOUND);
        }
    }

    public int getCmptVerCnt(String servType) {
        PaasCmptVer cmptVer = metaCmptVerMap.get(servType);
        if (cmptVer == null)
            return 0;
        
        return cmptVer.getVerionCnt();
    }

    public void addCmptVersion(String servType, String version) {
        PaasCmptVer cmptVer = metaCmptVerMap.get(servType);
        if (cmptVer == null) {
            cmptVer = new PaasCmptVer(servType);
            cmptVer.addVersion(version);
            
            metaCmptVerMap.put(version, cmptVer);
        } else {
            cmptVer.addVersion(version);
        }
    }

    public void delCmptVersion(String servType, String version) {
        PaasCmptVer cmptVer = metaCmptVerMap.get(servType);
        if (cmptVer != null) {
            cmptVer.delVersion(version);
        }
    }

    public Set<String> getServTypeListFromLocalCache() {
        Set<String> res = new HashSet<String>();
        Set<Entry<String, PaasCmptVer>> entrySet = metaCmptVerMap.entrySet();
        for (Entry<String, PaasCmptVer> entry : entrySet) {
            res.add(entry.getKey());
        }
        return res;
    }
    
    public boolean isServiceNameExists(String servName) {
        boolean result = false;
        Set<Entry<String, PaasService>> entrySet = metaServiceMap.entrySet();
        for (Entry<String, PaasService> entry : entrySet) {
            PaasService service = entry.getValue();
            if (service.getServName().equals(servName)) {
                result = true;
                break;
            }
        }
        
        return result;
    }
    
    public boolean isServerIpExists(String servIp) {
        return metaServerMap.containsKey(servIp);
    }
    
    public boolean isSshExists(String sshName, String servIp, String servClazz) {
        boolean result = false;
        lock.lock();
        try {
            Collection<PaasSsh> sshList = metaSshMMap.get(servIp);
            if (sshList == null || sshList.isEmpty())
                return false;
            
            Iterator<PaasSsh> it = sshList.iterator();
            while (it.hasNext()) {
                PaasSsh ssh = it.next();
                if (ssh.getSshName().equals(sshName) && ssh.getServClazz().equals(servClazz)) {
                    result = true;
                    break;
                }
            }
            
        } finally {
            lock.unlock();
        }
        
        return result;
    }
    
    public boolean isSshUsing(String sshId) {
        boolean result = false;
        lock.lock();
        try {
            Collection<Entry<String, PaasInstAttr>> entries = metaInstAttrMMap.entries();
            for (Entry<String, PaasInstAttr> entry : entries) {
                PaasInstAttr attr = entry.getValue();
                if (attr == null)
                    continue;
                
                // 116 -> 'SSH_ID'
                if (attr.getAttrId() == 116 && attr.getAttrValue().equals(sshId)) {
                    result = true;
                    break;
                }
            }
        } catch(Exception e) {
            logger.error("getInstAttr caught excetpion:{}", e.getMessage(), e);
        } finally {
            lock.unlock();
        }
        
        return result;
    }

}
