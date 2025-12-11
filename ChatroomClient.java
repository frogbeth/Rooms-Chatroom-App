import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class ChatroomClient extends Frame implements ActionListener, Runnable {

    // field
    private TextField data;
    private TextArea messages;
    private Label usernameLabel;
    private Label userCountLabel;
    private Button sendButton;
    private Button emojiButton;
    private Dialog emojiPicker;

    private String host = "localhost";
    private DatagramSocket ds = null;
    private DatagramPacket dp = null;
    private int port = 3306;
    private String userName;
    private Thread listenerThread;

    private final String[] animalNames = {
            "Dog", "Wombat", "Koala", "Sloth", "Cow", "Dolphin",
            "Raccoon", "Penguin", "Otter", "Kangaroo", "Wolf", "Fox"
    };
    private final String[] emojis = { ":)", ":D", ":'(", ":(", ";)", ":P", "8)", ">:-(", "<3", "-3-", "XD" };

    public ChatroomClient() {
        super("Rooms");

        Panel infoPanel = new Panel(new BorderLayout());
        Panel usernamePanel = new Panel(new FlowLayout(FlowLayout.LEFT));
        usernameLabel = new Label("Welcome, User");
        usernamePanel.add(usernameLabel);

        Panel countPanel = new Panel(new FlowLayout(FlowLayout.RIGHT));
        userCountLabel = new Label("Users: 0");
        countPanel.add(userCountLabel);

        infoPanel.add(usernamePanel, BorderLayout.WEST);
        infoPanel.add(countPanel, BorderLayout.EAST);
        infoPanel.setBackground(Color.LIGHT_GRAY);

        Panel mainPanel = new Panel(new BorderLayout());

        messages = new TextArea("", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY);
        messages.setEditable(false);
        messages.setFont(new Font("Dialog", Font.PLAIN, 12));
        mainPanel.add(messages, BorderLayout.CENTER);

        Panel inputPanel = new Panel(new BorderLayout(5, 0));

        data = new TextField("");
        data.setFont(new Font("Dialog", Font.PLAIN, 12));

        emojiButton = new Button("Emojis");
        emojiButton.addActionListener(this);

        sendButton = new Button("Send");
        sendButton.addActionListener(this);

        Panel textAndSendPanel = new Panel(new BorderLayout(5, 0));
        textAndSendPanel.add(data, BorderLayout.CENTER);
        textAndSendPanel.add(sendButton, BorderLayout.EAST);

        inputPanel.add(emojiButton, BorderLayout.WEST);
        inputPanel.add(textAndSendPanel, BorderLayout.CENTER);

        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        add(infoPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);

        data.addActionListener(this);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sendExitMessage();
                if (ds != null) {
                    ds.close();
                }
                System.exit(0);
            }
        });
        setSize(480, 480);
    }

    public void randomizeUsername(String inputName) {
        Random rand = new Random();
        if (inputName != null && !inputName.isEmpty()) {
            this.userName = inputName.replaceAll(":", "_");
        } else {
            int randomIndex = rand.nextInt(animalNames.length);
            this.userName = animalNames[randomIndex] + rand.nextInt(100);
        }
        usernameLabel.setText("Chatting as " + userName);
    }

    private void sendJoinMessage() {
        try {
            String joinMessage = "[JOIN] " + userName;
            byte[] joinData = joinMessage.getBytes();
            DatagramPacket joinDp = new DatagramPacket(joinData, joinData.length, InetAddress.getByName(host), port);
            ds.send(joinDp);
        } catch (IOException ioe) {
            System.err.println("Error sending join message: " + ioe.getMessage());
        }
    }

    private void sendExitMessage() {
        try {
            String exitMessage = "[EXIT]" + userName;
            byte[] exitData = exitMessage.getBytes();
            DatagramPacket exitDp = new DatagramPacket(exitData, exitData.length, InetAddress.getByName(host), port);
            ds.send(exitDp);
        } catch (IOException ioe) {
            System.err.println("Error sending exit message: " + ioe.getMessage());
        }
    }

    public void runClient() {
        if (this.userName == null) {
            randomizeUsername("");
        }
        try {
            ds = new DatagramSocket();
            sendJoinMessage();
            messages.append(userName + " has entered the room.\n");
            this.setVisible(true);
            listenerThread = new Thread(this);
            listenerThread.start();
        } catch (SocketException se) {
            messages.append("Socket error: " + se.toString() + "\n");
        }
    }

    private void showEmojiPicker() {
        if (emojiPicker == null) {
            emojiPicker = new Dialog(this, "Emojis", false);
            emojiPicker.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
            emojiPicker.setSize(300, 100);

            for (String emoji : emojis) {
                Button emojiBtn = new Button(emoji);
                emojiBtn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        data.setText(data.getText() + " " + emoji + " ");
                        data.requestFocus();
                        emojiPicker.setVisible(false);
                    }
                });
                emojiPicker.add(emojiBtn);
            }
            emojiPicker.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    emojiPicker.setVisible(false);
                }
            });
            Point loc = this.getLocation();
            emojiPicker.setLocation(loc.x + 50, loc.y + 100);
        }
        emojiPicker.setVisible(true);
    }

    @Override
    public void run() {
        byte[] buff = new byte[100];
        while (true) {
            try {
                DatagramPacket receiveDp = new DatagramPacket(buff, buff.length);
                ds.receive(receiveDp);
                String rdata = new String(receiveDp.getData(), 0, receiveDp.getLength()).trim();
                if (rdata.startsWith("[USER COUNT]")) {
                    String countStr = rdata.substring("[USER COUNT]".length());
                    userCountLabel.setText("Active Users: " + countStr);
                    continue;
                }
                messages.append(rdata + "\n");

            } catch (SocketException e) {
                if (e.getMessage().equals("Socket closed")) {
                    break;
                }
                messages.append("Listener Error: " + e.toString() + "\n");
                break;
            } catch (IOException e) {
                messages.append("Listener Error: " + e.toString() + "\n");
            }
        }
    }

    public static void main(String[] args) {
        ChatroomClient dc = new ChatroomClient();
        new WelcomeScreen(dc);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source == data || source == sendButton) {
            sendMessage();
        } else if (source == emojiButton) {
            showEmojiPicker();
        }
    }

    private void sendMessage() {
        try {
            String str = data.getText();

            if (str.trim().isEmpty())
                return;

            String messageToSend = userName + ": " + str;
            data.setText("");

            byte[] dataToSend = messageToSend.getBytes();
            dp = new DatagramPacket(dataToSend, dataToSend.length, InetAddress.getByName(host), port);
            ds.send(dp);
        } catch (IOException ioe) {
            messages.append(ioe.toString() + "\n");
        }
    }
}