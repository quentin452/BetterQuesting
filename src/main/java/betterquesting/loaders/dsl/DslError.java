package betterquesting.loaders.dsl;

public class DslError {

    public enum Severity {
        ERROR,
        WARNING,
        INFO
    }

    private final Severity severity;
    private final String fileName;
    private final int lineNumber;
    private final String message;
    private final String context;

    public DslError(Severity severity, String fileName, int lineNumber, String message, String context) {
        this.severity = severity;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.message = message;
        this.context = context;
    }

    public DslError(Severity severity, String fileName, int lineNumber, String message) {
        this(severity, fileName, lineNumber, message, null);
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getMessage() {
        return message;
    }

    public String getContext() {
        return context;
    }

    public String getColorCode() {
        switch (severity) {
            case ERROR:
                return "§c";
            case WARNING:
                return "§e";
            case INFO:
                return "§a";
            default:
                return "§f";
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[")
            .append(severity)
            .append("] ");
        sb.append(fileName)
            .append(":")
            .append(lineNumber);
        sb.append(" - ")
            .append(message);
        if (context != null && !context.isEmpty()) {
            sb.append("\n  Context: ")
                .append(context);
        }
        return sb.toString();
    }

    public String toColoredString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getColorCode())
            .append("[")
            .append(severity)
            .append("] §f");
        sb.append(fileName)
            .append(":")
            .append(lineNumber);
        sb.append(getColorCode())
            .append(" - ")
            .append(message);
        if (context != null && !context.isEmpty()) {
            sb.append("\n  §7Context: ")
                .append(context);
        }
        return sb.toString();
    }
}
