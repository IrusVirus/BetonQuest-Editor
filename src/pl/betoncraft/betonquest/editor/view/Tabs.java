/**
 * BetonQuest Editor - advanced quest creating tool for BetonQuest
 * Copyright (C) 2015  Jakub "Co0sh" Sapalski
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
package pl.betoncraft.betonquest.editor.view;

import javafx.geometry.Side;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import pl.betoncraft.betonquest.editor.view.tab.ConditionTab;
import pl.betoncraft.betonquest.editor.view.tab.ConversationTab;
import pl.betoncraft.betonquest.editor.view.tab.EventTab;
import pl.betoncraft.betonquest.editor.view.tab.ItemTab;
import pl.betoncraft.betonquest.editor.view.tab.JournalTab;
import pl.betoncraft.betonquest.editor.view.tab.MainTab;
import pl.betoncraft.betonquest.editor.view.tab.ObjectiveTab;
import pl.betoncraft.betonquest.editor.view.tab.QuestTab;
import pl.betoncraft.betonquest.editor.view.tab.TranslationTab;

/**
 * Main tabs in the application.
 *
 * @author Jakub Sapalski
 */
public class Tabs extends TabPane {
	
	public final MainTab mainTab;
	public final ConversationTab conversations;
	public final EventTab events;
	public final ConditionTab conditions;
	public final ObjectiveTab objectives;
	public final JournalTab journal;
	public final ItemTab items;
	public final QuestTab quests;
	public final TranslationTab translations;

	public Tabs(Root root) {
		mainTab = new MainTab(this);
		conversations = new ConversationTab(this);
		events = new EventTab(this);
		conditions = new ConditionTab(this);
		objectives = new ObjectiveTab(this);
		journal = new JournalTab(this);
		items = new ItemTab(this);
		quests = new QuestTab(this);
		translations = new TranslationTab(this);
		setSide(Side.RIGHT);
		getTabs().addAll(mainTab, conversations, events, conditions, objectives, journal, items, translations);
		for (Tab tab : getTabs()) {
			tab.setClosable(false);
		}
		//setDisable(true); // this will disable interactions with empty view; the user must load a package first
	}
}
