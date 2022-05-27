package ru.gb.Chatterbox.server;

import ru.gb.Chatterbox.server.service.UserService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.gb.Chatterbox.constants.MessageConstants.REGEX;
import static ru.gb.Chatterbox.enums.Command.BROADCAST_MESSAGE;
import static ru.gb.Chatterbox.enums.Command.LIST_USERS;

public class Server {

    private static final int PORT = 6830;

    private List<ClientHandler> handlers;

    private UserService userService;

    public Server(UserService userService) {
        this.userService = userService;
        this.handlers = new ArrayList<>();
    }

    public void start() {
        try(ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("Server start.");
            userService.start();
            while(true){
                System.out.println("Waiting for connection......");
                Socket socket = serverSocket.accept();
                System.out.println("Client connection.");
                ClientHandler handler = new ClientHandler(socket, this);
                handler.handle();
            }
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    public void broadcast(String from, String message){
        String msg = String.format("[%s]: %s", from, message);
        for (ClientHandler handler : handlers){
            handler.send(BROADCAST_MESSAGE.getCommand() + REGEX + msg);
        }
    }

    public UserService getUserService() {
        return userService;
    }

    public synchronized boolean isUserAlreadyOnline(String nick){
        for (ClientHandler handler : handlers) {
            if(handler.getUser().equals(nick)){
                return true;
            }
        }
        return false;
    }

    public synchronized void addHandler(ClientHandler handler){
        this.handlers.add(handler);
        sendContacts();
    }

    public synchronized void removeHandler(ClientHandler handler) {
        this.handlers.remove(handler);
        sendContacts();
    }

    private void shutdown(){
        userService.stop();
    }

    private void sendContacts(){
        String contacts = handlers.stream()
                .map(ClientHandler::getUser)
                .collect(Collectors.joining(REGEX));

        String msg = LIST_USERS.getCommand() + REGEX + contacts;

        for (ClientHandler handler : handlers) {
            handler.send(msg);
        }
    }
}
