package ru.gb.Chatterbox.client;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.gb.Chatterbox.client.net.MessageProcessor;
import ru.gb.Chatterbox.client.net.NetworkService;
import ru.gb.Chatterbox.enums.Command;

import javax.swing.event.ChangeEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static ru.gb.Chatterbox.client.Application.primaryStage;
import static ru.gb.Chatterbox.constants.MessageConstants.REGEX;
import static ru.gb.Chatterbox.enums.Command.*;

public class ChatController implements Initializable, MessageProcessor {

    public TreeView <String> contactPanel;

    @FXML
    private Button add;

    @FXML
    private Button addGroup;

    @FXML
    private Button del;

    @FXML
    private VBox changeNickPanel;

    @FXML
    private VBox changePasswordPanel;

    @FXML
    private TextField newNickField;

    @FXML
    private TextField newPassField;

    @FXML
    private TextField oldPassField;

    @FXML
    private VBox loginPanel;

    @FXML
    private TextField PasswordField;

    @FXML
    private TextField LoginField;

    @FXML
    private VBox mainPanel;

    @FXML
    private TextArea chatArea;

    @FXML
    private ListView<String> contacts;

    @FXML
    private TextField inputField;

    @FXML
    private Button btnSend;

    private NetworkService networkService;

    private String user;

    private static Map <String, Group> groups;

//    private static ObservableList <target> list;

    TreeItem <String> root;

    public void mockAction(ActionEvent actionEvent) {
        System.out.println("mock");
    }

