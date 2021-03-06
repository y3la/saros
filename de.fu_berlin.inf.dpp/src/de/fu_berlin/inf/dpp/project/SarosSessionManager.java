/*
 * DPP - Serious Distributed Pair Programming
 * (c) Freie Universität Berlin - Fachbereich Mathematik und Informatik - 2006
 * (c) Riad Djemili - 2006
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 1, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package de.fu_berlin.inf.dpp.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Connection;
import org.picocontainer.annotations.Inject;

import de.fu_berlin.inf.dpp.ISarosContext;
import de.fu_berlin.inf.dpp.annotations.Component;
import de.fu_berlin.inf.dpp.filesystem.IProject;
import de.fu_berlin.inf.dpp.filesystem.IResource;
import de.fu_berlin.inf.dpp.monitoring.IProgressMonitor;
import de.fu_berlin.inf.dpp.negotiation.FileList;
import de.fu_berlin.inf.dpp.negotiation.IncomingProjectNegotiation;
import de.fu_berlin.inf.dpp.negotiation.IncomingSessionNegotiation;
import de.fu_berlin.inf.dpp.negotiation.NegotiationListener;
import de.fu_berlin.inf.dpp.negotiation.OutgoingProjectNegotiation;
import de.fu_berlin.inf.dpp.negotiation.OutgoingSessionNegotiation;
import de.fu_berlin.inf.dpp.negotiation.ProcessTools.CancelOption;
import de.fu_berlin.inf.dpp.negotiation.ProjectNegotiation;
import de.fu_berlin.inf.dpp.negotiation.ProjectNegotiationData;
import de.fu_berlin.inf.dpp.negotiation.SessionNegotiation;
import de.fu_berlin.inf.dpp.net.ConnectionState;
import de.fu_berlin.inf.dpp.net.IReceiver;
import de.fu_berlin.inf.dpp.net.ITransmitter;
import de.fu_berlin.inf.dpp.net.xmpp.IConnectionListener;
import de.fu_berlin.inf.dpp.net.xmpp.JID;
import de.fu_berlin.inf.dpp.net.xmpp.XMPPConnectionService;
import de.fu_berlin.inf.dpp.observables.ProjectNegotiationObservable;
import de.fu_berlin.inf.dpp.observables.SarosSessionObservable;
import de.fu_berlin.inf.dpp.observables.SessionIDObservable;
import de.fu_berlin.inf.dpp.observables.SessionNegotiationObservable;
import de.fu_berlin.inf.dpp.preferences.Preferences;
import de.fu_berlin.inf.dpp.project.internal.SarosSession;
import de.fu_berlin.inf.dpp.session.ISarosSession;
import de.fu_berlin.inf.dpp.session.ISarosSessionListener;
import de.fu_berlin.inf.dpp.session.ISarosSessionManager;
import de.fu_berlin.inf.dpp.session.User;
import de.fu_berlin.inf.dpp.ui.util.SWTUtils;
import de.fu_berlin.inf.dpp.util.StackTrace;

/**
 * The SessionManager is responsible for initiating new Saros sessions and for
 * reacting to invitations. The user can be only part of one session at most.
 * 
 * @author rdjemili
 */

@Component(module = "core")
public class SarosSessionManager implements ISarosSessionManager {

    /**
     * @JTourBusStop 6, Architecture Overview, Invitation Management:
     * 
     *               While Activities are used to keep a running session
     *               consistent, we use MESSAGES whenever the Session itself is
     *               modified. This includes adding users or projects to the
     *               session.
     * 
     *               The Invitation Process is managed by the "Invitation
     *               Management"-Component. This class is the main entrance
     *               point of this Component. During the invitation Process, the
     *               Network Layer is used to send MESSAGES between the host and
     *               the invitees and the Session Management is informed about
     *               joined users and added projects.
     * 
     *               For more information about the Invitation Process see the
     *               "Invitation Process"-Tour.
     * 
     */

    private static final Logger log = Logger
        .getLogger(SarosSessionManager.class.getName());

    private static final Random SESSION_ID_GENERATOR = new Random();

    private static final long LOCK_TIMEOUT = 10000L;

    private static final long NEGOTIATION_PROCESS_TIMEOUT = 10000L;

    private final SarosSessionObservable sarosSessionObservable;

    // FIMXE remove this crappy global state variable
    private final SessionIDObservable sessionIDObservable;

    private final Preferences preferences;

    // FIXME remove @Inject
    @Inject
    private ISarosContext sarosContext;

