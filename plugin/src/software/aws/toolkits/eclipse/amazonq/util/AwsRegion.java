package software.aws.toolkits.eclipse.amazonq.util;

import software.amazon.awssdk.regions.Region;

public final class AwsRegion {
    private final String id;
    private final String name;
    private final String partitionId;
    private final String category;
    private final String displayName;

    private AwsRegion(final Region region) {
        this.id = region.id();
        this.name = region.metadata().description();
        this.partitionId = region.metadata().partition().id();
        this.category = getCategory(this.id);
        this.displayName = getDisplayName(this.name, this.category, this.id);
    }

    public static AwsRegion from(final Region region) {
        return new AwsRegion(region);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String partitionId() {
        return partitionId;
    }

    public String category() {
        return category;
    }

    public String displayName() {
        return displayName;
    }

    private static String getCategory(final String id) {
        if (id.startsWith("af")) {
            return "Africa";
        } else if (id.startsWith("us") || id.startsWith("ca")) {
            return "North America";
        } else if (id.startsWith("eu")) {
            return "Europe";
        } else if (id.startsWith("ap")) {
            return "Asia Pacific";
        } else if (id.startsWith("sa")) {
            return "South America";
        } else if (id.startsWith("cn")) {
            return "China";
        } else if (id.startsWith("me")) {
            return "Middle East";
        } else {
            return null;
        }
    }

    private static String getDisplayName(final String name, final String category, final String id) {
        if (category == null) {
            return name;
        } else if (category.equals("Europe")) {
            return name.replaceFirst("Europe", "").trim() + " (" + id + ")";
        } else if (category.equals("North America")) {
            return name.replaceFirst("US West", "").replaceFirst("US East", "").trim() + " (" + id + ")";
        } else {
            return category + " (" + id + ")";
        }
    }

    public String toString() {
        return "{\"id\":\"" + id + "\",\"name\":\"" + name + "\",\"partitionId\":\"" + partitionId
                + "\",\"category\":\"" + category + "\",\"displayName\":\"" + displayName + "\"}";
    }
}

