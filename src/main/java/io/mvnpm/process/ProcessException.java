package io.mvnpm.process;

public class ProcessException extends RuntimeException {

    private final String output;

    public ProcessException(String message, String output) {
        super(message);
        this.output = output;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + ": \n" + output;
    }

    public String output() {
        return output;
    }
}
