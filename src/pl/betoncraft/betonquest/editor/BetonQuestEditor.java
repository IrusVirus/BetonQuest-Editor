/**
 * BetonQuest Editor - advanced quest creating tool for BetonQuest
 * Copyright (C) 2016  Jakub "Co0sh" Sapalski
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package pl.betoncraft.betonquest.editor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.zip.ZipFile;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pl.betoncraft.betonquest.editor.controller.ConversationController;
import pl.betoncraft.betonquest.editor.controller.EcoController;
import pl.betoncraft.betonquest.editor.controller.ExceptionController;
import pl.betoncraft.betonquest.editor.controller.MainController;
import pl.betoncraft.betonquest.editor.controller.OtherController;
import pl.betoncraft.betonquest.editor.controller.TabsController;
import pl.betoncraft.betonquest.editor.model.Condition;
import pl.betoncraft.betonquest.editor.model.Conversation;
import pl.betoncraft.betonquest.editor.model.Event;
import pl.betoncraft.betonquest.editor.model.Item;
import pl.betoncraft.betonquest.editor.model.JournalEntry;
import pl.betoncraft.betonquest.editor.model.Objective;
import pl.betoncraft.betonquest.editor.model.PointCategory;
import pl.betoncraft.betonquest.editor.model.QuestPackage;
import pl.betoncraft.betonquest.editor.model.Tag;

/**
 * Main class for the application.
 * 
 * @author Jakub Sapalski
 */
public class BetonQuestEditor extends Application {

	private static BetonQuestEditor instance;
	private Stage stage;
	private ResourceBundle language;
	
	private HashMap<String, QuestPackage> loadedPackages = new HashMap<>();
	private QuestPackage currentPackage;
	private static File autoLoadPackage;
	private static File autoSavePackage;
	private static int autoSelect = -1;

	/**
	 * Parses the arguments and starts the application.
	 */
	public static void main(String[] args) {
		if (args.length > 2) {
			autoLoadPackage = new File(args[0]);
			if (!autoLoadPackage.exists() || !autoLoadPackage.getName().endsWith(".zip")) {
				autoLoadPackage = null;
			}
			autoSavePackage = new File(args[1]);
			if (!autoSavePackage.getName().endsWith(".zip")) {
				autoSavePackage = null;
			}
			autoSelect = Integer.parseInt(args[2]);
		}
		launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) {
		instance = this;
		stage = primaryStage;
		try {
			URL location = getClass().getResource("view/Root.fxml");
			language = ResourceBundle.getBundle("pl.betoncraft.betonquest.editor.resource.lang.lang");
			FXMLLoader fxmlLoader = new FXMLLoader(location, language);
			BorderPane root = (BorderPane) fxmlLoader.load();
			TabsController.setDisabled(true);
			Scene scene = new Scene(root, 1280, 720);
			scene.getStylesheets().add(getClass().getResource("resource/style.css").toExternalForm());
			stage.setScene(scene);
			stage.setTitle(language.getString("betonquest-editor"));
			stage.getIcons().add(new Image(getClass().getResourceAsStream("resource/icon.png")));
			stage.setMinHeight(600);
			stage.setMinWidth(800);
			stage.setMaximized(true);
			stage.show();
			// load package for debugging
			if (autoLoadPackage != null) {
				QuestPackage pack = QuestPackage.loadFromZip(new ZipFile(autoLoadPackage));
				display(pack);
			}
			if (autoSelect > 0) {
				TabsController.selectTab(autoSelect);
			}
		} catch (Exception e) {
			ExceptionController.display(e);
		}
	}
	
	/**
	 * @return the instance of the editor
	 */
	public static BetonQuestEditor getInstance() {
		return instance;
	}
	
	/**
	 * @return the primary stage of this application
	 */
	public Stage getPrimaryStage() {
		return stage;
	}
	
	/**
	 * @return ResourceBundle containing language strings
	 */
	public ResourceBundle getLanguage() {
		return language;
	}
	
	/**
	 * @return the list of loaded packages
	 */
	public HashMap<String, QuestPackage> getPackages() {
		return loadedPackages;
	}

	/**
	 * @return the package currently displayed in the view
	 */
	public QuestPackage getDisplayedPackage() {
		return currentPackage;
	}
	
	/**
	 * Displays a package in the view.
	 */
	public void display(QuestPackage pack) {
		currentPackage = pack;
		currentPackage.sort();
		MainController.setNpcBindings(pack.getNpcBindings());
		MainController.setGlobVariables(pack.getVariables());
		MainController.setStaticEvents(pack.getStaticEvents());
		MainController.setGlobalLocations(pack.getLocations());
		MainController.setQuestCancelers(pack.getCancelers());
		MainController.setMainPageLines(pack.getMainPage());
		ConversationController.setConversations(pack.getConversations());
		EcoController.setConditions(pack.getConditions());
		EcoController.setEvents(pack.getEvents());
		EcoController.setObjectives(pack.getObjectives());
		OtherController.setItems(pack.getItems());
		OtherController.setJournal(pack.getJournal());
		TabsController.setDisabled(false);
	}
	
	/**
	 * Clears the view. Should be called before displaying different package,
	 * but not before refreshing. That way current conversation, option etc. are
	 * kept.
	 */
	public void clearView() {
		currentPackage = null;
		// TODO clear view
		TabsController.setDisabled(true);
	}
	
