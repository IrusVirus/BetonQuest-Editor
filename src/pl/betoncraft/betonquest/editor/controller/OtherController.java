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

package pl.betoncraft.betonquest.editor.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import pl.betoncraft.betonquest.editor.BetonQuestEditor;
import pl.betoncraft.betonquest.editor.custom.DraggableListCell;
import pl.betoncraft.betonquest.editor.model.Item;
import pl.betoncraft.betonquest.editor.model.JournalEntry;

/**
 * Controls "Other" tab.
 *
 * @author Jakub Sapalski
 */
public class OtherController {
	
	private static OtherController instance;

	@FXML private ListView<Item> itemsList;
	@FXML private ListView<JournalEntry> journalList;
	@FXML private TextArea entryText;
	
	public OtherController() {
		instance = this;
	}
	
	public static void setItems(ObservableList<Item> items) {
		instance.itemsList.setCellFactory(param -> new DraggableListCell<>());
		instance.itemsList.setItems(items);
	}
	
	public static void setJournal(ObservableList<JournalEntry> journal) {
		instance.journalList.setCellFactory(param -> new DraggableListCell<>());
		instance.journalList.setItems(journal);
	}
	
	@FXML private void addItem() {
		try {
			Item item = new Item(BetonQuestEditor.getInstance().getDisplayedPackage(), new String());
			if (item.edit()) {
				item.setIndex(itemsList.getItems().size());
				itemsList.getItems().add(item);
			}
		} catch (Exception e) {
			ExceptionController.display(e);
		}
	}
	
	@FXML private void renameItem() {
		try {
			Item item = itemsList.getSelectionModel().getSelectedItem();
			if (item != null) {
				item.edit();
			}
		} catch (Exception e) {
			ExceptionController.display(e);
		}
	}
	
	@FXML private void delItem() {
		try {
			Item item = itemsList.getSelectionModel().getSelectedItem();
			if (item != null) {
				itemsList.getItems().remove(item);
			}
			BetonQuestEditor.getInstance().refresh();
		} catch (Exception e) {
			ExceptionController.display(e);
		}
	}
	
	@FXML private void addEntry() {
		try {
			JournalEntry entry = new JournalEntry(BetonQuestEditor.getInstance().getDisplayedPackage(), new String());
			if (entry.edit()) {
				entry.setIndex(journalList.getItems().size());
				journalList.getItems().add(entry);
			}
		} catch (Exception e) {
			ExceptionController.display(e);
		}
	}
	
	@FXML private void editEntry() {
		try {
			JournalEntry entry = journalList.getSelectionModel().getSelectedItem();
			if (entry != null) {
				entry.edit();
			}
		} catch (Exception e) {
			ExceptionController.display(e);
		}
	}
	
	@FXML private void delEntry() {
		try {
			JournalEntry entry = journalList.getSelectionModel().getSelectedItem();
			if (entry != null) {
				journalList.getItems().remove(entry);
			}
		} catch (Exception e) {
			ExceptionController.display(e);
		}
	}
	
	@FXML private void selectEntry() {
		try {
			JournalEntry entry = journalList.getSelectionModel().getSelectedItem();
			if (entry != null) {
				entryText.setText(entry.getText().getLang(BetonQuestEditor.getInstance().getDisplayedPackage().getDefLang()).get());
			}
		} catch (Exception e) {
			ExceptionController.display(e);
		}
	}
	
}
