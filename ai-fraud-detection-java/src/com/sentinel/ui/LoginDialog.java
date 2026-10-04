package com.sentinel.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Random;

/**
 * SENTINEL AI - Multi-Role Authentication Gateway.
 * Demonstrates OOP (Inheritance from JDialog), Exception Handling, Collections.
 *
 * Role routing:
 *  - "Admin" / "12345"  -> ROLE_ADMIN
 *  - Any other user     -> card + PIN step -> ROLE_USER
 */
public class LoginDialog extends JDialog {

    // ---- Role constants ----
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER  = "USER";

    // ---- Outcome fields (read after dialog closes) ----
    private String authenticatedRole = null;   // ROLE_ADMIN | ROLE_USER | null = cancelled
    private String loggedInUsername  = null;
    private String selectedCard      = null;
    private String selectedBank      = null;

    // ---- Mock card catalogue ----
    private static final String[][] CARD_CATALOGUE = {
        {"Visa •• 4912",       "Chase"},
        {"Mastercard •• 8820", "Wells Fargo"},
        {"Amex •• 1042",       "Barclays"},
        {"Visa •• 3371",       "HSBC"},
        {"Mastercard •• 5509", "Citi"},
        {"Virtual •• 7700",    "Revolut"},
    };

    // ---- Theme (mirrors FraudDetectionDashboard) ----
    private static final Color BG_DARK       = new Color(7,  10, 18);
    private static final Color SURFACE       = new Color(17, 25, 46);
    private static final Color SURFACE_LIGHT = new Color(28, 41, 71);
    private static final Color BORDER        = new Color(34, 50, 84);
    private static final Color TEXT_MAIN     = new Color(241, 245, 249);
    private static final Color TEXT_MUTED    = new Color(148, 163, 184);
    private static final Color ACCENT        = new Color(99, 102, 241);
    private static final Color COLOR_GREEN   = new Color(16, 185, 129);
    private static final Color COLOR_ROSE    = new Color(244, 63, 94);

    public LoginDialog(Frame owner) {
        super(owner, "SENTINEL AI // Secure Authentication Gateway", true);
        setUndecorated(false);
        getContentPane().setBackground(BG_DARK);
        setSize(480, 500);
        setResizable(false);
        setLocationRelativeTo(owner);
        buildPhase1UI();
    }

    // ==========================================================
    //  PHASE 1 — Username + Password
    // ==========================================================
    private void buildPhase1UI() {
        getContentPane().removeAll();
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_DARK);
        root.setBorder(new EmptyBorder(32, 40, 32, 40));

        // --- Brand header ---
        root.add(buildBrandHeader(), BorderLayout.NORTH);

        // --- Form ---
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        JLabel subTitle = new JLabel("Enter your credentials to access the system");
        subTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subTitle.setForeground(TEXT_MUTED);
        gbc.gridy = 0;
        form.add(subTitle, gbc);

        // Username
        gbc.gridy = 1;
        form.add(formLabel("Username"), gbc);
        JTextField txtUsername = styledField();
        gbc.gridy = 2;
        form.add(txtUsername, gbc);

        // Password
        gbc.gridy = 3;
        form.add(formLabel("Password"), gbc);
        JPasswordField txtPassword = new JPasswordField();
        stylePasswordField(txtPassword);
        gbc.gridy = 4;
        form.add(txtPassword, gbc);

        // Status label
        JLabel lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Monospaced", Font.BOLD, 11));
        lblStatus.setForeground(COLOR_ROSE);
        gbc.gridy = 5;
        form.add(lblStatus, gbc);

        // Login button
        JButton btnLogin = accentButton("🔐  Sign In");
        gbc.gridy = 6;
        gbc.insets = new Insets(4, 0, 4, 0);
        form.add(btnLogin, gbc);

        // Exit button
        JButton btnExit = ghostButton("Cancel / Exit");
        gbc.gridy = 7;
        form.add(btnExit, gbc);

        root.add(form, BorderLayout.CENTER);

