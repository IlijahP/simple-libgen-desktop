import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputAdapter;
import java.util.prefs.Preferences;
public class LibGenSearchApp {

    private static JFrame frame;
    private static JPanel panel;
    private static JTextField searchField;
    private static JButton searchButton;
    private static JPanel imagePanel;
    private static JLabel loadingStatusLabel;
    private static Path downloadDirectory;

    private static String languageCode = "eng";
    private static List<String> selectedFilters = new ArrayList<>();

    private static int currentPage = 1; // New field to track the current page
    private static JButton previousButton; // New field for the previous page button
    private static JButton nextButton; // New field for the next page button
    private static JLabel pageCountLabel;
    private static boolean isSearchInProgress = false;
    private static final int RESULTS_PER_PAGE = 5;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(LibGenSearchApp::createAndShowGUI);
    }

    private static void openLinkInBrowser(String finalMirror) {
        // Create a JTextArea to display the message
        JTextArea messageTextArea = new JTextArea(
                "Do you want to open the uploader in the browser?\n" + finalMirror
                        + "\n\n" + "Username: genesis\nPassword: upload");
        messageTextArea.setEditable(true);
        messageTextArea.setWrapStyleWord(true);
        messageTextArea.setLineWrap(true);
        messageTextArea.setCaretPosition(0);
        messageTextArea.setBackground(UIManager.getColor("Label.background"));
        messageTextArea.setFont(UIManager.getFont("Label.font"));
        messageTextArea.setBorder(UIManager.getBorder("TextField.border"));
    
        // Wrap the JTextArea in a JScrollPane
        JScrollPane scrollPane = new JScrollPane(messageTextArea);
        scrollPane.setPreferredSize(new Dimension(350, 100)); // Set the preferred size for the scroll pane
    
        // Create a JDialog for the custom dialog
        JDialog dialog = new JDialog();
        try {
            dialog.setIconImage(
                    ImageIO.read(Thread.currentThread().getContextClassLoader().getResourceAsStream("icon.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        dialog.setTitle("Open uploader");
        dialog.setModal(true); // Set the dialog to be modal
    
        // Create a panel to hold the components
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER); // Add the scroll pane instead of the text area
    
        // Create a button to open the link
        JButton openButton = new JButton("Open Link");
        openButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI(finalMirror));
                // Optionally, close the dialog after opening the link
                // dialog.dispose();
            } catch (IOException | URISyntaxException ex) {
                ex.printStackTrace();
            }
        });
    
        // Create a button to close the dialog
        JButton cancelButton = new JButton("Close");
        cancelButton.addActionListener(e -> dialog.dispose());
    
        // Add buttons to the panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(openButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
    
        // Add the panel to the dialog
        dialog.getContentPane().add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(null); // Center the dialog
        dialog.setVisible(true); // Show the dialog
    }

    private static void createAndShowGUI() {
        frame = new JFrame("Simple Libgen Desktop");
        try {
            frame.setIconImage(
                    ImageIO.read(Thread.currentThread().getContextClassLoader().getResourceAsStream("icon.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Get the screen size
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double width = screenSize.getWidth();
        double height = screenSize.getHeight();

        // Set the frame size as a fraction of the screen size
        frame.setSize((int) (width * 0.625), (int) (height * 0.46)); // for example, 62.5% of screen width and 46% of
                                                                     // screen height

        JMenuBar menuBar = new JMenuBar();
        JMenu optionsMenu = new JMenu("Options");
        JMenuItem setLanguageItem = new JMenuItem("Set Language Code");
        setLanguageItem.addActionListener(e -> setLanguageCode());
        JMenuItem defaultStorageLocation = new JMenuItem("Set Default Storage Location");

        defaultStorageLocation.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Download Directory");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            // Create a custom dialog
            JDialog dialog = new JDialog();
            dialog.setTitle("Select Download Directory");
            try {
                // Set custom icon for the dialog
                dialog.setIconImage(
                        ImageIO.read(Thread.currentThread().getContextClassLoader().getResourceAsStream("icon.png")));
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            dialog.add(fileChooser);
            dialog.pack();
            dialog.setLocationRelativeTo(null); // Center the dialog on screen

            // Show the dialog and get the user's selection
            int userSelection = fileChooser.showOpenDialog(dialog);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                downloadDirectory = fileChooser.getSelectedFile().toPath();
                System.out.println("Download location set to: " + downloadDirectory);
                Preferences prefs = Preferences.userRoot().node("org/serverboi/lgd");
                prefs.put("downloadDirectory", downloadDirectory.toString());
            } else {
                System.out.println("Download directory selection canceled.");
            }
        });
        optionsMenu.add(defaultStorageLocation);

        JMenu filterMenu = new JMenu("Filter");
        addFilterCheckBox(filterMenu, "Libgen", "l");
        addFilterCheckBox(filterMenu, "Comics", "c");
        addFilterCheckBox(filterMenu, "Fiction", "f");
        addFilterCheckBox(filterMenu, "Scientific Articles", "a");
        addFilterCheckBox(filterMenu, "Magazines", "m");
        addFilterCheckBox(filterMenu, "Fiction RUS", "r");
        addFilterCheckBox(filterMenu, "Standards", "s");
        optionsMenu.add(setLanguageItem);
        optionsMenu.add(filterMenu);
        menuBar.add(optionsMenu);

        JMenu uploadMenu = new JMenu("Upload");
        JMenuItem fictionItem = new JMenuItem("Fiction");
        fictionItem.addActionListener(e -> openLinkInBrowser("https://library.bz/fiction/upload/"));
        uploadMenu.add(fictionItem);
        JMenuItem nonFictionItem = new JMenuItem("Non-Fiction");
        nonFictionItem.addActionListener(e -> openLinkInBrowser("https://library.bz/main/upload/"));
        uploadMenu.add(nonFictionItem);
        menuBar.add(uploadMenu);
        frame.setJMenuBar(menuBar);

        frame.setLayout(new BorderLayout());
        panel = new JPanel(new BorderLayout());
        searchField = new JTextField(20);
        searchButton = new JButton("Search");

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        searchPanel.add(new JLabel("Enter search query: "));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        JPanel containerPanel = new JPanel(new GridBagLayout());
        imagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JScrollPane scrollableImagePanel = new JScrollPane(imagePanel);
        scrollableImagePanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollableImagePanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        containerPanel.add(scrollableImagePanel, gbc);

        loadingStatusLabel = new JLabel("");
        Dimension labelSize = new Dimension(50, 30);
        loadingStatusLabel.setPreferredSize(labelSize);
        loadingStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        searchButton.addActionListener(e -> {
            currentPage = 1;
            performSearch();
        });

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(containerPanel, BorderLayout.CENTER);
        panel.add(loadingStatusLabel, BorderLayout.SOUTH);

        JPanel paginationPanel = new JPanel();
        pageCountLabel = new JLabel("Page " + currentPage);
        previousButton = new JButton("Previous");
        nextButton = new JButton("Next");
        previousButton.addActionListener(e -> navigatePage(-1));
        nextButton.addActionListener(e -> navigatePage(1));
        paginationPanel.add(pageCountLabel);
        pageCountLabel.setVisible(false);
        paginationPanel.add(previousButton);
        paginationPanel.add(nextButton);
        nextButton.setEnabled(false);
        previousButton.setEnabled(false);

        frame.add(panel, BorderLayout.CENTER);
        frame.add(paginationPanel, BorderLayout.SOUTH);

        frame.setVisible(true);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateButtonStates();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateButtonStates();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateButtonStates();
            }
        });
    }

    private static void navigatePage(int delta) {
        currentPage += delta;

        performSearch(); // Call performSearch to load the new page
    }

    private static void setLanguageCode() {
        // Create a text field for input
        JTextField inputField = new JTextField(10);

        // Create the buttons for the dialog
        JButton okButton = new JButton("OK");
        okButton.setEnabled(false); // Initially disabled
        JButton cancelButton = new JButton("Cancel");

        // Panel to hold the input field and label
        JPanel panel = new JPanel();
        panel.add(new JLabel("Enter three-letter language code:"));
        panel.add(inputField);

        // Listener to enable OK button only when input is three letters
        inputField.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                String text = inputField.getText().trim();
                okButton.setEnabled(text.matches("[a-zA-Z]{3}"));
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });

        // Create a JOptionPane
        JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null,
                new Object[] {}, null);

        // Create a JDialog and set its button behavior
        final JDialog dialog = new JDialog(frame, "Set Language Code", true);
        dialog.setContentPane(optionPane);

        okButton.addActionListener(e -> {
            dialog.dispose();
            String newLanguageCode = inputField.getText().trim();
            if (newLanguageCode.matches("[a-zA-Z]{3}")) {
                languageCode = newLanguageCode.toLowerCase();
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        optionPane.setOptions(new Object[] { okButton, cancelButton });

        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private static void addFilterCheckBox(JMenu filterMenu, String label, String filterValue) {
        JCheckBoxMenuItem filterItem = new JCheckBoxMenuItem(label);
        filterItem.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", Boolean.TRUE);
        filterItem.setSelected(true);
        filterItem.addActionListener(e -> handleFilterSelection(filterItem, filterValue));

        filterMenu.add(filterItem);
    }

    private static void handleFilterSelection(JCheckBoxMenuItem filterItem, String filterValue) {
        if (filterItem.isSelected()) {
            selectedFilters.add(filterValue);
        } else {
            selectedFilters.remove(filterValue);
        }
    }

    private static void performSearch() {
        String userInput = searchField.getText().trim();
        if (!userInput.isEmpty() && !isSearchInProgress) {
            showLoadingStatusLabel();
            isSearchInProgress = true;
            nextButton.setEnabled(false);
            previousButton.setEnabled(false);
            updateButtonStates();

            try {
                String encodedQuery = URLEncoder.encode(userInput, StandardCharsets.UTF_8.toString());
                String url = constructLibGenUrl(encodedQuery, currentPage);

                SwingWorker<List<ImageDetails>, Integer> worker = new SwingWorker<List<ImageDetails>, Integer>() {
                    @Override
                    protected List<ImageDetails> doInBackground() throws Exception {
                        return scrapeLibGenImages(url);
                    }

                    @Override
                    protected void done() {
                        SwingUtilities.invokeLater(() -> {
                            try {
                                List<ImageDetails> imageDetailsList = get();

                                // Efficiently clear the panel on the Event Dispatch Thread
                                imagePanel.removeAll();
                                imagePanel.revalidate();
                                imagePanel.repaint();

                                if (imageDetailsList.isEmpty()) {
                                    handleNoResultsFound();
                                } else {
                                    updateSearchResults(imageDetailsList);
                                    nextButton.setEnabled(false);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            } finally {
                                hideLoadingStatusLabel();
                                isSearchInProgress = false;
                                nextButton.setEnabled(true);
                                updateButtonStates();
                            }
                        });
                    }
                };

                worker.execute();
            } catch (IOException ex) {
                ex.printStackTrace();
                hideLoadingStatusLabel();
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Please enter a search query.");
        }
    }

    private static void handleNoResultsFound() {
        searchButton.setEnabled(true);
        pageCountLabel.setVisible(false);
        JOptionPane.showMessageDialog(frame, "No results found", "Search Results", JOptionPane.INFORMATION_MESSAGE);
        nextButton.setEnabled(false);
        previousButton.setEnabled(searchField.getText().trim().isEmpty() && currentPage > 1);
    }

    private static void updateSearchResults(List<ImageDetails> imageDetailsList) {
        searchButton.setEnabled(true);
        pageCountLabel.setText("Page " + currentPage);
        pageCountLabel.setVisible(true);
        previousButton.setEnabled(!searchField.getText().trim().isEmpty() && currentPage > 1);
        nextButton.setEnabled(true);

        // Populate image panel with results
        populateImagePanel(imageDetailsList);
    }

    private static ImageIcon scaleImageIcon(URL imageUrl, int maxWidth, int maxHeight) throws IOException {
        // Load the original image
        BufferedImage originalImage = ImageIO.read(imageUrl);

        // Calculate the scaling factors to fit within maxWidth and maxHeight while
        // maintaining the aspect ratio
        double widthScaleFactor = (double) maxWidth / originalImage.getWidth();
        double heightScaleFactor = (double) maxHeight / originalImage.getHeight();
        double scaleFactor = Math.min(widthScaleFactor, heightScaleFactor);

        // Calculate the new dimensions
        int newWidth = (int) (originalImage.getWidth() * scaleFactor);
        int newHeight = (int) (originalImage.getHeight() * scaleFactor);

        // Create a new buffered image with the new dimensions
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();

        // Draw the original image scaled to the new size
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return new ImageIcon(scaledImage);
    }

    private static void populateImagePanel(List<ImageDetails> imageDetailsList) {
        // Calculate the range of results to display for the current page
        int startIndex = (currentPage - 1) * RESULTS_PER_PAGE;
        int endIndex = Math.min(startIndex + RESULTS_PER_PAGE, imageDetailsList.size());

        for (int i = startIndex; i < endIndex; i++) {
            ImageDetails details = imageDetailsList.get(i);

            SwingWorker<ImageIcon, Void> imageLoader = new SwingWorker<ImageIcon, Void>() {
                @Override
                protected ImageIcon doInBackground() throws Exception {
                    // Load and scale the image in the background
                    URL imageUrl = new URL(details.getImageUrl());
                    return scaleImageIcon(imageUrl, 229, 327);
                }

                @Override
                protected void done() {
                    try {
                        // Update the UI with the scaled image
                        ImageIcon icon = get();
                        JLabel imageLabel = new JLabel(icon);
                        imageLabel.addMouseListener(new ImageClickListener(details));
                        imagePanel.add(imageLabel);
                        imagePanel.revalidate();
                        imagePanel.repaint();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        // Handle exceptions (e.g., image loading failed)
                    }
                }
            };
            imageLoader.execute();
        }
    }

    private static void updateButtonStates() {
        boolean isSearchFieldEmpty = searchField.getText().trim().isEmpty();
        searchButton.setEnabled(!isSearchFieldEmpty && !isSearchInProgress); // Disable search button if the field is
                                                                             // empty or a search is in progress
    }

    private static void showLoadingStatusLabel() {
        loadingStatusLabel.setText("Loading...");
    }

    private static void hideLoadingStatusLabel() {
        loadingStatusLabel.setText("");
    }

    private static String constructLibGenUrl(String encodedQuery, int page) {
        // Estimate the initial capacity to avoid incremental capacity increase
        int estimatedSize = 200 + encodedQuery.length() + selectedFilters.size() * 10;
        StringBuilder urlBuilder = new StringBuilder(estimatedSize);

        urlBuilder.append("https://libgen.li/index.php?req=")
                .append(encodedQuery)
                .append("+lang%3A")
                .append(languageCode)
                .append("&columns[]=t&columns[]=a&columns[]=s&columns[]=y&columns[]=p&columns[]=i&objects[]=f&objects[]=e&objects[]=s&objects[]=a&objects[]=p&objects[]=w");

        // Use stream API for appending filters
        selectedFilters.stream().forEach(filter -> urlBuilder.append("&topics[]=").append(filter));

        urlBuilder.append("&res=25&covers=on&gmode=on&filesuns=all");

        return urlBuilder.toString();
    }

    private static List<ImageDetails> scrapeLibGenImages(String url) {
        try {
            Document document = Jsoup.connect(url).get();
            Elements rows = document.select("table > tbody > tr");

            return rows.stream().map(row -> {
                Elements imgElement = row.select("td a img[src]");
                if (imgElement.isEmpty())
                    return null;

                String title = getTextOrPlaceholder(row, "td:nth-child(2) b", "No title set by uploader");
                String author = getTextOrPlaceholder(row, "td:nth-child(3)", "No author set by uploader");
                String publisher = getTextOrPlaceholder(row, "td:nth-child(4)", "No publisher set by uploader");
                String year = getTextOrPlaceholder(row, "td:nth-child(5)", "No year set by uploader");
                String lang = getTextOrPlaceholder(row, "td:nth-child(6)", "No lang set by uploader");
                String size = getTextOrPlaceholder(row, "td:nth-child(8)", "No size calculated by libgen");
                List<String> mirrors = row.select("td:last-child a").stream()
                        .map(mirrorElement -> mirrorElement.attr("href"))
                        .collect(Collectors.toList());
                String imageUrl = imgElement.first().attr("abs:src").replace("_small", "");

                return new ImageDetails(imageUrl, title, author, publisher, year, lang, size, mirrors);
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private static String getTextOrPlaceholder(Element row, String cssQuery, String placeholder) {
        String text = row.select(cssQuery).text().trim();
        return text.isEmpty() ? placeholder : text;
    }

    private static class ImageClickListener extends MouseInputAdapter {
        private final ImageDetails imageDetails;

        public ImageClickListener(ImageDetails imageDetails) {
            this.imageDetails = imageDetails;
        }

        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
            String detailsMessage = "Title: " + imageDetails.getTitle() + "\n" +
                    "Author: " + imageDetails.getAuthor() + "\n" +
                    "Publisher: " + imageDetails.getPublisher() + "\n" +
                    "Year: " + imageDetails.getYear() + "\n" +
                    "Language: " + imageDetails.getLang() + "\n" +
                    "Size: " + imageDetails.getSize();

            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new FlowLayout());

            for (String mirror : imageDetails.getMirrors()) {
                if (!mirror.startsWith("http")) {
                    mirror = "https://libgen.li" + mirror;
                }

                JButton mirrorButton = new JButton(getBaseURL(mirror));
                final String finalMirror = mirror;

                mirrorButton.addActionListener(event -> {
                    if (finalMirror.contains("library.lol") || finalMirror.contains("libgen.li")) {
                        handleLibgenMirrorButtonClick(finalMirror);
                    } else {
                        openLinkInBrowser(finalMirror);
                    }
                });

                buttonsPanel.add(mirrorButton);
            }

            JPanel detailsPanel = new JPanel();
            detailsPanel.setLayout(new BorderLayout());
            detailsPanel.add(new JTextArea(detailsMessage), BorderLayout.CENTER);
            detailsPanel.add(buttonsPanel, BorderLayout.SOUTH);

            // Create a JOptionPane
            JOptionPane pane = new JOptionPane(detailsPanel, JOptionPane.PLAIN_MESSAGE);
            pane.setOptions(new Object[] {}); // Remove default buttons

            // Create a JDialog from JOptionPane
            JDialog dialog = pane.createDialog("Image Details");

            // Load your custom icon
            ImageIcon icon = new ImageIcon(getClass().getResource("icon.png")); // Replace with your icon's path
            dialog.setIconImage(icon.getImage());

            // Display the dialog
            dialog.setVisible(true);
        }

        private String getBaseURL(String mirrorLink) {
            int slashIndex = mirrorLink.indexOf("/", 8);
            if (slashIndex != -1) {
                return mirrorLink.substring(0, slashIndex);
            } else {
                return mirrorLink;
            }
        }

        private void handleLibgenMirrorButtonClick(String finalMirror) {
            System.out.println("Mirror Link Clicked: " + finalMirror);

            if (selectDownloadDirectory()) {
                showDownloadStartedAlert(imageDetails.getTitle());

                Thread downloadThread = new Thread(() -> {
                    Downloader.setDownloadDirectory(downloadDirectory);

                    if (downloadDirectory != null) {
                        if (finalMirror.contains("library.lol")) {
                            System.out.println("Downloading from library.lol mirror");
                            Downloader.downloadFromLibraryLolMirror(finalMirror);
                        } else {
                            System.out.println("Downloading from other mirror");
                            Downloader.downloadFromLibgenMirror(finalMirror);
                        }
                    } else {
                        System.out.println("Download canceled: No directory selected.");
                    }
                });

                downloadThread.start();
            }
        }

        private void openLinkInBrowser(String finalMirror) {
            try {
                Desktop.getDesktop().browse(new URI(finalMirror));
            } catch (IOException | URISyntaxException ex) {
                ex.printStackTrace();
            }
        }

        private void showDownloadStartedAlert(String bookTitle) {
            StringBuilder alertMessage = new StringBuilder("Download started for: " + bookTitle + "\n");

            // Create an "OK" button
            JButton okButton = new JButton("OK");
            okButton.addActionListener(e -> {
                // Close the dialog when button is clicked
                Window window = SwingUtilities.getWindowAncestor(okButton);
                if (window instanceof JDialog) {
                    JDialog dialog = (JDialog) window;
                    dialog.dispose();
                }
            });

            // Create a JOptionPane with PLAIN_MESSAGE type and custom OK button
            JOptionPane pane = new JOptionPane(alertMessage.toString(), JOptionPane.PLAIN_MESSAGE,
                    JOptionPane.DEFAULT_OPTION, null, new Object[] { okButton });

            // Create a JDialog from JOptionPane
            JDialog dialog = pane.createDialog("Download Started");

            // Load your custom icon
            ImageIcon icon = new ImageIcon(getClass().getResource("icon.png")); // Replace with your icon's path
            dialog.setIconImage(icon.getImage());

            // Display the dialog
            dialog.setVisible(true);
        }

        private boolean selectDownloadDirectory() {
            if (downloadDirectory == null) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select Download Directory");
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                // Create a custom dialog
                JDialog dialog = new JDialog();
                dialog.setTitle("Select Download Directory");
                try {
                    // Set custom icon for the dialog
                    dialog.setIconImage(ImageIO.read(
                            Thread.currentThread().getContextClassLoader().getResourceAsStream("icon.png")));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                dialog.add(fileChooser);
                dialog.pack();
                dialog.setLocationRelativeTo(null); // Center the dialog on screen

                // Show the dialog and get the user's selection
                int userSelection = fileChooser.showOpenDialog(dialog);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    downloadDirectory = fileChooser.getSelectedFile().toPath();
                    System.out.println("Download location set to: " + downloadDirectory);
                    Preferences prefs = Preferences.userRoot().node("org/serverboi/lgd");
                    prefs.put("downloadDirectory", downloadDirectory.toString());
                    return true;
                } else {
                    System.out.println("Download directory selection canceled.");
                    return false;
                }
            } else {
                return true;
            }
        }
    }

    private static class ImageDetails {
        private final String imageUrl;
        private final String title;
        private final String author;
        private final String publisher;
        private final String year;
        private final String lang;
        private final String size;
        private final List<String> mirrors;

        public ImageDetails(String imageUrl, String title, String author, String publisher, String year, String lang,
                String size, List<String> mirrors) {
            this.imageUrl = imageUrl;
            this.title = title;
            this.author = author;
            this.publisher = publisher;
            this.year = year;
            this.lang = lang;
            this.size = size;
            this.mirrors = mirrors;
        }
//
        public String getImageUrl() {
            return imageUrl;
        }

        public String getTitle() {
            return title;
        }

        public String getAuthor() {
            return author;
        }

        public String getPublisher() {
            return publisher;
        }

        public String getYear() {
            return year;
        }

        public String getLang() {
            return lang;
        }

        public String getSize() {
            return size;
        }

        public List<String> getMirrors() {
            return mirrors;
        }
    }
}
