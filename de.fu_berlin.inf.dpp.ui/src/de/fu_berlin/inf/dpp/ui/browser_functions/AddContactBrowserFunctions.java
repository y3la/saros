package de.fu_berlin.inf.dpp.ui.browser_functions;

import de.fu_berlin.inf.ag_se.browser.IBrowserFunction;
import de.fu_berlin.inf.ag_se.browser.extensions.IJQueryBrowser;
import de.fu_berlin.inf.dpp.SarosPluginContext;
import de.fu_berlin.inf.dpp.net.xmpp.JID;
import de.fu_berlin.inf.dpp.ui.core_services.ContactListCoreService;
import de.fu_berlin.inf.dpp.ui.manager.IDialogManager;
import de.fu_berlin.inf.dpp.ui.view_parts.AddContactPage;
import de.fu_berlin.inf.dpp.util.ThreadUtils;
import org.apache.log4j.Logger;
import org.picocontainer.annotations.Inject;

/**
 * Encapsulates the browser functions for the add contact page.
 */
public class AddContactBrowserFunctions {

    private static final Logger LOG = Logger
        .getLogger(AddContactBrowserFunctions.class);

    @Inject
    private ContactListCoreService contactListCoreService;

    @Inject
    private IDialogManager dialogManager;

    @Inject
    private AddContactPage addContactPage;

    private IJQueryBrowser browser;

    public AddContactBrowserFunctions(IJQueryBrowser browser) {
        SarosPluginContext.initComponent(this);
        this.browser = browser;
    }

    /**
     * Injects Javascript functions into the HTML page. These functions
     * call Java code below when invoked.
     */
    public void createJavascriptFunctions() {
        browser
            .createBrowserFunction(new IBrowserFunction("__java_addContact") {
                @Override
                public Object function(final Object[] arguments) {
                    ThreadUtils.runSafeAsync(LOG, new Runnable() {
                        @Override
                        public void run() {
                            contactListCoreService
                                .addContact(new JID((String) arguments[0]));

                        }
                    });
                    dialogManager.closeDialogWindow(addContactPage);
                    return null;
                }
            });

        browser.createBrowserFunction(
            new IBrowserFunction("__java_cancelAddContactWizard") {
                @Override
                public Object function(Object[] arguments) {
                    dialogManager.closeDialogWindow(addContactPage);
                    return null;
                }
            });
    }
}
