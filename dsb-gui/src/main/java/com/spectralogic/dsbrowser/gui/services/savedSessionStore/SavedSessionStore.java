package com.spectralogic.dsbrowser.gui.services.savedSessionStore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class SavedSessionStore {
    private final static Logger LOG = LoggerFactory.getLogger(SavedSessionStore.class);
    private final static ObjectMapper MAPPER = new ObjectMapper();
    private final static Path PATH = Paths.get(System.getProperty("user.home"), ".dsbrowser", "sessions.json");

    private final ObservableList<SavedSession> sessions;

    private boolean dirty = false;

    public static SavedSessionStore loadSavedSessionStore() throws IOException {
        final List<SavedSession> sessions;
        if (Files.exists(PATH)) {
            try (final InputStream inputStream = Files.newInputStream(PATH)) {
                final SerializedSessionStore store = MAPPER.readValue(inputStream, SerializedSessionStore.class);
                sessions = store.getSessions();
            }
        } else {
            LOG.info("Creating new empty saved session store");
            sessions = new ArrayList<>();
        }
        return new SavedSessionStore(sessions);
    }

    public static void saveSavedSessionStore(final SavedSessionStore sessionStore) throws IOException {
        if (sessionStore.dirty) {
            LOG.info("Session store was dirty, saving...");
            final SerializedSessionStore store = new SerializedSessionStore(sessionStore.sessions);
            if (!Files.exists(PATH.getParent())) {
                Files.createDirectories(PATH.getParent());
            }
            try (final OutputStream outputStream = Files.newOutputStream(PATH, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
                MAPPER.writeValue(outputStream, store);
            }
        }
    }

    private SavedSessionStore(final List<SavedSession> sessionList) {
        this.sessions = FXCollections.observableArrayList(sessionList);
        this.sessions.addListener((ListChangeListener<SavedSession>) c -> {
            if (c.next() && (c.wasAdded() || c.wasRemoved())) {
                dirty = true;
            }
        });
    }

    public ObservableList<SavedSession> getSessions() {
        return sessions;
    }

    public void saveSession(final Session session) {
        this.sessions.add(
                new SavedSession(session.getSessionName(),
                        session.getEndpoint(),
                        SavedCredentials.fromCredentials(session.getClient().getConnectionDetails().getCredentials())));
    }

    public void removeSession(final SavedSession sessionName) {
        this.sessions.remove(sessionName);
    }

    private static class SerializedSessionStore {
        @JsonProperty("sessions")
        private final List<SavedSession> sessions;

        @JsonCreator
        private SerializedSessionStore(@JsonProperty("sessions") final List<SavedSession> sessions) {
            this.sessions = sessions;
        }

        public List<SavedSession> getSessions() {
            return sessions;
        }
    }
}