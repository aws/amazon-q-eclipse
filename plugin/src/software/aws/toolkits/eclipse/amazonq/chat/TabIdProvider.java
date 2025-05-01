package software.aws.toolkits.eclipse.amazonq.chat;

public class TabIdProvider {
    private static String tabId;

    public static void setTabId(String newTabId) {
        tabId = newTabId;
    }

    public static String getTabId() {
        return tabId;
    }

}
