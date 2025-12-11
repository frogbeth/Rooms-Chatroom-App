import java.awt.*;
import java.awt.event.*;

class WelcomeScreen extends Frame implements ActionListener {

    private Button chatNowButton;
    private TextField nameInput;
    private ChatroomClient client; 

    public WelcomeScreen(ChatroomClient clientInstance) {
        super("Rooms");
        this.client = clientInstance;

        setLayout(new BorderLayout());

        Panel centerPanel = new Panel(new GridLayout(3, 1, 10, 10));

        Label welcomeLabel = new Label("Welcome to Rooms!", Label.CENTER);
        welcomeLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        centerPanel.add(welcomeLabel);
        Label usernameLabel = new Label("Enter a username or leave empty to randomize.", Label.CENTER);
        usernameLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        centerPanel.add(usernameLabel);

        Panel inputPanel = new Panel(new FlowLayout(FlowLayout.CENTER));
        nameInput = new TextField(20);
        inputPanel.add(nameInput);
        centerPanel.add(inputPanel);

        Panel southPanel = new Panel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        
        chatNowButton = new Button("Chat Now");
        chatNowButton.addActionListener(this);
        southPanel.add(chatNowButton);

        add(centerPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        
        setSize(400, 250);

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (dim.width - 400) / 2;
        int y = (dim.height - 250) / 2;
        setLocation(x, y);
        
        setVisible(true);
        nameInput.requestFocus();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == chatNowButton || e.getSource() == nameInput) {

            String displayName = nameInput.getText().trim();
            client.randomizeUsername(displayName);

            this.setVisible(false);
            this.dispose(); 

            client.runClient();
        }
    }
}