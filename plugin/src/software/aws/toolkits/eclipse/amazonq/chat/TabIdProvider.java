package software.aws.toolkits.eclipse.amazonq.chat;

public final class TabIdProvider {
    private static String tabId;

    private TabIdProvider() {
        // prevent initialization
    }

    public static void setTabId(final String newTabId) {
        tabId = newTabId;
    }

    public static String getTabId() {
        return tabId;
    }

}
