package com.zzstack.paas.underlying.metasvr.exception;

public class SeqException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 6205653092015953630L;

    public SeqException(Exception e) {
        super(e);
    }
    
    public SeqException(String message) {
        super(message);
    }
    
    public SeqException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