        // --- Footer ---
        JLabel hint = new JLabel("Admin access: Username 'Admin' | Standard: any username");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 10));
        hint.setForeground(TEXT_MUTED);
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        root.add(hint, BorderLayout.SOUTH);

        // --- Actions ---
        btnLogin.addActionListener(e -> {
            String user = txtUsername.getText().trim();
            String pass = new String(txtPassword.getPassword());
            if (user.isEmpty() || pass.isEmpty()) {
                lblStatus.setText("⚠  Username and password are required.");
                return;
            }
            if ("Admin".equalsIgnoreCase(user) && "12345".equals(pass)) {
                authenticatedRole = ROLE_ADMIN;
                loggedInUsername  = "Admin";
                selectedCard      = "N/A";
                selectedBank      = "All Banks";
                dispose();
            } else if ("Admin".equalsIgnoreCase(user)) {
                lblStatus.setText("✗  Invalid admin credentials.");
            } else {
                // Non-admin — proceed to card + PIN step
                buildPhase2UI(user);
            }
        });

        // Allow Enter key
        getRootPane().setDefaultButton(btnLogin);

        btnExit.addActionListener(e -> {
            authenticatedRole = null;
            dispose();
        });

        getContentPane().add(root);
        revalidate();
        repaint();
    }

    // ==========================================================
    //  PHASE 2 — Card selection + PIN (Standard User)
    // ==========================================================
    private void buildPhase2UI(String username) {
        getContentPane().removeAll();
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_DARK);
        root.setBorder(new EmptyBorder(32, 40, 32, 40));

        root.add(buildBrandHeader(), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        JLabel greeting = new JLabel("Welcome, " + username + " — Select your card to continue");
        greeting.setFont(new Font("SansSerif", Font.BOLD, 13));
        greeting.setForeground(TEXT_MAIN);
        gbc.gridy = 0;
        form.add(greeting, gbc);

        // Card dropdown — populate from catalogue + custom option
        gbc.gridy = 1;
        form.add(formLabel("Payment Card"), gbc);

        // Build display strings
        String[] cardDisplays = new String[CARD_CATALOGUE.length + 1];
        for (int i = 0; i < CARD_CATALOGUE.length; i++) {
            cardDisplays[i] = CARD_CATALOGUE[i][0] + "  [" + CARD_CATALOGUE[i][1] + "]";
        }
        cardDisplays[CARD_CATALOGUE.length] = "➕  Enter custom 16-digit card number...";

        JComboBox<String> cmbCard = new JComboBox<>(cardDisplays);
        styleCombo(cmbCard);
        gbc.gridy = 2;
        form.add(cmbCard, gbc);

        // Custom card panel (hidden by default)
        JTextField txtCustomCard = styledField();
        txtCustomCard.setToolTipText("Enter 16-digit card number");
        JLabel lblCustomCard = formLabel("Custom Card Number (16 digits)");
        lblCustomCard.setVisible(false);
        txtCustomCard.setVisible(false);
        gbc.gridy = 3;
        form.add(lblCustomCard, gbc);
        gbc.gridy = 4;
        form.add(txtCustomCard, gbc);

        cmbCard.addActionListener(e -> {
            boolean custom = cmbCard.getSelectedIndex() == CARD_CATALOGUE.length;
            lblCustomCard.setVisible(custom);
            txtCustomCard.setVisible(custom);
            revalidate();
        });

        // PIN / Password
        gbc.gridy = 5;
        form.add(formLabel("Account PIN / Password"), gbc);
        JPasswordField txtPin = new JPasswordField();
        stylePasswordField(txtPin);
        gbc.gridy = 6;
        form.add(txtPin, gbc);

        // Status
        JLabel lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Monospaced", Font.BOLD, 11));
        lblStatus.setForeground(COLOR_ROSE);
        gbc.gridy = 7;
        form.add(lblStatus, gbc);

        // Confirm button
        JButton btnConfirm = accentButton("✅  Confirm & Access Dashboard");
        gbc.gridy = 8;
        gbc.insets = new Insets(4, 0, 4, 0);
        form.add(btnConfirm, gbc);

        // Back button
        JButton btnBack = ghostButton("← Back to Login");
        gbc.gridy = 9;
        form.add(btnBack, gbc);

        root.add(form, BorderLayout.CENTER);

        btnConfirm.addActionListener(e -> {
            String pin = new String(txtPin.getPassword());
            if (pin.isEmpty()) {
                lblStatus.setText("⚠  PIN / Password is required.");
                return;
            }
            // Resolve card & bank
            int selIdx = cmbCard.getSelectedIndex();
            String card, bank;
            if (selIdx == CARD_CATALOGUE.length) {
                // Custom card
                String raw = txtCustomCard.getText().trim().replaceAll("\\D", "");
                if (raw.length() < 4) {
                    lblStatus.setText("⚠  Enter at least a 4-digit card number.");
                    return;
                }
                card = "Custom •• " + raw.substring(raw.length() - 4);
                bank = "Chase"; // default bank for custom
            } else {
                card = CARD_CATALOGUE[selIdx][0];
                bank = CARD_CATALOGUE[selIdx][1];
            }
            // PIN accepted (mock — any non-empty PIN passes for demo)
            authenticatedRole = ROLE_USER;
            loggedInUsername  = username;
            selectedCard      = card;
            selectedBank      = bank;
            dispose();
        });

        getRootPane().setDefaultButton(btnConfirm);
        btnBack.addActionListener(e -> buildPhase1UI());

        getContentPane().add(root);
        revalidate();
        repaint();
    }

    // ==========================================================
    //  OUTCOME ACCESSORS
    // ==========================================================
    public String getAuthenticatedRole() { return authenticatedRole; }
    public String getLoggedInUsername()  { return loggedInUsername; }
    public String getSelectedCard()      { return selectedCard; }
    public String getSelectedBank()      { return selectedBank; }

    // ==========================================================
    //  UI HELPERS
    // ==========================================================
    private JPanel buildBrandHeader() {
        JPanel p = new JPanel(new GridLayout(3, 1, 0, 2));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel logo = new JLabel("SENTINEL AI");
        logo.setFont(new Font("SansSerif", Font.BOLD, 26));
        logo.setForeground(ACCENT);

        JLabel tagline = new JLabel("Autonomous Fraud Defense System");
        tagline.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tagline.setForeground(TEXT_MAIN);

        JLabel guvi = new JLabel("GUVI Capstone #70 — Secure Authentication Gateway");
        guvi.setFont(new Font("SansSerif", Font.PLAIN, 10));
        guvi.setForeground(TEXT_MUTED);

        p.add(logo);
        p.add(tagline);
        p.add(guvi);
        return p;
    }

    private JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(TEXT_MAIN);
        return l;
    }

    private JTextField styledField() {
        JTextField tf = new JTextField();
        tf.setBackground(SURFACE_LIGHT);
        tf.setForeground(TEXT_MAIN);
        tf.setCaretColor(TEXT_MAIN);
        tf.setFont(new Font("Monospaced", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1),
                new EmptyBorder(6, 8, 6, 8)
        ));
        return tf;
    }

    private void stylePasswordField(JPasswordField pf) {
        pf.setBackground(SURFACE_LIGHT);
        pf.setForeground(TEXT_MAIN);
        pf.setCaretColor(TEXT_MAIN);
        pf.setFont(new Font("Monospaced", Font.PLAIN, 13));
        pf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1),
                new EmptyBorder(6, 8, 6, 8)
        ));
    }

    private void styleCombo(JComboBox<String> cb) {
        cb.setBackground(SURFACE_LIGHT);
        cb.setForeground(TEXT_MAIN);
        cb.setFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    private JButton accentButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ACCENT.darker(), 1, true),
                new EmptyBorder(10, 16, 10, 16)
        ));
        b.setPreferredSize(new Dimension(380, 40));
        return b;
    }

    private JButton ghostButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(SURFACE);
        b.setForeground(TEXT_MUTED);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        b.setPreferredSize(new Dimension(380, 36));
        return b;
    }
}
