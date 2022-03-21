package proto

type PaasNode struct {
	INST_ID string      `json:"inst_id,required"`
	TEXT    string      `json:"text,required"`
	NODES   []*PaasNode `json:"nodes,omitempty"`
}