	/**
	 * Refreshes the currently displayed package
	 */
	public void refresh() {
		display(currentPackage);
	}
	
	/**
	 * @return all conditions from loaded packages, the ones from current package first
	 */
	public ObservableList<Condition> getAllConditions() {
		ObservableList<Condition> list = FXCollections.observableArrayList();
		list.addAll(currentPackage.getConditions());
		loadedPackages.values().forEach(pack -> {
			if (!pack.equals(currentPackage)) {
				list.addAll(pack.getConditions());
			}
		});
		return list;
	}
	
	/**
	 * @return all events from loaded packages, the ones from current package first
	 */
	public ObservableList<Event> getAllEvents() {
		ObservableList<Event> list = FXCollections.observableArrayList();
		list.addAll(currentPackage.getEvents());
		loadedPackages.values().forEach(pack -> {
			if (!pack.equals(currentPackage)) {
				list.addAll(pack.getEvents());
			}
		});
		return list;
	}
	
	/**
	 * @return all objectives from loaded packages, the ones from current package first
	 */
	public ObservableList<Objective> getAllObjectives() {
		ObservableList<Objective> list = FXCollections.observableArrayList();
		list.addAll(currentPackage.getObjectives());
		loadedPackages.values().forEach(pack -> {
			if (!pack.equals(currentPackage)) {
				list.addAll(pack.getObjectives());
			}
		});
		return list;
	}
	
	/**
	 * @return all conversations from loaded packages, the ones from current package first
	 */
	public ObservableList<Conversation> getAllConversations() {
		ObservableList<Conversation> list = FXCollections.observableArrayList();
		list.addAll(currentPackage.getConversations());
		loadedPackages.values().forEach(pack -> {
			if (!pack.equals(currentPackage)) {
				list.addAll(pack.getConversations());
			}
		});
		return list;
	}
	
	/**
	 * @return all items from loaded packages, the ones from current package first
	 */
	public ObservableList<Item> getAllItems() {
		ObservableList<Item> list = FXCollections.observableArrayList();
		list.addAll(currentPackage.getItems());
		loadedPackages.values().forEach(pack -> {
			if (!pack.equals(currentPackage)) {
				list.addAll(pack.getItems());
			}
		});
		return list;
	}
	
	public ObservableList<JournalEntry> getAllEntries() {
		ObservableList<JournalEntry> list = FXCollections.observableArrayList();
		list.addAll(currentPackage.getJournal());
		loadedPackages.values().forEach(pack -> {
			if (!pack.equals(currentPackage)) {
				list.addAll(pack.getJournal());
			}
		});
		return list;
	}

	/**
	 * @return all tags from loaded packages, the ones from current package first
	 */
	public ObservableList<Tag> getAllTags() {
		return FXCollections.observableArrayList(); // TODO extract tags from events, conditions etc.
	}

	/**
	 * @return all point categories from loaded packages, the ones from current package first
	 */
	public ObservableList<PointCategory> getAllPoints() {
		return FXCollections.observableArrayList(); // TODO extract points from events, conditions etc.
	}
	
	/**
	 * Shows an error pop-up window with specified translated message.
	 * 
	 * @param message ID of message
	 */
	public static void showError(String message) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setHeaderText(BetonQuestEditor.getInstance().getLanguage().getString(message));
		alert.getDialogPane().getStylesheets().add(instance.getClass().getResource("resource/style.css").toExternalForm());
		alert.showAndWait();
	}
	
	public static boolean confirm(String message) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setHeaderText(BetonQuestEditor.getInstance().getLanguage().getString(message));
		alert.getDialogPane().getStylesheets().add(instance.getClass().getResource("resource/style.css").toExternalForm());
		Optional<ButtonType> action = alert.showAndWait();
		if (action.isPresent() && action.get() == ButtonType.OK) {
			return true;
		}
		return false;
	}

	public static Object createWindow(String controller, String title, int width, int height) {
		try {
			Stage window = new Stage();
			URL location = BetonQuestEditor.class.getResource(controller);
			ResourceBundle resources = ResourceBundle.getBundle("pl.betoncraft.betonquest.editor.resource.lang.lang");
			FXMLLoader fxmlLoader = new FXMLLoader(location, resources);
			Parent root = (Parent) fxmlLoader.load();
			Scene scene = new Scene(root);
			scene.getStylesheets().add(BetonQuestEditor.class.getResource("resource/style.css").toExternalForm());
			window.setScene(scene);
			window.setTitle(resources.getString(title));
			window.getIcons().add(new Image(BetonQuestEditor.class.getResourceAsStream("resource/icon.png")));
			window.setHeight(height);
			window.setWidth(width);
			window.setResizable(false);
			window.initModality(Modality.WINDOW_MODAL);
			window.initOwner(instance.stage);
			return fxmlLoader.getController();
		} catch (IOException e) {
			ExceptionController.display(e);
			return null;
		}
	}

	@Override
	public void stop() throws Exception {
		if (autoSavePackage != null) try {
			autoSavePackage.createNewFile();
			currentPackage.saveToZip(autoSavePackage);
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.stop();
	}
}
