import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*; 

class WinListener extends WindowAdapter {
    @Override
    public void windowClosing(WindowEvent e) {
        System.exit(0);
    }
}

public class ChatroomServer extends Frame {

    private int totalMsgCount = 0;
    private int blockedMsgCount = 0;
    private Label activeUserLabel;
    private Label msgCountLabel;
    private Label blockedCountLabel;
    private TextArea messages; 

    private int port = 3306;
    private int buffsize = 100;
    private DatagramSocket ds = null;

    private Set<String> clients = new HashSet<>();
    private Map<String, String> clientNames = new HashMap<>(); 
    private final Set<String> inappropriateWords = loadFilter("badwords.txt");

    private Set<String> loadFilter(String filename) {
        Set<String> words = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                words.add(line.trim().toLowerCase());
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error getting " + filename);
        } catch (IOException e) {
            System.err.println("Error reading filter file: " + e.getMessage());
        }
        return words;
    }

    private void updateDashboard() {
        activeUserLabel.setText("Active Users: " + clients.size());
        msgCountLabel.setText("Total Messages: " + totalMsgCount);
        blockedCountLabel.setText("Blocked Messages: " + blockedMsgCount);
    }

    public ChatroomServer() {
        super("Rooms - Chat Log"); 
        setLayout(new BorderLayout());

        Panel infoPanel = new Panel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        infoPanel.setBackground(Color.LIGHT_GRAY); 
        activeUserLabel = new Label("Active Users: 0");
        msgCountLabel = new Label("Total Messages: 0");
        blockedCountLabel = new Label("Blocked Messages: 0");
        infoPanel.add(activeUserLabel);
        infoPanel.add(new Label("|"));
        infoPanel.add(msgCountLabel);
        infoPanel.add(new Label("|"));
        infoPanel.add(blockedCountLabel);
        add(infoPanel, BorderLayout.NORTH);

        messages = new TextArea("", 10, 40, TextArea.SCROLLBARS_VERTICAL_ONLY); 
        add(messages, BorderLayout.CENTER); 
        addWindowListener(new WinListener());
        setSize(500, 450);
        setVisible(true);
        updateDashboard();
    }

    private boolean languageFilter(String message) {
        String lowerCaseMessage = message.toLowerCase();
        StringTokenizer st = new StringTokenizer(lowerCaseMessage, " \t\n\r\f,.:;?![](){}*+/=<>\"'-");
        while (st.hasMoreTokens()) {
            String word = st.nextToken();
            if (inappropriateWords.contains(word)) {
                return true;
            }
        }
        return false;
    }

    public void runServer(){
        DatagramPacket dp = null;
        try{
            ds = new DatagramSocket(port);
            messages.append("[SERVER START] port " + port + "\n");
            while(true){
                try{
                    byte[] buff = new byte[buffsize];
                    dp = new DatagramPacket(buff, buff.length);
                    ds.receive(dp);
                
                    String rdata = new String(dp.getData(), 0, dp.getLength()).trim();
                    InetAddress rAdd = dp.getAddress();
                    int rPort = dp.getPort();
                    String clientEndpoint = rAdd.getHostAddress() + ":" + rPort;

                    if (rdata.startsWith("[JOIN]")) {
                        String name = rdata.substring("[JOIN]".length());
                        
                        if (clients.add(clientEndpoint)) {
                            clientNames.put(clientEndpoint, name);
                            messages.append("[JOIN] " + name + " (" + clientEndpoint + ")\n");
                            broadcastUserCount();
                            updateDashboard();
                        }
                        continue;
                    }
                    
                    if (rdata.startsWith("[EXIT]")) {
                        String name = rdata.substring("[EXIT]".length());
                        if (clients.remove(clientEndpoint)) {
                            clientNames.remove(clientEndpoint);
                            messages.append("[EXIT] " + name + " (" + clientEndpoint + ")\n");
                            broadcastUserCount();
                            updateDashboard();
                        }
                        continue;
                    }

                    totalMsgCount++;

                    if (languageFilter(rdata)) {
                        messages.append("[BLOCKED] " + rdata + "\n");
                        blockedMsgCount++;
                        updateDashboard();

                        String warningMessage = "[SYSTEM] Your message was blocked for inappropriate content.";
                        byte[] warningData = warningMessage.getBytes();
                        DatagramPacket warningDp = new DatagramPacket(warningData, warningData.length, rAdd, rPort);
                        ds.send(warningDp);
                        continue;
                    }
                    messages.append(rdata + "\n");
                    String broadcastMessage = rdata; 
                    byte[] bMessage = broadcastMessage.getBytes();
                    sendToAll(bMessage);
                    updateDashboard();
                } catch(IOException e){
                    messages.append(e.toString() + "\n");  
                } 
            } 
        } catch(SocketException se){
            messages.append(se.toString() + "\n");
        } 
    }

    private void sendToAll(byte[] message) {
        for (String client : clients) {
            try {
                String[] parts = client.split(":");
                InetAddress clientAddress = InetAddress.getByName(parts[0]);
                int clientPort = Integer.parseInt(parts[1]);
                
                DatagramPacket sendDp = new DatagramPacket(message, message.length, clientAddress, clientPort);
                ds.send(sendDp);
            } catch (UnknownHostException e) {
                 messages.append("Error parsing client address: " + e.getMessage() + "\n");
            } catch (IOException e) {
                messages.append("Error sending to client " + client + ": " + e.getMessage() + "\n");
            }
        }
    }

    private void broadcastUserCount() {
        String countMessage = "[USER COUNT]" + clients.size();
        sendToAll(countMessage.getBytes());
    }

    public static void main (String[] args){
        ChatroomServer ds = new ChatroomServer();
        ds.runServer();
    }
}