    public void closeApplication(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void sendMessage(ActionEvent actionEvent) {
        try{
            String text = inputField.getText();
            if (text == null || text.isBlank()) {
                return;
            }

            MultipleSelectionModel<TreeItem<String>> selectionModel = contactPanel.getSelectionModel();
            selectionModel.setSelectionMode(SelectionMode.MULTIPLE);

            StringBuilder forMessage = new StringBuilder();

            for(TreeItem<String> item : selectionModel.getSelectedItems()){
                String recipient = item.getValue();
                forMessage.append(" ").append(recipient).append(",");
                if (groups.containsKey(recipient)){
                    for (String s : groups.get(recipient).getUsers().keySet()) {
                        networkService.sendMessage(PRIVATE_MESSAGE.getCommand() + REGEX + s + REGEX + text);
                    }
                } else {
                    networkService.sendMessage(PRIVATE_MESSAGE.getCommand() + REGEX + recipient + REGEX + text);
                }
            }
            forMessage.deleteCharAt(forMessage.length()-1);
            text = "[Message for" + forMessage + ":] " + text;
            chatArea.appendText(text + System.lineSeparator());
            inputField.clear();
        }catch (IOException e){
            showError("Network error.");
        }
    }

    private void showError(String s) {
        Alert alert = new Alert(Alert.AlertType.ERROR,
                s,
                ButtonType.CLOSE
                );
        alert.showAndWait();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        networkService = new NetworkService(this);
        try {
            authorization();
        } catch (IOException e) {
            e.printStackTrace();
        }

        groups = new HashMap<>();
        Group allUsers = new Group("Все");
        groups.put(allUsers.getTitle(), allUsers);

        // необязательные группы
        Group myOffice = new Group("Мой отдел");
        myOffice.add(new User("Толик"));
        myOffice.add(new User("Ваня"));
        myOffice.add(new User("Рома"));
        myOffice.add(new User("Ира"));
        groups.put(myOffice.getTitle(), myOffice);
        Group btcOffice = new Group("БТКашки");
        btcOffice.add(new User("Дашка"));
        btcOffice.add(new User("Женька-печенька"));
        btcOffice.add(new User("Танюха"));
        groups.put(btcOffice.getTitle(), btcOffice);

        File usersArchive = new File(String.valueOf(getClass().getResource("users.txt")));
        File groupsArchive = new File(String.valueOf(getClass().getResource("groups.txt")));

        if(usersArchive.length() != 0){
            downloadUsers(usersArchive, allUsers);
        }
        setItems();

//        list = FXCollections.observableArrayList();

/*        for (Group g : groups) {
            list.add(g.getTitle());
            if (g.getUnfold()) {
                for (User user : g.getUsers()) {
                    if (!this.user.equals(user.getNick())) {
                        list.add(user.getName(user.getNick()));
                    }
                }
            }
        }*/

//        contacts.setItems(list);

/*        contacts.setOnMouseClicked(e -> {

            if(mouseEvent.getClickCount() == 2){
                System.out.println("Double clicked");
            }

            for (Group g: groups){
                if (g.getTitle().equals(contacts.getFocusModel().getFocusedItem())){
                    if (g.isUnfold()){
                        g.setUnfold(false);
                        for(Name n: g.getGroup()){
                            list.remove(n.getName());
                        }
                    } else {
                        g.setUnfold(true);
                        for(Name n: g.getGroup()){
                            list.add(n.getName());
                        }
                    }
                }
            }
        });*/
    }

    private void setItems() {
        root = new TreeItem<>();
        for (Group g : groups.values()) {
            TreeItem <String> item = new TreeItem<>(g.toString());
            root.getChildren().add(item);
            for (String s : g.getUsers().keySet()) {
                TreeItem <String> childrenItem = new TreeItem<>(s);
                item.getChildren().add(childrenItem);
            }
            root.setExpanded(g.getUnfold());
        }
        root.setExpanded(true);
        contactPanel.setShowRoot(false);
        contactPanel.setRoot(root);
        contactPanel.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void downloadUsers(File usersArchive, Group allUsers) {

    }

    public void authorization () throws IOException {
        FXMLLoader rLoader = new FXMLLoader();
        rLoader.setLocation(this.getClass().getResource("/Authorization.fxml"));
        Parent rParent = rLoader.load();
        Scene authScene = new Scene(rParent);
        Stage authWindow = new Stage();
        authWindow.setResizable(false);
        authWindow.setTitle("Authentication");
        authWindow.setScene(authScene);

        authWindow.initModality(Modality.APPLICATION_MODAL);
        authWindow.initOwner(primaryStage);
        authWindow.showAndWait();
    }

    public void helpAction(ActionEvent actionEvent) throws IOException {
        FXMLLoader hLoader = new FXMLLoader();
        hLoader.setLocation(this.getClass().getResource("/HelpWindow.fxml"));
        Parent hParent = hLoader.load();
        Scene helpScene = new Scene(hParent);
        Stage helpWindow = new Stage();
        helpWindow.setTitle("Help");
        helpWindow.setScene(helpScene);
        helpWindow.show();
    }

    @Override
    public void processMessage(String message) {

        Platform.runLater(() -> parseMessage(message));
    }
    private void parseMessage(String message){

        String[] split = message.split(REGEX);
        Command command = Command.getByCommand(split[0]);

        switch (command){
            case AUTH_OK -> authOk(split);
            case ERROR_MESSAGE -> showError(split[1]);
            case LIST_USERS -> parseUsers(split);
            default -> chatArea.appendText(split[1] + System.lineSeparator());
        }
    }

    private void parseUsers(String[] split){
        List<String> contact = new ArrayList<>(Arrays.asList(split));
//        contact.set(0, "ALL");

        contact.remove(0);
        contact.remove(user);
        contact.removeIf(s -> groups.get("Все").getUsers().containsKey(s));
        groups.get("Все").addAll(contact);
        setItems();
    }

    private void authOk(String[] split){
        user = split[1];
        loginPanel.setVisible(false);
        primaryStage.setTitle("Chatterbox - " + user);
        mainPanel.setVisible(true);
    }

    public void sendChangeNick(ActionEvent actionEvent) {
        //@TODO
    }

    public void returnToChat(ActionEvent actionEvent) {
        //@TODO
    }

    public void sendChangePass(ActionEvent actionEvent) {
        //@TODO
    }

    public void sendAuth(ActionEvent actionEvent) {
        String login = LoginField.getText();
        String password = PasswordField.getText();

        if (login.isBlank() || password.isBlank()){
            return;
        }
        String msg = AUTH_MESSAGE.getCommand() + REGEX + login + REGEX + password;

        try{
            if (!networkService.isConnected()) {
                networkService.connect();

            }
            networkService.sendMessage(msg);
        }catch (IOException e){
            showError("Network error.");
        }
    }
}
