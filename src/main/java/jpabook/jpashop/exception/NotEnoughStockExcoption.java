package jpabook.jpashop.exception;

public class NotEnoughStockExcoption extends RuntimeException {

    public NotEnoughStockExcoption() {
        super();
    }

    public NotEnoughStockExcoption(String message) {
        super(message);
    }

    public NotEnoughStockExcoption(String message, Throwable cause) {
        super(message, cause);
    }

    public NotEnoughStockExcoption(Throwable cause) {
        super(cause);
    }

}
