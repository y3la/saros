package de.fu_berlin.inf.dpp.stf.client.testProject.testsuits.permissionsAndFollowmode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;

import org.junit.BeforeClass;
import org.junit.Test;

import de.fu_berlin.inf.dpp.stf.client.testProject.testsuits.STFTest;

public class TestWriteAccessChangeAndImmediateWrite extends STFTest {

    @BeforeClass
    public static void runBeforeClass() throws RemoteException,
        InterruptedException {
        initTesters(TypeOfTester.ALICE, TypeOfTester.BOB);
        setUpWorkbench();
        setUpSaros();
        setUpSessionWithAJavaProjectAndAClass(alice, bob);
    }

    /**
     * Steps:
     * 
     * 1. alice restrict to read only access.
     * 
     * 2. bob try to create inconsistency (set Text)
     * 
     * 3. alice grants write access to bob
     * 
     * 4. bob immediately begins to write it.
     * 
     * Expected Results:
     * 
     * 2. inconsistency should occur by bob.
     * 
     * 4. no inconsistency occur by bob.
     * 
     */
    @Test
    public void testFollowModeByOpenClassbyAlice() throws RemoteException {

        alice.sarosSessionV.restrictToReadOnlyAccess(bob.jid);
        bob.openC.openClass(VIEW_PACKAGE_EXPLORER, PROJECT1, PKG1, CLS1);
        bob.bot().editor(CLS1_SUFFIX).setTextWithoutSave(CP1);
        bob.sarosSessionV.waitUntilIsInconsistencyDetected();
        assertTrue(bob.bot().view(VIEW_SAROS_SESSION)
            .toolbarButton(TB_INCONSISTENCY_DETECTED)
            .isEnabled());
        bob.sarosSessionV.inconsistencyDetected();

        alice.sarosSessionV.grantWriteAccess(bob.jid);
        bob.bot().editor(CLS1_SUFFIX).setTextWithoutSave(CP2);
        bob.workbench.sleep(5000);
        assertFalse(bob.bot().view(VIEW_SAROS_SESSION)
            .toolbarButton(TB_INCONSISTENCY_DETECTED)
            .isEnabled());
    }
}
