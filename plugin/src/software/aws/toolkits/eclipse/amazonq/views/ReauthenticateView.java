package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

import software.aws.toolkits.eclipse.amazonq.views.actions.SignoutAction;


public class ReauthenticateView extends CallToActionView {
	public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.ReauthenticateView";
	
	private static final String ICON_PATH = "icons/AmazonQ64.png";
	private static final String HEADER_LABEL = "Connection to Amazon Q Expired";
	private static final String DETAIL_MESSAGE = "Please re-authenticate to continue";
	private static final String BUTTON_LABEL = "Re-authenticate";
	private static final String LINK_LABEL = "Sign out";

	@Override
	protected String getIconPath() {
		return ICON_PATH;
	}

	@Override
	protected String getHeaderLabel() {
		return HEADER_LABEL;
	}

	@Override
	protected String getDetailMessage() {
		return DETAIL_MESSAGE;
	}

	@Override
	protected String getButtonLabel() {
		return BUTTON_LABEL;
	}

	@Override
	protected SelectionListener getButtonHandler() {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
            	// TODO
            }
        };
	}

	@Override
	protected String getLinkLabel() {
		return LINK_LABEL;
	}

	@Override
	protected SelectionListener getLinkHandler() {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
            	SignoutAction signoutAction = new SignoutAction();
            	signoutAction.run();
            }
        };
	}

}
