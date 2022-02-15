package com.zzstack.paas.underlying.utils.exception;

public class PaasCollectException extends Exception {

    private static final long serialVersionUID = -141935044296575874L;

    public static enum CollectErrInfo {
        DEFAULT(81010000, "default"),
        
        
        
        END    (81019999, "end");
        
        
        private int code;
        private String errInfo;
        
        private CollectErrInfo(int code, String errInfo) {
            this.code = code;
            this.errInfo = errInfo;
        }
        
        public int getCode() {
            // 得到枚举值代表的字符串。
            return code;
        }
        
        public String getErrInfo() {
            return errInfo;
        }
    }
    
    private CollectErrInfo errInfo;
    
    public PaasCollectException(CollectErrInfo errInfo) {
        super(errInfo.getErrInfo());
        this.errInfo = errInfo;
    }
    
    public CollectErrInfo getErrInfo() {
        return errInfo;
    }

}
