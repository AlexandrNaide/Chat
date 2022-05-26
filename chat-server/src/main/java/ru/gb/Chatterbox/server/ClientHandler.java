package ru.gb.Chatterbox.server;

import ru.gb.Chatterbox.enums.Command;
import ru.gb.Chatterbox.server.error.WrongCredentialsException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static ru.gb.Chatterbox.constants.MessageConstants.REGEX;
import static ru.gb.Chatterbox.enums.Command.*;

public class ClientHandler {

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Thread handlerThread;
    private Server server;
    private String user;

    public ClientHandler(Socket socket, Server server){
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            System.out.println("Handler created.");
        } catch (IOException e){
            System.err.println("Connection problems with user: " + user);
        }
    }

    public void handle(){
        handlerThread = new Thread(() -> {
            authorize();

            while (!Thread.currentThread().isInterrupted() && socket.isConnected()){
                try{
                    String message = in.readUTF();
                    parseMessage(message);

                } catch (IOException e){
                    System.out.println("Connection broken with client: " + user);
                    server.removeHandler(this);
                }
            }
        });
        handlerThread.start();
    }

    private void parseMessage(String message) {
        String[] split = message.split(REGEX);

        Command command = Command.getByCommand(split[0]);

        switch (command){
            case BROADCAST_MESSAGE -> server.broadcast(user, split[1]);
            default -> System.out.println("Unknown message" + message);
        }
    }

    private void authorize() {
        System.out.println("Authorizing");

        try{
            while (!socket.isClosed()){
                String msg = in.readUTF();
                if (msg.startsWith(AUTH_MESSAGE.getCommand())){
                    String[] parsed = msg.split(REGEX);
                    String response = "";
                    String nickname = null;

                    try{
                        nickname = server.getUserService().authenticate(parsed[1], parsed[2]);
                    } catch (WrongCredentialsException e){
                        response = ERROR_MESSAGE.getCommand() + REGEX + e.getMessage();
                        System.out.println("Wrong credentials: " + parsed[1]);
                    }
                    if(server.isUserAlreadyOnline(nickname)){
                        response = ERROR_MESSAGE.getCommand() + REGEX + "This client already connected.";
                        System.out.println("Already connected.");
                    }

                    if (!response.equals("")){
                        send(response);
                    } else {
                        this.user = nickname;
                        send(AUTH_OK.getCommand() + REGEX + nickname);
                        server.addHandler(this);
                        break;
                    }
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }

    }

    public void send(String msg){
        try{
            out.writeUTF(msg);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public String getUser() {
        return user;
    }
}
