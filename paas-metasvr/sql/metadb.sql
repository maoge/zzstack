/*
Integrated basic service platform
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE metadb;

-- 创建用户
-- set global validate_password.length = 6;
-- set global validate_password.policy = low;
CREATE USER 'paas_metadb'@'%' IDENTIFIED BY 'paas_metadb';

-- 授权用户数据库权限
GRANT ALL ON metadb.* TO 'paas_metadb'@'%' WITH GRANT OPTION;

USE `metadb`;

DROP TABLE IF EXISTS `t_account`;
CREATE TABLE `t_account` (
  `ACC_ID`        varchar(48)  not null COMMENT '账户ID',
  `ACC_NAME`      varchar(32)  not null COMMENT '账户NAME',
  `PHONE_NUM`     varchar(15)  not null COMMENT 'phone',
  `MAIL`          varchar(48)  not null COMMENT 'mail',
  `PASSWD`        varchar(128) not null COMMENT 'password',
  `CREATE_TIME`   bigint(14)   not null COMMENT '创建时间',
  primary key (`acc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

create unique index IDX_ACC_NAME on t_account(acc_name);
create unique index IDX_ACC_PHONE on t_account(phone_num);
create unique index IDX_ACC_MAIL on t_account(mail);

-- md5(user|orginal_passwd)
insert into `t_account`(`ACC_ID`, `ACC_NAME`, `PHONE_NUM`, `MAIL`, `PASSWD`, `CREATE_TIME`) values
('a2891a5d-1370-315d-9671-fabd2ee90afb', 'admin', '13800000001', 'a@b', '84453f90c268601347f75502c6601bba', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000)),
('a3451a5d-1d70-e15d-y671-habdbee90afe', 'dev',   '13800000002', 'b@b', 'b3fc7e7f88ef098fbc0671303a3a2d4a', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000));

/*Table structure for attribute table `t_meta_attr` */
DROP TABLE IF EXISTS `t_meta_attr`;
CREATE TABLE `t_meta_attr` (
  `ATTR_ID`       int         NOT NULL COMMENT '属性ID',
  `ATTR_NAME`     varchar(48) NOT NULL COMMENT '属性名字(EN)',
  `ATTR_NAME_CN`  varchar(72) NOT NULL COMMENT '属性名字(CN)',
  `AUTO_GEN`      char(1)     NOT NULL COMMENT '0:非自动生成;1:自动生成',
  PRIMARY KEY (`ATTR_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

insert into `t_meta_attr`(`ATTR_ID`,`ATTR_NAME`,`ATTR_NAME_CN`, `AUTO_GEN`) values
(100, 'IP',                  'ip',                           '0'),
(101, 'PORT',                'service port',                 '0'),
(102, 'MGR_PORT',            'manager port',                 '0'),
(103, 'SYNC_PORT',           'sync port',                    '0'),
(104, 'STAT_PORT',           'statistic port',               '0'),
(105, 'COLLECTD_ID',         'collectd id',                  '1'),
(106, 'COLLECTD_NAME',       'collectd name',                '0'),
(107, 'CLUSTER_PORT',        'cluster port',                 '0'),
(108, 'OS_USER',             'os login user',                '0'),
(109, 'OS_PWD',              'system login passwd',          '0'),
(110, 'HOST_NAME',           'host name',                    '0'),
(111, 'MAX_MEMORY',          'max memory limit(GB)',         '0'),
(112, 'MAX_CONN',            'max conn limit',               '0'),
(113, 'NODE_CONN_POOL_SIZE', 'cache node poolled conn size', '0'),
(114, 'INST_ID',             'instance id',                  '1'),
(115, 'NAME',                'name',                         '0'),
(116, 'SSH_ID',              'deploy host id',               '0'),
(117, 'CLIENT_PORT',         'etcd client port',             '0'),
(118, 'PEER_PORT',           'etcd peer port',               '0'),
(119, 'NODE_TYPE',           'master/slave',                 '0'),
(120, 'CLIENT_URLS_PORT',    'client-urls-port',             '0'),
(121, 'PEER_URLS_PORT',      'peer-urls-port',               '0'),
(122, 'HTTP_PORT',           'http port',                    '0'),
(123, 'SSL_PORT',            'ssl port',                     '0'),
(124, 'CONTROL_PORT',        'control port',                 '0'),
(125, 'DASHBOARD_PORT',      'dashboard-port',               '0'),
(126, 'LISTEN_PORT',         'listen-port',                  '0'),
(127, 'BROKER_IP',           'broker-ip',                    '0'),
(128, 'BROKER_ROLE',         'broker-role',                  '0'),
(129, 'FLUSH_DISK_TYPE',     'flushDiskType',                '0'),
(130, 'BROKER_PORT',         'brokerServicePort',            '0'),
(131, 'WEB_PORT',            'webServicePort',               '0'),
(132, 'BOOKIE_PORT',         'bookiePort',                   '0'),
(133, 'CLIENT_PORT',         'clientPort',                   '0'),
(134, 'ADMIN_PORT',          'adminServerPort',              '0'),
(135, 'ORA_LSNR_PORT',       'oraListenPort',                '0'),
(136, 'DG_NAME',             'dgName',                       '0'),
(137, 'DB_USER',             'dbUser',                       '0'),
(138, 'DB_PASSWD',           'dbPasswd',                     '0'),
(139, 'DB_NAME',             'dbName',                       '0'),
(140, 'SERV_INST_ID',        'servInstID',                   '0'),
(141, 'WEIGHT',              'weight',                       '0'),
(142, 'SERV_CONTAINER_NAME', 'servContainerName',            '0'),

(143, 'SYSTEM_PROPERTY',     'system_property',              '0'),
(144, 'MAX_SMS_TASK_PROC',   'max_sms_task_proc',            '0'),
(145, 'BATCH_SAVE_PROCESS',  'batch_save_process',           '0'),
(146, 'WEB_CONSOLE_PORT',    'web_console_port',             '0'),

(147, 'CMPP20_PORT',         'cmpp20_port',                  '0'),
(148, 'CMPP30_PORT',         'cmpp30_port',                  '0'),
(149, 'SGIP12_PORT',         'sgip12_port',                  '0'),
(150, 'SMPP34_PORT',         'smpp34_port',                  '0'),
(151, 'SMGP30_PORT',         'smgp30_port',                  '0'),
(152, 'HTTP_PORT',           'http_port',                    '0'),
(153, 'HTTP_PORT2',          'http_port2',                   '0'),
(154, 'HTTPS_PORT',          'https_port',                   '0'),

(155, 'MEISHENG_PORT',       'meisheng_port',                '0'),
(156, 'HTTP_GBK_PORT',       'http_gbk_port',                '0'),
(157, 'WJSX_PORT',           'wjsx_port',                    '0'),
(158, 'JDWS_ADDR',           'jdws_addr',                    '0'),
(159, 'WEB_SERVICE_ADDR',    'web_service_addr',             '0'),
(160, 'WEB_SERVICE_TASK_URL','web_service_task_url',         '0'),

(161, 'MO_SCAN_INTERVAL',    'mo_scan_interval',             '0'),
(162, 'HTTP_REPORT_INTERVAL','http_report_interval',         '0'),
(163, 'SW_REF_INTERVAL',     'sw_ref_interval',              '0'),
(164, 'CP_REF_INTERVAL',     'cp_ref_interval',              '0'),
(165, 'LOCAL_IP',            'local_ip',                     '0'),
(166, 'CMPP20_PACKLOG',      'cmpp20_packlog',               '0'),
(167, 'CMPP30_PACKLOG',      'cmpp30_packlog',               '0'),
(168, 'SMGP_PACKLOG',        'smgp_packlog',                 '0'),
(169, 'SGIP_PACKLOG',        'sgip_packlog',                 '0'),

(170, 'HTTP_PACKLOG',        'http_packlog',                 '0'),
(171, 'HTTP2_PACKLOG',       'http2_packlog',                '0'),
(172, 'HTTPS_PACKLOG',       'https_packlog',                '0'),
(173, 'WARN_SVC_URL',        'warn_svc_url',                 '0'),
(174, 'DAT_CORE_SIZE',       '行业核心线程数',                    '0'),
(175, 'DAT_MAX_SIZE',        '行业最大线程数',                    '0'),
(176, 'DAT_QUEUE_SIZE',      '行业核心队列大小',                   '0'),
(177, 'ALT_CORE_SIZE',       '营销核心线程数',                    '0'),
(178, 'ALT_MAX_SIZE',        '营销最大线程数',                    '0'),
(179, 'ALT_QUEUE_SIZE',      '营销核心队列大小',                   '0'),

(180, 'BST_CORE_SIZE',       '状态报告核心线程数',                  '0'),
(181, 'BST_MAX_SIZE',        '状态报告最大线程数',                  '0'),
(182, 'BST_QUEUE_SIZE',      '状态报告核心队列大小',                 '0'),
(183, 'RPT_QUEUE_SIZE',      '状态报告推送更新队列大小',              '0'),
(184, 'HTTP_REPORT_PUSH',    'http_report_push',             '0'),
(185, 'HTTP2_REPORT_PUSH',   'http2_report_push',            '0'),
(186, 'HTTPS_REPORT_PUSH',   'https_report_push',            '0'),
(187, 'SGIP_REPORT_PUSH',    'sgip_report_push',             '0'),
(188, 'ACCT_SERVICE',        'acct_service',                 '0'),

(189, 'CMPP_ISMG_ID',        'cmpp_ismg_id',                 '0'),
(190, 'SMGP_ISMG_ID',        'smgp_ismg_id',                 '0'),
(191, 'COLLECT_MSI',         'collect_msi',                  '0'),
(192, 'SPECIAL_REPORT_CUSTID','special_report_custid',       '0'),
(193, 'UNIQUE_LINK_URL',     'unique_link_url',              '0'),
(194, 'MAX_REPORT_FETCH',    'max_report_fetch',             '0'),
(195, 'NO_REPORT_EXECUTE',   'no_report_execute',            '0'),
(196, 'DECISION_ENABLE',     'decision_enable',              '0'),
(197, 'PROMETHEUS_PORT',     'prometheus_port',              '0'),

(198, 'META_SVR_URL',        'meta_svr_url',                 '0'),
(199, 'META_SVR_USR',        'meta_svr_usr',                 '0'),
(200, 'META_SVR_PASSWD',     'meta_svr_passwd',              '0'),
(201, 'JVM_OPS',             'jvm_ops',                      '0'),
(202, 'REDIS_CLUSTER_CACHE', 'redis_cluster_cache',          '0'),
(203, 'REDIS_CLUSTER_QUEUE', 'redis_cluster_queue',          '0'),
(204, 'ORACLE_DG_SERV',      'oracle_dg_serv',               '0'),

(205, 'PROCESSOR',           'processor',                    '0'),
(206, 'SERVICE_IMPL',        '数据对接方式',                     '0'),
(207, 'TST_CORE_SIZE',       '大营销核心线程数',                   '0'),
(208, 'TST_MAX_SIZE',        '大营销最大线程数',                   '0'),
(209, 'TST_QUEUE_SIZE',      '大营销核心队列大小',                  '0'),

(210, 'STS_CORE_SIZE',       '压力测试核心线程数',                  '0'),
(211, 'STS_MAX_SIZE',        '压力测试最大线程数',                  '0'),
(212, 'STS_QUEUE_SIZE',      '压力测试核心队列大小',                 '0'),

(213, 'DB_INST_ID',          '数据库分区ID',                     '0'),
(214, 'SAMPLING_SWITCH',     '相似度检查开关',                    '0'),
(215, 'CRON_EXPRESSION',     '相似度检查调度表达式',                '0'),
(216, 'MT_QUEUE_CLEAR_EXPRESSION', '未消费数据调度表达式',          '0'),
(217, 'REDIS_CLUSTER_PFM',   'redis_cluster_pfm',            '0'),
(218, 'ROCKETMQ_SERV',       'rocketmq服务',                   '0'),
(219, 'BATCHSAVE_PROCESS',   'batsave进程数',                   '0'),
(220, 'SMPP_PACKLOG',        'smpp_packlog',                 '0'),
(221, 'MT_MO_MATCHER_IMPL',  '上/下行匹配模式',                   '0'),
(222, 'PARSE_RPT_TYPE',      'parseRpt.type',                '0'),
(223, 'ES_SERVER',           'elasticsearch.server',         '0'),
(224, 'INTERNAL_PORT',       '内部状态报告接收端口',                '0'),
(225, 'ACTIVE_DB_TYPE',      '数据库当前生效类型(主|备)',            '0'),
(226, 'COLLECTD_PORT',       'collectd_port',                '0'),
(227, 'VERSION',             'version',                      '0'),
(228, 'PROXY_THREADS',       '接入机线程数',                     '0'),
(229, 'REDIS_CLUSTER_IPNUM', 'redis_cluster_ipnum',          '0'),
(230, 'DASHBOARD_PORT',      'dashboard_port',               '0'),
(231, 'REPLICAS',            '副本数',                         '0'),
(232, 'ZK_CLIENT_PORT1',     'zk_client_port1',              '0'),
(233, 'ZK_CLIENT_PORT2',     'zk_client_port2',              '0'),
(234, 'ADVERTISED_ADDRESS',  'advertised_address',           '0'),
(235, 'HTTP_SERVER_PORT',    'http_server_port',             '0'),
(236, 'META_DATA_PORT',      'meta_data_port',               '0'),
(237, 'GRPC_PORT',           'grpc_port',                    '0'),
(238, 'TCP_PORT',            'tcp_port',                     '0'),
(239, 'MYSQL_PORT',          'mysql_port',                   '0'),
(240, 'INTERSERVER_HTTP_PORT',   'interserver_http_port',    '0'),
(241, 'LISTEN_HOST',             'listen_host',              '0'),
(242, 'MAX_CONNECTIONS',         'max_connections',          '0'),
(243, 'MAX_CONCURRENT_QUERIES',  'max_concurrent_queries',   '0'),
(244, 'MAX_SERVER_MEMORY_USAGE', 'max_server_memory_usage',  '0'),
(245, 'MAX_MEMORY_USAGE',        'max_memory_usage',         '0'),
(246, 'INTERNAL_REPLICATION',    'internal_replication',     '0'),
(247, 'EXPORTER_PORT',       'exporter_port',                '0'),
(248, 'METRIC_PORT',         'metric_port',                  '0'),
(249, 'CONSOLE_PORT',        'console_port',                 '0'),
(250, 'MNP_ALI_URL',         'mnp_ali_url',                  '0'),
(251, 'MNP_ALI_CID',         'mnp_ali_cid',                  '0'),
(252, 'MNP_ALI_PASSWD',      'mnp_ali_passwd',               '0'),
(253, 'ES_MT_SERVER',        'elasticsearch.mt.server',      '0'),
(254, 'VOLT_CLIENT_PORT',    'voltdb.client_port',           '0'),
(255, 'VOLT_ADMIN_PORT',     'voltdb.admin_port',            '0'),
(256, 'VOLT_WEB_PORT',       'voltdb.web_port',              '0'),
(257, 'VOLT_INTERNAL_PORT',  'voltdb.internal_port',         '0'),
(258, 'VOLT_REPLI_PORT',     'voltdb.REPLI_port',            '0'),
(259, 'VOLT_ZK_PORT',        'voltdb.zk_port',               '0'),
(260, 'SITES_PER_HOST',      'voltdb.sits_per_host',         '0'),
(261, 'KFACTOR',             'voltdb.kfactor',               '0'),
(262, 'MEM_LIMIT',           'voltdb.memory_limit(GB)',      '0'),
(263, 'HEARTBEAT_TIMEOUT',   'heartbeat.timeout(s)',         '0'),
(264, 'TEMPTABLES_MAXSIZE',  'temptables.maxsize(MB)',       '0'),
(265, 'ELASTIC_DURATION',    'elastic.duration(ms)',         '0'),
(266, 'ELASTIC_THROUGHPUT',  'elastic.throughput(MB)',       '0'),
(267, 'QUERY_TIMEOUT',       'query.timeout(ms)',            '0'),
(268, 'PROCEDURE_LOGINFO',   'procedure.loginfo(ms)',        '0'),
(269, 'MEM_ALERT',           'voltdb.memory_alert(GB)',      '0'),
(270, 'PULSAR_MGR_PORT',     'pulsar.manager_port',          '0'),
(271, 'HERDDB_PORT',         'herddb.port',                  '0'),
(272, 'THREE_CHANNEL_LAST_UPDATE_REPORT_TIME', 'three_channel_last_update_report_time', '0'),
(273, 'YB_MASTER_PORT',      'yb_master.port',               '0'),
(274, 'RPC_BIND_PORT',       'rpc_bind_port',                '0'),
(275, 'WEBSERVER_PORT',      'webserver_port',               '0'),
(276, 'DURABLE_WAL_WRITE',   'durable_wal_write',            '0'),
(277, 'ENABLE_LOAD_BALANCING', 'enable_load_balancing',      '0'),
(278, 'MAX_CLOCK_SKEW_USEC', 'max_clock_skew_usec',          '0'),
(279, 'REPLICATION_FACTOR',  'replication_factor',           '0'),
(280, 'YB_NUM_SHARDS_PER_TSERVER', 'yb_num_shards_per_tserver', '0'),
(281, 'YSQL_NUM_SHARDS_PER_TSERVER', 'ysql_num_shards_per_tserver', '0'),
(282, 'PLACEMENT_CLOUD',     'placement_cloud',              '0'),
(283, 'PLACEMENT_ZONE',      'placement_zone',               '0'),
(284, 'PLACEMENT_REGION',    'placement_region',             '0'),
(285, 'CDC_WAL_RETENTION_TIME_SECS', 'cdc_wal_retention_time_secs', '0'),
(286, 'PGSQL_PROXY_BIND_PORT', 'pgsql_proxy_bind_port',      '0'),
(287, 'PGSQL_PROXY_WEBSERVER_PORT', 'pgsql_proxy_webserver_port', '0'),
(288, 'YSQL_MAX_CONNECTIONS', 'ysql_max_connections',        '0'),
(289, 'CQL_PROXY_BIND_PORT', 'cql_proxy_bind_port',          '0'),
(290, 'CQL_PROXY_WEBSERVER_PORT', 'cql_proxy_webserver_port', '0'),
(291, 'ROCKSDB_COMPACT_FLUSH_RATE_LIMIT_BYTES_PER_SEC', 'rocksdb_compact_flush_rate_limit_bytes_per_sec', '0'),
(292, 'ROCKSDB_UNIVERSAL_COMPACTION_MIN_MERGE_WIDTH', 'rocksdb_universal_compaction_min_merge_width', '0'),
(293, 'ROCKSDB_UNIVERSAL_COMPACTION_SIZE_RATIO', 'rocksdb_universal_compaction_size_ratio', '0'),
(294, 'TIMESTAMP_HISTORY_RETENTION_INTERVAL_SEC', 'timestamp_history_retention_interval_sec', '0'),
(295, 'REMOTE_BOOTSTRAP_RATE_LIMIT_BYTES_PER_SEC', 'remote_bootstrap_rate_limit_bytes_per_sec', '0'),
(296, 'VERTX_PORT',            'vertx.port',                 '0'),
(297, 'VERTX_SSL',             'vertx.ssl',                  '0'),
(298, 'VERTX_EVENT_LOOP_SIZE', 'vertx.eventLoopSize',        '0'),
(299, 'VERTX_WORKER_SIZE',     'vertx.workerSize',           '0'),
(300, 'VERTX_TASK_TIMEOUT',    'vertx.taskTimeout',          '0'),
(301, 'ROCKETMQ_PRODUCER_GROUP_NAME', 'rocketmq.producerGroupName', '0'),
(302, 'EXPORT_URL',            'export.url',                 '0'),
(303, 'EXPORT_CREATE_TASK',    'export.createTask',          '0'),
(304, 'WORKER_PROCESSES',      'worker_processes',           '0'),
(305, 'EXCEL_TEMPDIR',         'excel_tempdir',              '0'),
(306, 'EXCEL_MAXEXCELROW',     'excel_maxexcelrow',          '0'),
(307, 'CLICKHOUSE_SERV',       'clickhouse_serv',            '0'),
(308, 'ES_SCROLL_SIZE',        'es_scroll_size',             '0'),
(309, 'ES_SCROLL_LISTSIZE',    'es_scroll_listsize',         '0'),
(310, 'ES_SCROLL_TIMEOUT',     'es_scroll_timeout',          '0'),
(311, 'ES_MT_HIS_INDEX',       'es_mt_his_index',            '0'),
(312, 'ES_MT_HIS_TYPE',        'es_mt_his_type',             '0'),
(313, 'ES_MT_HIS_SORTFIELD',   'es_mt_his_sortfield',        '0'),
(314, 'ES_REPORT_HIS_INDEX',   'es_report_his_index',        '0'),
(315, 'ES_REPORT_HIS_TYPE',    'es_report_his_type',         '0'),
(316, 'ES_REPORT_HIS_SORTFIELD', 'es_report_his_sortfield',  '0'),
(317, 'ES_MO_HIS_INDEX',       'es_mo_his_index',            '0'),
(318, 'ES_MO_HIS_TYPE',        'es_mo_his_type',             '0'),
(319, 'ES_MO_HIS_SORTFIELD',   'es_mo_his_sortfield',        '0'),
(320, 'PRE_EMBEDDED',          'pre_embedded',               '0'),
(321, 'SMS_EXT_PROTO_SWITCH',  'sms_ext_proto_switch',       '0'),
(322, 'SMS_EXT_PROTO_PORT',    'sms_ext_proto_port',         '0'),
(323, 'MINIO_REGION',          'region',                     '0'),
(324, 'MINIO_MOUNT',           '挂载路径',                      '0');


/*Table structure for component table `t_meta_cmpt` */
DROP TABLE IF EXISTS `t_meta_cmpt`;
CREATE TABLE `t_meta_cmpt` (
  `CMPT_ID`        int         NOT NULL COMMENT '组件ID',
  `CMPT_NAME`      varchar(48) NOT NULL COMMENT '组件名字(EN)',
  `CMPT_NAME_CN`   varchar(72) NOT NULL COMMENT '组件名字(CN)',

  `IS_NEED_DEPLOY` char(1)      NOT NULL COMMENT '是否需要部署 0:不需要,1:需要',
  `SERV_TYPE`      varchar(32)  NOT NULL COMMENT '服务类别',
  `SERV_CLAZZ`     varchar(32)  NOT NULL COMMENT '服务大类',
  `NODE_JSON_TYPE` varchar(12)  NOT NULL COMMENT 'TOPO JSON TYPE',
  `SUB_CMPT_ID`    varchar(256) NOT NULL COMMENT '子组件ID',
  PRIMARY KEY (`CMPT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

insert into `t_meta_cmpt`(`CMPT_ID`,`CMPT_NAME`,`CMPT_NAME_CN`,`IS_NEED_DEPLOY`,`SERV_TYPE`,`SERV_CLAZZ`,`NODE_JSON_TYPE`,`SUB_CMPT_ID`) values
(100, 'COLLECTD',                      '采集器',                    '1',   'COMMON_COLLECTD',            'COMMON_TOOLS', 'object',    ''),
(101, 'PROMETHEUS',                    'prometheus',              '1',   'COMMON_MONITORING',          'COMMON_TOOLS', 'object',    ''),
(102, 'GRAFANA',                       'grafana',                 '1',   'COMMON_MONITORING',          'COMMON_TOOLS', 'object',    ''),
(103, 'ZOOKEEPER_CONTAINER',           'zookeeper-container',     '0',   'COMMON_ZK',                  'COMMON_TOOLS', 'object',    '104'),
(104, 'ZOOKEEPER',                     'zookeeper',               '1',   'COMMON_ZK',                  'COMMON_TOOLS', 'array',     ''),
(105, 'ETCD_CONTAINER',                'etcd-container',          '0',   'COMMON_ETCD',                'COMMON_TOOLS', 'object',    '106'),
(106, 'ETCD',                          'etcd',                    '1',   'COMMON_ETCD',                'COMMON_TOOLS', 'array',     ''),
(107, 'NGX_CONTAINER',                 'ngx-container',           '0',   'COMMON_NGX',                 'COMMON_TOOLS', 'object',    '108'),
(108, 'NGX',                           'ngx',                     '1',   'COMMON_NGX',                 'COMMON_TOOLS', 'array',     ''),

(200, 'REDIS_SERV_CLUSTER_CONTAINER',  'Redis集群服务容器',            '1',   'CACHE_REDIS_CLUSTER',        'CACHE',        'object',    '201,202,100'),
(201, 'REDIS_PROXY_CONTAINER',         'RedisProxy容器',            '0',   'CACHE_REDIS_CLUSTER',        'CACHE',        'object',    '204'),
(202, 'REDIS_NODE_CONTAINER',          'RedisNode容器',             '0',   'CACHE_REDIS_CLUSTER',        'CACHE',        'object',    '203'),
(203, 'REDIS_NODE',                    'RedisNode',                '1',   'CACHE_REDIS_CLUSTER',        'CACHE',        'array',     ''),
(204, 'REDIS_PROXY',                   'Redis接入机',                '1',   'CACHE_REDIS_CLUSTER',        'CACHE',        'array',     ''),

(205, 'REDIS_SERV_MS_CONTAINER',       'Redis主从服务容器',            '1',   'CACHE_REDIS_MASTER_SLAVE',   'CACHE',         'object',   '202,100'),

(210, 'TIDB_SERV_CONTAINER',           'TiDB服务容器',                '1',   'DB_TIDB',                    'DB',           'object',    '211,212,213,217'),
(211, 'TIDB_SERVER_CONTAINER',         'TiDB-Server容器',            '0',   'DB_TIDB',                    'DB',           'object',    '214'),
(212, 'PD_SERVER_CONTAINER',           'PD-Server容器',              '0',   'DB_TIDB',                    'DB',           'object',    '215'),
(213, 'TIKV_SERVER_CONTAINER',         'TiKV-Server容器',            '0',   'DB_TIDB',                    'DB',           'object',    '216'),
(214, 'TIDB_SERVER',                   'TiDB-Server',               '1',   'DB_TIDB',                    'DB',           'array',     ''),
(215, 'PD_SERVER',                     'PD-Server',                 '1',   'DB_TIDB',                    'DB',           'array',     ''),
(216, 'TIKV_SERVER',                   'TiKV-Server',               '1',   'DB_TIDB',                    'DB',           'array',     ''),
(217, 'DASHBOARD_PROXY',               'Dashboard-Proxy',           '1',   'DB_TIDB',                    'DB',           'object',    ''),

(230, 'APISIX_SERV_CONTAINER',         'apisix微服务网关容器',           '1',   'SERVERLESS_APISIX',          'SERVERLESS',   'object',    '231,101,102,105'),
(231, 'APISIX_CONTAINER',              'apisix server容器',          '0',   'SERVERLESS_APISIX',          'SERVERLESS',   'object',    '232'),
(232, 'APISIX_SERVER',                 'apisix',                    '1',   'SERVERLESS_APISIX',          'SERVERLESS',   'array',     ''),

(240, 'ROCKETMQ_SERV_CONTAINER',       'rocketmq服务容器',             '1',   'MQ_ROCKETMQ',                'MQ',           'object',    '241,242,246,100'),
(241, 'ROCKETMQ_VBROKER_CONTAINER',    'rocketmq vbroker容器',        '0',   'MQ_ROCKETMQ',                'MQ',           'object',    '243'),
(242, 'ROCKETMQ_NAMESRV_CONTAINER',    'rocketmq namesrv容器',        '0',   'MQ_ROCKETMQ',                'MQ',           'object',    '245'),
(243, 'ROCKETMQ_VBROKER',              'rocketmq vbroker',          '0',   'MQ_ROCKETMQ',                'MQ',           'array',     '244'),
(244, 'ROCKETMQ_BROKER',               'rocketmq broker',           '1',   'MQ_ROCKETMQ',                'MQ',           'array',     ''),
(245, 'ROCKETMQ_NAMESRV',              'rocketmq namesrv',          '1',   'MQ_ROCKETMQ',                'MQ',           'array',     ''),
(246, 'ROCKETMQ_CONSOLE',              'rocketmq console',          '1',   'MQ_ROCKETMQ',                'MQ',           'object',    ''),

(250, 'TDENGINE_SERV_CONTAINER',       'tdengine服务容器',             '1',   'DB_TDENGINE',                'DB',           'object',    '251,252,100'),
(251, 'ARBITRATOR_CONTAINER',          'arbitrator容器',             '0',   'DB_TDENGINE',                'DB',           'object',    '253'),
(252, 'DNODE_CONTAINER',               'dnode容器',                  '0',   'DB_TDENGINE',                'DB',           'object',    '254'),
(253, 'TD_ARBITRATOR',                 'arbitrator',                '1',   'DB_TDENGINE',                'DB',           'object',    ''),
(254, 'TD_DNODE',                      'dnode',                     '1',   'DB_TDENGINE',                'DB',           'array',     ''),

(260, 'PULSAR_SERV_CONTAINER',         'pulsar服务容器',               '1',   'MQ_PULSAR',                  'MQ',           'object',    '261,262,263,103,101,102'),
(261, 'PULSAR_BROKER_CONTAINER',       'pulsar broker容器',           '0',   'MQ_PULSAR',                  'MQ',           'object',    '264'),
(262, 'PULSAR_BOOKKEEPER_CONTAINER',   'bookkeeper容器',              '0',   'MQ_PULSAR',                  'MQ',           'object',    '265'),
(263, 'PULSAR_MANAGER',                'pulsar manager',            '1',   'MQ_PULSAR',                  'MQ',           'object',    ''),
(264, 'PULSAR_BROKER',                 'pulsar broker',             '1',   'MQ_PULSAR',                  'MQ',           'array',     ''),
(265, 'PULSAR_BOOKKEEPER',             'pulsar bookkeeper',         '1',   'MQ_PULSAR',                  'MQ',           'array',     ''),

(270, 'ORACLE_DG_SERV_CONTAINER',      'oracle_dg服务容器',            '1',   'DB_ORACLE_DG',               'DB',           'object',    '271'),
(271, 'DG_CONTAINER',                  'dg容器',                     '0',   'DB_ORACLE_DG',               'DB',           'array',     '272'),
(272, 'ORCL_INSTANCE',                 'oracle实例',                 '1',   'DB_ORACLE_DG',               'DB',           'array',     ''),

(280, 'CLICKHOUSE_SERV_CONTAINER',     'clickhouse服务容器',           '1',    'DB_CLICKHOUSE',             'DB',           'object',    '103,281,101,102'),
(281, 'CLICKHOUSE_REPLICAS_CONTAINER', 'clickhouse-replicas容器',     '0',   'DB_CLICKHOUSE',             'DB',           'object',    '282'),
(282, 'CLICKHOUSE_REPLICAS',           'clickhouse-replicas',       '0',    'DB_CLICKHOUSE',             'DB',           'array',    '283'),
(283, 'CLICKHOUSE_SERVER',             'clickhouse-server',         '1',    'DB_CLICKHOUSE',             'DB',           'array',     ''),

(290, 'VOLTDB_SERV_CONTAINER',         'voltdb服务容器',               '1',    'DB_VOLTDB',                  'DB',           'object',    '291'),
(291, 'VOLTDB_CONTAINER',              'voltdb-replicas容器',         '0',   'DB_VOLTDB',                  'DB',           'object',    '292'),
(292, 'VOLTDB_SERVER',                 'voltdb-server',              '1',   'DB_VOLTDB',                  'DB',           'array',     ''),

(300, 'YUGABYTEDB_SERV_CONTAINER',     'yugabytedb服务容器',            '1',   'DB_YUGABYTEDB',              'DB',           'object',    '301,302'),
(301, 'YB_MASTER_CONTAINER',           'yb-master容器',               '0',   'DB_YUGABYTEDB',              'DB',           'object',    '303'),
(302, 'YB_TSERVER_CONTAINER',          'yb-tserver容器',              '0',   'DB_YUGABYTEDB',              'DB',           'object',    '304'),
(303, 'YB_MASTER',                     'yb-master',                  '1',   'DB_YUGABYTEDB',              'DB',           'array',     ''),
(304, 'YB_TSERVER',                    'yb-tserver',                 '1',   'DB_YUGABYTEDB',              'DB',           'array',     ''),

(310, 'MINIO_SERV_CONTAINER',          'minio服务容器',                 '1',   'STORE_MINIO',               'KVSTORE',      'object',    '311'),
(311, 'MINIO_CONTAINER',               'minio容器',                   '0',    'STORE_MINIO',               'KVSTORE',      'object',    '312'),
(312, 'MINIO',                         'minio',                      '1',   'STORE_MINIO',               'KVSTORE',      'array',     ''),

(700, 'SMS_GATEWAY_SERV_CONTAINER',    'sms_gateway服务容器',           '1',   'SMS_GATEWAY',                'SMS',          'object',    '701,702,703,704,705,711'),
(701, 'SMS_SERVER_CONTAINER',          'sms_server容器',              '0',   'SMS_GATEWAY',                'SMS',          'object',    '706'),
(702, 'SMS_PROCESS_CONTAINER',         'sms_process容器',             '0',   'SMS_GATEWAY',                'SMS',          'object',    '707'),
(703, 'SMS_CLIENT_CONTAINER',          'sms_client容器',              '0',   'SMS_GATEWAY',                'SMS',          'object',    '708'),
(704, 'SMS_BATSAVE_CONTAINER',         'batsave容器',                 '0',   'SMS_GATEWAY',                'SMS',          'object',    '709'),
(705, 'SMS_STATS_CONTAINER',           'statistics容器',              '0',   'SMS_GATEWAY',                'SMS',          'object',    '710'),
(706, 'SMS_SERVER',                    'sms_server',                '1',   'SMS_GATEWAY',                'SMS',          'array',     ''),
(707, 'SMS_PROCESS',                   'sms_process',               '1',   'SMS_GATEWAY',                'SMS',          'array',     ''),
(708, 'SMS_CLIENT',                    'sms_client',                '1',   'SMS_GATEWAY',                'SMS',          'array',     ''),
(709, 'SMS_BATSAVE',                   'sms_batsave',               '1',   'SMS_GATEWAY',                'SMS',          'array',     ''),
(710, 'SMS_STATS',                     'sms_stats',                 '1',   'SMS_GATEWAY',                'SMS',          'array',     ''),
(711, 'SMS_SERVER_EXT_CONTAINER',      'sms_server_ext容器',          '0',   'SMS_GATEWAY',                'SMS',          'object',    '712'),
(712, 'SMS_SERVER_EXT',                'sms_server_ext',            '1',   'SMS_GATEWAY',                'SMS',          'array',     ''),

(720, 'SMS_QUERY_SERV_CONTAINER',      'sms_query服务容器',            '1',   'SMS_QUERY_SERVICE',          'SMS',          'object',    '107,721'),
(721, 'SMS_QUERY_CONTAINER',           'sms_query容器',               '0',   'SMS_QUERY_SERVICE',          'SMS',         'object',    '722'),
(722, 'SMS_QUERY',                     'sms_query',                 '1',   'SMS_QUERY_SERVICE',          'SMS',          'array',     ''),

(800, 'REDIS_HA_CLUSTER_CONTAINER',    'redis cluster A/B容灾集群容器',  '1',   'CACHE_REDIS_HA_CLUSTER',     'CACHE',        'object',    '801'),
(801, 'HA_CONTAINER',                  'A/B容器',                     '0',   'CACHE_REDIS_HA_CLUSTER',     'CACHE',        'array',     '200');

/*Table structure for component-attribute table `t_meta_cmpt_attr` */
DROP TABLE IF EXISTS `t_meta_cmpt_attr`;
CREATE TABLE `t_meta_cmpt_attr` (
  `CMPT_ID`       int         NOT NULL COMMENT '组件ID',
  `ATTR_ID`       int         NOT NULL COMMENT '属性ID',
  `ATTR_NAME`     varchar(48) not null COMMENT '属性名字(EN)',
  UNIQUE KEY `IDX_CMPT_ATTR` (`CMPT_ID`,`ATTR_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

insert into `t_meta_cmpt_attr`(`CMPT_ID`,`ATTR_ID`, `ATTR_NAME`) values
(100, 114, 'INST_ID'),
(100, 140, 'SERV_INST_ID'),
(100, 116, 'SSH_ID'),
(100, 198, 'META_SVR_URL'),
(100, 199, 'META_SVR_USR'),
(100, 200, 'META_SVR_PASSWD'),
(100, 226, 'COLLECTD_PORT'),

(101, 114, 'INST_ID'),
(101, 116, 'SSH_ID'),
(101, 197, 'PROMETHEUS_PORT'),

(102, 114, 'INST_ID'),
(102, 116, 'SSH_ID'),
(102, 122, 'HTTP_PORT'),

(103, 114, 'INST_ID'),

(104, 114, 'INST_ID'),
(104, 116, 'SSH_ID'),
(104, 117, 'CLIENT_PORT'),
(104, 134, 'ADMIN_PORT'),
(104, 232, 'ZK_CLIENT_PORT1'),
(104, 233, 'ZK_CLIENT_PORT2'),

(105, 114, 'INST_ID'),

(106, 114, 'INST_ID'),
(106, 116, 'SSH_ID'),
(106, 120, 'CLIENT_URLS_PORT'),
(106, 121, 'PEER_URLS_PORT'),

(107, 114, 'INST_ID'),

(108, 114, 'INST_ID'),
(108, 116, 'SSH_ID'),
(108, 304, 'WORKER_PROCESSES'),
(108, 126, 'LISTEN_PORT'),

(200, 114, 'INST_ID'),
(201, 114, 'INST_ID'),
(202, 114, 'INST_ID'),

(203, 114, 'INST_ID'),
(203, 116, 'SSH_ID'),
(203, 101, 'PORT'),
(203, 111, 'MAX_MEMORY'),
(203, 112, 'MAX_CONN'),
(203, 119, 'NODE_TYPE'),

(204, 114, 'INST_ID'),
(204, 116, 'SSH_ID'),
(204, 101, 'PORT'),
(204, 113, 'NODE_CONN_POOL_SIZE'),
(204, 112, 'MAX_CONN'),
(204, 228, 'PROXY_THREADS'),

(205, 114, 'INST_ID'),

(210, 114, 'INST_ID'),
(211, 114, 'INST_ID'),
(212, 114, 'INST_ID'),
(213, 114, 'INST_ID'),

(214, 114, 'INST_ID'),
(214, 116, 'SSH_ID'),
(214, 101, 'PORT'),
(214, 104, 'STAT_PORT'),

(215, 114, 'INST_ID'),
(215, 116, 'SSH_ID'),
(215, 117, 'CLIENT_PORT'),
(215, 118, 'PEER_PORT'),
(215, 231, 'REPLICAS'),

(216, 114, 'INST_ID'),
(216, 116, 'SSH_ID'),
(216, 101, 'PORT'),
(216, 104, 'STAT_PORT'),

(217, 114, 'INST_ID'),
(217, 116, 'SSH_ID'),
(217, 230, 'DASHBOARD_PORT'),

(230, 114, 'INST_ID'),
(231, 114, 'INST_ID'),

(232, 114, 'INST_ID'),
(232, 116, 'SSH_ID'),
(232, 122, 'HTTP_PORT'),
(232, 123, 'SSL_PORT'),
(232, 124, 'CONTROL_PORT'),
(232, 125, 'DASHBOARD_PORT'),
(232, 248, 'METRIC_PORT'),

(240, 114, 'INST_ID'),
(241, 114, 'INST_ID'),
(242, 114, 'INST_ID'),
(243, 114, 'INST_ID'),

(244, 114, 'INST_ID'),
(244, 116, 'SSH_ID'),
(244, 126, 'LISTEN_PORT'),
(244, 128, 'BROKER_ROLE'),
(244, 129, 'FLUSH_DISK_TYPE'),

(245, 114, 'INST_ID'),
(245, 116, 'SSH_ID'),
(245, 126, 'LISTEN_PORT'),

(246, 114, 'INST_ID'),
(246, 116, 'SSH_ID'),
(246, 249, 'CONSOLE_PORT'),

(250, 114, 'INST_ID'),
(251, 114, 'INST_ID'),
(252, 114, 'INST_ID'),

(253, 114, 'INST_ID'),
(253, 116, 'SSH_ID'),
(253, 101, 'PORT'),

(254, 114, 'INST_ID'),
(254, 116, 'SSH_ID'),
(254, 101, 'PORT'),

(260, 114, 'INST_ID'),
(261, 114, 'INST_ID'),
(262, 114, 'INST_ID'),

(263, 114, 'INST_ID'),
(263, 116, 'SSH_ID'),
(263, 270, 'PULSAR_MGR_PORT'),
(263, 271, 'HERDDB_PORT'),

(264, 114, 'INST_ID'),
(264, 116, 'SSH_ID'),
(264, 130, 'BROKER_PORT'),
(264, 131, 'WEB_PORT'),

(265, 114, 'INST_ID'),
(265, 116, 'SSH_ID'),
(265, 132, 'BOOKIE_PORT'),
(265, 235, 'HTTP_SERVER_PORT'),
(265, 237, 'GRPC_PORT'),

(270, 114, 'INST_ID'),

(271, 114, 'INST_ID'),
(271, 136, 'DG_NAME'),
(271, 225, 'ACTIVE_DB_TYPE'),

(272, 114, 'INST_ID'),
(272, 116, 'SSH_ID'),
(272, 119, 'NODE_TYPE'),
(272, 135, 'ORA_LSNR_PORT'),
(272, 137, 'DB_USER'),
(272, 138, 'DB_PASSWD'),
(272, 139, 'DB_NAME'),

(280, 114, 'INST_ID'),
(281, 114, 'INST_ID'),

(282, 114, 'INST_ID'),
(282, 246, 'INTERNAL_REPLICATION'),

(283, 114, 'INST_ID'),
(283, 116, 'SSH_ID'),
(283, 122, 'HTTP_PORT'),
(283, 238, 'TCP_PORT'),
(283, 239, 'MYSQL_PORT'),
(283, 240, 'INTERSERVER_HTTP_PORT'),
(283, 242, 'MAX_CONNECTIONS'),
(283, 243, 'MAX_CONCURRENT_QUERIES'),
(283, 244, 'MAX_SERVER_MEMORY_USAGE'),
(283, 245, 'MAX_MEMORY_USAGE'),
(283, 247, 'EXPORTER_PORT'),

(290, 114, 'INST_ID'),
(291, 114, 'INST_ID'),

(292, 114, 'INST_ID'),
(292, 116, 'SSH_ID'),
(292, 254, 'VOLT_CLIENT_PORT'),
(292, 255, 'VOLT_ADMIN_PORT'),
(292, 256, 'VOLT_WEB_PORT'),
(292, 257, 'VOLT_INTERNAL_PORT'),
(292, 258, 'VOLT_REPLI_PORT'),
(292, 259, 'VOLT_ZK_PORT'),
(292, 260, 'SITES_PER_HOST'),
(292, 261, 'KFACTOR'),
(292, 262, 'MEM_LIMIT'),
(292, 263, 'HEARTBEAT_TIMEOUT'),
(292, 264, 'TEMPTABLES_MAXSIZE'),
(292, 265, 'ELASTIC_DURATION'),
(292, 266, 'ELASTIC_THROUGHPUT'),
(292, 267, 'QUERY_TIMEOUT'),
(292, 268, 'PROCEDURE_LOGINFO'),
(292, 269, 'MEM_ALERT'),

(300, 114, 'INST_ID'),
(301, 114, 'INST_ID'),
(302, 114, 'INST_ID'),

(303, 114, 'INST_ID'),
(303, 116, 'SSH_ID'),
(303, 274, 'RPC_BIND_PORT'),
(303, 275, 'WEBSERVER_PORT'),
(303, 276, 'DURABLE_WAL_WRITE'),
(303, 277, 'ENABLE_LOAD_BALANCING'),
(303, 278, 'MAX_CLOCK_SKEW_USEC'),
(303, 279, 'REPLICATION_FACTOR'),
(303, 280, 'YB_NUM_SHARDS_PER_TSERVER'),
(303, 281, 'YSQL_NUM_SHARDS_PER_TSERVER'),
(303, 282, 'PLACEMENT_CLOUD'),
(303, 283, 'PLACEMENT_ZONE'),
(303, 284, 'PLACEMENT_REGION'),
(303, 285, 'CDC_WAL_RETENTION_TIME_SECS'),

(304, 114, 'INST_ID'),
(304, 116, 'SSH_ID'),
(304, 274, 'RPC_BIND_PORT'),
(304, 275, 'WEBSERVER_PORT'),
(304, 286, 'PGSQL_PROXY_BIND_PORT'),
(304, 287, 'PGSQL_PROXY_WEBSERVER_PORT'),
(304, 289, 'CQL_PROXY_BIND_PORT'),
(304, 290, 'CQL_PROXY_WEBSERVER_PORT'),
(304, 288, 'YSQL_MAX_CONNECTIONS'),
(304, 278, 'MAX_CLOCK_SKEW_USEC'),
(304, 276, 'DURABLE_WAL_WRITE'),
(304, 280, 'YB_NUM_SHARDS_PER_TSERVER'),
(304, 281, 'YSQL_NUM_SHARDS_PER_TSERVER'),
(304, 282, 'PLACEMENT_CLOUD'),
(304, 283, 'PLACEMENT_ZONE'),
(304, 284, 'PLACEMENT_REGION'),
(304, 291, 'ROCKSDB_COMPACT_FLUSH_RATE_LIMIT_BYTES_PER_SEC'),
(304, 292, 'ROCKSDB_UNIVERSAL_COMPACTION_MIN_MERGE_WIDTH'),
(304, 293, 'ROCKSDB_UNIVERSAL_COMPACTION_SIZE_RATIO'),
(304, 294, 'TIMESTAMP_HISTORY_RETENTION_INTERVAL_SEC'),
(304, 295, 'REMOTE_BOOTSTRAP_RATE_LIMIT_BYTES_PER_SEC'),

(310, 114, 'INST_ID'),
(311, 114, 'INST_ID'),

(312, 114, 'INST_ID'),
(312, 116, 'SSH_ID'),
(312, 101, 'PORT'),
(312, 249, 'CONSOLE_PORT'),
(312, 323, 'MINIO_REGION'),
(312, 324, 'MINIO_MOUNT'),

(700, 114, 'INST_ID'),

(701, 114, 'INST_ID'),
(701, 227, 'VERSION'),

(702, 114, 'INST_ID'),
(702, 227, 'VERSION'),

(703, 114, 'INST_ID'),
(703, 227, 'VERSION'),

(704, 114, 'INST_ID'),
(704, 227, 'VERSION'),

(705, 114, 'INST_ID'),
(705, 227, 'VERSION'),

(706, 114, 'INST_ID'),
(706, 116, 'SSH_ID'),
(706, 227, 'VERSION'),
(706, 198, 'META_SVR_URL'),
(706, 199, 'META_SVR_USR'),
(706, 200, 'META_SVR_PASSWD'),
(706, 202, 'REDIS_CLUSTER_CACHE'),
(706, 203, 'REDIS_CLUSTER_QUEUE'),
(706, 204, 'ORACLE_DG_SERV'),
(706, 143, 'SYSTEM_PROPERTY'),
(706, 144, 'MAX_SMS_TASK_PROC'),
(706, 145, 'BATCH_SAVE_PROCESS'),
(706, 146, 'WEB_CONSOLE_PORT'),
(706, 147, 'CMPP20_PORT'),
(706, 148, 'CMPP30_PORT'),
(706, 149, 'SGIP12_PORT'),
(706, 150, 'SMPP34_PORT'),
(706, 151, 'SMGP30_PORT'),
(706, 152, 'HTTP_PORT'),
(706, 153, 'HTTP_PORT2'),
(706, 154, 'HTTPS_PORT'),
(706, 155, 'MEISHENG_PORT'),
(706, 156, 'HTTP_GBK_PORT'),
(706, 157, 'WJSX_PORT'),
(706, 158, 'JDWS_ADDR'),
(706, 159, 'WEB_SERVICE_ADDR'),
(706, 160, 'WEB_SERVICE_TASK_URL'),
(706, 161, 'MO_SCAN_INTERVAL'),
(706, 162, 'HTTP_REPORT_INTERVAL'),
(706, 163, 'SW_REF_INTERVAL'),
(706, 164, 'CP_REF_INTERVAL'),
(706, 165, 'LOCAL_IP'),
(706, 166, 'CMPP20_PACKLOG'),
(706, 167, 'CMPP30_PACKLOG'),
(706, 168, 'SMGP_PACKLOG'),
(706, 169, 'SGIP_PACKLOG'),
(706, 170, 'HTTP_PACKLOG'),
(706, 171, 'HTTP2_PACKLOG'),
(706, 172, 'HTTPS_PACKLOG'),
(706, 173, 'WARN_SVC_URL'),
(706, 174, 'DAT_CORE_SIZE'),
(706, 175, 'DAT_MAX_SIZE'),
(706, 176, 'DAT_QUEUE_SIZE'),
(706, 177, 'ALT_CORE_SIZE'),
(706, 178, 'ALT_MAX_SIZE'),
(706, 179, 'ALT_QUEUE_SIZE'),
(706, 180, 'BST_CORE_SIZE'),
(706, 181, 'BST_MAX_SIZE'),
(706, 182, 'BST_QUEUE_SIZE'),
(706, 183, 'RPT_QUEUE_SIZE'),
(706, 184, 'HTTP_REPORT_PUSH'),
(706, 185, 'HTTP2_REPORT_PUSH'),
(706, 186, 'HTTPS_REPORT_PUSH'),
(706, 187, 'SGIP_REPORT_PUSH'),
(706, 188, 'ACCT_SERVICE'),
(706, 189, 'CMPP_ISMG_ID'),
(706, 190, 'SMGP_ISMG_ID'),
(706, 191, 'COLLECT_MSI'),
(706, 192, 'SPECIAL_REPORT_CUSTID'),
(706, 193, 'UNIQUE_LINK_URL'),
(706, 194, 'MAX_REPORT_FETCH'),
(706, 195, 'NO_REPORT_EXECUTE'),
(706, 196, 'DECISION_ENABLE'),
(706, 197, 'PROMETHEUS_PORT'),
(706, 201, 'JVM_OPS'),

(707, 114, 'INST_ID'),
(707, 116, 'SSH_ID'),
(707, 227, 'VERSION'),
(707, 198, 'META_SVR_URL'),
(707, 199, 'META_SVR_USR'),
(707, 200, 'META_SVR_PASSWD'),
(707, 202, 'REDIS_CLUSTER_CACHE'),
(707, 203, 'REDIS_CLUSTER_QUEUE'),
(707, 217, 'REDIS_CLUSTER_PFM'),
(707, 229, 'REDIS_CLUSTER_IPNUM'),
(707, 204, 'ORACLE_DG_SERV'),
(707, 218, 'ROCKETMQ_SERV'),
(707, 205, 'PROCESSOR'),
(707, 213, 'DB_INST_ID'),
(707, 143, 'SYSTEM_PROPERTY'),
(707, 206, 'SERVICE_IMPL'),
(707, 164, 'CP_REF_INTERVAL'),
(707, 173, 'WARN_SVC_URL'),
(707, 174, 'DAT_CORE_SIZE'),
(707, 175, 'DAT_MAX_SIZE'),
(707, 176, 'DAT_QUEUE_SIZE'),
(707, 177, 'ALT_CORE_SIZE'),
(707, 178, 'ALT_MAX_SIZE'),
(707, 179, 'ALT_QUEUE_SIZE'),
(707, 207, 'TST_CORE_SIZE'),
(707, 208, 'TST_MAX_SIZE'),
(707, 209, 'TST_QUEUE_SIZE'),
(707, 210, 'STS_CORE_SIZE'),
(707, 211, 'STS_MAX_SIZE'),
(707, 212, 'STS_QUEUE_SIZE'),
(707, 214, 'SAMPLING_SWITCH'),
(707, 215, 'CRON_EXPRESSION'),
(707, 146, 'WEB_CONSOLE_PORT'),
(707, 216, 'MT_QUEUE_CLEAR_EXPRESSION'),
(707, 196, 'DECISION_ENABLE'),
(707, 191, 'COLLECT_MSI'),
(707, 201, 'JVM_OPS'),
(707, 250, 'MNP_ALI_URL'),
(707, 251, 'MNP_ALI_CID'),
(707, 252, 'MNP_ALI_PASSWD'),
(707, 320, 'PRE_EMBEDDED'),

(708, 114, 'INST_ID'),
(708, 116, 'SSH_ID'),
(708, 227, 'VERSION'),
(708, 198, 'META_SVR_URL'),
(708, 199, 'META_SVR_USR'),
(708, 200, 'META_SVR_PASSWD'),
(708, 202, 'REDIS_CLUSTER_CACHE'),
(708, 203, 'REDIS_CLUSTER_QUEUE'),
(708, 204, 'ORACLE_DG_SERV'),
(708, 218, 'ROCKETMQ_SERV'),
(708, 146, 'WEB_CONSOLE_PORT'),
(708, 205, 'PROCESSOR'),
(708, 206, 'SERVICE_IMPL'),
(708, 219, 'BATCHSAVE_PROCESS'),
(708, 166, 'CMPP20_PACKLOG'),
(708, 167, 'CMPP30_PACKLOG'),
(708, 168, 'SMGP_PACKLOG'),
(708, 169, 'SGIP_PACKLOG'),
(708, 170, 'HTTP_PACKLOG'),
(708, 171, 'HTTP2_PACKLOG'),
(708, 172, 'HTTPS_PACKLOG'),
(708, 220, 'SMPP_PACKLOG'),
(708, 173, 'WARN_SVC_URL'),
(708, 180, 'BST_CORE_SIZE'),
(708, 181, 'BST_MAX_SIZE'),
(708, 182, 'BST_QUEUE_SIZE'),
(708, 221, 'MT_MO_MATCHER_IMPL'),
(708, 222, 'PARSE_RPT_TYPE'),
(708, 201, 'JVM_OPS'),
(708, 272, 'THREE_CHANNEL_LAST_UPDATE_REPORT_TIME'),
(708, 320, 'PRE_EMBEDDED'),

(709, 114, 'INST_ID'),
(709, 116, 'SSH_ID'),
(709, 227, 'VERSION'),
(709, 198, 'META_SVR_URL'),
(709, 199, 'META_SVR_USR'),
(709, 200, 'META_SVR_PASSWD'),
(709, 202, 'REDIS_CLUSTER_CACHE'),
(709, 203, 'REDIS_CLUSTER_QUEUE'),
(709, 204, 'ORACLE_DG_SERV'),
(709, 218, 'ROCKETMQ_SERV'),
(709, 205, 'PROCESSOR'),
(709, 223, 'ES_SERVER'),
(709, 253, 'ES_MT_SERVER'),
(709, 143, 'SYSTEM_PROPERTY'),
(709, 206, 'SERVICE_IMPL'),
(709, 173, 'WARN_SVC_URL'),
(709, 174, 'DAT_CORE_SIZE'),
(709, 175, 'DAT_MAX_SIZE'),
(709, 176, 'DAT_QUEUE_SIZE'),
(709, 164, 'CP_REF_INTERVAL'),
(709, 213, 'DB_INST_ID'),
(709, 146, 'WEB_CONSOLE_PORT'),
(709, 201, 'JVM_OPS'),
(709, 272, 'THREE_CHANNEL_LAST_UPDATE_REPORT_TIME'),
(709, 320, 'PRE_EMBEDDED'),

(710, 114, 'INST_ID'),
(710, 116, 'SSH_ID'),
(710, 227, 'VERSION'),
(710, 198, 'META_SVR_URL'),
(710, 199, 'META_SVR_USR'),
(710, 200, 'META_SVR_PASSWD'),
(710, 202, 'REDIS_CLUSTER_CACHE'),
(710, 203, 'REDIS_CLUSTER_QUEUE'),
(710, 204, 'ORACLE_DG_SERV'),
(710, 143, 'SYSTEM_PROPERTY'),
(710, 146, 'WEB_CONSOLE_PORT'),
(710, 224, 'INTERNAL_PORT'),
(710, 163, 'SW_REF_INTERVAL'),
(710, 173, 'WARN_SVC_URL'),
(710, 174, 'DAT_CORE_SIZE'),
(710, 175, 'DAT_MAX_SIZE'),
(710, 176, 'DAT_QUEUE_SIZE'),
(710, 177, 'ALT_CORE_SIZE'),
(710, 178, 'ALT_MAX_SIZE'),
(710, 179, 'ALT_QUEUE_SIZE'),
(710, 214, 'SAMPLING_SWITCH'),
(710, 215, 'CRON_EXPRESSION'),
(710, 144, 'MAX_SMS_TASK_PROC'),
(710, 201, 'JVM_OPS'),
(710, 320, 'PRE_EMBEDDED'),

(711, 114, 'INST_ID'),
(711, 227, 'VERSION'),

(712, 114, 'INST_ID'),
(712, 116, 'SSH_ID'),
(712, 227, 'VERSION'),
(712, 198, 'META_SVR_URL'),
(712, 199, 'META_SVR_USR'),
(712, 200, 'META_SVR_PASSWD'),
(712, 321, 'SMS_EXT_PROTO_SWITCH'),
(712, 322, 'SMS_EXT_PROTO_PORT'),
(712, 202, 'REDIS_CLUSTER_CACHE'),
(712, 203, 'REDIS_CLUSTER_QUEUE'),
(712, 204, 'ORACLE_DG_SERV'),
(712, 143, 'SYSTEM_PROPERTY'),
(712, 144, 'MAX_SMS_TASK_PROC'),
(712, 145, 'BATCH_SAVE_PROCESS'),
(712, 146, 'WEB_CONSOLE_PORT'),
(712, 147, 'CMPP20_PORT'),
(712, 148, 'CMPP30_PORT'),
(712, 149, 'SGIP12_PORT'),
(712, 150, 'SMPP34_PORT'),
(712, 151, 'SMGP30_PORT'),
(712, 152, 'HTTP_PORT'),
(712, 153, 'HTTP_PORT2'),
(712, 154, 'HTTPS_PORT'),
(712, 155, 'MEISHENG_PORT'),
(712, 156, 'HTTP_GBK_PORT'),
(712, 157, 'WJSX_PORT'),
(712, 158, 'JDWS_ADDR'),
(712, 159, 'WEB_SERVICE_ADDR'),
(712, 160, 'WEB_SERVICE_TASK_URL'),
(712, 161, 'MO_SCAN_INTERVAL'),
(712, 162, 'HTTP_REPORT_INTERVAL'),
(712, 163, 'SW_REF_INTERVAL'),
(712, 164, 'CP_REF_INTERVAL'),
(712, 165, 'LOCAL_IP'),
(712, 166, 'CMPP20_PACKLOG'),
(712, 167, 'CMPP30_PACKLOG'),
(712, 168, 'SMGP_PACKLOG'),
(712, 169, 'SGIP_PACKLOG'),
(712, 170, 'HTTP_PACKLOG'),
(712, 171, 'HTTP2_PACKLOG'),
(712, 172, 'HTTPS_PACKLOG'),
(712, 173, 'WARN_SVC_URL'),
(712, 174, 'DAT_CORE_SIZE'),
(712, 175, 'DAT_MAX_SIZE'),
(712, 176, 'DAT_QUEUE_SIZE'),
(712, 177, 'ALT_CORE_SIZE'),
(712, 178, 'ALT_MAX_SIZE'),
(712, 179, 'ALT_QUEUE_SIZE'),
(712, 180, 'BST_CORE_SIZE'),
(712, 181, 'BST_MAX_SIZE'),
(712, 182, 'BST_QUEUE_SIZE'),
(712, 183, 'RPT_QUEUE_SIZE'),
(712, 184, 'HTTP_REPORT_PUSH'),
(712, 185, 'HTTP2_REPORT_PUSH'),
(712, 186, 'HTTPS_REPORT_PUSH'),
(712, 187, 'SGIP_REPORT_PUSH'),
(712, 188, 'ACCT_SERVICE'),
(712, 189, 'CMPP_ISMG_ID'),
(712, 190, 'SMGP_ISMG_ID'),
(712, 191, 'COLLECT_MSI'),
(712, 192, 'SPECIAL_REPORT_CUSTID'),
(712, 193, 'UNIQUE_LINK_URL'),
(712, 194, 'MAX_REPORT_FETCH'),
(712, 195, 'NO_REPORT_EXECUTE'),
(712, 196, 'DECISION_ENABLE'),
(712, 197, 'PROMETHEUS_PORT'),
(712, 201, 'JVM_OPS'),

(720, 114, 'INST_ID'),
(721, 114, 'INST_ID'),

(722, 114, 'INST_ID'),
(722, 116, 'SSH_ID'),
(722, 227, 'VERSION'),
(722, 198, 'META_SVR_URL'),
(722, 199, 'META_SVR_USR'),
(722, 200, 'META_SVR_PASSWD'),
(722, 296, 'VERTX_PORT'),
(722, 297, 'VERTX_SSL'),
(722, 298, 'VERTX_EVENT_LOOP_SIZE'),
(722, 299, 'VERTX_WORKER_SIZE'),
(722, 300, 'VERTX_TASK_TIMEOUT'),
(722, 202, 'REDIS_CLUSTER_CACHE'),
(722, 203, 'REDIS_CLUSTER_QUEUE'),
(722, 218, 'ROCKETMQ_SERV'),
(722, 301, 'ROCKETMQ_PRODUCER_GROUP_NAME'),
(722, 253, 'ES_MT_SERVER'),
(722, 308, 'ES_SCROLL_SIZE'),
(722, 309, 'ES_SCROLL_LISTSIZE'),
(722, 310, 'ES_SCROLL_TIMEOUT'),
(722, 311, 'ES_MT_HIS_INDEX'),
(722, 312, 'ES_MT_HIS_TYPE'),
(722, 313, 'ES_MT_HIS_SORTFIELD'),
(722, 314, 'ES_REPORT_HIS_INDEX'),
(722, 315, 'ES_REPORT_HIS_TYPE'),
(722, 316, 'ES_REPORT_HIS_SORTFIELD'),
(722, 317, 'ES_MO_HIS_INDEX'),
(722, 318, 'ES_MO_HIS_TYPE'),
(722, 319, 'ES_MO_HIS_SORTFIELD'),
(722, 302, 'EXPORT_URL'),
(722, 303, 'EXPORT_CREATE_TASK'),
(722, 201, 'JVM_OPS'),
(722, 305, 'EXCEL_TEMPDIR'),
(722, 306, 'EXCEL_MAXEXCELROW'),
(722, 307, 'CLICKHOUSE_SERV'),
(722, 204, 'ORACLE_DG_SERV'),
(722, 320, 'PRE_EMBEDDED'),

(800, 114, 'INST_ID'),

(801, 114, 'INST_ID'),
(801, 140, 'SERV_INST_ID'),
(801, 141, 'WEIGHT'),
(801, 142, 'SERV_CONTAINER_NAME');

/*---------------组件实例----------------*/
DROP TABLE IF EXISTS `t_meta_instance`;
CREATE TABLE `t_meta_instance` (
  `INST_ID`       varchar(36) NOT NULL COMMENT '实例ID',
  `CMPT_ID`       int         NOT NULL COMMENT '组件ID',
  `IS_DEPLOYED`   varchar(1)  NOT NULL COMMENT '0:未部署;1:已部署;2:异常停止',
  `POS_X`         int         NOT NULL COMMENT '组件左上顶点X坐标',
  `POS_Y`         int         NOT NULL COMMENT '组件左上顶点Y坐标',
  `WIDTH`         int         DEFAULT -1 COMMENT '组件宽度',
  `HEIGHT`        int         DEFAULT -1 COMMENT '组件高度',
  `ROW_`          int         DEFAULT -1 COMMENT 'layout row',
  `COL_`          int         DEFAULT -1 COMMENT 'layout column',
  PRIMARY KEY (`INST_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/*----------------实例属性----------------*/
DROP TABLE IF EXISTS `t_meta_instance_attr`;
CREATE TABLE `t_meta_instance_attr` (
  `INST_ID`       varchar(36)   NOT NULL COMMENT '实例ID',
  `ATTR_ID`       int           NOT NULL COMMENT '属性ID',
  `ATTR_NAME`     varchar(48)   NOT NULL COMMENT '属性key',
  `ATTR_VALUE`    varchar(2048) NOT NULL COMMENT '属性value',
  UNIQUE KEY `IDX_INSTANCE_INST_ATTR_ID` (`INST_ID`,`ATTR_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/*----------------部署服务----------------*/
DROP TABLE IF EXISTS `t_meta_service`;
CREATE TABLE `t_meta_service` (
  `INST_ID`            varchar(36) NOT NULL     COMMENT '服务ID,即最外层的容器ID',
  `SERV_NAME`          varchar(32) NOT NULL     COMMENT '服务名字',
  `SERV_TYPE`          varchar(32) NOT NULL     COMMENT '服务类别',
  `SERV_CLAZZ`         varchar(32) NOT NULL     COMMENT '服务大类:MQ,CACHE,DB',
  `VERSION`            varchar(36) NOT NULL     COMMENT '版本',
  `IS_DEPLOYED`        varchar(1)  NOT NULL     COMMENT '0:未部署;1:已部署',
  `IS_PRODUCT`         varchar(1)  NOT NULL     COMMENT '0:非生产;1:生产',
  `CREATE_TIME`        bigint(14)  NOT NULL     COMMENT '创建时间',
  `USER`               varchar(32) NOT NULL     COMMENT '服务默认用户',
  `PASSWORD`           varchar(64) NOT NULL     COMMENT '服务默认密码',
  `PSEUDO_DEPLOY_FLAG` char(1)     DEFAULT  '1' COMMENT '伪部署标志:1.物理实际部署;2.伪部署',
  PRIMARY KEY (`INST_ID`),
  UNIQUE KEY `SERV_NAME` (`SERV_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/*----------------拓扑关系----------------*/
DROP TABLE IF EXISTS `t_meta_topology`;
CREATE TABLE `t_meta_topology` (
  `INST_ID1`     varchar(36) NOT NULL COMMENT 'A端INST_ID或父INST_ID',
  `INST_ID2`     varchar(36) NOT NULL COMMENT 'Z端INST_ID或子INST_ID',
  `TOPO_TYPE`    int         NOT NULL COMMENT 'TOPO类型:1 link;2 contain',
  KEY `IDX_TOPO_INST_ID1` (`INST_ID1`),
  KEY `IDX_TOPO_INST_ID2` (`INST_ID2`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/*----------------组件关联版本-----------------*/
DROP TABLE IF EXISTS `t_meta_cmpt_versions`;
CREATE TABLE `t_meta_cmpt_versions` (
  `SERV_TYPE`    varchar(32) NOT NULL COMMENT '服务类别',
  `VERSION`      varchar(36) NOT NULL COMMENT '版本',
  UNIQUE KEY `IDX_CMPT_VER_IDX1` (`SERV_TYPE`, `VERSION`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT  INTO `t_meta_cmpt_versions`(`SERV_TYPE`,`VERSION`) VALUES
('CACHE_REDIS_CLUSTER', '5.0.2'),
('CACHE_REDIS_CLUSTER', '6.2.5'),

('CACHE_REDIS_MASTER_SLAVE', '5.0.2'),
('CACHE_REDIS_MASTER_SLAVE', '6.2.5'),

('CACHE_REDIS_HA_CLUSTER', '5.0.2'),
('CACHE_REDIS_HA_CLUSTER', '6.2.5'),

('DB_TIDB', '5.0.1'),
('DB_TIDB', '5.2.0'),

('DB_ORACLE_DG', '11g'),

('DB_TDENGINE', '2.0.16'),
('DB_TDENGINE', '2.3.3.0'),

('DB_CLICKHOUSE', '21.3.13'),

('DB_VOLTDB', '10.0'),

('DB_YUGABYTEDB', '2.9.0'),

('MQ_PULSAR', '2.7.2'),
('MQ_PULSAR', '2.8.0'),

('MQ_ROCKETMQ', '4.4.0'),

('SERVERLESS_APISIX', '2.7'),

('STORE_MINIO', '2022-04-26T01'),

('SMS_GATEWAY', '3.8.0'),
('SMS_GATEWAY', '3.8.0.1'),
('SMS_GATEWAY', '3.8.0.2'),
('SMS_GATEWAY', '3.8.1'),
('SMS_GATEWAY', '3.8.2'),

('SMS_QUERY_SERVICE', '3.8.0.1');


/*----------------自动部署物料源----------------*/
DROP TABLE IF EXISTS `t_meta_deploy_host`;
CREATE TABLE `t_meta_deploy_host` (
  `HOST_ID`      int         NOT NULL COMMENT '主机标识ID',
  `IP_ADDRESS`   varchar(16) NOT NULL COMMENT 'IP地址',
  `USER_NAME`    varchar(32) NOT NULL COMMENT '用户名',
  `USER_PWD`     varchar(32) NOT NULL COMMENT '密码',
  `SSH_PORT`     varchar(8)  NOT NULL COMMENT 'ftp端口',
  `CREATE_TIME`  bigint(14)  NOT NULL COMMENT '添加时间',
  PRIMARY KEY (`HOST_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

insert  into `t_meta_deploy_host`(`HOST_ID`,`IP_ADDRESS`,`USER_NAME`,`USER_PWD`,`SSH_PORT`,`CREATE_TIME`) values
(1, '127.0.0.1', 'ultravirs', 'wwwqqq.', '22', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000));

DROP TABLE IF EXISTS `t_meta_deploy_file`;
CREATE TABLE `t_meta_deploy_file` (
  `FILE_ID`      int          NOT NULL COMMENT '文件ID',
  `HOST_ID`      int          NOT NULL COMMENT '主机标识ID',
  `SERV_TYPE`    varchar(32)  NOT NULL COMMENT '服务分类',
  `VERSION`      varchar(36)  NOT NULL DEFAULT '' COMMENT '版本',
  `FILE_NAME`    varchar(256) NOT NULL COMMENT '文件名',
  `FILE_DIR`     varchar(255) NOT NULL COMMENT '文件所在目录',
  `CREATE_TIME`  bigint(14)   NOT NULL COMMENT '添加时间',
  PRIMARY KEY (`FILE_ID`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

INSERT INTO `t_meta_deploy_file`(`FILE_ID`,`HOST_ID`,`SERV_TYPE`,`FILE_NAME`,`FILE_DIR`,`CREATE_TIME`,`VERSION`) VALUES
(0,  1, 'COMMON_COLLECTD',     'paas-collectd-%VERSION%.zip',      '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '1.0.10'),
(1,  1, 'CACHE_REDIS_CLUSTER', 'redis-%VERSION%.tar.gz',           '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '5.0.2'),
(2,  1, 'CACHE_REDIS_CLUSTER', 'redis-cluster-proxy-1.0.tar.gz',   '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), ''),
(3,  1, 'DB_TIDB',             'pd-%VERSION%.tar.gz',              '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '5.2.0'),
(4,  1, 'DB_TIDB',             'tikv-%VERSION%.tar.gz',            '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '5.2.0'),
(5,  1, 'DB_TIDB',             'tidb-%VERSION%.tar.gz',            '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '5.2.0'),
(6,  1, 'MQ_ROCKETMQ',         'rocketmq-%VERSION%.tar.gz',        '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '4.4.0'),
(7,  1, 'SERVERLESS_APISIX',   'etcd-3.4.14.tar.gz',               '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), ''),
(8,  1, 'SERVERLESS_APISIX',   'apisix-%VERSION%.tar.gz',          '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '2.7'),
(9,  1, 'COMMON_TOOLS',        'jdk1.8.0_202.tar.gz',              '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), ''),
(10, 1, 'DB_TDENGINE',         'tdengine-%VERSION%.tar.gz',        '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '2.0.16'),
(11, 1, 'SERVERLESS_APISIX',   'lua-5.1.5.tar.gz',                 '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), ''),
(12, 1, 'SERVERLESS_APISIX',   'luarocks.tar.gz',                  '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), ''),
(13, 1, 'SERVERLESS_APISIX',   'openresty-1.19.3.1.tar.gz',        '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), ''),
(14, 1, 'SMS_GATEWAY',         'smsserver-%VERSION%.zip',          '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '3.8.0'),
(15, 1, 'SMS_GATEWAY',         'smsprocess-%VERSION%.zip',         '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '3.8.0'),
(16, 1, 'SMS_GATEWAY',         'smsclient-standard-%VERSION%.zip', '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '3.8.0'),
(17, 1, 'SMS_GATEWAY',         'smsbatsave-%VERSION%.zip',         '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '3.8.0'),
(18, 1, 'SMS_GATEWAY',         'smsstatistics-%VERSION%.zip',      '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '3.8.0'),
(19, 1, 'DB_TIDB',             'dashboard-proxy-5.0.1.tar.gz',     '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), ''),
(20, 1, 'MQ_PULSAR',           'zookeeper-3.7.0.tar.gz',           '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), ''),
(21, 1, 'MQ_PULSAR',           'bookkeeper-4.14.0.tar.gz',         '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), ''),
(22, 1, 'MQ_PULSAR',           'pulsar-%VERSION%.tar.gz',          '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '2.7.2'),
(23, 1, 'COMMON_MONITORING',   'prometheus-2.27.1.tar.gz',         '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), ''),
(24, 1, 'COMMON_MONITORING',   'grafana-7.5.7.tar.gz',             '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), ''),
(25, 1, 'COMMON_MONITORING',   'prometheus_pulsar.yml',            '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), ''),
(26, 1, 'DB_CLICKHOUSE',       'clickhouse-%VERSION%.tar.gz',      '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '21.3.13'),
(27, 1, 'COMMON_ZK',           'zookeeper-3.7.0.tar.gz',           '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), ''),
(28, 1, 'DB_CLICKHOUSE',       'prometheus_clickhouse.yml',        '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), ''),
(29, 1, 'SERVERLESS_APISIX',   'prometheus_apisix.yml',            '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), ''),
(30, 1, 'MQ_ROCKETMQ',         'rocketmq-console-1.1.0.tar.gz',    '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), ''),
(31, 1, 'DB_VOLTDB',           'voltdb-%VERSION%.tar.gz',          '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '10.0'),
(32, 1, 'MQ_PULSAR',           'pulsar-manager-0.2.0.tar.gz',      '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), ''),
(33, 1, 'DB_YUGABYTEDB',       'yugabyte-%VERSION%.tar.gz',        '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '2.9.0'),
(34, 1, 'COMMON_NGX',          'nginx-1.19.6.tar.gz',              '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '1.19.6'),
(35, 1, 'SMS_QUERY_SERVICE',   'smsqueryserver-%VERSION%.zip',     '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '1.2.0'),
(36, 1, 'COMMON_NGX',          'nginx_sms_query.conf',             '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), ''),
(37, 1, 'STORE_MINIO',         'minio-%VERSION%.zip',              '/DATA/sms/work/ftp/', ROUND(UNIX_TIMESTAMP(CURTIME(4))*1000), '2022-04-26T01');


DROP TABLE IF EXISTS `t_meta_server`;
CREATE TABLE `t_meta_server` (
  `SERVER_IP`    varchar(16)   NOT NULL COMMENT '服务器IP',
  `SERVER_NAME`  varchar(32)            COMMENT '服务器主机名',
  `CREATE_TIME`  bigint(14)    NOT NULL COMMENT '添加时间',
  PRIMARY KEY (`SERVER_IP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `t_meta_ssh`;
CREATE TABLE `t_meta_ssh` (
  `SSH_ID`       varchar(36)   NOT NULL COMMENT 'SSH唯一ID',
  `SSH_NAME`     varchar(32)   NOT NULL COMMENT 'SSH用户名',
  `SSH_PWD`      varchar(32)   NOT NULL COMMENT 'SSH密码',
  `SSH_PORT`     int           NOT NULL COMMENT 'SSH Port',
  `SERV_CLAZZ`   varchar(64)   NOT NULL COMMENT '安装的服务类型:MQ,CACHE,DB',
  `SERVER_IP`    varchar(16)   NOT NULL COMMENT '服务器IP',
  PRIMARY KEY (`SSH_ID`),
  UNIQUE KEY `SSH_KEY` (`SSH_NAME`,`SERVER_IP`,`SERV_CLAZZ`)/*,
  CONSTRAINT `SSH_IP_FOREIGN` FOREIGN KEY (`SERVER_IP`) REFERENCES `t_server` (`SERVER_IP`) ON DELETE NO ACTION*/
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `t_meta_oplogs`;
CREATE TABLE `t_meta_oplogs` (
  `ACC_NAME`     varchar(32)   NOT NULL COMMENT '账户NAME',
  `EVENT_TYPE`   varchar(32)            COMMENT '操作类型',
  `OP_DETAIL`    varchar(2048) NOT NULL COMMENT '操作详情',
  `INSERT_TIME`  bigint(14)    NOT NULL COMMENT '添加时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

create index IDX1_OP_LOGS on t_meta_oplogs(INSERT_TIME);

DROP TABLE IF EXISTS `t_meta_alarm`;
CREATE TABLE `t_meta_alarm` (
  `ALARM_ID`       bigint(10)    NOT NULL COMMENT '告警ID',
  `SERV_INST_ID`   varchar(36)   NOT NULL COMMENT '服务ID',
  `SERV_TYPE`      varchar(32)   NOT NULL COMMENT '服务类别',
  `INST_ID`        varchar(36)   NOT NULL COMMENT '实例ID',
  `CMPT_NAME`      varchar(48)   NOT NULL COMMENT '组件名字(EN)',
  `ALARM_TYPE`     int           NOT NULL COMMENT '告警类型',
  `ALARM_TIME`     bigint(14)    NOT NULL COMMENT '告警时间',
  `DEAL_TIME`      bigint(14)                COMMENT '处理时间',
  `DEAL_ACC_NAME`  varchar(32)               COMMENT '处理人',
  `IS_DEALED`      char(1)       DEFAULT '0' COMMENT '是否已处理 0:未处理,1:已处理'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

create index IDX1_ALARM on t_meta_alarm(SERV_INST_ID, INST_ID);
create index IDX2_ALARM on t_meta_alarm(ALARM_ID);

DROP TABLE IF EXISTS `t_meta_sequence`;
CREATE TABLE `t_meta_sequence` (
  `SEQ_NAME`      varchar(80) NOT NULL,
  `CURRENT_VALUE` bigint UNSIGNED DEFAULT 0 NOT NULL,
  PRIMARY KEY (`SEQ_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create index IDX1_SEQUENCE on t_meta_sequence(SEQ_NAME);

INSERT INTO `t_meta_sequence`(`SEQ_NAME`,`CURRENT_VALUE`) VALUES
('SEQ_ALARM',  0);
