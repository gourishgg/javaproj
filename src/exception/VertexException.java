package exception;

/**
 * Base checked exception for the Vertex Banking system.
 * Demonstrates Java Exception Hierarchy and custom application exceptions.
 */
public class VertexException extends Exception {
    public VertexException(String message) {
        super(message);
    }

    public VertexException(String message, Throwable cause) {
        super(message, cause);
    }
}
