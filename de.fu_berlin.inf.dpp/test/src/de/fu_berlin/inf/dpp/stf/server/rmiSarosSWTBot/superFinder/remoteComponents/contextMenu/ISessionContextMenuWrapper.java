package de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.contextMenu;

import java.rmi.RemoteException;

import de.fu_berlin.inf.dpp.User;
import de.fu_berlin.inf.dpp.stf.STF;

public interface ISessionContextMenuWrapper extends ISarosContextMenuWrapper {

    /**********************************************
     * 
     * contextMenus showed in session-area
     * 
     **********************************************/

    /**
     * Grant the selected buddy a write access by clicking his contextMenu
     * {@link STF#CM_GRANT_WRITE_ACCESS}.
     */
    public void grantWriteAccess() throws RemoteException;

    /**
     * Restrict the selected buddy to read only access by clicking his
     * contextMenu {@link STF#CM_RESTRICT_TO_READ_ONLY_ACCESS}
     * 
     * @throws RemoteException
     */
    public void restrictToReadOnlyAccess() throws RemoteException;

    /**
     * Follow the selected buddy by clicking his contextMenu
     * {@link STF#CM_FOLLOW_PARTICIPANT}
     * 
     * @throws RemoteException
     */
    public void followParticipant() throws RemoteException;

    /**
     * Stop following the selected buddy by clicking his contextMenu
     * {@link STF#CM_STOP_FOLLOWING}
     * 
     * @throws RemoteException
     */
    public void stopFollowing() throws RemoteException;

    /**
     * Jump to the position of the selected buddy by clicking his contextMenu
     * {@link STF#CM_JUMP_TO_POSITION_SELECTED_BUDDY}
     * 
     * @throws RemoteException
     */
    public void jumpToPositionOfSelectedBuddy() throws RemoteException;

    public void addProjects(String... projectNames) throws RemoteException;

    public void addBuddies(String... jidOfInvitees) throws RemoteException;

    /**
     * 
     * @return<tt>true</tt>, if the selected buddy has write access.
     * @throws RemoteException
     */
    public boolean hasWriteAccess() throws RemoteException;

    /**
     * 
     * @return<tt>true</tt>, if the selected buddy has read only access.
     * @throws RemoteException
     */
    public boolean hasReadOnlyAccess() throws RemoteException;

    /**
     * 
     * @return<tt>true</tt>, if the local user is following the selected buddy
     * @throws RemoteException
     */
    public boolean isFollowing() throws RemoteException;

    /**
     * wait until the condition
     * {@link ISessionContextMenuWrapper#hasWriteAccess()} is true . This method
     * should be used after performing the action
     * {@link ISessionContextMenuWrapper#grantWriteAccess()} to guarantee the
     * invitee has really got {@link User.Permission#WRITE_ACCESS} .
     * 
     * @throws RemoteException
     */
    public void waitUntilHasWriteAccess() throws RemoteException;

    /**
     * waits until the condition
     * {@link ISessionContextMenuWrapper#hasReadOnlyAccess()} is true. This
     * method should be used after performing the action
     * {@link ISessionContextMenuWrapper#restrictToReadOnlyAccess()}
     * 
     * @throws RemoteException
     */
    public void waitUntilHasReadOnlyAccess() throws RemoteException;

    /**
     * Wait until the condition {@link ISessionContextMenuWrapper#isFollowing()}
     * is true.
     * 
     * @throws RemoteException
     */
    public void waitUntilIsFollowing() throws RemoteException;

    /**
     * Wait until the condition {@link ISessionContextMenuWrapper#isFollowing()}
     * is false.
     * 
     * @throws RemoteException
     */
    public void waitUntilIsNotFollowing() throws RemoteException;

}