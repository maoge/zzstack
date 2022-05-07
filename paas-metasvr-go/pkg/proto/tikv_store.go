package proto

type TikvStore struct {
	COUNT  int         `json:"count,required"`
	STORES []StoreItem `json:"stores,required"`
}

type StoreItem struct {
	STORE  StoreInfo  `json:"store,required"`
	STATUS StatusInfo `json:"status,required"`
}

type StoreInfo struct {
	ID              int    `json:"id,required"`
	ADDRESS         string `json:"address,required"`
	VERSION         string `json:"version,required"`
	STATUS_ADDRESS  string `json:"status_address,required"`
	GIT_HASH        string `json:"git_hash,required"`
	START_TIMESTAMP int64  `json:"start_timestamp,required"`
	DEPLOY_PATH     string `json:"deploy_path,required"`
	LAST_HEARTBEAT  int64  `json:"last_heartbeat,required"`
	STATE_NAME      string `json:"state_name,required"`
}

type StatusInfo struct {
	CAPACITY          string  `json:"capacity,required"`
	AVAILABLE         string  `json:"available,required"`
	USED_SIZE         string  `json:"used_size,required"`
	LEADER_COUNT      int     `json:"leader_count,required"`
	LEADER_WEIGHT     int     `json:"leader_weight,required"`
	LEADER_SCORE      int     `json:"leader_score,required"`
	LEADER_SIZE       int     `json:"leader_size,required"`
	REGION_COUNT      int     `json:"region_count,required"`
	REGION_WEIGHT     int     `json:"region_weight,required"`
	REGION_SCORE      float64 `json:"region_score,required"`
	REGION_SIZE       int     `json:"region_size,required"`
	START_TS          string  `json:"start_ts,required"`
	LAST_HEARTBEAT_TS string  `json:"last_heartbeat_ts,required"`
	UPTIME            string  `json:"uptime,required"`
}