    private final NegotiationPacketListener negotiationPacketLister;

    private final SessionNegotiationObservable currentSessionNegotiations;

    private final ProjectNegotiationObservable currentProjectNegotiations;

    private XMPPConnectionService connectionService;

    private final List<ISarosSessionListener> sarosSessionListeners = new CopyOnWriteArrayList<ISarosSessionListener>();

    private final Lock startStopSessionLock = new ReentrantLock();

    private volatile boolean sessionStartup = false;

    private volatile boolean sessionShutdown = false;

    private volatile INegotiationHandler negotiationHandler;

    private final NegotiationListener negotiationListener = new NegotiationListener() {
        @Override
        public void negotiationTerminated(final SessionNegotiation negotiation) {
            currentSessionNegotiations.remove(negotiation);
        }

        @Override
        public void negotiationTerminated(final ProjectNegotiation negotiation) {
            currentProjectNegotiations
                .removeProjectExchangeProcess(negotiation);
        }
    };

    private final IConnectionListener listener = new IConnectionListener() {
        @Override
        public void connectionStateChanged(Connection connection,
            ConnectionState state) {

            if (state == ConnectionState.DISCONNECTING) {
                stopSarosSession();
            }
        }
    };

    public SarosSessionManager(XMPPConnectionService connectionService,
        SarosSessionObservable sarosSessionObservable,
        SessionIDObservable sessionID,
        SessionNegotiationObservable currentSessionNegotiations,
        ProjectNegotiationObservable currentProjectNegotiations,
        ITransmitter transmitter, IReceiver receiver, Preferences preferences) {
        this.connectionService = connectionService;
        this.sarosSessionObservable = sarosSessionObservable;
        this.sessionIDObservable = sessionID;
        this.currentSessionNegotiations = currentSessionNegotiations;
        this.currentProjectNegotiations = currentProjectNegotiations;
        this.preferences = preferences;
        this.connectionService.addListener(listener);

        this.negotiationPacketLister = new NegotiationPacketListener(this,
            currentSessionNegotiations, currentProjectNegotiations,
            transmitter, receiver);
    }

    // FIXME add back to interface
    // @Override
    public void setNegotiationHandler(INegotiationHandler handler) {
        negotiationHandler = handler;
    }

