package server;

import com.sun.corba.se.spi.activation.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    DataInputStream in;
    DataOutputStream out;
    private String nick;
    private String login;

    public ClientHandler(server.Server server, Socket socket) {
    }

    public String getLogin() {
        return login;
    }

    public String getNick() {
        return nick;
    }

    public ClientHandler(Server server, Socket socket) {

        try {
            this.server = server;
            this.socket = socket;

//            socket.setSoTimeout(8000);

//            System.out.println("LocalPort: "+socket.getLocalPort());
//            System.out.println("Port: "+socket.getPort());
//            System.out.println("InetAddress: "+socket.getInetAddress());
            System.out.println("RemoteSocketAddress: " + socket.getRemoteSocketAddress());

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //цикл авторизации
                        while (true) {
                            String str = in.readUTF();
                            if (str.startsWith("/reg ")) {
                                String[] token = str.split(" ");
                                boolean b = server.getAuthService()
                                        .registration(token[1], token[2], token[3]);
                                if (!b) {
                                    ClientHandler.this.sendMsg("Ошибка: с этим логином уже Зарегистированы.");
                                } else {
                                    ClientHandler.this.sendMsg("Регистрация прошла успешно.");
                                }
                            }

                            if (str.startsWith("/auth ")) {
                                String[] token = str.split(" ");
                                String newNick = server.getAuthService()
                                        .getNicknameByLoginAndPassword(token[1], token[2]);
                                if (newNick != null) {
                                    if (!server.isLoginAuthorized(token[1])) {
                                        login = token[1];
                                        ClientHandler.this.sendMsg("/authok " + newNick);
                                        nick = newNick;
                                        server.subscribe(ClientHandler.this);
                                        System.out.println("Клиент " + nick + " подключился");
                                        break;
                                    } else {
                                        ClientHandler.this.sendMsg("С этим логином уже авторизовались");
                                    }
                                } else {
                                    ClientHandler.this.sendMsg("Неверный логин / пароль");
                                }
                            }

                        }
                        // цикл работы
                        while (true) {
                            String str = in.readUTF();
                            if (str.startsWith("/")) {
                                if (str.equals("/end")) {
                                    ClientHandler.this.sendMsg("/end");
                                    break;
                                }
                                if (str.startsWith("/w ")) {
                                    String[] token = str.split(" ", 3);
                                    server.privateMsg(ClientHandler.this, token[1], token[2]);
                                }

                            } else {
                                server.broadcastMsg(nick, str);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        server.unsubscribe(ClientHandler.this);
                        System.out.println("Клиент отключился");
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

