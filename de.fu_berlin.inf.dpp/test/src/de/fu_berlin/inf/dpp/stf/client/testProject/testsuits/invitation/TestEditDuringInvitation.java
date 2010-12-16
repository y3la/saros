package de.fu_berlin.inf.dpp.stf.client.testProject.testsuits.invitation;

import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fu_berlin.inf.dpp.stf.client.testProject.helpers.STFTest;

public class TestEditDuringInvitation extends STFTest {

    private static final Logger log = Logger
        .getLogger(TestEditDuringInvitation.class);

    /**
     * Preconditions:
     * <ol>
     * <li>alice (Host, Driver), alice share a java project with bob and carl.</li>
     * <li>bob (Observer)</li>
     * <li>carl (Observer)</li>
     * </ol>
     * 
     * @throws RemoteException
     * 
     */
    @BeforeClass
    public static void runBeforeClass() throws RemoteException {
        initTesters(TypeOfTester.ALICE, TypeOfTester.BOB, TypeOfTester.CARL);
        setUpWorkbenchs();
        setUpSaros();
        alice.pEV.newJavaProjectWithClass(PROJECT1, PKG1, CLS1);
    }

    @AfterClass
    public static void runAfterClass() throws RemoteException,
        InterruptedException {
        alice.leaveSessionHostFirstDone(bob, carl);
    }

    @Before
    public void runBeforeEveryTest() {
        //
    }

    @After
    public void runAfterEveryTest() {
        //
    }

    /**
     * 
     * Steps:
     * <ol>
     * <li>Alice invites Bob.</li>
     * <li>Bob accepts the invitation</li>
     * <li>Alice gives Bob driver capability</li>
     * <li>Alice invites Carl</li>
     * <li>Bob changes data during the running invtiation of Carl.</li>
     * </ol>
     * 
     * 
     * Expected Results:
     * <ol>
     * <li>All changes that Bob has done should be on Carl's side. There should
     * not be an inconsistency.</li>.
     * </ol>
     * 
     * @throws RemoteException
     */
    @Test
    public void testEditDuringInvitation() throws RemoteException {
        log.trace("starting testEditDuringInvitation, alice.buildSession");
        alice.buildSessionDoneSequentially(PROJECT1,
            TypeOfShareProject.SHARE_PROJECT, TypeOfCreateProject.NEW_PROJECT,
            bob);

        log.trace("alice.giveDriverRole");
        alice.sessionV.giveDriverRoleGUI(bob.sessionV);

        assertTrue(bob.sessionV.isDriver());

        log.trace("alice.inviteUser(carl");
        alice.sessionV.openInvitationInterface(carl.getBaseJid());

        log.trace("carl.confirmSessionInvitationWindowStep1");
        carl.pEV.confirmFirstPageOfWizardSessionInvitation();

        log.trace("bob.setTextInJavaEditor");
        bob.editor.setTextInJavaEditorWithSave(CP1, PROJECT1, PKG1, CLS1);

        log.trace("carl.confirmSessionInvitationWindowStep2UsingNewproject");
        carl.pEV.confirmSecondPageOfWizardSessionInvitationUsingNewproject();

        log.trace("getTextOfJavaEditor");
        String textFromCarl = carl.editor.getTextOfJavaEditor(PROJECT1, PKG1,
            CLS1);
        String textFormAlice = alice.editor.getTextOfJavaEditor(PROJECT1, PKG1,
            CLS1);

        String textFormBob = bob.editor.getTextOfJavaEditor(PROJECT1, PKG1,
            CLS1);
        assertTrue(textFromCarl.equals(textFormAlice));
        assertTrue(textFromCarl.equals(textFormBob));
        // assertTrue(carl.sessionV.isInconsistencyDetectedEnabled());

        log.trace("testEditDuringInvitation done");
    }
}