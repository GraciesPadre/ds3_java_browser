package com.spectralogic.dsbrowser.gui.components.newsession;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.controlsfx.control.PropertySheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class NewSessionPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(NewSessionPresenter.class);

    private final NewSessionModel model = new NewSessionModel();

    @FXML
    AnchorPane propertySheetAnchor;

    @FXML
    TableView<SavedSession> savedSessions;

    @Inject
    Ds3SessionStore store;

    @Inject
    SavedSessionStore savedSessionStore;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            initSessionList();
            initPropertySheet();
        } catch (final Exception e) {
             LOG.error("Failed to load NewSessionPresenter", e);
        }
    }

    private void initSessionList() {
        savedSessions.setRowFactory( tv -> {
            final TableRow<SavedSession> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    final SavedSession rowData = row.getItem();
                    final Ds3Client client = Ds3ClientBuilder.create(rowData.getEndpoint(), rowData.getCredentials().toCredentials()).withHttps(false).build();
                    final Session session = new Session(rowData.getName(), rowData.getEndpoint(), client);
                    store.addSession(session);
                    closeDialog();
                }
            });
            return row ;
        });

        savedSessions.setEditable(false);
        savedSessions.setItems(savedSessionStore.getSessions());
    }

    public void cancelSession() {
        LOG.info("Cancelling session");
        closeDialog();
    }

    public void createSession() {
        LOG.info("Performing session validation");
        // perform any validation
        // store in the session store
        store.addSession(model.toSession());
        closeDialog();
    }

    public void saveSession() {
        LOG.info("Creating new session");
        // perform validation
        // store in saved session store
        savedSessionStore.saveSession(model.toSession());
    }

    private void closeDialog() {
        final Stage popupStage = (Stage) propertySheetAnchor.getScene().getWindow();
        popupStage.close();
    }

    private void initPropertySheet() {

        final ObservableList<PropertySheet.Item> items = FXCollections.observableArrayList();

        items.add(new PropertyItem("Name", model.sessionNameProperty(), "Access Credentials", "The name for the session", String.class));
        items.add(new PropertyItem("Endpoint", model.endpointProperty(), "Access Credentials", "The Spectra S3 Endpoint", String.class));
        items.add(new PropertyItem("Access ID", model.accessKeyProperty(), "Access Credentials", "The Spectra S3 Endpoint Access ID", String.class));
        items.add(new PropertyItem("Secret Key", model.secretKeyProperty(), "Access Credentials", "The Spectra S3 Endpoint Secret Key", String.class));

        final PropertySheet propertySheet = new PropertySheet(items);
        propertySheet.setMode(PropertySheet.Mode.NAME);
        propertySheet.setModeSwitcherVisible(false);
        propertySheet.setSearchBoxVisible(false);
        propertySheetAnchor.getChildren().add(propertySheet);
    }


}