package com.zzstack.paas.underlying.metasvr.exception;

public class SSHException extends Exception {
    
    private static final long serialVersionUID = 3789688987466853522L;

    public SSHException(Exception e) {
        super(e);
    }
    
    public SSHException(String message) {
        super(message);
    }
    
    public SSHException(String message, Throwable cause) {
		super(message, cause);
	}

}
