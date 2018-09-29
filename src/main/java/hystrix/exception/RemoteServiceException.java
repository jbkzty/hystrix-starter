package hystrix.exception;

/**
 * @author spuerKun
 * @date 2018/9/23.
 */
public class RemoteServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RemoteServiceException(String message) {
        super(message);
    }

}
