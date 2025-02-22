import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpenseTracker {

    private JFrame frame;
    private JTabbedPane tabbedPane;
    private JTable spendingTable, transactionTable;
    private DefaultTableModel spendingModel, transactionModel;
    private JLabel totalExpenseLabel, balanceLabel, savingsLabel;
    private HashMap<String, Double> spendingData;
    private List<Object[]> transactionHistory;
    private double income = 0.0;
    private String currentUser = null;

    private static final String DATA_FILE_PREFIX = "expense_data_"; // Prefix to save per user data file

    public ExpenseTracker() {
        spendingData = new HashMap<>();
        transactionHistory = new ArrayList<>();
        showLoginDialog();
    }

    private void showLoginDialog() {
        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));

        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        loginPanel.add(new JLabel("Username:"));
        loginPanel.add(usernameField);
        loginPanel.add(new JLabel("Password:"));
        loginPanel.add(passwordField);
        loginPanel.add(loginButton);
        loginPanel.add(registerButton);

        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please enter both username and password.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (login(username, password)) {
                currentUser = username;
                loadData();
                initialize();
                JOptionPane.showMessageDialog(frame, "Login successful!");
                frame.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid credentials", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        registerButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please enter both username and password.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (register(username, password)) {
                JOptionPane.showMessageDialog(frame, "Registration successful!");
            } else {
                JOptionPane.showMessageDialog(frame, "Username already taken", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        frame = new JFrame("Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.add(loginPanel);
        frame.setLocationRelativeTo(null); // Center the frame on the screen
        frame.setVisible(true);
    }

    private boolean login(String username, String password) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("user_data.dat"))) {
            HashMap<String, String> userData = (HashMap<String, String>) ois.readObject();
            return userData.containsKey(username) && userData.get(username).equals(password);
        } catch (IOException | ClassNotFoundException e) {
            return false; // Error in loading data, assuming user doesn't exist
        }
    }

    private boolean register(String username, String password) {
        HashMap<String, String> userData;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("user_data.dat"))) {
            userData = (HashMap<String, String>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            userData = new HashMap<>();
        }

        if (userData.containsKey(username)) {
            return false;
        }

        userData.put(username, password);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("user_data.dat"))) {
            oos.writeObject(userData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void initialize() {
        frame = new JFrame("Expense Tracker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        tabbedPane = new JTabbedPane();

        // Spending Tab
        JPanel spendingPanel = new JPanel(new BorderLayout());
        spendingModel = new DefaultTableModel(new String[]{"Category", "Amount"}, 0);
        spendingTable = new JTable(spendingModel);
        updateSpendingTable();
        spendingPanel.add(new JScrollPane(spendingTable), BorderLayout.CENTER);

        JPanel summaryPanel = new JPanel(new GridLayout(4, 1)); // Adjusted layout
        totalExpenseLabel = new JLabel();
        balanceLabel = new JLabel();
        savingsLabel = new JLabel();
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout()); // Logout functionality
        updateSummaryLabels();
        summaryPanel.add(totalExpenseLabel);
        summaryPanel.add(balanceLabel);
        summaryPanel.add(savingsLabel);
        summaryPanel.add(logoutButton); // Add logout button
        spendingPanel.add(summaryPanel, BorderLayout.NORTH);

        JPanel spendingButtons = new JPanel();
        JButton addIncomeButton = new JButton("+ Income");
        addIncomeButton.addActionListener(e -> addIncome());
        JButton addExpenseButton = new JButton("+ Expense");
        addExpenseButton.addActionListener(e -> addExpense());
        spendingButtons.add(addIncomeButton);
        spendingButtons.add(addExpenseButton);
        spendingPanel.add(spendingButtons, BorderLayout.SOUTH);

        tabbedPane.add("Spending", spendingPanel);

        // Transaction Tab
        JPanel transactionPanel = new JPanel(new BorderLayout());
        transactionModel = new DefaultTableModel(new String[]{"Category", "Date", "Amount"}, 0);
        transactionTable = new JTable(transactionModel);
        loadTransactionHistory();
        transactionPanel.add(new JScrollPane(transactionTable), BorderLayout.CENTER);

        JButton deleteTransactionButton = new JButton("Delete");
        deleteTransactionButton.addActionListener(e -> deleteTransaction());
        transactionPanel.add(deleteTransactionButton, BorderLayout.SOUTH);

        tabbedPane.add("Transaction", transactionPanel);

        // Graph Tab
        JPanel graphPanel = new JPanel(new BorderLayout());
        JButton refreshGraphButton = new JButton("Refresh Graph");
        refreshGraphButton.addActionListener(e -> updateGraph(graphPanel));
        graphPanel.add(refreshGraphButton, BorderLayout.NORTH);
        updateGraph(graphPanel); // Ensure graph updates initially
        tabbedPane.add("Graph", graphPanel);

        frame.add(tabbedPane);
        frame.setVisible(true);
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to log out?", "Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            frame.dispose();
            currentUser = null;
            showLoginDialog();
        }
    }

    private void addIncome() {
        String incomeStr = JOptionPane.showInputDialog(frame, "Enter income amount (in \u09f3):");
        try {
            double amount = Double.parseDouble(incomeStr);
            income += amount;
            updateSummaryLabels();
            saveData();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Invalid amount.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addExpense() {
        String category = JOptionPane.showInputDialog(frame, "Enter category:");
        String amountStr = JOptionPane.showInputDialog(frame, "Enter amount (in \u09f3):");
        try {
            double amount = Double.parseDouble(amountStr);
            spendingData.put(category, spendingData.getOrDefault(category, 0.0) + amount);
            Object[] transaction = {category, java.time.LocalDate.now().toString(), amount};
            transactionHistory.add(transaction);
            transactionModel.addRow(transaction);
            updateSpendingTable();
            updateSummaryLabels();
            saveData();
            updateGraph((JPanel) tabbedPane.getComponentAt(2)); // Automatically refresh graph after adding expense
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Invalid amount.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteTransaction() {
        int selectedRow = transactionTable.getSelectedRow();
        if (selectedRow >= 0) {
            String category = (String) transactionModel.getValueAt(selectedRow, 0);
            double amount = (Double) transactionModel.getValueAt(selectedRow, 2);
            spendingData.put(category, spendingData.getOrDefault(category, 0.0) - amount);
            transactionHistory.remove(selectedRow);
            transactionModel.removeRow(selectedRow);
            updateSpendingTable();
            updateSummaryLabels();
            saveData();
            updateGraph((JPanel) tabbedPane.getComponentAt(2)); // Automatically refresh graph after deletion
        } else {
            JOptionPane.showMessageDialog(frame, "Select a transaction to delete.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateSpendingTable() {
        spendingModel.setRowCount(0);
        for (Map.Entry<String, Double> entry : spendingData.entrySet()) {
            spendingModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }
    }

    private void updateSummaryLabels() {
        DecimalFormat df = new DecimalFormat("0.00");
        double totalExpense = spendingData.values().stream().mapToDouble(Double::doubleValue).sum();
        double balance = income - totalExpense;
        double savings = balance > 0 ? balance : 0;

        totalExpenseLabel.setText("Total Expense: \u09f3" + df.format(totalExpense));
        balanceLabel.setText("Balance: \u09f3" + df.format(balance));
        savingsLabel.setText("Total Savings: \u09f3" + df.format(savings));
    }

    private void updateGraph(JPanel graphPanel) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Map.Entry<String, Double> entry : spendingData.entrySet()) {
            dataset.setValue(entry.getKey(), entry.getValue());
        }

        JFreeChart chart = ChartFactory.createPieChart("Spending Chart", dataset, true, true, false);
        ChartPanel chartPanel = new ChartPanel(chart);

        graphPanel.removeAll();
        graphPanel.add(chartPanel, BorderLayout.CENTER);
        graphPanel.revalidate();
        graphPanel.repaint();
    }

    private void saveData() {
        String dataFileName = DATA_FILE_PREFIX + currentUser + ".dat";
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFileName))) {
            oos.writeObject(spendingData);
            oos.writeDouble(income);
            oos.writeObject(transactionHistory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        String dataFileName = DATA_FILE_PREFIX + currentUser + ".dat";
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFileName))) {
            spendingData = (HashMap<String, Double>) ois.readObject();
            income = ois.readDouble();
            transactionHistory = (List<Object[]>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            spendingData = new HashMap<>();
            income = 0.0;
            transactionHistory = new ArrayList<>();
        }
    }

    private void loadTransactionHistory() {
        transactionModel.setRowCount(0);
        for (Object[] transaction : transactionHistory) {
            transactionModel.addRow(transaction);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExpenseTracker::new);
    }
}
