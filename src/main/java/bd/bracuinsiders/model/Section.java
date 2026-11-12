package bd.bracuinsiders.model;

public enum Section {
    CAMPUS("Campus Updates"),
    STORIES("Student Stories"),
    EVENTS("Events");

    private final String label;

    Section(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