    /**
     * @JTourBusStop 3, Invitation Process:
     * 
     *               This class manages the current Saros session.
     * 
     *               Saros makes a distinction between a session and a shared
     *               project. A session is an on-line collaboration between
     *               users which allows users to carry out activities. The main
     *               activity is to share projects. Hence, before you share a
     *               project, a session has to be started and all users added to
     *               it.
     * 
     *               (At the moment, this separation is invisible to the user.
     *               He/she must share a project in order to start a session.)
     * 
     */
    @Override
    public void startSession(
        final Map<IProject, List<IResource>> projectResourcesMapping) {

        /*
         * FIXME split the logic, start a session without anything and then add
         * resources !
         */
        try {
            if (!startStopSessionLock.tryLock(LOCK_TIMEOUT,
                TimeUnit.MILLISECONDS)) {
                log.warn("could not start a new session because another operation still tries to start or stop a session");
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        try {

            if (sessionShutdown)
                throw new IllegalStateException(
                    "cannot start the session from the same thread context that is currently about to stop the session: "
                        + Thread.currentThread().getName());

            if (sessionStartup) {
                log.warn(
                    "recursive execution detected, ignoring session start request",
                    new StackTrace());
                return;
            }

            if (sarosSessionObservable.getValue() != null) {
                log.warn("could not start a new session because a session has already been started");
                return;
            }

            sessionStartup = true;

            sessionIDObservable.setValue(String.valueOf(SESSION_ID_GENERATOR
                .nextInt(Integer.MAX_VALUE)));

            negotiationPacketLister.setRejectSessionNegotiationRequests(true);

            // FIXME should be passed in (colorID, nickname)
            final SarosSession sarosSession = new SarosSession(
                preferences.getSessionNickname(),
                preferences.getFavoriteColorID(), sarosContext);

            sarosSessionObservable.setValue(sarosSession);

            sessionStarting(sarosSession);
            sarosSession.start();
            sessionStarted(sarosSession);

            for (Entry<IProject, List<IResource>> mapEntry : projectResourcesMapping
                .entrySet()) {

                final IProject project = mapEntry.getKey();
                final List<IResource> resourcesList = mapEntry.getValue();

                String projectID = String.valueOf(SESSION_ID_GENERATOR
                    .nextInt(Integer.MAX_VALUE));

                sarosSession.addSharedResources(project, projectID,
                    resourcesList);

                projectAdded(projectID);
            }

            log.info("session started");
        } finally {
            sessionStartup = false;
            startStopSessionLock.unlock();
        }
    }

    // FIXME offer a startSession method for the client and host !
    @Override
    public ISarosSession joinSession(JID host, String clientNickname,
        String hostNickname, int clientColor, int hostColor) {

        assert getSarosSession() == null;

        SarosSession sarosSession = new SarosSession(host, clientNickname,
            hostNickname, clientColor, hostColor, sarosContext);

        sarosSessionObservable.setValue(sarosSession);

        log.info("joined uninitialized Saros session");

        return sarosSession;
    }

    /**
     * @nonSWT
     */
    @Override
    public void stopSarosSession() {

        assert !SWTUtils.isSWT() : "stopSarosSession must not be called from SWT";

        try {
            if (!startStopSessionLock.tryLock(LOCK_TIMEOUT,
                TimeUnit.MILLISECONDS)) {
                log.warn("could not stop the current session because another operation still tries to start or stop a session");
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        try {

            if (sessionStartup)
                throw new IllegalStateException(
                    "cannot stop the session from the same thread context that is currently about to start the session: "
                        + Thread.currentThread().getName());

            if (sessionShutdown) {
                log.warn(
                    "recursive execution detected, ignoring session stop request",
                    new StackTrace());
                return;
            }

            SarosSession sarosSession = (SarosSession) sarosSessionObservable
                .getValue();

            if (sarosSession == null) {
                sessionIDObservable
                    .setValue(SessionIDObservable.NOT_IN_SESSION);
                return;
            }

            sessionShutdown = true;

            log.debug("terminating all running negotiation processes");

            if (!terminateNegotiationProcesses())
                log.warn("there are still running negotiation processes");

            sessionEnding(sarosSession);

            log.debug("Leave message sent.");

            try {
                sarosSession.stop();
            } catch (RuntimeException e) {
                log.error("Error stopping project: ", e);
            }

            sarosSessionObservable.setValue(null);

            sessionEnded(sarosSession);

            sessionIDObservable.setValue(SessionIDObservable.NOT_IN_SESSION);

            log.info("session stopped");
        } finally {
            sessionShutdown = false;
            negotiationPacketLister.setRejectSessionNegotiationRequests(false);
            startStopSessionLock.unlock();
        }
    }

    /**
     * This method and the sarosSessionObservable are dangerous to use. The
     * session might be in the process of being destroyed while you call this
     * method. The caller needs to save the returned value to a local variable
     * and do a null check. For new code you should consider being scoped by the
     * SarosSession and get the SarosSession in the constructor.
     * 
     * @deprecated Error prone method, which produces NPE if not handled
     *             correctly. Will soon get removed.
     */
    @Override
    @Deprecated
    public ISarosSession getSarosSession() {
        return sarosSessionObservable.getValue();
    }

    @Override
    public void invitationReceived(JID from, String sessionID,
        String invitationID, String version, String description) {

        INegotiationHandler handler = negotiationHandler;

        if (handler == null) {
            log.warn("could not accept invitation because no handler is installed");
            return;
        }

        IncomingSessionNegotiation process;

        synchronized (this) {
            if (!startStopSessionLock.tryLock()) {
                log.warn("could not accept invitation because the current session is about to stop");
                return;
            }

            try {

                // should not happen
                if (negotiationPacketLister
                    .isRejectingSessionNegotiationsRequests()) {
                    log.error("could not accept invitation because there is already a pending invitation");
                    return;
                }

                negotiationPacketLister
                    .setRejectSessionNegotiationRequests(true);

                sessionIDObservable.setValue(sessionID);

                process = new IncomingSessionNegotiation(this, from, version,
                    invitationID, description, sarosContext);

                process.setNegotiationListener(negotiationListener);
                currentSessionNegotiations.add(process);

            } finally {
                startStopSessionLock.unlock();
            }
        }
        handler.handleIncomingSessionNegotiation(process);

    }

    /**
     * This method is called when a new project was added to the session
     * 
     * @param from
     *            The one who added the project.
     * @param projectInfos
     *            what projects where added ({@link FileList}, projectName etc.)
     *            see: {@link ProjectNegotiationData}
     * @param processID
     *            ID of the exchanging process
     */
    @Override
    public void incomingProjectReceived(JID from,
        List<ProjectNegotiationData> projectInfos, String processID) {

        INegotiationHandler handler = negotiationHandler;

        if (handler == null) {
            log.warn("could not accept project negotiation because no handler is installed");
            return;
        }

        IncomingProjectNegotiation process;

        synchronized (this) {
            if (!startStopSessionLock.tryLock()) {
                log.warn("could not accept project negotiation because the current session is about to stop");
                return;
            }

            try {
                process = new IncomingProjectNegotiation(getSarosSession(),
                    from, processID, projectInfos, sarosContext);

                process.setNegotiationListener(negotiationListener);
                currentProjectNegotiations.addProjectExchangeProcess(process);

            } finally {
                startStopSessionLock.unlock();
            }
        }
        handler.handleIncomingProjectNegotiation(process);

    }

    @Override
    public void invite(JID toInvite, String description) {

        INegotiationHandler handler = negotiationHandler;

        if (handler == null) {
            log.warn("could not start an invitation because no handler is installed");
            return;
        }

        OutgoingSessionNegotiation process;

        synchronized (this) {
            if (!startStopSessionLock.tryLock()) {
                log.warn("could not start an invitation because the current session is about to start or stop");
                return;
            }

            try {

                ISarosSession session = getSarosSession();

                if (session == null)
                    return;

                /*
                 * this assumes that a user is added to the session before the
                 * negotiation terminates !
                 */
                if (session.getResourceQualifiedJID(toInvite) != null)
                    return;

                if (currentSessionNegotiations.exists(toInvite))
                    return;

                process = new OutgoingSessionNegotiation(toInvite, session,
                    description, sarosContext);

                process.setNegotiationListener(negotiationListener);
                currentSessionNegotiations.add(process);

            } finally {
                startStopSessionLock.unlock();
            }
        }
        handler.handleOutgoingSessionNegotiation(process);
    }

    @Override
    public void invite(Collection<JID> jidsToInvite, String description) {
        for (JID jid : jidsToInvite)
            invite(jid, description);
    }

    /**
     * Adds project resources to an existing session.
     * 
     * @param projectResourcesMapping
     * 
     */
    @Override
    public void addResourcesToSession(
        Map<IProject, List<IResource>> projectResourcesMapping) {

        ISarosSession session = getSarosSession();

        if (session == null) {
            log.warn("could not add resources because there is no active session");
            return;
        }

        /*
         * TODO: there are race conditions, USER A restricts USER B to read-only
         * while this code is executed
         */

        if (!session.hasWriteAccess()) {
            log.error("current local user has not enough privileges to add resources to the current session");
            return;
        }

        List<IProject> projectsToShare = new ArrayList<IProject>();

        for (Entry<IProject, List<IResource>> mapEntry : projectResourcesMapping
            .entrySet()) {

            final IProject project = mapEntry.getKey();
            final List<IResource> resourcesList = mapEntry.getValue();

            // side effect: non shared projects are always partial -.-
            if (!session.isCompletelyShared(project)) {
                String projectID = session.getProjectID(project);

                if (projectID == null)
                    projectID = String.valueOf(SESSION_ID_GENERATOR
                        .nextInt(Integer.MAX_VALUE));

                session.addSharedResources(project, projectID, resourcesList);
                projectAdded(projectID);
                projectsToShare.add(project);
            }
        }

        if (projectsToShare.isEmpty()) {
            log.warn("skipping project negotiation because no new projects were added to the current session");
            return;
        }

        INegotiationHandler handler = negotiationHandler;

        if (handler == null) {
            log.warn("could not start a project negotiation because no handler is installed");
            return;
        }

        List<OutgoingProjectNegotiation> negotiations = new ArrayList<OutgoingProjectNegotiation>();

        synchronized (this) {
            if (!startStopSessionLock.tryLock()) {
                log.warn("could not start a project negotiation because the current session is about to stop");
                return;
            }

            try {
                for (User user : session.getRemoteUsers()) {

                    OutgoingProjectNegotiation process = new OutgoingProjectNegotiation(
                        user.getJID(), session, projectsToShare, sarosContext);

                    process.setNegotiationListener(negotiationListener);
                    currentProjectNegotiations
                        .addProjectExchangeProcess(process);
                    negotiations.add(process);
                }
            } finally {
                startStopSessionLock.unlock();
            }
        }
        for (OutgoingProjectNegotiation negotiation : negotiations)
            handler.handleOutgoingProjectNegotiation(negotiation);
    }

    @Override
    public void startSharingProjects(JID user) {

        ISarosSession session = getSarosSession();

        if (session == null) {
            /*
             * as this currently only called by the OutgoingSessionNegotiation
             * job just silently return
             */
            log.error("cannot share projects when no session is running");
            return;
        }

        List<IProject> currentSharedProjects = new ArrayList<IProject>(
            session.getProjects());

        if (currentSharedProjects.isEmpty())
            return;

        INegotiationHandler handler = negotiationHandler;

        if (handler == null) {
            log.warn("could not start a project negotiation because no handler is installed");
            return;
        }

        OutgoingProjectNegotiation process;

        synchronized (this) {
            if (!startStopSessionLock.tryLock()) {
                log.warn("could not start a project negotiation because the current session is about to stop");
                return;
            }

            try {
                process = new OutgoingProjectNegotiation(user, session,
                    currentSharedProjects, sarosContext);

                process.setNegotiationListener(negotiationListener);
                currentProjectNegotiations.addProjectExchangeProcess(process);

            } finally {
                startStopSessionLock.unlock();
            }
        }
        handler.handleOutgoingProjectNegotiation(process);
    }

    @Override
    public void addSarosSessionListener(ISarosSessionListener listener) {
        sarosSessionListeners.add(listener);
    }

    @Override
    public void removeSarosSessionListener(ISarosSessionListener listener) {
        sarosSessionListeners.remove(listener);
    }

    @Override
    public void postOutgoingInvitationCompleted(IProgressMonitor monitor,
        User user) {
        try {
            for (ISarosSessionListener sarosSessionListener : sarosSessionListeners) {
                sarosSessionListener.postOutgoingInvitationCompleted(
                    sarosSessionObservable.getValue(), user, monitor);
            }
        } catch (RuntimeException e) {
            log.error("Internal error in notifying listener"
                + " of an outgoing invitation: ", e);
        }
    }

    @Override
    public void sessionStarting(ISarosSession sarosSession) {
        try {
            for (ISarosSessionListener sarosSessionListener : sarosSessionListeners) {
                sarosSessionListener.sessionStarting(sarosSession);
            }
        } catch (RuntimeException e) {
            log.error("error in notifying listener of session starting: ", e);
        }
    }

    @Override
    public void sessionStarted(ISarosSession sarosSession) {
        for (ISarosSessionListener sarosSessionListener : sarosSessionListeners) {
            try {
                sarosSessionListener.sessionStarted(sarosSession);
            } catch (RuntimeException e) {
                log.error("error in notifying listener of session start: ", e);
            }
        }
    }

    private void sessionEnding(ISarosSession sarosSession) {
        for (ISarosSessionListener sarosSessionListener : sarosSessionListeners) {
            try {
                sarosSessionListener.sessionEnding(sarosSession);
            } catch (RuntimeException e) {
                log.error("error in notifying listener of session ending: ", e);
            }
        }
    }

    private void sessionEnded(ISarosSession sarosSession) {
        for (ISarosSessionListener sessionlistener : sarosSessionListeners) {
            try {
                sessionlistener.sessionEnded(sarosSession);
            } catch (RuntimeException e) {
                log.error("error in notifying listener of session end: ", e);
            }
        }
    }

    @Override
    public void projectAdded(String projectID) {
        for (ISarosSessionListener sessionlistener : sarosSessionListeners) {
            try {
                sessionlistener.projectAdded(projectID);
            } catch (RuntimeException e) {
                log.error("error in notifying listener of an added project: ",
                    e);
            }
        }
    }

    private boolean terminateNegotiationProcesses() {

        for (SessionNegotiation process : currentSessionNegotiations.list()) {
            process.localCancel(null, CancelOption.NOTIFY_PEER);
        }

        for (ProjectNegotiation process : currentProjectNegotiations
            .getProcesses().values())
            process.localCancel(null, CancelOption.NOTIFY_PEER);

        log.trace("waiting for all invitation and project negotiation processes to terminate");

        long startTime = System.currentTimeMillis();

        boolean terminated = false;

        while (System.currentTimeMillis() - startTime < NEGOTIATION_PROCESS_TIMEOUT) {
            if (currentSessionNegotiations.list().isEmpty()
                && currentProjectNegotiations.getProcesses().isEmpty()) {
                terminated = true;
                break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return terminated;
    }
}
