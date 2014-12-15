package fbFetcher;

class ConnectionFailException extends Exception {	
    private static final long serialVersionUID = 1L;
	private String message = null;
 
    public ConnectionFailException() {
        super();
    }
 
    public ConnectionFailException(String message) {
        super(message);
        this.message = message;
    }
 
    public ConnectionFailException(Throwable cause) {
        super(cause);
    }
 
    @Override
    public String toString() {
        return message;
    }
 
    @Override
    public String getMessage() {
        return message;
    }
}