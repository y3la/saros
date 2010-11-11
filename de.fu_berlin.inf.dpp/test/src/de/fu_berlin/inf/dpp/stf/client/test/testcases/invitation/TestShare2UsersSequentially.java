package de.fu_berlin.inf.dpp.stf.client.test.testcases.invitation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fu_berlin.inf.dpp.stf.client.Musician;
import de.fu_berlin.inf.dpp.stf.client.test.helpers.InitMusician;
import de.fu_berlin.inf.dpp.stf.server.BotConfiguration;
import de.fu_berlin.inf.dpp.stf.server.SarosConstant;

public class TestShare2UsersSequentially {
    private static final Logger log = Logger
        .getLogger(TestShare2UsersSequentially.class);

    private static final String PROJECT = BotConfiguration.PROJECTNAME;
    private static final String CLS = BotConfiguration.CLASSNAME;
    private static final String PKG = BotConfiguration.PACKAGENAME;
    private static Musician alice;
    private static Musician bob;

    @BeforeClass
    public static void initMusicians() throws RemoteException {
        alice = InitMusician.newAlice();
        bob = InitMusician.newBob();
        alice.mainMenu.newJavaProjectWithClass(PROJECT, PKG, CLS);
    }

    @AfterClass
    public static void resetSaros() throws RemoteException {
        bob.bot.resetSaros();
        alice.bot.resetSaros();
    }

    @After
    public void cleanUp() throws RemoteException {
        bob.bot.resetWorkbench();
        alice.bot.resetWorkbench();
    }

    @Test
    public void testShareProject2UsersSequentially() throws RemoteException,
        InterruptedException {
        log.trace("testShareProject enter");

        alice.buildSessionSequential(PROJECT,
            SarosConstant.CONTEXT_MENU_SHARE_PROJECT, bob);
        bob.basic
            .captureScreenshot((bob.state.getPathToScreenShot() + "/invitee_in_sharedproject.png"));
        alice.basic
            .captureScreenshot((alice.state.getPathToScreenShot() + "/inviter_in_sharedproject.png"));
        log.trace("inviter.setTextInClass");
        alice.eclipseEditor.setTextInJavaEditorWithSave(
            BotConfiguration.CONTENTPATH, PROJECT, PKG, CLS);

        log.trace("invitee.openFile");
        bob.packageExplorerV.openClass(PROJECT, PKG, CLS);

        // invitee.sleep(2000);
        assertTrue(bob.state.isParticipant(bob.jid));
        assertTrue(bob.state.isObserver(bob.jid));
        assertTrue(bob.state.isParticipant(alice.jid));
        assertTrue(bob.state.isDriver(alice.jid));

        assertTrue(alice.state.isParticipant(alice.jid));
        assertTrue(alice.state.isDriver(alice.jid));
        assertTrue(alice.state.isParticipant(bob.jid));
        assertTrue(alice.state.isObserver(bob.jid));

        alice.leaveSessionFirstByPeers(bob);
        assertFalse(bob.state.isParticipant(bob.jid));
        assertFalse(alice.state.isParticipant(alice.jid));
    }
}